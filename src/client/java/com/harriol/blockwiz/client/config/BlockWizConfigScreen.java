package com.harriol.blockwiz.client.config;

import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.config.ConfigDropdownValues;
import com.harriol.blockwiz.common.config.ConfigHolder;
import com.harriol.blockwiz.common.config.ConfigIO;
import com.harriol.blockwiz.common.config.ConfigValidator;
import com.harriol.blockwiz.common.config.PresetLibrary;
import com.harriol.blockwiz.common.i18n.I18n;
import com.harriol.blockwiz.common.i18n.Keys;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BlockWiz 配置页（Cloth Config）。
 *
 * <p>字段编辑与保存走 ConfigIO（blockwiz.json，临时文件原子替换），
 * 不写入 Cloth Config 自身的配置文件。API Key 在界面掩码显示，不展示明文。</p>
 *
 * @author Harriol
 */
public final class BlockWizConfigScreen {

    /** 最大输出未设置标记（配置页用 -1 表示 null）。 */
    private static final int MAX_TOKENS_UNSET = -1;

    /** API Key 掩码展示占位。 */
    private static final String API_KEY_MASK = "••••••••";

    private BlockWizConfigScreen() {
    }

    /**
     * 创建配置页。
     *
     * @param parent 上一级页面
     * @return 配置页 Screen
     */
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal(I18n.get(Keys.CONFIG_TITLE)));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ScreenState state = new ScreenState();
        state.working = copy(ConfigHolder.get());
        state.configFile = FabricLoader.getInstance().getConfigDir()
                .resolve(ConfigIO.CONFIG_FILE_NAME);

        createApiEntries(state, entryBuilder);
        addApiCategory(builder, entryBuilder, state);
        createBehaviorEntries(state, entryBuilder);
        addBehaviorCategory(builder, state);
        configureSaving(builder, state);
        return builder.build();
    }

    /**
     * 创建 API 分类的全部条目（不添加，仅赋值到状态）。
     *
     * @param state        页面状态
     * @param entryBuilder 条目构建器
     */
    private static void createApiEntries(ScreenState state, ConfigEntryBuilder entryBuilder) {
        state.apiBaseUrl = entryBuilder
                .startStrField(literal(Keys.API_BASE_URL), state.working.getApiBaseUrl())
                .build();
        state.chatCompletionsPath = entryBuilder
                .startStrField(literal(Keys.CHAT_COMPLETIONS_PATH), state.working.getChatCompletionsPath())
                .build();
        state.apiKey = entryBuilder
                .startTextField(literal(Keys.API_KEY), displayApiKey(state.working.getApiKey()))
                .setTooltip(Component.literal(I18n.get(Keys.API_KEY_TOOLTIP)))
                .build();
        state.clearApiKey = entryBuilder
                .startBooleanToggle(literal(Keys.API_KEY_CLEAR), false)
                .build();
        state.apiKeyHeaderName = entryBuilder
                .startStrField(literal(Keys.API_KEY_HEADER_NAME), state.working.getApiKeyHeaderName())
                .build();
        state.apiKeyPrefix = entryBuilder
                .startStrField(literal(Keys.API_KEY_PREFIX), state.working.getApiKeyPrefix())
                .build();
        state.model = entryBuilder
                .startStrField(literal(Keys.MODEL), state.working.getModel())
                .build();
        state.temperature = entryBuilder
                .startDoubleField(literal(Keys.TEMPERATURE), state.working.getTemperature())
                .build();
        state.requestTimeoutMs = entryBuilder
                .startIntField(literal(Keys.REQUEST_TIMEOUT_MS), state.working.getRequestTimeoutMs())
                .build();
        state.maxTokens = entryBuilder
                .startIntField(literal(Keys.MAX_TOKENS),
                        state.working.getMaxTokens() == null ? MAX_TOKENS_UNSET : state.working.getMaxTokens())
                .build();
        state.extraHeaders = entryBuilder
                .startStrField(literal(Keys.EXTRA_HEADERS), serializeHeaders(state.working.getExtraHeaders()))
                .build();
        state.preset = entryBuilder
                .startDropdownMenu(literal(Keys.PRESET_SECTION), ConfigDropdownValues.NO_PRESET,
                        ConfigDropdownValues::resolvePreset,
                        code -> Component.literal(ConfigDropdownValues.presetLabel(code)))
                .setSelections(ConfigDropdownValues.presetOptions())
                .build();
    }

    /**
     * 组装 API 分类（状态行、字段、预设与测试提示）。
     *
     * @param builder      配置构建器
     * @param entryBuilder 条目构建器
     * @param state        页面状态
     */
    private static void addApiCategory(ConfigBuilder builder, ConfigEntryBuilder entryBuilder, ScreenState state) {
        ConfigCategory category = builder.getOrCreateCategory(literal(Keys.CATEGORY_API));
        addDescription(category, entryBuilder, ConfigHolder.isValid() ? Keys.STATUS_ENABLED : Keys.STATUS_DISABLED);
        category.addEntry(state.apiBaseUrl);
        category.addEntry(state.chatCompletionsPath);
        category.addEntry(state.apiKey);
        category.addEntry(state.clearApiKey);
        category.addEntry(state.apiKeyHeaderName);
        category.addEntry(state.apiKeyPrefix);
        category.addEntry(state.model);
        category.addEntry(state.temperature);
        category.addEntry(state.requestTimeoutMs);
        category.addEntry(state.maxTokens);
        category.addEntry(state.extraHeaders);
        addDescription(category, entryBuilder, Keys.PRESET_SECTION);
        category.addEntry(state.preset);
        addDescription(category, entryBuilder, Keys.TEST_HINT);
    }

    /**
     * 创建行为分类的全部条目（语言、速率、半径、超时、保护开关）。
     *
     * @param state        页面状态
     * @param entryBuilder 条目构建器
     */
    private static void createBehaviorEntries(ScreenState state, ConfigEntryBuilder entryBuilder) {
        state.blocksPerTick = entryBuilder
                .startIntField(literal(Keys.BLOCKS_PER_TICK), state.working.getBlocksPerTick())
                .build();
        state.scanRadius = entryBuilder
                .startIntField(literal(Keys.SCAN_RADIUS), state.working.getScanRadius())
                .build();
        state.confirmTimeoutSec = entryBuilder
                .startIntField(literal(Keys.CONFIRM_TIMEOUT_SEC), state.working.getConfirmTimeoutSec())
                .build();
        state.maxRetries = entryBuilder
                .startIntField(literal(Keys.MAX_RETRIES), state.working.getMaxRetries())
                .build();
        state.playerPositionProtection = entryBuilder
                .startBooleanToggle(literal(Keys.PLAYER_POSITION_PROTECTION),
                        state.working.isPlayerPositionProtection())
                .build();
        state.language = entryBuilder
                .startDropdownMenu(literal(Keys.LANGUAGE), state.working.getLanguage(),
                        ConfigDropdownValues::resolveLanguage,
                        code -> Component.literal(ConfigDropdownValues.languageLabel(code)))
                .setSelections(ConfigDropdownValues.languageOptions())
                .build();
    }

    /**
     * 组装行为分类。
     *
     * @param builder 配置构建器
     * @param state   页面状态
     */
    private static void addBehaviorCategory(ConfigBuilder builder, ScreenState state) {
        ConfigCategory category = builder.getOrCreateCategory(literal(Keys.CATEGORY_BEHAVIOR));
        category.addEntry(state.blocksPerTick);
        category.addEntry(state.scanRadius);
        category.addEntry(state.confirmTimeoutSec);
        category.addEntry(state.maxRetries);
        category.addEntry(state.playerPositionProtection);
        category.addEntry(state.language);
    }

    /**
     * 配置保存逻辑：收集条目 → 应用预设 → 校验 → 原子写入 blockwiz.json。
     *
     * @param builder 配置构建器
     * @param state   页面状态
     */
    private static void configureSaving(ConfigBuilder builder, ScreenState state) {
        builder.setSavingRunnable(() -> {
            ConfigData next = gatherConfig(state);
            applyKeyEntry(state, next);
            applyPreset(state, next);
            List<String> problems = ConfigValidator.validate(next);
            if (!problems.isEmpty()) {
                List<String> messages = new ArrayList<>();
                for (String key : problems) {
                    messages.add(I18n.get(key));
                }
                sendChat(I18n.format(Keys.SAVE_INVALID, String.join("；", messages)));
                return;
            }
            if (ConfigIO.save(state.configFile, next)) {
                ConfigHolder.update(next);
                I18n.setLanguage(next.getLanguage());
                sendChat(I18n.get(Keys.SAVE_SUCCESS));
            } else {
                sendChat(I18n.format(Keys.SAVE_INVALID, I18n.get(Keys.ERROR_LOAD_FAILED)));
            }
        });
    }

    /**
     * 从条目收集配置。
     *
     * @param state 页面状态
     * @return 新配置
     */
    private static ConfigData gatherConfig(ScreenState state) {
        ConfigData next = copy(state.working);
        next.setApiBaseUrl(state.apiBaseUrl.getValue());
        next.setChatCompletionsPath(state.chatCompletionsPath.getValue());
        next.setApiKeyHeaderName(state.apiKeyHeaderName.getValue());
        next.setApiKeyPrefix(state.apiKeyPrefix.getValue());
        next.setModel(state.model.getValue());
        next.setTemperature(state.temperature.getValue());
        next.setRequestTimeoutMs(state.requestTimeoutMs.getValue());
        int maxTokens = state.maxTokens.getValue();
        next.setMaxTokens(maxTokens == MAX_TOKENS_UNSET ? null : maxTokens);
        next.setExtraHeaders(parseHeaders(state.extraHeaders.getValue()));
        next.setBlocksPerTick(state.blocksPerTick.getValue());
        next.setScanRadius(state.scanRadius.getValue());
        next.setConfirmTimeoutSec(state.confirmTimeoutSec.getValue());
        next.setMaxRetries(state.maxRetries.getValue());
        next.setPlayerPositionProtection(state.playerPositionProtection.getValue());
        next.setLanguage(state.language.getValue());
        return next;
    }

    /**
     * 应用 API Key 输入：掩码/留空保留原 Key；勾选清除则置空；否则使用新输入。
     *
     * @param state 页面状态
     * @param next  目标配置
     */
    private static void applyKeyEntry(ScreenState state, ConfigData next) {
        if (Boolean.TRUE.equals(state.clearApiKey.getValue())) {
            next.setApiKey("");
            return;
        }
        String typedKey = state.apiKey.getValue();
        if (typedKey != null && !typedKey.isBlank() && !API_KEY_MASK.equals(typedKey)) {
            next.setApiKey(typedKey);
        }
    }

    /**
     * 应用预设（仅覆盖 Base URL、路径、鉴权头、前缀与模型）。
     *
     * @param state 页面状态
     * @param next  目标配置
     */
    private static void applyPreset(ScreenState state, ConfigData next) {
        String selectedPreset = state.preset.getValue();
        if (selectedPreset != null && !ConfigDropdownValues.NO_PRESET.equals(selectedPreset)) {
            PresetLibrary.byId(selectedPreset).ifPresent(preset -> preset.applyTo(next));
        }
    }

    /**
     * 在聊天栏展示本地化消息。
     *
     * @param message 消息文本
     */
    private static void sendChat(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("[BlockWiz] " + message), false);
        }
    }

    /**
     * 深拷贝配置，避免编辑时污染已保存配置。
     *
     * @param source 源配置
     * @return 拷贝
     */
    private static ConfigData copy(ConfigData source) {
        ConfigData copy = new ConfigData();
        copy.setApiBaseUrl(source.getApiBaseUrl());
        copy.setChatCompletionsPath(source.getChatCompletionsPath());
        copy.setApiKey(source.getApiKey());
        copy.setApiKeyHeaderName(source.getApiKeyHeaderName());
        copy.setApiKeyPrefix(source.getApiKeyPrefix());
        copy.setModel(source.getModel());
        copy.setTemperature(source.getTemperature());
        copy.setRequestTimeoutMs(source.getRequestTimeoutMs());
        copy.setMaxTokens(source.getMaxTokens());
        copy.setExtraHeaders(new LinkedHashMap<>(source.getExtraHeaders()));
        copy.setLanguage(source.getLanguage());
        copy.setBlocksPerTick(source.getBlocksPerTick());
        copy.setScanRadius(source.getScanRadius());
        copy.setConfirmTimeoutSec(source.getConfirmTimeoutSec());
        copy.setPlayerPositionProtection(source.isPlayerPositionProtection());
        copy.setMaxRetries(source.getMaxRetries());
        return copy;
    }

    /**
     * API Key 界面展示值：已设置则掩码，否则为空。
     *
     * @param apiKey 实际 Key
     * @return 展示值
     */
    private static String displayApiKey(String apiKey) {
        return apiKey == null || apiKey.isBlank() ? "" : API_KEY_MASK;
    }

    /**
     * 将请求头映射序列化为 "key=value;..." 文本。
     *
     * @param headers 请求头映射
     * @return 序列化文本
     */
    private static String serializeHeaders(Map<String, String> headers) {
        List<String> parts = new ArrayList<>();
        headers.forEach((key, value) -> parts.add(key + "=" + value));
        return String.join(";", parts);
    }

    /**
     * 将 "key=value;..." 文本解析为请求头映射。
     *
     * @param text 序列化文本
     * @return 请求头映射
     */
    private static Map<String, String> parseHeaders(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String part : text.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) {
                result.put(part.substring(0, index).trim(), part.substring(index + 1).trim());
            }
        }
        return result;
    }

    /**
     * 添加一段本地化说明文本。
     *
     * @param category     分类
     * @param entryBuilder 条目构建器
     * @param key          翻译键
     */
    private static void addDescription(ConfigCategory category, ConfigEntryBuilder entryBuilder, String key) {
        category.addEntry(entryBuilder.startTextDescription(Component.literal(I18n.get(key))).build());
    }

    /**
     * 构造本地化文本。
     *
     * @param key 翻译键
     * @return 文本组件
     */
    private static Component literal(String key) {
        return Component.literal(I18n.get(key));
    }

    /**
     * 配置页状态：工作副本与全部条目引用（保存时统一收集）。
     */
    private static final class ScreenState {
        private ConfigData working;
        private Path configFile;
        private AbstractConfigListEntry<String> apiBaseUrl;
        private AbstractConfigListEntry<String> chatCompletionsPath;
        private AbstractConfigListEntry<String> apiKey;
        private AbstractConfigListEntry<Boolean> clearApiKey;
        private AbstractConfigListEntry<String> apiKeyHeaderName;
        private AbstractConfigListEntry<String> apiKeyPrefix;
        private AbstractConfigListEntry<String> model;
        private AbstractConfigListEntry<Double> temperature;
        private AbstractConfigListEntry<Integer> requestTimeoutMs;
        private AbstractConfigListEntry<Integer> maxTokens;
        private AbstractConfigListEntry<String> extraHeaders;
        private AbstractConfigListEntry<String> preset;
        private AbstractConfigListEntry<Integer> blocksPerTick;
        private AbstractConfigListEntry<Integer> scanRadius;
        private AbstractConfigListEntry<Integer> confirmTimeoutSec;
        private AbstractConfigListEntry<Integer> maxRetries;
        private AbstractConfigListEntry<Boolean> playerPositionProtection;
        private AbstractConfigListEntry<String> language;
    }
}
