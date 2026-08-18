package com.harriol.blockwiz.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BlockWiz 配置数据模型，字段与 PRD V1.1 §5.1 一一对应。
 * 本类不依赖 Minecraft 类，可独立单元测试。
 *
 * @author Harriol
 */
public final class ConfigData {

    public static final String DEFAULT_LANGUAGE_ZH_CN = "zh_cn";
    public static final String DEFAULT_LANGUAGE_EN_US = "en_us";

    private String apiBaseUrl = "";
    private String chatCompletionsPath = "/chat/completions";
    private String apiKey = "";
    private String apiKeyHeaderName = "Authorization";
    private String apiKeyPrefix = "Bearer ";
    private String model = "";
    private double temperature = 0.2;
    private int requestTimeoutMs = 30_000;
    private Integer maxTokens = null;
    private Map<String, String> extraHeaders = new LinkedHashMap<>();
    private String language = DEFAULT_LANGUAGE_ZH_CN;
    private int blocksPerTick = 64;
    private int scanRadius = 64;
    private int confirmTimeoutSec = 60;
    private boolean playerPositionProtection = true;
    private int maxRetries = 3;

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getChatCompletionsPath() { return chatCompletionsPath; }
    public void setChatCompletionsPath(String chatCompletionsPath) { this.chatCompletionsPath = chatCompletionsPath; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiKeyHeaderName() { return apiKeyHeaderName; }
    public void setApiKeyHeaderName(String apiKeyHeaderName) { this.apiKeyHeaderName = apiKeyHeaderName; }

    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Map<String, String> getExtraHeaders() { return extraHeaders; }
    public void setExtraHeaders(Map<String, String> extraHeaders) { this.extraHeaders = extraHeaders; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getBlocksPerTick() { return blocksPerTick; }
    public void setBlocksPerTick(int blocksPerTick) { this.blocksPerTick = blocksPerTick; }

    public int getScanRadius() { return scanRadius; }
    public void setScanRadius(int scanRadius) { this.scanRadius = scanRadius; }

    public int getConfirmTimeoutSec() { return confirmTimeoutSec; }
    public void setConfirmTimeoutSec(int confirmTimeoutSec) { this.confirmTimeoutSec = confirmTimeoutSec; }

    public boolean isPlayerPositionProtection() { return playerPositionProtection; }
    public void setPlayerPositionProtection(boolean playerPositionProtection) { this.playerPositionProtection = playerPositionProtection; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    /**
     * 便于日志输出；apiKey 一律脱敏展示。
     *
     * @return 配置摘要
     */
    @Override
    public String toString() {
        return "ConfigData{" +
                "apiBaseUrl='" + apiBaseUrl + '\'' +
                ", chatCompletionsPath='" + chatCompletionsPath + '\'' +
                ", apiKey='" + (apiKey == null || apiKey.isEmpty() ? "" : "***") + '\'' +
                ", apiKeyHeaderName='" + apiKeyHeaderName + '\'' +
                ", model='" + model + '\'' +
                ", temperature=" + temperature +
                ", requestTimeoutMs=" + requestTimeoutMs +
                ", language='" + language + '\'' +
                ", blocksPerTick=" + blocksPerTick +
                ", scanRadius=" + scanRadius +
                ", confirmTimeoutSec=" + confirmTimeoutSec +
                ", playerPositionProtection=" + playerPositionProtection +
                ", maxRetries=" + maxRetries +
                '}';
    }
}
