package com.harriol.blockwiz.common.model;

import java.util.List;

/**
 * 环境扫描摘要（纯逻辑，供 AI 提示词与启发式边界使用）。
 *
 * @author Harriol
 */
public record ScanSummary(
        Pos center,
        int radius,
        Box scannedBounds,
        String facing,
        long scanned,
        int nonAirCount,
        Box blockBounds,
        List<BlockCount> topBlocks) {

    /**
     * 空扫描摘要（用于测试与启发式回退）。
     *
     * @param center 扫描中心
     * @param radius 扫描半径
     * @param facing 玩家朝向（NORTH/SOUTH/EAST/WEST）
     * @return 空摘要
     */
    public static ScanSummary empty(Pos center, int radius, String facing) {
        return new ScanSummary(center, radius, ScanGeometry.scanBounds(center, radius),
                facing, 0, 0, null, List.of());
    }

    /** 填充方块统计后的副本。 */
    public ScanSummary withBlocks(int newNonAirCount, Box newBlockBounds, List<BlockCount> newTopBlocks) {
        return new ScanSummary(center, radius, scannedBounds, facing, scanned,
                newNonAirCount, newBlockBounds, newTopBlocks == null ? List.of() : List.copyOf(newTopBlocks));
    }
}
