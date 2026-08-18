package com.harriol.blockwiz.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * blockwiz.json 读写：临时文件 + 原子替换，保存失败不覆盖上一次有效配置。
 * 路径由调用方注入（本类不依赖 Minecraft 类，便于测试）。
 *
 * @author Harriol
 */
public final class ConfigIO {

    public static final String CONFIG_FILE_NAME = "blockwiz.json";
    private static final String TEMP_SUFFIX = ".tmp";

    /** 加载结果：config 为空表示文件缺失或内容非法；problems 给出原因（i18n 键）。 */
    public record LoadResult(Optional<ConfigData> config, List<String> problems) {
    }

    private ConfigIO() {
    }

    public static LoadResult load(Path file) {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) {
            return new LoadResult(Optional.empty(), List.of());
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            ConfigData config = ConfigCodec.fromJson(json);
            List<String> problems = ConfigValidator.validate(config);
            if (problems.isEmpty()) {
                return new LoadResult(Optional.of(config), List.of());
            }
            return new LoadResult(Optional.empty(), problems);
        } catch (Exception e) {
            return new LoadResult(Optional.empty(), List.of("config.error.load.failed"));
        }
    }

    /** 原子保存；返回是否成功。失败时保留原文件。 */
    public static boolean save(Path file, ConfigData config) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(config, "config");
        Path parent = file.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                return false;
            }
        }
        Path temp = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            Files.writeString(temp, ConfigCodec.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                /* 清理失败不影响结果。 */
            }
            return false;
        }
    }
}
