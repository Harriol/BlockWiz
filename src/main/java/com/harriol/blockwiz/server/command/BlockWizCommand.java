package com.harriol.blockwiz.server.command;

import com.harriol.blockwiz.common.config.ConfigData;
import com.harriol.blockwiz.common.config.ConfigHolder;
import com.harriol.blockwiz.common.i18n.I18n;
import com.harriol.blockwiz.common.i18n.Keys;
import com.harriol.blockwiz.server.ai.AiClient;
import com.harriol.blockwiz.server.ai.ConnectionTestResult;
import com.harriol.blockwiz.server.task.TaskManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * /blockwiz 命令树（Sprint 2：交互基础）。
 * 已落地：test / range（两种形式）/ confirm / cancel / pause / resume / status / 自然语言描述入口；
 * undo（Sprint 4）、preview（Sprint 3）保持占位。
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
                /* V1.0 仅支持单人/局域网（集成服务器）；专用服务器直接禁用。
                   requires 谓词会在命令树序列化时被调用（此时 getServer() 可能为 null），必须 null 安全。 */
                .requires(source -> source.getServer() == null || !source.getServer().isDedicatedServer())
                .then(Commands.literal("test").executes(ctx -> runTest(ctx.getSource())))
                .then(Commands.literal("confirm").executes(ctx -> {
                    TaskManager.handleConfirm(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    TaskManager.handleCancel(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("pause").executes(ctx -> {
                    TaskManager.handlePause(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("resume").executes(ctx -> {
                    TaskManager.handleResume(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    TaskManager.handleStatus(ctx.getSource());
                    return 1;
                }))
                .then(Commands.literal("undo").executes(ctx -> placeholder(ctx.getSource(), "Sprint 4")))
                .then(Commands.literal("preview").executes(ctx -> placeholder(ctx.getSource(), "Sprint 3")))
                .then(Commands.literal("range")
                        .executes(ctx -> {
                            Feedback.sendKey(ctx.getSource(), Keys.COMMAND_RANGE_HELP);
                            return 1;
                        })
                        .then(Commands.argument("sx", IntegerArgumentType.integer())
                                .then(Commands.argument("sy", IntegerArgumentType.integer())
                                        .then(Commands.argument("sz", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    TaskManager.handleManualRange(ctx.getSource(),
                                                            IntegerArgumentType.getInteger(ctx, "sx"),
                                                            IntegerArgumentType.getInteger(ctx, "sy"),
                                                            IntegerArgumentType.getInteger(ctx, "sz"));
                                                    return 1;
                                                })))))
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                        .executes(ctx -> {
                                                                            TaskManager.handleManualRangeCorners(ctx.getSource(),
                                                                                    IntegerArgumentType.getInteger(ctx, "x1"),
                                                                                    IntegerArgumentType.getInteger(ctx, "y1"),
                                                                                    IntegerArgumentType.getInteger(ctx, "z1"),
                                                                                    IntegerArgumentType.getInteger(ctx, "x2"),
                                                                                    IntegerArgumentType.getInteger(ctx, "y2"),
                                                                                    IntegerArgumentType.getInteger(ctx, "z2"));
                                                                            return 1;
                                                                        })))))))
                .then(Commands.argument("description", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            TaskManager.handleDescription(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "description"));
                            return 1;
                        }))
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
        Feedback.sendKey(source, Keys.COMMAND_PLACEHOLDER_SPRINT, sprint);
        return 1;
    }

    private static int usage(CommandSourceStack source) {
        Feedback.sendKey(source, Keys.COMMAND_USAGE);
        return 1;
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[BlockWiz] " + message), false);
    }
}
