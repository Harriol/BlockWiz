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

    public TaskSnapshot withState(TaskState newState) {
        return new TaskSnapshot(taskId, description, newState, range, pendingConfirm, resumeTo, startedAtMs);
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
