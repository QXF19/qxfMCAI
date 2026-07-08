package cn.qxf.mcai.server;

import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.compat.MaidVisualBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

public final class McAiCommands {
    private McAiCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mcai")
            .executes(ctx -> help(ctx.getSource()))
            .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("summon").executes(ctx -> summon(ctx.getSource())))
            .then(modeCommand("follow", AiCompanionEntity.Mode.FOLLOW))
            .then(modeCommand("stay", AiCompanionEntity.Mode.STAY))
            .then(modeCommand("guard", AiCompanionEntity.Mode.GUARD))
            .then(actionCommand("gather"))
            .then(actionCommand("mine"))
            .then(Commands.literal("cave").executes(ctx -> action(ctx.getSource(), "find_cave", 1)))
            .then(actionCommand("explore"))
            .then(actionCommand("patrol"))
            .then(actionCommand("hunt"))
            .then(actionCommand("chop"))
            .then(actionCommand("farm"))
            .then(Commands.literal("build")
                .then(Commands.literal("shelter").executes(ctx -> action(ctx.getSource(), "build_shelter", 1)))
                .then(Commands.literal("house").executes(ctx -> action(ctx.getSource(), "build_house", 1)))
                .then(Commands.literal("bridge").executes(ctx -> action(ctx.getSource(), "build_bridge", 1))))
            .then(Commands.literal("equip")
                .then(Commands.literal("weapon").executes(ctx -> action(ctx.getSource(), "equip_weapon", 1)))
                .then(Commands.literal("pickaxe").executes(ctx -> action(ctx.getSource(), "equip_pickaxe", 1))))
            .then(Commands.literal("come").executes(ctx -> come(ctx.getSource())))
            .then(Commands.literal("inventory").executes(ctx -> inventory(ctx.getSource())))
            .then(Commands.literal("permit")
                .then(Commands.literal("teleport").executes(ctx -> permitTeleport(ctx.getSource()))))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("ask")
                .then(Commands.argument("内容", StringArgumentType.greedyString())
                    .executes(ctx -> ask(ctx.getSource(), StringArgumentType.getString(ctx, "内容")))))
            .then(Commands.literal("task")
                .then(Commands.argument("动作", StringArgumentType.word())
                    .then(Commands.argument("数量", IntegerArgumentType.integer(1, 256))
                        .executes(ctx -> action(ctx.getSource(), StringArgumentType.getString(ctx, "动作"),
                            IntegerArgumentType.getInteger(ctx, "数量"))))))
            .then(Commands.literal("ysm")
                .then(Commands.argument("模型ID", StringArgumentType.word())
                    .then(Commands.argument("材质ID", StringArgumentType.word())
                        .executes(ctx -> ysm(ctx.getSource(), StringArgumentType.getString(ctx, "模型ID"),
                            StringArgumentType.getString(ctx, "材质ID"))))))
            .then(Commands.literal("invincible").requires(source -> source.hasPermission(4))
                .then(Commands.argument("开启", BoolArgumentType.bool())
                    .executes(ctx -> invincible(ctx.getSource(), BoolArgumentType.getBool(ctx, "开启")))))
            .then(Commands.literal("provider").requires(source -> source.hasPermission(4))
                .then(Commands.argument("提供商", StringArgumentType.word())
                    .executes(ctx -> provider(ctx.getSource(), StringArgumentType.getString(ctx, "提供商")))))
            .then(Commands.literal("model").requires(source -> source.hasPermission(4))
                .then(Commands.argument("模型名", StringArgumentType.greedyString())
                    .executes(ctx -> model(ctx.getSource(), StringArgumentType.getString(ctx, "模型名")))))
            .then(Commands.literal("command")
                .then(Commands.argument("最高权限命令", StringArgumentType.greedyString())
                    .executes(ctx -> highestCommand(ctx.getSource(),
                        StringArgumentType.getString(ctx, "最高权限命令")))))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> modeCommand(
        String name, AiCompanionEntity.Mode mode) {
        return Commands.literal(name).executes(ctx -> mode(ctx.getSource(), mode));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> actionCommand(String action) {
        return Commands.literal(action).executes(ctx -> action(ctx.getSource(), action, 1));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("qxfMCAI v6：任务先执行、聊天后返回；工具位于隐藏装备仓。按 M 打开控制中心。")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("常用：summon、inventory、mine、cave、chop、farm、hunt、explore、build house、permit teleport、ask"), false);
        source.sendSuccess(() -> Component.literal("聊天：@龙龙 你的要求；Shift+右键龙龙也可打开27格背包。"), false);
        source.sendSuccess(() -> Component.literal("v6 固定提供 OP4 命令源；只应在私人且已备份的世界使用。")
            .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int summon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompanionManager.summon(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("[龙龙] 我来啦！这次会和你一起真正生活、工作和成长。")
            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int mode(CommandSourceStack source, AiCompanionEntity.Mode mode)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompanionManager.setMode(source.getPlayerOrException(), mode);
        source.sendSuccess(() -> Component.literal("[龙龙] 已切换为：" + mode.chinese).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int action(CommandSourceStack source, String type, int count)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        companion.enqueueAction(new AgentAction(type, "", count, "", ""));
        source.sendSuccess(() -> Component.literal("[龙龙] 任务已立即启动或进入连续队列：" + type + " × " + count), false);
        return 1;
    }

    private static int come(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        CompanionManager.come(player, companion);
        companion.speak("我在这里～", "happy");
        return 1;
    }

    private static int inventory(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null) companion = CompanionManager.summon(player);
        companion.openInventory(player);
        return 1;
    }

    private static int permitTeleport(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        if (companion == null || !companion.teleportOwnerWithPermission(player)) {
            source.sendFailure(Component.literal("龙龙当前没有等待批准的同维度传送请求。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已批准传送，正在前往龙龙发现的位置。"), false);
        return 1;
    }

    private static int ask(CommandSourceStack source, String text) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiService.ask(source.getPlayerOrException(), text, false);
        return 1;
    }

    private static int ysm(CommandSourceStack source, String modelId, String textureId)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        java.nio.file.Path file = McAiConfig.ysmModelDirectory().resolve(modelId + ".ysm");
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                byte[] header = input.readNBytes(16);
                String prefix = new String(header, 0, Math.min(header.length, 16), StandardCharsets.UTF_8);
                if (prefix.contains("YSGP")) {
                    companion.setYsmSelection(modelId, textureId);
                    source.sendSuccess(() -> Component.literal(modelId + ".ysm 已绑定到龙龙的车万女仆实体，不会改变玩家皮肤。"), false);
                    return 1;
                }
            } catch (java.io.IOException ignored) { }
        }
        companion.setYsmSelection(modelId, textureId);
        source.sendFailure(Component.literal("未在龙龙附属目录找到该 YSM 模型包，请检查模型 ID。"));
        return 0;
    }

    private static int invincible(CommandSourceStack source, boolean enabled)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        companion.setCompanionInvincible(enabled);
        source.sendSuccess(() -> Component.literal("龙龙无敌模式：" + (enabled ? "开启" : "关闭")).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int provider(CommandSourceStack source, String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("openai") && !normalized.equals("deepseek") && !normalized.equals("custom")) {
            source.sendFailure(Component.literal("提供商只能是 openai、deepseek 或 custom。"));
            return 0;
        }
        McAiConfig.setProvider(normalized);
        source.sendSuccess(() -> Component.literal("AI提供商已切换为 " + normalized), true);
        return 1;
    }

    private static int model(CommandSourceStack source, String value) {
        if (value.isBlank() || value.length() > 128) { source.sendFailure(Component.literal("模型名无效。")); return 0; }
        McAiConfig.setModel(value.trim());
        source.sendSuccess(() -> Component.literal("当前模型已设置为 " + value.trim()), true);
        return 1;
    }

    private static int highestCommand(CommandSourceStack source, String command)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return CompanionManager.executeAuthorizedCommand(player, command) ? 1 : 0;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = CompanionManager.find(source.getPlayerOrException());
        if (companion == null) { source.sendSuccess(() -> Component.literal("龙龙尚未召唤。"), false); return 1; }
        source.sendSuccess(() -> Component.literal("龙龙 Lv." + companion.getDragonLevel() + "（经验 "
            + companion.getDragonExperience() + "），模式=" + companion.getMode().chinese + "，活动=" + companion.getActivity()
            + "，习惯=" + companion.getHabit() + "，想法=" + companion.getThought()
            + "，已完成任务=" + companion.getCompletedTasks()), false);
        source.sendSuccess(() -> Component.literal("YSM=" + (companion.getYsmModel().isBlank() ? "未选择" : companion.getYsmModel()
            + "/" + companion.getYsmTexture()) + "，API=" + (AiService.isConfigured() ? "已配置" : "未配置")), false);
        source.sendSuccess(() -> Component.literal("隐藏装备仓=已启用（工具/武器/箭不占27格物资背包）"), false);
        source.sendSuccess(() -> Component.literal("好感度=" + MaidVisualBridge.favorability(companion)
            + "，五子棋胜场=" + MaidVisualBridge.gomokuWins(companion)
            + "，饰品/坐骑/棋类=车万女仆轻量接口已启用"), false);
        return 1;
    }

    private static AiCompanionEntity getOrSummon(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        return companion == null ? CompanionManager.summon(player) : companion;
    }
}
