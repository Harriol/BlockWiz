package com.harriol.blockwiz.common.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 配置字段校验器。返回 i18n 错误键（不含具体文案），由调用方翻译展示。
 * 校验规则来源：PRD V1.1 §5.1 配置字段约束。
 *
 * @author Harriol
 */
public final class ConfigValidator {

    public static final int MIN_TEMPERATURE = 0;
    public static final int MAX_TEMPERATURE = 2;
    public static final int MIN_REQUEST_TIMEOUT_MS = 1_000;
    public static final int MAX_REQUEST_TIMEOUT_MS = 120_000;
    public static final int MIN_BLOCKS_PER_TICK = 1;
    public static final int MAX_BLOCKS_PER_TICK = 256;
    public static final int MIN_SCAN_RADIUS = 16;
    public static final int MAX_SCAN_RADIUS = 128;
    public static final int MIN_CONFIRM_TIMEOUT_SEC = 5;
    public static final int MAX_CONFIRM_TIMEOUT_SEC = 600;
    public static final int MAX_RETRIES = 10;

    private static final Set<String> SUPPORTED_LANGUAGES =
            Set.of(ConfigData.DEFAULT_LANGUAGE_ZH_CN, ConfigData.DEFAULT_LANGUAGE_EN_US);
    private static final Set<String> LOCAL_HTTP_HOSTS =
            Set.of("127.0.0.1", "localhost", "::1", "[::1]");

    private ConfigValidator() {
    }

    /** 返回错误键列表；空列表表示配置合法。 */
    public static List<String> validate(ConfigData config) {
        List<String> problems = new ArrayList<>();

        if (isBlank(config.getApiBaseUrl())) {
            problems.add("config.error.apiBaseUrl.empty");
        } else if (!isAllowedBaseUrl(config.getApiBaseUrl())) {
            problems.add("config.error.apiBaseUrl.invalid");
        }

        String path = config.getChatCompletionsPath();
        if (isBlank(path) || !path.startsWith("/")) {
            problems.add("config.error.chatCompletionsPath.invalid");
        }
        if (isBlank(config.getApiKeyHeaderName())) {
            problems.add("config.error.apiKeyHeaderName.empty");
        }
        if (isBlank(config.getModel())) {
            problems.add("config.error.model.empty");
        }
        if (config.getTemperature() < MIN_TEMPERATURE || config.getTemperature() > MAX_TEMPERATURE) {
            problems.add("config.error.temperature.range");
        }
        if (config.getRequestTimeoutMs() < MIN_REQUEST_TIMEOUT_MS
                || config.getRequestTimeoutMs() > MAX_REQUEST_TIMEOUT_MS) {
            problems.add("config.error.requestTimeoutMs.range");
        }
        if (config.getMaxTokens() != null && config.getMaxTokens() <= 0) {
            problems.add("config.error.maxTokens.positive");
        }
        if (!SUPPORTED_LANGUAGES.contains(config.getLanguage())) {
            problems.add("config.error.language.unsupported");
        }
        if (config.getBlocksPerTick() < MIN_BLOCKS_PER_TICK
                || config.getBlocksPerTick() > MAX_BLOCKS_PER_TICK) {
            problems.add("config.error.blocksPerTick.range");
        }
        if (config.getScanRadius() < MIN_SCAN_RADIUS || config.getScanRadius() > MAX_SCAN_RADIUS) {
            problems.add("config.error.scanRadius.range");
        }
        if (config.getConfirmTimeoutSec() < MIN_CONFIRM_TIMEOUT_SEC
                || config.getConfirmTimeoutSec() > MAX_CONFIRM_TIMEOUT_SEC) {
            problems.add("config.error.confirmTimeoutSec.range");
        }
        if (config.getMaxRetries() < 0 || config.getMaxRetries() > MAX_RETRIES) {
            problems.add("config.error.maxRetries.range");
        }

        Map<String, String> extraHeaders = config.getExtraHeaders();
        if (extraHeaders != null && extraHeaders.keySet().stream().anyMatch(ConfigValidator::isBlank)) {
            problems.add("config.error.extraHeaders.emptyKey");
        }
        return problems;
    }

    public static boolean isValid(ConfigData config) {
        return validate(config).isEmpty();
    }

    /**
     * Base URL 白名单：https 任意主机；http 仅限本机（127.0.0.1 / localhost / ::1）。
     */
    private static boolean isAllowedBaseUrl(String raw) {
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ("https".equals(scheme)) {
                return uri.getHost() != null;
            }
            if ("http".equals(scheme)) {
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
                return LOCAL_HTTP_HOSTS.contains(host);
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
