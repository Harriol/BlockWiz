package com.harriol.blockwiz.common.config;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * API 地址策略（纯逻辑，可单测）：
 * <ul>
 *   <li>Base URL 白名单：https 任意主机；http 仅限本机（PRD §5.1）。</li>
 *   <li>本地免鉴权服务判定：http://127.0.0.1 或 http://localhost（Ollama 等），无需 API Key。</li>
 * </ul>
 *
 * @author Harriol
 */
public final class ApiUrlPolicy {

    private static final Set<String> LOCAL_HTTP_HOSTS =
            Set.of("127.0.0.1", "localhost", "::1", "[::1]");

    private ApiUrlPolicy() {
    }

    /**
     * Base URL 是否满足 PRD 白名单：https 任意主机；http 仅限本机。
     *
     * @param raw Base URL
     * @return 合法返回 true
     */
    public static boolean isAllowedBaseUrl(String raw) {
        try {
            URI uri = URI.create(raw == null ? "" : raw.trim());
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

    /**
     * 是否为本地免鉴权服务（http + 本机 host）。
     * 此类服务（如 Ollama）允许空 API Key；外部大模型必须配置 Key。
     *
     * @param raw Base URL
     * @return 本地服务返回 true
     */
    public static boolean isLocalService(String raw) {
        try {
            URI uri = URI.create(raw == null ? "" : raw.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return LOCAL_HTTP_HOSTS.contains(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
