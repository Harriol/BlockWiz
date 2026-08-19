package com.harriol.blockwiz.common.model;

/**
 * 扫描几何：以玩家为中心、半径 scanRadius 的方形邻域（PRD §5.2：V1.0 取方形邻域）。
 *
 * @author Harriol
 */
public final class ScanGeometry {

    private ScanGeometry() {
    }

    /**
     * 计算扫描范围：中心 ± 半径（含边界）。
     *
     * @param center 扫描中心
     * @param radius 扫描半径（≥0）
     * @return 扫描范围
     */
    public static Box scanBounds(Pos center, int radius) {
        if (center == null) {
            throw new IllegalArgumentException("center 不能为 null");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("radius 必须 ≥0");
        }
        return Box.fromCorners(
                center.x() - radius, center.y() - radius, center.z() - radius,
                center.x() + radius, center.y() + radius, center.z() + radius);
    }

    /** 扫描位置总数。 */
    public static long positionCount(Box bounds) {
        return bounds.volume();
    }

    /**
     * 把线性索引映射为范围内坐标（z → y → x 展开）。
     *
     * @param bounds 扫描范围
     * @param index  线性索引 [0, positionCount)
     * @return {x, y, z}
     */
    public static int[] coordinateAt(Box bounds, long index) {
        if (bounds == null) {
            throw new IllegalArgumentException("bounds 不能为 null");
        }
        long total = positionCount(bounds);
        if (index < 0 || index >= total) {
            throw new IndexOutOfBoundsException("索引越界: " + index + "（范围大小 " + total + "）");
        }
        int sizeX = bounds.edgeX();
        int sizeY = bounds.edgeY();
        long layer = (long) sizeX * sizeY;
        long zOffset = index / layer;
        long remainder = index % layer;
        int yOffset = (int) (remainder / sizeX);
        int xOffset = (int) (remainder % sizeX);
        return new int[]{
                bounds.min().x() + xOffset,
                bounds.min().y() + yOffset,
                bounds.min().z() + (int) zOffset};
    }
}
