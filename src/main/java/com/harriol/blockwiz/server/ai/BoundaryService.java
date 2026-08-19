package com.harriol.blockwiz.server.ai;

import com.google.gson.JsonObject;
import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.model.ScanSummary;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 候选边界编排：扫描摘要 → 提示词 → AI 请求 → 解析。
 * AI 不可用（未配置/网络错误/解析失败）时由调用方回退启发式候选。
 *
 * @author Harriol
 */
public final class BoundaryService {

    private static final AiClient CLIENT = new AiClient();

    private BoundaryService() {
    }

    /**
     * 异步请求 AI 提出候选边界。
     *
     * @param config      配置快照（任务启动时）
     * @param summary     扫描摘要
     * @param description 玩家描述
     * @return 未来结果（HTTP/解析失败时 ok=false 并携带错误键）
     */
    public static CompletableFuture<BoundaryProposalResult> proposeAsync(
            ConfigData config, ScanSummary summary, String description) {
        List<JsonObject> messages = BoundaryPromptBuilder.buildMessages(summary, description);
        return CLIENT.chatAsync(config, messages).thenApply(result -> {
            if (!result.ok()) {
                return BoundaryProposalResult.failure(result.categoryKey(), "HTTP " + result.statusCode());
            }
            BoundaryParseResult parsed = BoundaryParser.parse(result.content());
            if (!parsed.isOk()) {
                return BoundaryProposalResult.failure(parsed.errorKey(), parsed.errorDetail());
            }
            return BoundaryProposalResult.ok(parsed.proposal());
        });
    }
}
