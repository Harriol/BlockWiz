package com.harriol.blockwiz.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BlockWiz 模组客户端入口。
 *
 * @author Harriol
 */
public class BlockWizClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("blockwiz-client");

	@Override
	public void onInitializeClient() {
		/* 配置页由 Mod Menu entrypoint（BlockWizModMenu）提供。 */
		LOGGER.info("BlockWiz 客户端已初始化");
	}
}
