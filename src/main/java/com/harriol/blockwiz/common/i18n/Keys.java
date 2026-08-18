package com.harriol.blockwiz.common.i18n;

import java.util.Set;

/**
 * 翻译键常量。
 *
 * <p>所有键必须在 zh_cn.json 与 en_us.json 中成对存在。</p>
 *
 * @author Harriol
 */
public final class Keys {

    private Keys() {
    }

    /** 配置页标题。 */
    public static final String CONFIG_TITLE = "config.title";

    /** 配置页分类：API。 */
    public static final String CATEGORY_API = "config.category.api";

    /** 配置页分类：行为。 */
    public static final String CATEGORY_BEHAVIOR = "config.category.behavior";

    /** 配置页分类：高级。 */
    public static final String CATEGORY_ADVANCED = "config.category.advanced";

    /** API Base URL 字段。 */
    public static final String API_BASE_URL = "config.apiBaseUrl";

    /** Chat Completions 路径字段。 */
    public static final String CHAT_COMPLETIONS_PATH = "config.chatCompletionsPath";

    /** API Key 字段。 */
    public static final String API_KEY = "config.apiKey";

    /** 鉴权请求头名称字段。 */
    public static final String API_KEY_HEADER_NAME = "config.apiKeyHeaderName";

    /** Key 前缀字段。 */
    public static final String API_KEY_PREFIX = "config.apiKeyPrefix";

    /** 模型名称字段。 */
    public static final String MODEL = "config.model";

    /** 温度字段。 */
    public static final String TEMPERATURE = "config.temperature";

    /** 请求超时字段。 */
    public static final String REQUEST_TIMEOUT_MS = "config.requestTimeoutMs";

    /** 最大输出字段。 */
    public static final String MAX_TOKENS = "config.maxTokens";

    /** 自定义请求头字段。 */
    public static final String EXTRA_HEADERS = "config.extraHeaders";

    /** 语言字段。 */
    public static final String LANGUAGE = "config.language";

    /** 每 tick 方块数上限字段。 */
    public static final String BLOCKS_PER_TICK = "config.blocksPerTick";

    /** 扫描半径字段。 */
    public static final String SCAN_RADIUS = "config.scanRadius";

    /** 建造确认超时字段。 */
    public static final String CONFIRM_TIMEOUT_SEC = "config.confirmTimeoutSec";

    /** API 重试次数字段。 */
    public static final String MAX_RETRIES = "config.maxRetries";

    /** 玩家位置保护字段。 */
    public static final String PLAYER_POSITION_PROTECTION = "config.playerPositionProtection";

    /** 预设区块说明。 */
    public static final String PRESET_SECTION = "config.preset.section";

    /** 预设应用按钮文案。 */
    public static final String PRESET_APPLY = "config.preset.apply";

    /** 不使用预设选项。 */
    public static final String PRESET_NONE = "config.preset.none";

    /** 连接测试提示。 */
    public static final String TEST_HINT = "config.test.hint";

    /** 清除已保存的 Key 开关。 */
    public static final String API_KEY_CLEAR = "config.apiKeyClear";

    /** API Key 输入框工具提示。 */
    public static final String API_KEY_TOOLTIP = "config.apiKeyTooltip";

    /** AI 请求状态：已启用。 */
    public static final String STATUS_ENABLED = "config.status.enabled";

    /** AI 请求状态：未配置（已禁用）。 */
    public static final String STATUS_DISABLED = "config.status.disabled";

    /** 命令提示：AI 请求未启用。 */
    public static final String COMMAND_TEST_NOT_CONFIGURED = "command.test.notConfigured";

    /** 配置保存成功提示。 */
    public static final String SAVE_SUCCESS = "config.save.success";

    /** 配置保存失败提示。 */
    public static final String SAVE_INVALID = "config.save.invalid";

    /** 语言选项：简体中文。 */
    public static final String LANGUAGE_ZH_CN = "config.language.zh_cn";

    /** 语言选项：English。 */
    public static final String LANGUAGE_EN_US = "config.language.en_us";

    /** 校验错误：Base URL 为空。 */
    public static final String ERROR_BASE_URL_EMPTY = "config.error.apiBaseUrl.empty";

    /** 校验错误：Base URL 非法。 */
    public static final String ERROR_BASE_URL_INVALID = "config.error.apiBaseUrl.invalid";

    /** 校验错误：路径非法。 */
    public static final String ERROR_PATH_INVALID = "config.error.chatCompletionsPath.invalid";

    /** 校验错误：鉴权头名为空。 */
    public static final String ERROR_HEADER_NAME_EMPTY = "config.error.apiKeyHeaderName.empty";

    /** 校验错误：模型名为空。 */
    public static final String ERROR_MODEL_EMPTY = "config.error.model.empty";

    /** 校验错误：温度越界。 */
    public static final String ERROR_TEMPERATURE_RANGE = "config.error.temperature.range";

    /** 校验错误：超时越界。 */
    public static final String ERROR_TIMEOUT_RANGE = "config.error.requestTimeoutMs.range";

    /** 校验错误：最大输出非法。 */
    public static final String ERROR_MAX_TOKENS = "config.error.maxTokens.positive";

    /** 校验错误：语言不支持。 */
    public static final String ERROR_LANGUAGE = "config.error.language.unsupported";

    /** 校验错误：每 tick 方块数越界。 */
    public static final String ERROR_BLOCKS_PER_TICK = "config.error.blocksPerTick.range";

    /** 校验错误：扫描半径越界。 */
    public static final String ERROR_SCAN_RADIUS = "config.error.scanRadius.range";

    /** 校验错误：确认超时越界。 */
    public static final String ERROR_CONFIRM_TIMEOUT = "config.error.confirmTimeoutSec.range";

    /** 校验错误：重试次数越界。 */
    public static final String ERROR_MAX_RETRIES = "config.error.maxRetries.range";

    /** 校验错误：自定义请求头键名为空。 */
    public static final String ERROR_EXTRA_HEADERS_KEY = "config.error.extraHeaders.emptyKey";

    /** 校验错误：配置文件读取/解析失败。 */
    public static final String ERROR_LOAD_FAILED = "config.error.load.failed";

    /** 命令用法提示。 */
    public static final String COMMAND_USAGE = "command.usage";

    /** 子命令占位提示。 */
    public static final String COMMAND_PLACEHOLDER_SPRINT = "command.placeholder.sprint";

    /** 连接测试开始提示。 */
    public static final String COMMAND_TEST_START = "command.test.start";

    /** 连接测试进行中提示。 */
    public static final String COMMAND_TEST_RUNNING = "command.test.running";

    /** 连接测试成功结果。 */
    public static final String COMMAND_TEST_OK = "command.test.ok";

    /** 连接测试失败结果。 */
    public static final String COMMAND_TEST_FAIL = "command.test.fail";

    /** 测试分类：请求超时。 */
    public static final String TEST_CATEGORY_TIMEOUT = "test.category.timeout";

    /** 测试分类：网络错误。 */
    public static final String TEST_CATEGORY_NETWORK = "test.category.network";

    /** 测试分类：鉴权失败。 */
    public static final String TEST_CATEGORY_AUTH = "test.category.auth";

    /** 测试分类：被限流。 */
    public static final String TEST_CATEGORY_RATE_LIMIT = "test.category.rateLimit";

    /** 测试分类：HTTP 错误。 */
    public static final String TEST_CATEGORY_HTTP = "test.category.http";

    /** 测试分类：服务端错误。 */
    public static final String TEST_CATEGORY_SERVER = "test.category.server";

    /** 测试分类：响应格式错误。 */
    public static final String TEST_CATEGORY_FORMAT = "test.category.format";

    /** 确认词。 */
    public static final String CONFIRM_WORD = "confirmWord";

    /** 取消词。 */
    public static final String CANCEL_WORD = "cancelWord";

    /** 校验器可能返回的全部错误键，供测试断言语言文件完整。 */
    public static final Set<String> ERROR_KEYS = Set.of(
            ERROR_BASE_URL_EMPTY, ERROR_BASE_URL_INVALID, ERROR_PATH_INVALID,
            ERROR_HEADER_NAME_EMPTY, ERROR_MODEL_EMPTY, ERROR_TEMPERATURE_RANGE,
            ERROR_TIMEOUT_RANGE, ERROR_MAX_TOKENS, ERROR_LANGUAGE, ERROR_BLOCKS_PER_TICK,
            ERROR_SCAN_RADIUS, ERROR_CONFIRM_TIMEOUT, ERROR_MAX_RETRIES,
            ERROR_EXTRA_HEADERS_KEY, ERROR_LOAD_FAILED);
}
