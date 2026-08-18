package com.harriol.blockwiz;

import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.config.ConfigHolder;
import com.harriol.blockwiz.common.config.ConfigIO;
import com.harriol.blockwiz.server.command.BlockWizCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * BlockWiz 模组主入口（服务端侧）。
 *
 * @author Harriol
 */
public class BlockWiz implements ModInitializer {
	public static final String MOD_ID = "blockwiz";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		loadConfig();
		BlockWizCommand.register();
		LOGGER.info("BlockWiz 已加载，AI 请求状态：{}", ConfigHolder.isValid() ? "已启用" : "未配置（禁用）");
	}

	/**
	 * 加载 .minecraft/config/blockwiz.json。
	 * 缺失或非法配置不阻塞游戏启动，仅禁用 AI 请求。
	 */
	private void loadConfig() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ConfigIO.CONFIG_FILE_NAME);
		ConfigIO.LoadResult result = ConfigIO.load(configFile);
		if (result.config().isPresent()) {
			ConfigHolder.update(result.config().get());
			LOGGER.info("配置已加载：{}", configFile);
			return;
		}
		ConfigHolder.update(new ConfigData());
		if (result.problems().isEmpty()) {
			LOGGER.info("配置文件不存在，使用默认配置；请在 Mod Menu 中配置 API 后使用");
		} else {
			LOGGER.warn("配置不合法，AI 请求已禁用：{}", result.problems());
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
