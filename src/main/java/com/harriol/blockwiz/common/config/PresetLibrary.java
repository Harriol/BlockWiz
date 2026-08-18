package com.harriol.blockwiz.common.config;

import java.util.List;
import java.util.Optional;

/**
 * 五类预设模板：OpenAI / Claude（兼容网关）/ 通义千问 / 智谱 / Ollama。
 * 地址与模型名为推荐值，玩家可在配置页覆盖。
 *
 * @author Harriol
 */
public final class PresetLibrary {

    private PresetLibrary() {
    }

    public static final PresetTemplate OPENAI = new PresetTemplate(
            "openai", "preset.openai",
            "https://api.openai.com/v1", "/chat/completions",
            "Authorization", "Bearer ", "gpt-4o-mini", "");

    public static final PresetTemplate CLAUDE_GATEWAY = new PresetTemplate(
            "claude", "preset.claude",
            "", "/chat/completions",
            "Authorization", "Bearer ", "claude-sonnet-4-20250514", "preset.claude.note");

    public static final PresetTemplate QWEN = new PresetTemplate(
            "qwen", "preset.qwen",
            "https://dashscope.aliyuncs.com/compatible-mode/v1", "/chat/completions",
            "Authorization", "Bearer ", "qwen-plus", "");

    public static final PresetTemplate ZHIPU = new PresetTemplate(
            "zhipu", "preset.zhipu",
            "https://open.bigmodel.cn/api/paas/v4", "/chat/completions",
            "Authorization", "Bearer ", "glm-4-flash", "");

    public static final PresetTemplate OLLAMA = new PresetTemplate(
            "ollama", "preset.ollama",
            "http://127.0.0.1:11434/v1", "/chat/completions",
            "Authorization", "Bearer ", "llama3.1", "");

    public static final List<PresetTemplate> ALL =
            List.of(OPENAI, CLAUDE_GATEWAY, QWEN, ZHIPU, OLLAMA);

    public static Optional<PresetTemplate> byId(String id) {
        return ALL.stream().filter(preset -> preset.id().equals(id)).findFirst();
    }
}
