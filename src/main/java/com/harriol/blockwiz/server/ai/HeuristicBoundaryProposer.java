package com.harriol.blockwiz.server.ai;

import com.harriol.blockwiz.common.model.Box;
import com.harriol.blockwiz.common.model.ScanSummary;

import java.util.List;

/**
 * 启发式候选边界（AI 未配置/失败时的回退，保证流程可用）：
 * 扫描到合法方块边界则直接采用；否则以玩家为中心取默认 16³（裁剪到扫描范围）。
 *
 * @author Harriol
 */
public final class HeuristicBoundaryProposer {

    /** 启发式来源标记。 */
    public static final String ORIGIN = "HEURISTIC";

    /** 默认候选边长。 */
    public static final int DEFAULT_EDGE = 16;

    /** 启发式理由（i18n 键）。 */
    public static final String REASON_KEY = "command.candidate.heuristic.reason";

    private HeuristicBoundaryProposer() {
    }

    /**
     * 基于扫描摘要提出候选边界。
     *
     * @param summary 扫描摘要
     * @return 候选边界（必然满足 64³ 上限）
     */
    public static BoundaryProposal propose(ScanSummary summary) {
        Box candidate;
        if (summary.blockBounds() != null && summary.blockBounds().isWithinLimits()) {
            candidate = summary.blockBounds();
        } else {
            Box scan = summary.scannedBounds();
            int edgeX = Math.min(DEFAULT_EDGE, scan.edgeX());
            int edgeY = Math.min(DEFAULT_EDGE, scan.edgeY());
            int edgeZ = Math.min(DEFAULT_EDGE, scan.edgeZ());
            candidate = Box.fromCenterAndSize(summary.center(), edgeX, edgeY, edgeZ);
        }
        return BoundaryProposal.of(candidate, ORIGIN, REASON_KEY, List.of());
    }
}
