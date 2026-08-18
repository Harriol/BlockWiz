package com.harriol.blockwiz.server.command;

import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.config.ConfigHolder;
import com.harriol.blockwiz.common.i18n.I18n;
import com.harriol.blockwiz.common.i18n.Keys;
import com.harriol.blockwiz.server.ai.AiClient;
import com.harriol.blockwiz.server.ai.ConnectionTestResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * /blockwiz 命令树。Sprint 1 提供 test 与占位子命令；
 * 范围/确认/取消/暂停/恢复/撤销/状态由后续 Sprint 落地。
 *
 * @author Harriol
 */
public final class BlockWizCommand {

    private BlockWizCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(BlockWizCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         net.minecraft.commands.CommandBuildContext context,
                                         net.minecraft.commands.Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("blockwiz")
                /* V1.0 仅支持单人/局域网（集成服务器）；专用服务器直接禁用。 */
                .requires(source -> !source.getServer().isDedicatedServer())
                .then(Commands.literal("test").executes(ctx -> runTest(ctx.getSource())))
                .then(Commands.literal("confirm").executes(ctx -> placeholder(ctx.getSource(), "Sprint 3")))
                .then(Commands.literal("cancel").executes(ctx -> placeholder(ctx.getSource(), "Sprint 2")))
                .then(Commands.literal("pause").executes(ctx -> placeholder(ctx.getSource(), "Sprint 2")))
                .then(Commands.literal("resume").executes(ctx -> placeholder(ctx.getSource(), "Sprint 2")))
                .then(Commands.literal("undo").executes(ctx -> placeholder(ctx.getSource(), "Sprint 4")))
                .then(Commands.literal("status").executes(ctx -> placeholder(ctx.getSource(), "Sprint 2")))
                .then(Commands.literal("preview").executes(ctx -> placeholder(ctx.getSource(), "Sprint 3")))
                .then(Commands.literal("range").executes(ctx -> placeholder(ctx.getSource(), "Sprint 2")))
                .then(Commands.argument("description", StringArgumentType.greedyString())
                        .executes(ctx -> placeholder(ctx.getSource(), "Sprint 3")))
                .executes(ctx -> usage(ctx.getSource())));
    }

    private static int runTest(CommandSourceStack source) {
        ConfigData config = ConfigHolder.get();
        if (!ConfigHolder.isValid()) {
            send(source, I18n.get(Keys.COMMAND_TEST_NOT_CONFIGURED));
            return 1;
        }
        send(source, I18n.get(Keys.COMMAND_TEST_RUNNING));
        AiClient client = new AiClient();
        client.testConnectionAsync(config).whenComplete((result, error) -> {
            MinecraftServer server = source.getServer();
            server.execute(() -> {
                if (result == null) {
                    send(source, I18n.get(Keys.COMMAND_TEST_FAIL));
                    return;
                }
                if (result.ok()) {
                    send(source, I18n.format(Keys.COMMAND_TEST_OK, result.statusCode(), result.durationMs()));
                } else {
                    String category = I18n.get(result.categoryKey());
                    int status = result.statusCode() > 0 ? result.statusCode() : -1;
                    send(source, I18n.format(Keys.COMMAND_TEST_FAIL, category, status, result.durationMs()));
                }
            });
        });
        return 1;
    }

    private static int placeholder(CommandSourceStack source, String sprint) {
        send(source, I18n.format(Keys.COMMAND_PLACEHOLDER_SPRINT, sprint));
        return 1;
    }

    private static int usage(CommandSourceStack source) {
        send(source, I18n.get(Keys.COMMAND_USAGE));
        return 1;
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[BlockWiz] " + message), false);
    }
}
