package com.harriol.blockwiz.server.ai;

/**
 * 连接测试结果。categoryKey 为 i18n 错误分类键（空表示成功）。
 *
 * @author Harriol
 */
public record ConnectionTestResult(boolean ok, int statusCode, long durationMs, String categoryKey, String detail) {

    public static ConnectionTestResult success(int statusCode, long durationMs) {
        return new ConnectionTestResult(true, statusCode, durationMs, "", "");
    }

    public static ConnectionTestResult failure(String categoryKey, int statusCode, long durationMs, String detail) {
        return new ConnectionTestResult(false, statusCode, durationMs, categoryKey, detail);
    }
}
