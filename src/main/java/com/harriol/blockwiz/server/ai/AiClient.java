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
        HttpRequest request = buildRequest(config);
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

    private HttpRequest buildRequest(ConfigData config) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint(config.getApiBaseUrl(), config.getChatCompletionsPath())))
                .timeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                .header("Content-Type", CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(config)));

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

    private static String buildBody(ConfigData config) {
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

    private ConnectionTestResult classifyResponse(HttpResponse<String> response, long durationMs) {
        int status = response.statusCode();
        if (status >= HTTP_OK_MIN && status <= HTTP_OK_MAX) {
            if (isValidChatResponse(response.body())) {
                return ConnectionTestResult.success(status, durationMs);
            }
            return ConnectionTestResult.failure(Keys.TEST_CATEGORY_FORMAT, status, durationMs, "choices[0].message.content 缺失");
        }
        String category;
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            category = Keys.TEST_CATEGORY_AUTH;
        } else if (status == HTTP_TOO_MANY_REQUESTS) {
            category = Keys.TEST_CATEGORY_RATE_LIMIT;
        } else if (status >= HTTP_SERVER_ERROR_MIN) {
            category = Keys.TEST_CATEGORY_SERVER;
        } else {
            category = Keys.TEST_CATEGORY_HTTP;
        }
        return ConnectionTestResult.failure(category, status, durationMs, "HTTP " + status);
    }

    private ConnectionTestResult classifyFailure(Throwable error, long durationMs) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String category = Keys.TEST_CATEGORY_NETWORK;
        if (cause instanceof HttpTimeoutException) {
            category = Keys.TEST_CATEGORY_TIMEOUT;
        } else if (cause instanceof IOException) {
            category = Keys.TEST_CATEGORY_NETWORK;
        }
        return ConnectionTestResult.failure(category, -1, durationMs, cause.getClass().getSimpleName());
    }

    private static boolean isValidChatResponse(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return false;
            }
            JsonArray choices = root.getAsJsonObject().getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return false;
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            return message != null && message.get("content").isJsonPrimitive();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
