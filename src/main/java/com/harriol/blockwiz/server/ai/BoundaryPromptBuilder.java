package com.harriol.blockwiz.server.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.harriol.blockwiz.common.model.BlockCount;
import com.harriol.blockwiz.common.model.ScanSummary;

import java.util.List;

/**
 * 候选边界提示词构建：system 固定约束 + user 携带描述与扫描摘要。
 *
 * @author Harriol
 */
public final class BoundaryPromptBuilder {

    /** 固定系统提示词（边界识别专用，范围上限与 PRD 一致）。 */
    public static final String SYSTEM_PROMPT = "你是 Minecraft 建造助手 BlockWiz 的边界识别器。"
            + "玩家会给出建造或拆除的自然语言描述，并附带环境扫描摘要。"
            + "请只输出一个 JSON 对象，不要输出任何解释文字。JSON 结构："
            + "{\"range\":{\"min\":{\"x\":0,\"y\":0,\"z\":0},\"max\":{\"x\":0,\"y\":0,\"z\":0}},"
            + "\"origin\":\"AI_PROPOSED\",\"reasoning\":\"一句话说明\",\"keep\":[\"保留项描述\"]}。"
            + "硬性约束：坐标必须是整数；range 任一边长 ≤64 且体积 ≤262144（64×64×64 封顶）；"
            + "范围必须尽量落在扫描范围内或玩家附近；keep 列出玩家明确要求保留的结构或区域。"
            + "无法确定时给出最合理的候选范围并说明理由，不要返回空范围。";

    private BoundaryPromptBuilder() {
    }

    /**
     * 构建 system + user 两条消息。
     *
     * @param summary     扫描摘要
     * @param description 玩家自然语言描述
     * @return OpenAI 兼容 messages
     */
    public static List<JsonObject> buildMessages(ScanSummary summary, String description) {
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", SYSTEM_PROMPT);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", buildUserContent(summary, description));
        return List.of(system, user);
    }

    private static String buildUserContent(ScanSummary summary, String description) {
        JsonObject data = new JsonObject();
        data.addProperty("description", description == null ? "" : description);

        JsonObject player = new JsonObject();
        player.addProperty("x", summary.center().x());
        player.addProperty("y", summary.center().y());
        player.addProperty("z", summary.center().z());
        player.addProperty("facing", summary.facing() == null ? "UNKNOWN" : summary.facing());
        data.add("player", player);

        data.addProperty("scanRadius", summary.radius());
        data.addProperty("scannedBlocks", summary.scanned());
        data.addProperty("nonAirBlocks", summary.nonAirCount());

        if (summary.blockBounds() != null) {
            JsonObject bounds = new JsonObject();
            bounds.add("min", posJson(summary.blockBounds().min().x(), summary.blockBounds().min().y(), summary.blockBounds().min().z()));
            bounds.add("max", posJson(summary.blockBounds().max().x(), summary.blockBounds().max().y(), summary.blockBounds().max().z()));
            data.add("blockBounds", bounds);
        }

        JsonArray topBlocks = new JsonArray();
        for (BlockCount count : summary.topBlocks()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", count.id());
            entry.addProperty("count", count.count());
            topBlocks.add(entry);
        }
        data.add("topBlocks", topBlocks);
        return data.toString();
    }

    private static JsonObject posJson(int x, int y, int z) {
        JsonObject pos = new JsonObject();
        pos.addProperty("x", x);
        pos.addProperty("y", y);
        pos.addProperty("z", z);
        return pos;
    }
}
