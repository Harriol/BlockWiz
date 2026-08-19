package com.harriol.blockwiz.server.ai;

/**
 * 候选边界解析结果（成功携带 proposal，失败携带错误键与详情）。
 *
 * @author Harriol
 */
public record BoundaryParseResult(BoundaryProposal proposal, String errorKey, String errorDetail) {

    public boolean isOk() {
        return proposal != null;
    }

    public static BoundaryParseResult ok(BoundaryProposal proposal) {
        return new BoundaryParseResult(proposal, null, null);
    }

    public static BoundaryParseResult failure(String errorKey, String errorDetail) {
        return new BoundaryParseResult(null, errorKey, errorDetail);
    }
}
