package com.harriol.blockwiz.common.model;

import java.util.Objects;

/**
 * 方块坐标（纯逻辑，不依赖 Minecraft）。
 *
 * @author Harriol
 */
public record Pos(int x, int y, int z) {

    /**
     * 由长度为 3 的数组构造坐标。
     *
     * @param xyz x/y/z 数组
     * @return 坐标
     */
    public static Pos of(int[] xyz) {
        Objects.requireNonNull(xyz, "xyz");
        if (xyz.length != 3) {
            throw new IllegalArgumentException("坐标数组长度必须为 3");
        }
        return new Pos(xyz[0], xyz[1], xyz[2]);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
