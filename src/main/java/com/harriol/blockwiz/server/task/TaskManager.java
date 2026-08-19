package com.harriol.blockwiz.server.task;

import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.config.ConfigHolder;
import com.harriol.blockwiz.common.i18n.I18n;
import com.harriol.blockwiz.common.i18n.Keys;
import com.harriol.blockwiz.common.model.Box;
import com.harriol.blockwiz.common.model.ConfirmType;
import com.harriol.blockwiz.common.model.PendingConfirm;
import com.harriol.blockwiz.common.model.ScanSummary;
import com.harriol.blockwiz.common.model.TaskSnapshot;
import com.harriol.blockwiz.common.model.TaskState;
import com.harriol.blockwiz.common.model.TaskStateMachine;
import com.harriol.blockwiz.server.ai.BoundaryProposal;
import com.harriol.blockwiz.server.ai.BoundaryProposalResult;
import com.harriol.blockwiz.server.ai.BoundaryService;
import com.harriol.blockwiz.server.ai.HeuristicBoundaryProposer;
import com.harriol.blockwiz.server.command.Feedback;
import com.harriol.blockwiz.server.scan.WorldScanner;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 全局任务管理器（Sprint 2 状态机骨架）。
 * 同一时刻仅一个活动任务（PRD §5.5）；所有状态变更发生在服务端主线程
 * （命令执行或 END_SERVER_TICK），AI 结果经线程安全队列回传。
 *
 * @author Harriol
 */
public final class TaskManager {

    /** 可暂停的状态。 */
    private static final Set<TaskState> PAUSABLE = Set.of(
            TaskState.PENDING_CONFIRM, TaskState.PLANNING, TaskState.EXECUTING, TaskState.VERIFYING);

    /** AI 候选边界结果队列（daemon 线程写入，主线程 tick 消费）。 */
    private static final ConcurrentLinkedQueue<BoundaryProposalResult> PROPOSAL_RESULTS = new ConcurrentLinkedQueue<>();

    private static volatile TaskSnapshot current;
    private static volatile WorldScanner.ScanSession activeScan;
    private static volatile ScanSummary lastScanSummary;
    private static volatile BoundaryProposal pendingProposal;
    private static volatile ConfigData taskConfig;
    private static volatile ServerPlayer operator;
    private static volatile boolean proposalRequested;

    private TaskManager() {
    }

    /**
     * 主线程 tick：推进扫描分片、续接 AI 规划、消费 AI 结果。
     * 由 {@code ServerTickEvents.END_SERVER_TICK} 调用。
     */
    public static void tick() {
        if (activeScan != null) {
            boolean done = activeScan.tick();
            if (done) {
                activeScan = null;
            }
        }
        if (current != null && current.state() == TaskState.PLANNING
                && activeScan == null && lastScanSummary != null && !proposalRequested) {
            requestProposal();
        }
        if (current != null && current.state() == TaskState.PLANNING) {
            BoundaryProposalResult result;
            while ((result = PROPOSAL_RESULTS.poll()) != null) {
                applyProposal(result);
                if (current == null || current.state() != TaskState.PLANNING) {
                    break;
                }
            }
        }
    }

    /** 自然语言描述入口（/blockwiz <描述>）：扫描 → AI 候选边界 → 确认。 */
    public static void handleDescription(CommandSourceStack source, String description) {
        if (!guardHost(source)) {
            return;
        }
        if (!ensureNoActiveTask(source)) {
            return;
        }
        if (!ConfigHolder.isValid()) {
            Feedback.sendKey(source, Keys.COMMAND_TEST_NOT_CONFIGURED);
            return;
        }
        ServerPlayer player = source.getPlayer();
        taskConfig = ConfigHolder.get();
        resetTaskState();
        current = new TaskSnapshot(UUID.randomUUID().toString(), description,
                TaskState.PLANNING, null, null, null, System.currentTimeMillis());
        String facing = Direction.fromYRot(player.getYRot()).name();
        int radius = taskConfig.getScanRadius();
        activeScan = WorldScanner.start(source.getLevel(), player.blockPosition(), radius, facing);
        activeScan.onComplete(TaskManager::onScanComplete);
        Feedback.sendKey(source, Keys.COMMAND_SCAN_START, radius);
    }

    /** 手动范围：中心 + X/Y/Z 尺寸（/blockwiz range sx sy sz）。 */
    public static void handleManualRange(CommandSourceStack source, int sizeX, int sizeY, int sizeZ) {
        if (!guardHost(source)) {
            return;
        }
        ServerPlayer player = source.getPlayer();
        Box box = Box.fromCenterAndSize(
                new com.harriol.blockwiz.common.model.Pos(player.blockPosition().getX(),
                        player.blockPosition().getY(), player.blockPosition().getZ()),
                sizeX, sizeY, sizeZ);
        setManualRange(source, box);
    }

    /** 手动范围：两个角点（/blockwiz range x1 y1 z1 x2 y2 z2）。 */
    public static void handleManualRangeCorners(CommandSourceStack source,
                                                int x1, int y1, int z1, int x2, int y2, int z2) {
        if (!guardHost(source)) {
            return;
        }
        setManualRange(source, Box.fromCorners(x1, y1, z1, x2, y2, z2));
    }

    public static void handleConfirm(CommandSourceStack source) {
        if (!guardHost(source)) {
            return;
        }
        if (current == null) {
            Feedback.sendKey(source, Keys.COMMAND_NO_TASK);
            return;
        }
        PendingConfirm pending = current.pendingConfirm();
        if (pending == null || current.state() != TaskState.PENDING_CONFIRM || pendingProposal == null) {
            Feedback.sendKey(source, Keys.COMMAND_CONFIRM_NONE);
            return;
        }
        if (pending.isExpired(System.currentTimeMillis())) {
            Feedback.sendKey(source, Keys.COMMAND_CONFIRM_NONE);
            return;
        }
        Box range = pendingProposal.range();
        TaskStateMachine.requireTransition(current.state(), TaskState.PLANNING);
        current = current.withRange(range).withPendingConfirm(null).withState(TaskState.PLANNING);
        pendingProposal = null;
        Feedback.sendKey(source, Keys.COMMAND_CONFIRM_OK, I18n.get(Keys.CONFIRM_TYPE_RANGE));
        Feedback.sendKey(source, Keys.COMMAND_PLANNING_STUB);
    }

    public static void handleCancel(CommandSourceStack source) {
        if (!guardHost(source)) {
            return;
        }
        if (current == null) {
            Feedback.sendKey(source, Keys.COMMAND_NO_TASK);
            return;
        }
        if (isTerminal(current.state())) {
            current = null;
            Feedback.sendKey(source, Keys.COMMAND_NO_TASK);
            return;
        }
        TaskStateMachine.requireTransition(current.state(), TaskState.CANCELLED);
        current = null;
        resetTaskState();
        Feedback.sendKey(source, Keys.COMMAND_CANCEL_OK);
    }

    public static void handlePause(CommandSourceStack source) {
        if (!guardHost(source)) {
            return;
        }
        if (current == null) {
            Feedback.sendKey(source, Keys.COMMAND_NO_TASK);
            return;
        }
        if (current.state() == TaskState.PAUSED || !PAUSABLE.contains(current.state())) {
            Feedback.sendKey(source, Keys.COMMAND_PAUSE_INVALID);
            return;
        }
        TaskState resumeTo = current.state();
        TaskStateMachine.requireTransition(current.state(), TaskState.PAUSED);
        current = current.withState(TaskState.PAUSED).withResumeTo(resumeTo);
        Feedback.sendKey(source, Keys.COMMAND_PAUSE_OK);
    }

    public static void handleResume(CommandSourceStack source) {
        if (!guardHost(source)) {
            return;
        }
        if (current == null || current.state() != TaskState.PAUSED) {
            Feedback.sendKey(source, Keys.COMMAND_RESUME_INVALID);
            return;
        }
        TaskState target = current.resumeTo() != null ? current.resumeTo() : TaskState.PLANNING;
        TaskStateMachine.requireTransition(TaskState.PAUSED, target);
        current = current.withState(target).withResumeTo(null);
        Feedback.sendKey(source, Keys.COMMAND_RESUME_OK);
        if (target == TaskState.PLANNING) {
            Feedback.sendKey(source, Keys.COMMAND_PLANNING_STUB);
        }
    }

    public static void handleStatus(CommandSourceStack source) {
        if (!guardHost(source)) {
            return;
        }
        if (current == null) {
            Feedback.sendKey(source, Keys.COMMAND_STATUS_NONE);
            Feedback.sendKey(source, Keys.COMMAND_STATUS_RECORDS_PENDING);
            return;
        }
        Feedback.sendKey(source, Keys.COMMAND_STATUS_HEADER);
        Feedback.sendKey(source, Keys.COMMAND_STATUS_STATE, I18n.get(current.state().labelKey()));
        Feedback.sendKey(source, Keys.COMMAND_STATUS_DESCRIPTION, current.description());
        Feedback.sendKey(source, Keys.COMMAND_STATUS_RANGE,
                current.range() == null ? I18n.get(Keys.COMMAND_STATUS_RANGE_NONE) : current.range().toString());
        Feedback.sendKey(source, Keys.COMMAND_STATUS_PENDING,
                current.pendingConfirm() == null
                        ? I18n.get(Keys.COMMAND_STATUS_PENDING_NONE)
                        : current.pendingConfirm().summary());
    }

    private static void setManualRange(CommandSourceStack source, Box box) {
        if (!box.isWithinLimits()) {
            Feedback.sendKey(source, Keys.COMMAND_RANGE_INVALID, box);
            Feedback.sendKey(source, Keys.COMMAND_RANGE_LIMIT);
            return;
        }
        if (!ensureNoActiveTask(source)) {
            return;
        }
        if (!isBoxLoaded(source.getLevel(), box)) {
            Feedback.sendKey(source, Keys.COMMAND_RANGE_UNLOADED);
            return;
        }
        resetTaskState();
        current = new TaskSnapshot(UUID.randomUUID().toString(), "手动指定范围",
                TaskState.PLANNING, box, null, null, System.currentTimeMillis());
        Feedback.sendKey(source, Keys.COMMAND_RANGE_SET, box);
        Feedback.sendKey(source, Keys.COMMAND_PLANNING_STUB);
    }

    private static void onScanComplete(ScanSummary summary) {
        if (current == null || lastScanSummary != null) {
            return;
        }
        lastScanSummary = summary;
        if (current.state() != TaskState.PLANNING) {
            // 暂停期间扫描完成：不发起 AI 请求，恢复时由 tick() 续接
            return;
        }
        Feedback.sendKey(operatorSource(), Keys.COMMAND_SCAN_DONE,
                summary.nonAirCount(),
                summary.blockBounds() == null ? "-" : summary.blockBounds());
        requestProposal();
    }

    private static void requestProposal() {
        if (current == null || lastScanSummary == null || proposalRequested) {
            return;
        }
        proposalRequested = true;
        Feedback.sendKey(operatorSource(), Keys.COMMAND_PROPOSE_START);
        BoundaryService.proposeAsync(taskConfig, lastScanSummary, current.description())
                .whenComplete((result, error) -> {
                    if (error != null) {
                        PROPOSAL_RESULTS.add(BoundaryProposalResult.failure(
                                Keys.TEST_CATEGORY_NETWORK, error.getClass().getSimpleName()));
                    } else {
                        PROPOSAL_RESULTS.add(result);
                    }
                });
    }

    private static void applyProposal(BoundaryProposalResult result) {
        if (current == null || current.state() != TaskState.PLANNING || lastScanSummary == null) {
            return;
        }
        proposalRequested = false;
        BoundaryProposal proposal;
        if (result.ok()) {
            proposal = result.proposal();
        } else {
            Feedback.sendKey(operatorSource(), Keys.COMMAND_PROPOSE_FAIL, I18n.get(result.errorKey()));
            proposal = HeuristicBoundaryProposer.propose(lastScanSummary);
        }
        pendingProposal = proposal;
        current = current.withState(TaskState.PENDING_CONFIRM)
                .withPendingConfirm(PendingConfirm.of(ConfirmType.RANGE, proposal.range().toString()));
        Feedback.sendKey(operatorSource(), Keys.COMMAND_CANDIDATE_SHOW,
                proposal.range(), originLabel(proposal.origin()), reasonLabel(proposal));
    }

    private static String originLabel(String origin) {
        if (origin == null) {
            return I18n.get(Keys.ORIGIN_AI);
        }
        return switch (origin) {
            case "HEURISTIC" -> I18n.get(Keys.ORIGIN_HEURISTIC);
            case "PLAYER" -> I18n.get(Keys.ORIGIN_PLAYER);
            default -> I18n.get(Keys.ORIGIN_AI);
        };
    }

    private static String reasonLabel(BoundaryProposal proposal) {
        if ("HEURISTIC".equals(proposal.origin())) {
            return I18n.get(Keys.COMMAND_CANDIDATE_HEURISTIC_REASON);
        }
        return proposal.reasoning() == null || proposal.reasoning().isBlank()
                ? "-" : proposal.reasoning();
    }

    /** 8 个角点 + 中心点落在已加载区块内才算可执行（PRD §5.2）。 */
    private static boolean isBoxLoaded(net.minecraft.server.level.ServerLevel level, Box box) {
        int[] xs = {box.min().x(), box.max().x()};
        int[] ys = {box.min().y(), box.max().y()};
        int[] zs = {box.min().z(), box.max().z()};
        for (int x : xs) {
            for (int y : ys) {
                for (int z : zs) {
                    if (!level.isLoaded(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return level.isLoaded(new BlockPos(
                (box.min().x() + box.max().x()) / 2,
                (box.min().y() + box.max().y()) / 2,
                (box.min().z() + box.max().z()) / 2));
    }

    private static boolean ensureNoActiveTask(CommandSourceStack source) {
        if (current != null && !isTerminal(current.state())) {
            Feedback.sendKey(source, Keys.COMMAND_TASK_ACTIVE, I18n.get(current.state().labelKey()));
            return false;
        }
        return true;
    }

    private static boolean isTerminal(TaskState state) {
        return state == TaskState.COMPLETED || state == TaskState.CANCELLED || state == TaskState.FAILED;
    }

    private static void resetTaskState() {
        activeScan = null;
        lastScanSummary = null;
        pendingProposal = null;
        taskConfig = null;
        proposalRequested = false;
        PROPOSAL_RESULTS.clear();
    }

    /** V1.0 仅主机玩家可操作（单人/局域网集成服务器所有者）。 */
    private static boolean guardHost(CommandSourceStack source) {
        if (!source.isPlayer()) {
            Feedback.sendKey(source, Keys.COMMAND_HOST_ONLY);
            return false;
        }
        ServerPlayer player = source.getPlayer();
        MinecraftServer server = source.getServer();
        if (server == null || server.isDedicatedServer()) {
            Feedback.sendKey(source, Keys.COMMAND_HOST_ONLY);
            return false;
        }
        GameProfile owner = server.getSingleplayerProfile();
        if (owner == null || !server.isSingleplayerOwner(new NameAndId(player.getGameProfile()))) {
            Feedback.sendKey(source, Keys.COMMAND_HOST_ONLY);
            return false;
        }
        operator = player;
        return true;
    }

    private static CommandSourceStack operatorSource() {
        ServerPlayer player = operator;
        if (player == null) {
            throw new IllegalStateException("任务操作者未初始化");
        }
        return player.createCommandSourceStack();
    }
}
