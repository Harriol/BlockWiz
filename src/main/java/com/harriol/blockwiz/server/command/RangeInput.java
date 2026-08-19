package com.harriol.blockwiz.server.command;

import java.util.ArrayList;
import java.util.List;

/**
 * /blockwiz range 文本参数解析（纯逻辑，可单测）。
 * 接受 3 个整数（中心 + X/Y/Z 尺寸）或 6 个整数（两个角点）；
 * 其余数量或非数字一律返回错误键，由命令处理器展示可读提示。
 *
 * @author Harriol
 */
public final class RangeInput {

    /** 参数格式错误（i18n 键）。 */
    public static final String ERROR_FORMAT = "command.range.error.format";

    private RangeInput() {
    }

    /**
     * 解析范围参数文本。
     *
     * @param text 用户输入（空格分隔的 3 或 6 个整数）
     * @return 解析结果（isOk() 为 true 时 values 为 3 或 6 个整数）
     */
    public static RangeParseResult parse(String text) {
        if (text == null || text.isBlank()) {
            return RangeParseResult.failure(ERROR_FORMAT);
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 3 && parts.length != 6) {
            return RangeParseResult.failure(ERROR_FORMAT);
        }
        List<Integer> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                values.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                return RangeParseResult.failure(ERROR_FORMAT);
            }
        }
        return RangeParseResult.ok(values);
    }

    /** 解析结果：成功携带 3 或 6 个整数，失败携带错误键。 */
    public record RangeParseResult(List<Integer> values, String errorKey) {

        public boolean isOk() {
            return values != null;
        }

        /** 3 个整数：中心 + 尺寸。 */
        public boolean isCenterAndSize() {
            return values != null && values.size() == 3;
        }

        /** 6 个整数：两个角点。 */
        public boolean isCorners() {
            return values != null && values.size() == 6;
        }

        public static RangeParseResult ok(List<Integer> parsedValues) {
            return new RangeParseResult(List.copyOf(parsedValues), null);
        }

        public static RangeParseResult failure(String errorKey) {
            return new RangeParseResult(null, errorKey);
        }
    }
}
