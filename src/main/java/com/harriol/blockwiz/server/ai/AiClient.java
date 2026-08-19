package com.harriol.blockwiz.server.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.i18n.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容 /chat/completions 异步客户端。
 * 本类只做请求与响应分类，不涉及世界修改；日志不记录 Key 与请求头。
 *
 * @author Harriol
 */
public final class AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("blockwiz/AiClient");
    private static final String CONTENT_TYPE = "application/json";
    private static final String USER_CONTENT = "ping";
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MIN = 500;
    private static final int HTTP_THREAD_COUNT = 2;
    private static final int HTTP_CONNECT_TIMEOUT_SECONDS = 10;
    private static final long RESULT_WAIT_MARGIN_MS = 5_000L;

    private final HttpClient httpClient;

    public AiClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_CONNECT_TIMEOUT_SECONDS))
                .executor(Executors.newFixedThreadPool(HTTP_THREAD_COUNT, runnable -> {
                    Thread thread = new Thread(runnable, "blockwiz-http");
                    thread.setDaemon(true);
                    return thread;
                }))
                .build());
    }

    public AiClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public CompletableFuture<ConnectionTestResult> testConnectionAsync(ConfigData config) {
        Objects.requireNonNull(config, "config");
        HttpRequest request = buildRequest(config, buildTestBody(config));
        long startNanos = System.nanoTime();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                    ConnectionTestResult result = error != null
                            ? classifyFailure(error, durationMs)
                            : classifyResponse(response, durationMs);
                    LOGGER.info("Connection test finished: ok={} status={} durationMs={} category={}",
                            result.ok(), result.statusCode(), result.durationMs(), result.categoryKey());
                    return result;
                });
    }

    /**
     * 通用聊天请求（候选边界等 AI 调用）。响应中的 content 原样返回，由调用方解析。
     *
     * @param config   配置快照
     * @param messages OpenAI 兼容消息列表（role/content）
     * @return 未来结果
     */
    public CompletableFuture<AiChatResult> chatAsync(ConfigData config, List<JsonObject> messages) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(messages, "messages");
        HttpRequest request = buildRequest(config, buildMessagesBody(config, messages));
        long startNanos = System.nanoTime();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                    AiChatResult result = error != null
                            ? classifyChatFailure(error, durationMs)
                            : classifyChatResponse(response, durationMs);
                    LOGGER.info("Chat request finished: ok={} status={} durationMs={} category={}",
                            result.ok(), result.statusCode(), result.durationMs(), result.categoryKey());
                    return result;
                });
    }

    /** 阻塞等待测试结果（测试与单元测试用）；超时由 requestTimeoutMs 决定。 */
    public ConnectionTestResult testConnectionSync(ConfigData config) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        return testConnectionAsync(config).get(config.getRequestTimeoutMs() + RESULT_WAIT_MARGIN_MS, TimeUnit.MILLISECONDS);
    }

    /** 拼接 endpoint：处理 Base URL 尾部斜杠。 */
    static String buildEndpoint(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String suffix = path == null ? "" : path.trim();
        if (base.endsWith("/") && suffix.startsWith("/")) {
            return base + suffix.substring(1);
        }
        return base + suffix;
    }

    private HttpRequest buildRequest(ConfigData config, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint(config.getApiBaseUrl(), config.getChatCompletionsPath())))
                .timeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                .header("Content-Type", CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        for (Map.Entry<String, String> entry : config.getExtraHeaders().entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank()
                    && entry.getValue() != null
                    && !"content-type".equalsIgnoreCase(entry.getKey())) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header(config.getApiKeyHeaderName(), config.getApiKeyPrefix() + config.getApiKey());
        }
        return builder.build();
    }

    private static String buildTestBody(ConfigData config) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("temperature", config.getTemperature());
        if (config.getMaxTokens() != null) {
            body.addProperty("max_tokens", config.getMaxTokens());
        }
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", USER_CONTENT);
        messages.add(userMessage);
        body.add("messages", messages);
        return body.toString();
    }

    private static String buildMessagesBody(ConfigData config, List<JsonObject> messages) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.getModel());
        body.addProperty("temperature", config.getTemperature());
        if (config.getMaxTokens() != null) {
            body.addProperty("max_tokens", config.getMaxTokens());
        }
        JsonArray messageArray = new JsonArray();
        for (JsonObject message : messages) {
            messageArray.add(message);
        }
        body.add("messages", messageArray);
        return body.toString();
    }

    private ConnectionTestResult classifyResponse(HttpResponse<String> response, long durationMs) {
        int status = response.statusCode();
        if (status >= HTTP_OK_MIN && status <= HTTP_OK_MAX) {
            if (isValidChatResponse(response.body())) {
                return ConnectionTestResult.success(status, durationMs);
            }
            return ConnectionTestResult.failure(Keys.TEST_CATEGORY_FORMAT, status, durationMs, "choices[0].message.content 缺失");
        }
        return ConnectionTestResult.failure(categoryForStatus(status), status, durationMs, "HTTP " + status);
    }

    private AiChatResult classifyChatResponse(HttpResponse<String> response, long durationMs) {
        int status = response.statusCode();
        if (status >= HTTP_OK_MIN && status <= HTTP_OK_MAX) {
            String content = extractContent(response.body());
            if (content != null) {
                return AiChatResult.success(content, status, durationMs);
            }
            return AiChatResult.failure(Keys.TEST_CATEGORY_FORMAT, status, durationMs);
        }
        return AiChatResult.failure(categoryForStatus(status), status, durationMs);
    }

    private static String categoryForStatus(int status) {
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            return Keys.TEST_CATEGORY_AUTH;
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return Keys.TEST_CATEGORY_RATE_LIMIT;
        }
        if (status >= HTTP_SERVER_ERROR_MIN) {
            return Keys.TEST_CATEGORY_SERVER;
        }
        return Keys.TEST_CATEGORY_HTTP;
    }

    private ConnectionTestResult classifyFailure(Throwable error, long durationMs) {
        Throwable cause = unwrap(error);
        String category = Keys.TEST_CATEGORY_NETWORK;
        if (cause instanceof HttpTimeoutException) {
            category = Keys.TEST_CATEGORY_TIMEOUT;
        } else if (cause instanceof IOException) {
            category = Keys.TEST_CATEGORY_NETWORK;
        }
        return ConnectionTestResult.failure(category, -1, durationMs, cause.getClass().getSimpleName());
    }

    private AiChatResult classifyChatFailure(Throwable error, long durationMs) {
        Throwable cause = unwrap(error);
        String category = Keys.TEST_CATEGORY_NETWORK;
        if (cause instanceof HttpTimeoutException) {
            category = Keys.TEST_CATEGORY_TIMEOUT;
        } else if (cause instanceof IOException) {
            category = Keys.TEST_CATEGORY_NETWORK;
        }
        return AiChatResult.failure(category, -1, durationMs);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static boolean isValidChatResponse(String body) {
        return extractContent(body) != null;
    }

    /** 提取 choices[0].message.content；缺失或类型不符返回 null。 */
    static String extractContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonArray choices = root.getAsJsonObject().getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JsonElement message = choices.get(0).getAsJsonObject().get("message");
            if (message == null || !message.isJsonObject()) {
                return null;
            }
            JsonElement content = message.getAsJsonObject().get("content");
            if (content != null && content.isJsonPrimitive() && content.getAsJsonPrimitive().isString()) {
                return content.getAsString();
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
