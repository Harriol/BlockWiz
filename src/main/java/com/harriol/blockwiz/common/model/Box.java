package com.harriol.blockwiz.common.model;

/**
 * 建造/拆除范围（长方体）。构造时总是规范化为 min≤max；
 * 上限：任一边长 ≤64 且体积 ≤262144（64×64×64 封顶，PRD V1.1 §5.2/附录 B-8）。
 *
 * @author Harriol
 */
public record Box(Pos min, Pos max) {

    /** 单边长度上限（PRD：64）。 */
    public static final int MAX_EDGE = 64;

    /** 体积上限（PRD：64×64×64=262144）。 */
    public static final long MAX_VOLUME = 262_144L;

    public Box {
        if (min == null || max == null) {
            throw new IllegalArgumentException("min/max 不能为 null");
        }
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("min 必须不大于 max");
        }
    }

    /**
     * 由两个角点构造范围（自动规范化最小/最大坐标）。
     */
    public static Box fromCorners(int x1, int y1, int z1, int x2, int y2, int z2) {
        Pos min = new Pos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
        Pos max = new Pos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        return new Box(min, max);
    }

    /**
     * 以中心点 + X/Y/Z 尺寸构造范围（PRD §5.0：{@code /blockwiz range sx sy sz}）。
     * 偶数尺寸向负方向偏一格。
     *
     * @param center 中心坐标
     * @param sizeX  X 方向边长（≥1）
     * @param sizeY  Y 方向边长（≥1）
     * @param sizeZ  Z 方向边长（≥1）
     * @return 规范化后的范围
     */
    public static Box fromCenterAndSize(Pos center, int sizeX, int sizeY, int sizeZ) {
        if (center == null) {
            throw new IllegalArgumentException("center 不能为 null");
        }
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
            throw new IllegalArgumentException("尺寸必须 ≥1");
        }
        int minX = center.x() - sizeX / 2;
        int minY = center.y() - sizeY / 2;
        int minZ = center.z() - sizeZ / 2;
        return fromCorners(minX, minY, minZ, minX + sizeX - 1, minY + sizeY - 1, minZ + sizeZ - 1);
    }

    /** X 方向边长。 */
    public int edgeX() {
        return max.x() - min.x() + 1;
    }

    /** Y 方向边长。 */
    public int edgeY() {
        return max.y() - min.y() + 1;
    }

    /** Z 方向边长。 */
    public int edgeZ() {
        return max.z() - min.z() + 1;
    }

    /** 体积（long，避免 int 溢出）。 */
    public long volume() {
        return (long) edgeX() * edgeY() * edgeZ();
    }

    /**
     * 是否满足 PRD 范围上限：任一边长 ≤64 且体积 ≤262144。
     *
     * @return 合法返回 true
     */
    public boolean isWithinLimits() {
        return isWithinLimits(MAX_EDGE);
    }

    /**
     * 是否满足范围上限：任一边长 ≤maxEdge 且体积 ≤262144。
     * 玩家配置的扫描半径小于 64 时，maxEdge 取 min(64, scanRadius)，限制随玩家设定收紧。
     *
     * @param maxEdge 单边上限（≥1）
     * @return 合法返回 true
     */
    public boolean isWithinLimits(int maxEdge) {
        if (maxEdge < 1) {
            throw new IllegalArgumentException("maxEdge 必须 ≥1");
        }
        return edgeX() <= maxEdge && edgeY() <= maxEdge && edgeZ() <= maxEdge
                && volume() <= MAX_VOLUME;
    }

    /** 坐标是否落在范围内（含边界）。 */
    public boolean contains(Pos pos) {
        return pos != null
                && pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }

    /** 两个范围是否有交集。 */
    public boolean intersects(Box other) {
        return other != null
                && min.x() <= other.max.x() && max.x() >= other.min.x()
                && min.y() <= other.max.y() && max.y() >= other.min.y()
                && min.z() <= other.max.z() && max.z() >= other.min.z();
    }

    @Override
    public String toString() {
        return "[" + min + " ~ " + max + "]";
    }
}
