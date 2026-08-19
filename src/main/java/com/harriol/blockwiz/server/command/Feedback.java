package com.harriol.blockwiz.server.command;

import com.harriol.blockwiz.common.i18n.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * 聊天反馈统一出口：前缀 [BlockWiz] + i18n 文案。
 *
 * @author Harriol
 */
public final class Feedback {

    private static final String PREFIX = "[BlockWiz] ";

    private Feedback() {
    }

    /** 发送原文消息。 */
    public static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(PREFIX + message), false);
    }

    /** 发送 i18n 键文案（支持 {0}/{1} 占位符）。 */
    public static void sendKey(CommandSourceStack source, String key, Object... args) {
        send(source, I18n.format(key, args));
    }
}
