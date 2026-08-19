package com.harriol.blockwiz.server.ai;

import com.harriol.blockwiz.common.model.Box;

import java.util.List;

/**
 * 候选边界（AI 或启发式提出，等待玩家确认）。
 *
 * @author Harriol
 */
public record BoundaryProposal(Box range, String origin, String reasoning, List<String> keepNotes) {

    public BoundaryProposal {
        if (range == null) {
            throw new IllegalArgumentException("range 不能为 null");
        }
        if (origin == null || origin.isBlank()) {
            origin = "AI_PROPOSED";
        }
        reasoning = reasoning == null ? "" : reasoning;
        keepNotes = keepNotes == null ? List.of() : List.copyOf(keepNotes);
    }

    public static BoundaryProposal of(Box range, String origin, String reasoning, List<String> keepNotes) {
        return new BoundaryProposal(range, origin, reasoning, keepNotes);
    }
}
