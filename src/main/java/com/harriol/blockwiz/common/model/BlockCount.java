package com.harriol.blockwiz.common.model;

/**
 * 单个方块类型的统计（注册表 ID + 数量）。
 *
 * @author Harriol
 */
public record BlockCount(String id, int count) {
}
