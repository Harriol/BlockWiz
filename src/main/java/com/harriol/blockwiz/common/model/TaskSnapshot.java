package com.harriol.blockwiz.common.model;

/**
 * 当前任务不可变快照（状态机骨架）。持久化到 blockwiz_tasks.json 属 Sprint 6。
 *
 * @author Harriol
 */
public record TaskSnapshot(
        String taskId,
        String description,
        TaskState state,
        Box range,
        PendingConfirm pendingConfirm,
        TaskState resumeTo,
        long startedAtMs) {

    /** /blockwiz range 创建的"范围骨架任务"描述标记，等待自然语言描述接管。 */
    public static final String RANGE_SKELETON_DESCRIPTION = "手动指定范围";

    public TaskSnapshot withState(TaskState newState) {
        return new TaskSnapshot(taskId, description, newState, range, pendingConfirm, resumeTo, startedAtMs);
    }

    public TaskSnapshot withDescription(String newDescription) {
        return new TaskSnapshot(taskId, newDescription, state, range, pendingConfirm, resumeTo, startedAtMs);
    }

    /**
     * 是否为"范围骨架任务"：由 /blockwiz range 创建、范围已设定但尚无自然语言描述，
     * 可被后续 /blockwiz <描述> 接管（复用玩家指定范围，不触发单任务拒绝）。
     *
     * @return 是骨架任务返回 true
     */
    public boolean isRangeSkeleton() {
        return RANGE_SKELETON_DESCRIPTION.equals(description)
                && range != null
                && state == TaskState.PLANNING
                && pendingConfirm == null;
    }

    public TaskSnapshot withRange(Box newRange) {
        return new TaskSnapshot(taskId, description, state, newRange, pendingConfirm, resumeTo, startedAtMs);
    }

    public TaskSnapshot withPendingConfirm(PendingConfirm newPending) {
        return new TaskSnapshot(taskId, description, state, range, newPending, resumeTo, startedAtMs);
    }

    public TaskSnapshot withResumeTo(TaskState newResumeTo) {
        return new TaskSnapshot(taskId, description, state, range, pendingConfirm, newResumeTo, startedAtMs);
    }
}
