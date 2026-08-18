package com.harriol.blockwiz.common.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.harriol.blockwiz.common.config.ConfigData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量 i18n：从 classpath 资源 assets/blockwiz/lang/{language}.json 加载文案。
 * 缺键回退到 zh_cn，再回退到键本身。支持 {0}/{1} 占位符。
 *
 * @author Harriol
 */
public final class I18n {

    private static final String RESOURCE_PREFIX = "assets/blockwiz/lang/";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Map<String, Map<String, String>> LANGUAGES = new ConcurrentHashMap<>();
    private static volatile String currentLanguage = ConfigData.DEFAULT_LANGUAGE_ZH_CN;

    static {
        loadLanguage(ConfigData.DEFAULT_LANGUAGE_ZH_CN);
        loadLanguage(ConfigData.DEFAULT_LANGUAGE_EN_US);
    }

    private I18n() {
    }

    /** 切换当前语言；不支持的语言回退到 zh_cn。 */
    public static void setLanguage(String language) {
        if (language != null && LANGUAGES.containsKey(language)) {
            currentLanguage = language;
        } else {
            currentLanguage = ConfigData.DEFAULT_LANGUAGE_ZH_CN;
        }
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static String get(String key) {
        return get(key, currentLanguage);
    }

    public static String get(String key, String language) {
        Objects.requireNonNull(key, "key");
        String value = LANGUAGES.getOrDefault(language, Map.of()).get(key);
        if (value == null) {
            value = LANGUAGES.getOrDefault(ConfigData.DEFAULT_LANGUAGE_ZH_CN, Map.of()).get(key);
        }
        return value != null ? value : key;
    }

    /** 格式化：将 {0}、{1}... 替换为参数。 */
    public static String format(String key, Object... args) {
        String template = get(key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }

    /** 检查某语言文件是否加载成功（测试用）。 */
    public static boolean isLanguageLoaded(String language) {
        return LANGUAGES.containsKey(language);
    }

    private static void loadLanguage(String language) {
        try (InputStream stream = I18n.class.getClassLoader()
                .getResourceAsStream(RESOURCE_PREFIX + language + ".json")) {
            if (stream == null) {
                return;
            }
            Map<String, String> entries = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), MAP_TYPE);
            if (entries != null) {
                LANGUAGES.put(language, entries);
            }
        } catch (IOException | RuntimeException e) {
            /** 语言文件加载失败不影响游戏启动；缺键按回退规则展示。 */
        }
    }
}
