package com.harriol.blockwiz.common.config;

/**
 * API 服务商预设模板。预设不是固定依赖：玩家可覆盖全部字段。
 * baseUrl 为空表示该预设需要玩家自行填写（如 Claude 需自备兼容网关）。
 *
 * @author Harriol
 */
public record PresetTemplate(
        String id,
        String displayNameKey,
        String baseUrl,
        String chatCompletionsPath,
        String apiKeyHeaderName,
        String apiKeyPrefix,
        String model,
        String noteKey) {

    public void applyTo(ConfigData config) {
        config.setApiBaseUrl(baseUrl);
        config.setChatCompletionsPath(chatCompletionsPath);
        config.setApiKeyHeaderName(apiKeyHeaderName);
        config.setApiKeyPrefix(apiKeyPrefix);
        config.setModel(model);
    }
}
