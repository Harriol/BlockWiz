package com.harriol.blockwiz.common.model;

/**
 * 任务状态机迁移表（PRD §5.2 状态流转）。
 * 纯逻辑、可单测；TaskManager 在服务端主线程调用。
 *
 * @author Harriol
 */
public final class TaskStateMachine {

    private TaskStateMachine() {
    }

    /**
     * 状态迁移是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 合法返回 true
     */
    public static boolean canTransition(TaskState from, TaskState to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return switch (from) {
            case PENDING_CONFIRM -> to == TaskState.PLANNING
                    || to == TaskState.PAUSED
                    || to == TaskState.CANCELLED
                    || to == TaskState.FAILED;
            case PLANNING -> to == TaskState.EXECUTING
                    || to == TaskState.PAUSED
                    || to == TaskState.CANCELLED
                    || to == TaskState.FAILED;
            case EXECUTING -> to == TaskState.VERIFYING
                    || to == TaskState.PAUSED
                    || to == TaskState.CANCELLED
                    || to == TaskState.FAILED;
            case VERIFYING -> to == TaskState.EXECUTING
                    || to == TaskState.COMPLETED
                    || to == TaskState.PAUSED
                    || to == TaskState.CANCELLED
                    || to == TaskState.FAILED;
            case PAUSED -> to == TaskState.PENDING_CONFIRM
                    || to == TaskState.PLANNING
                    || to == TaskState.EXECUTING
                    || to == TaskState.VERIFYING
                    || to == TaskState.CANCELLED
                    || to == TaskState.FAILED;
            case COMPLETED, CANCELLED, FAILED -> false;
        };
    }

    /**
     * 执行状态迁移；非法迁移抛异常（调用方保证在主线程）。
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void requireTransition(TaskState from, TaskState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("非法任务状态迁移: " + from + " -> " + to);
        }
    }
}
