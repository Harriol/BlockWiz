package com.harriol.blockwiz.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.Objects;

/**
 * 配置 JSON 编解码（Gson）。未知字段忽略；缺失字段保留默认值；
 * 非法类型抛出 {@link JsonSyntaxException}。
 *
 * @author Harriol
 */
public final class ConfigCodec {

    /** Gson 会调用 ConfigData 的无参构造器，缺失字段保留默认值。 */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ConfigCodec() {
    }

    public static ConfigData fromJson(String json) {
        Objects.requireNonNull(json, "json");
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new JsonSyntaxException("配置不是合法 JSON", e);
        }
        if (!root.isJsonObject()) {
            throw new JsonSyntaxException("配置必须是 JSON 对象");
        }
        ConfigData config = GSON.fromJson(root, ConfigData.class);
        if (config == null) {
            throw new JsonSyntaxException("配置解析结果为空");
        }
        return config;
    }

    public static String toJson(ConfigData config) {
        Objects.requireNonNull(config, "config");
        return GSON.toJson(config);
    }
}
