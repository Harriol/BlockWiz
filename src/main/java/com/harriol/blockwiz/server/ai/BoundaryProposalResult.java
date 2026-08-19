package com.harriol.blockwiz.server.ai;

/**
 * 候选边界请求结果。
 *
 * @author Harriol
 */
public record BoundaryProposalResult(boolean ok, BoundaryProposal proposal, String errorKey, String errorDetail) {

    public static BoundaryProposalResult ok(BoundaryProposal proposal) {
        return new BoundaryProposalResult(true, proposal, null, null);
    }

    public static BoundaryProposalResult failure(String errorKey, String errorDetail) {
        return new BoundaryProposalResult(false, null, errorKey, errorDetail);
    }
}
