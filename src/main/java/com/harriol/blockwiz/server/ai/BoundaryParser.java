package com.harriol.blockwiz.server.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.harriol.blockwiz.common.model.Box;
import com.harriol.blockwiz.common.model.Pos;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 候选边界最小 JSON 解析器（Sprint 2；完整计划 Schema 属 Sprint 3）。
 * 解析失败或超限一律返回失败，不产生任何修改。
 *
 * @author Harriol
 */
public final class BoundaryParser {

    /** 错误：非 JSON 或非对象。 */
    public static final String ERROR_JSON = "ai.boundary.error.json";
    /** 错误：range 缺失。 */
    public static final String ERROR_MISSING_RANGE = "ai.boundary.error.missingRange";
    /** 错误：坐标缺失/类型非法。 */
    public static final String ERROR_INVALID_RANGE = "ai.boundary.error.invalidRange";
    /** 错误：范围超过 64³ 上限。 */
    public static final String ERROR_LIMITS = "ai.boundary.error.limits";

    private BoundaryParser() {
    }

    /**
     * 解析 AI 返回的候选边界 JSON：
     * {@code {"range":{"min":{"x","y","z"},"max":{"x","y","z"}},"origin":"AI_PROPOSED","reasoning":"...","keep":["..."]}}
     *
     * @param json AI 输出内容
     * @return 解析结果
     */
    public static BoundaryParseResult parse(String json) {
        if (json == null || json.isBlank()) {
            return BoundaryParseResult.failure(ERROR_JSON, "empty");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            return BoundaryParseResult.failure(ERROR_JSON, "invalid json");
        }
        if (!root.isJsonObject()) {
            return BoundaryParseResult.failure(ERROR_JSON, "not an object");
        }
        JsonObject object = root.getAsJsonObject();
        JsonElement rangeElement = object.get("range");
        if (rangeElement == null || !rangeElement.isJsonObject()) {
            return BoundaryParseResult.failure(ERROR_MISSING_RANGE, "range missing");
        }
        JsonObject rangeObject = rangeElement.getAsJsonObject();
        Pos min = parsePos(rangeObject.get("min"));
        Pos max = parsePos(rangeObject.get("max"));
        if (min == null || max == null) {
            return BoundaryParseResult.failure(ERROR_INVALID_RANGE, "min/max");
        }
        Box range = Box.fromCorners(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        if (!range.isWithinLimits()) {
            return BoundaryParseResult.failure(ERROR_LIMITS, range.toString());
        }
        String origin = getString(object, "origin", "AI_PROPOSED");
        String reasoning = getString(object, "reasoning", "");
        List<String> keepNotes = new ArrayList<>();
        JsonElement keepElement = object.get("keep");
        if (keepElement != null && keepElement.isJsonArray()) {
            JsonArray keepArray = keepElement.getAsJsonArray();
            for (JsonElement element : keepArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    keepNotes.add(element.getAsString());
                }
            }
        }
        return BoundaryParseResult.ok(BoundaryProposal.of(range, origin, reasoning, keepNotes));
    }

    private static Pos parsePos(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        Integer x = asInt(object.get("x"));
        Integer y = asInt(object.get("y"));
        Integer z = asInt(object.get("z"));
        if (x == null || y == null || z == null) {
            return null;
        }
        return new Pos(x, y, z);
    }

    private static Integer asInt(JsonElement element) {
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            try {
                return element.getAsInt();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        return fallback;
    }
}
