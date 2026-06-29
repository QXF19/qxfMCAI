package cn.qxf.mcai.server;

import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.AiCompanionEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class McAiCommands {
    private McAiCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mcai")
            .executes(ctx -> help(ctx.getSource()))
            .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("summon").executes(ctx -> summon(ctx.getSource())))
            .then(Commands.literal("follow").executes(ctx -> mode(ctx.getSource(), AiCompanionEntity.Mode.FOLLOW)))
            .then(Commands.literal("stay").executes(ctx -> mode(ctx.getSource(), AiCompanionEntity.Mode.STAY)))
            .then(Commands.literal("guard").executes(ctx -> mode(ctx.getSource(), AiCompanionEntity.Mode.GUARD)))
            .then(Commands.literal("gather").executes(ctx -> mode(ctx.getSource(), AiCompanionEntity.Mode.GATHER)))
            .then(Commands.literal("come").executes(ctx -> come(ctx.getSource())))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("ask")
                .then(Commands.argument("内容", StringArgumentType.greedyString())
                    .executes(ctx -> ask(ctx.getSource(), StringArgumentType.getString(ctx, "内容")))))
            .then(Commands.literal("skin")
                .then(Commands.argument("PNG文件名", StringArgumentType.word())
                    .executes(ctx -> skin(ctx.getSource(), StringArgumentType.getString(ctx, "PNG文件名")))))
            .then(Commands.literal("invincible").requires(source -> source.hasPermission(4))
                .then(Commands.argument("开启", BoolArgumentType.bool())
                    .executes(ctx -> invincible(ctx.getSource(), BoolArgumentType.getBool(ctx, "开启")))))
            .then(Commands.literal("provider").requires(source -> source.hasPermission(4))
                .then(Commands.argument("提供商", StringArgumentType.word())
                    .executes(ctx -> provider(ctx.getSource(), StringArgumentType.getString(ctx, "提供商")))))
            .then(Commands.literal("model").requires(source -> source.hasPermission(4))
                .then(Commands.argument("模型名", StringArgumentType.greedyString())
                    .executes(ctx -> model(ctx.getSource(), StringArgumentType.getString(ctx, "模型名")))))
        );
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("qxfMCAI：/mcai summon|follow|stay|guard|gather|come|ask|skin|status").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("聊天也可用：@小麦 你的要求。按 M 打开中文控制菜单。"), false);
        source.sendSuccess(() -> Component.literal("OP 4：/mcai invincible、provider、model").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int summon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CompanionManager.summon(player);
        source.sendSuccess(() -> Component.literal("[小麦] 我来啦！按 M 可以打开控制菜单。").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int mode(CommandSourceStack source, AiCompanionEntity.Mode mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CompanionManager.setMode(player, mode);
        source.sendSuccess(() -> Component.literal("[小麦] 已切换为：" + mode.chinese).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int come(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        CompanionManager.come(player, companion);
        source.sendSuccess(() -> Component.literal("[小麦] 到你身边啦。").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int ask(CommandSourceStack source, String text) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiService.ask(source.getPlayerOrException(), text, false);
        return 1;
    }

    private static int skin(CommandSourceStack source, String fileName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!fileName.matches("[A-Za-z0-9._-]+\\.png")) {
            source.sendFailure(Component.literal("皮肤文件名必须以 .png 结尾，且只能包含字母、数字、点、下划线和短横线。"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        companion.setSkinName(fileName);
        source.sendSuccess(() -> Component.literal("皮肤已切换为 " + fileName + "。若未刷新，请重新进入世界。"), false);
        return 1;
    }

    private static int invincible(CommandSourceStack source, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        companion.setCompanionInvincible(enabled);
        source.sendSuccess(() -> Component.literal("伙伴无敌模式：" + (enabled ? "开启" : "关闭")).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int provider(CommandSourceStack source, String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("openai") && !normalized.equals("deepseek") && !normalized.equals("custom")) {
            source.sendFailure(Component.literal("提供商只能是 openai、deepseek 或 custom。"));
            return 0;
        }
        McAiConfig.setProvider(normalized);
        source.sendSuccess(() -> Component.literal("AI提供商已切换为 " + normalized + "。"), true);
        return 1;
    }

    private static int model(CommandSourceStack source, String value) {
        if (value.isBlank() || value.length() > 128) {
            source.sendFailure(Component.literal("模型名不能为空且最多128个字符。"));
            return 0;
        }
        McAiConfig.setModel(value.trim());
        source.sendSuccess(() -> Component.literal("当前模型已设置为 " + value.trim() + "。"), true);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        String entity = companion == null ? "未召唤" : companion.getMode().chinese + (companion.isCompanionInvincible() ? " / 无敌" : "");
        source.sendSuccess(() -> Component.literal("伙伴=" + entity + "，提供商=" + McAiConfig.provider() + "，模型=" + McAiConfig.model()
            + "，API=" + (AiService.isConfigured() ? "已配置" : "未配置")), false);
        return 1;
    }
}

