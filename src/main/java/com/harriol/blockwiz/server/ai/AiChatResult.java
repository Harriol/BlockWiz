package com.harriol.blockwiz.server.ai;

/**
 * OpenAI 兼容聊天请求结果（通用：连接测试与候选边界请求共用）。
 *
 * @author Harriol
 */
public record AiChatResult(boolean ok, String content, String categoryKey, int statusCode, long durationMs) {

    public static AiChatResult success(String content, int statusCode, long durationMs) {
        return new AiChatResult(true, content, null, statusCode, durationMs);
    }

    public static AiChatResult failure(String categoryKey, int statusCode, long durationMs) {
        return new AiChatResult(false, null, categoryKey, statusCode, durationMs);
    }
}
