package com.harriol.blockwiz.common.model;

/**
 * 任务状态（PRD §5.5 状态清单）。
 *
 * @author Harriol
 */
public enum TaskState {
    /** 待确认（范围/方案/阶段/拆除）。 */
    PENDING_CONFIRM,
    /** 规划中（扫描、AI 规划）。 */
    PLANNING,
    /** 执行中（分批修改）。 */
    EXECUTING,
    /** 复检中（阶段复检）。 */
    VERIFYING,
    /** 已暂停（玩家暂停、确认超时、API 失败、关键偏差）。 */
    PAUSED,
    /** 已完成。 */
    COMPLETED,
    /** 已取消。 */
    CANCELLED,
    /** 失败。 */
    FAILED;

    /**
     * i18n 标签键：state.pendingConfirm / state.planning / ...
     * 枚举名（PENDING_CONFIRM）需转换为语言文件中的驼峰键（pendingConfirm）。
     *
     * @return 翻译键
     */
    public String labelKey() {
        String[] parts = name().toLowerCase().split("_");
        StringBuilder key = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            key.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return "state." + key;
    }
}
