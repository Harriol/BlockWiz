package com.harriol.blockwiz.common.model;

import java.util.UUID;

/**
 * 确认门：一次性 token + 可选截止时间（PRD §5.5 超时语义统一）。
 * Sprint 2 的范围确认无强制超时（NO_DEADLINE），阶段/拆除确认由后续 Sprint 使用截止时间。
 *
 * @author Harriol
 */
public record PendingConfirm(ConfirmType type, String token, long deadlineMs, String summary) {

    /** 无截止时间标记。 */
    public static final long NO_DEADLINE = -1L;

    /** 创建无超时的确认门（自动生成一次性 token）。 */
    public static PendingConfirm of(ConfirmType type, String summary) {
        return new PendingConfirm(type, UUID.randomUUID().toString(), NO_DEADLINE, summary);
    }

    /** 创建带截止时间（毫秒时间戳）的确认门。 */
    public static PendingConfirm withDeadline(ConfirmType type, String summary, long deadlineMs) {
        return new PendingConfirm(type, UUID.randomUUID().toString(), deadlineMs, summary);
    }

    /** 是否带截止时间。 */
    public boolean hasDeadline() {
        return deadlineMs >= 0;
    }

    /**
     * 是否已过期（无截止时间时永不过期）。
     *
     * @param nowMillis 当前时间戳
     * @return 已过期返回 true
     */
    public boolean isExpired(long nowMillis) {
        return hasDeadline() && nowMillis > deadlineMs;
    }

    /** token 是否匹配（空 token 永不匹配）。 */
    public boolean matches(String candidate) {
        return candidate != null && candidate.equals(token);
    }
}
