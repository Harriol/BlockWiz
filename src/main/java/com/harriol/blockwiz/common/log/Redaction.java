package com.harriol.blockwiz.common.log;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 日志与消息脱敏：按已知密钥值全文替换、Bearer/Basic/Token 模式替换、敏感请求头值掩码。
 *
 * @author Harriol
 */
public final class Redaction {

    public static final String MASK = "***";
    private static final Pattern AUTH_TOKEN_PATTERN =
            Pattern.compile("(?i)(Bearer|Basic|Token)\\s+\\S+");

    private Redaction() {
    }

    /** 按已知密钥值（如 apiKey）在文本中逐项替换为掩码。 */
    public static String redact(String text, Collection<String> secrets) {
        String result = text == null ? "" : text;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                result = result.replace(secret, MASK);
            }
        }
        return result;
    }

    /** 将 "Bearer xxx / Basic xxx / Token xxx" 中的凭证替换为掩码。 */
    public static String redactAuthorizationTokens(String text) {
        return AUTH_TOKEN_PATTERN.matcher(text == null ? "" : text).replaceAll("$1 " + MASK);
    }

    /** 对疑似敏感的请求头值统一掩码（保留头名）。 */
    public static Map<String, String> redactHeaders(Map<String, String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) -> result.put(name, isSensitiveHeader(name) ? MASK : value));
        }
        return result;
    }

    private static boolean isSensitiveHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("authorization")
                || lower.contains("api-key")
                || lower.contains("token")
                || lower.contains("secret");
    }
}
