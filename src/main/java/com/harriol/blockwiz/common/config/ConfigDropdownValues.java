package com.harriol.blockwiz.common.config;

import com.harriol.blockwiz.common.i18n.I18n;
import com.harriol.blockwiz.common.i18n.Keys;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置页下拉框的「值 ↔ 本地化标签」双向映射。
 *
 * <p>Cloth Config 下拉框（suggestion 模式）的文本框内容为本地化标签
 * （如「简体中文」「OpenAI」），保存时需将标签反解为内部代码
 * （zh_cn / en_us / openai / none）。本类集中维护该映射，便于单元测试。</p>
 *
 * @author Harriol
 */
public final class ConfigDropdownValues {

    /** 预设下拉的「不使用」选项 id。 */
    public static final String NO_PRESET = "none";

    /** 支持的语言代码列表（顺序即下拉框展示顺序）。 */
    private static final List<String> LANGUAGE_CODES =
            List.of(ConfigData.DEFAULT_LANGUAGE_ZH_CN, ConfigData.DEFAULT_LANGUAGE_EN_US);

    /** 用于标签反查的语言文件列表。 */
    private static final List<String> LANGUAGE_FILES =
            List.of(ConfigData.DEFAULT_LANGUAGE_ZH_CN, ConfigData.DEFAULT_LANGUAGE_EN_US);

    private ConfigDropdownValues() {
    }

    /**
     * 语言代码的本地化标签。
     *
     * @param code 语言代码
     * @return 本地化标签
     */
    public static String languageLabel(String code) {
        if (ConfigData.DEFAULT_LANGUAGE_ZH_CN.equals(code)) {
            return I18n.get(Keys.LANGUAGE_ZH_CN);
        }
        if (ConfigData.DEFAULT_LANGUAGE_EN_US.equals(code)) {
            return I18n.get(Keys.LANGUAGE_EN_US);
        }
        return code;
    }

    /**
     * 将语言标签或代码解析为语言代码。
     *
     * <p>优先识别已知代码；其次按当前语言与全部语言文件的标签反查；
     * 空文本回退默认语言；未知文本原样返回，由校验器给出「不支持的语言」。</p>
     *
     * @param text 界面文本
     * @return 语言代码
     */
    public static String resolveLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ConfigData.DEFAULT_LANGUAGE_ZH_CN;
        }
        String trimmed = text.trim();
        if (LANGUAGE_CODES.contains(trimmed)) {
            return trimmed;
        }
        if (equalsLabel(trimmed, Keys.LANGUAGE_ZH_CN)) {
            return ConfigData.DEFAULT_LANGUAGE_ZH_CN;
        }
        if (equalsLabel(trimmed, Keys.LANGUAGE_EN_US)) {
            return ConfigData.DEFAULT_LANGUAGE_EN_US;
        }
        return trimmed;
    }

    /**
     * 预设 id 的本地化标签。
     *
     * @param presetId 预设 id
     * @return 本地化标签
     */
    public static String presetLabel(String presetId) {
        if (NO_PRESET.equals(presetId)) {
            return I18n.get(Keys.PRESET_NONE);
        }
        return PresetLibrary.byId(presetId)
                .map(preset -> I18n.get(preset.displayNameKey()))
                .orElse(presetId);
    }

    /**
     * 将预设标签或 id 解析为预设 id。
     *
     * <p>优先识别已知 id；其次按当前语言与全部语言文件的标签反查；
     * 未知文本原样返回（应用预设时按 id 查不到则静默不应用）。</p>
     *
     * @param text 界面文本
     * @return 预设 id
     */
    public static String resolvePreset(String text) {
        if (text == null || text.trim().isEmpty()) {
            return NO_PRESET;
        }
        String trimmed = text.trim();
        if (NO_PRESET.equals(trimmed)) {
            return trimmed;
        }
        for (PresetTemplate preset : PresetLibrary.ALL) {
            if (preset.id().equals(trimmed)) {
                return trimmed;
            }
        }
        if (equalsLabel(trimmed, Keys.PRESET_NONE)) {
            return NO_PRESET;
        }
        for (PresetTemplate preset : PresetLibrary.ALL) {
            if (equalsLabel(trimmed, preset.displayNameKey())) {
                return preset.id();
            }
        }
        return trimmed;
    }

    /**
     * 语言下拉可选代码列表。
     *
     * @return 语言代码列表
     */
    public static List<String> languageOptions() {
        return LANGUAGE_CODES;
    }

    /**
     * 预设下拉可选 id 列表：none + 五类预设。
     *
     * @return 预设 id 列表
     */
    public static List<String> presetOptions() {
        List<String> options = new ArrayList<>();
        options.add(NO_PRESET);
        for (PresetTemplate preset : PresetLibrary.ALL) {
            options.add(preset.id());
        }
        return options;
    }

    /**
     * 判断文本是否等于某翻译键在任一支持语言下的标签。
     *
     * @param text 界面文本
     * @param key  翻译键
     * @return 是否匹配
     */
    private static boolean equalsLabel(String text, String key) {
        if (I18n.get(key).equals(text)) {
            return true;
        }
        for (String language : LANGUAGE_FILES) {
            if (I18n.get(key, language).equals(text)) {
                return true;
            }
        }
        return false;
    }
}
