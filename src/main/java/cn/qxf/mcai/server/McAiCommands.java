package cn.qxf.mcai.server;

import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.block.ModBlocks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
            .then(Commands.literal("board").executes(ctx -> board(ctx.getSource())))
            .then(Commands.literal("permit")
                .then(Commands.literal("teleport").executes(ctx -> permitTeleport(ctx.getSource()))))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("ride").executes(ctx -> ride(ctx.getSource())))
            .then(Commands.literal("hide")
                .executes(ctx -> hide(ctx.getSource(), null))
                .then(Commands.argument("隐藏", BoolArgumentType.bool())
                    .executes(ctx -> hide(ctx.getSource(), BoolArgumentType.getBool(ctx, "隐藏")))))
            .then(Commands.literal("accessory").executes(ctx -> accessory(ctx.getSource())))
            .then(Commands.literal("gomoku")
                .then(Commands.literal("start").executes(ctx -> gomokuStart(ctx.getSource())))
                .then(Commands.literal("board").executes(ctx -> gomokuBoard(ctx.getSource())))
                .then(Commands.literal("move")
                    .then(Commands.argument("x", IntegerArgumentType.integer(0, 8))
                        .then(Commands.argument("y", IntegerArgumentType.integer(0, 8))
                            .executes(ctx -> gomoku(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x"),
                                IntegerArgumentType.getInteger(ctx, "y"))))))
                .then(Commands.argument("x", IntegerArgumentType.integer(0, 8))
                    .then(Commands.argument("y", IntegerArgumentType.integer(0, 8))
                        .executes(ctx -> gomoku(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "x"),
                            IntegerArgumentType.getInteger(ctx, "y"))))))
            .then(Commands.literal("chess")
                .then(Commands.literal("start").executes(ctx -> xiangqiStart(ctx.getSource())))
                .then(Commands.literal("board").executes(ctx -> xiangqiBoard(ctx.getSource())))
                .then(Commands.literal("move")
                    .then(Commands.argument("起点x", IntegerArgumentType.integer(0, 8))
                        .then(Commands.argument("起点y", IntegerArgumentType.integer(0, 9))
                            .then(Commands.argument("终点x", IntegerArgumentType.integer(0, 8))
                                .then(Commands.argument("终点y", IntegerArgumentType.integer(0, 9))
                                    .executes(ctx -> xiangqi(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "起点x"), IntegerArgumentType.getInteger(ctx, "起点y"),
                                        IntegerArgumentType.getInteger(ctx, "终点x"), IntegerArgumentType.getInteger(ctx, "终点y")))))))))
            .then(Commands.literal("play")
                .executes(ctx -> play(ctx.getSource(), ""))
                .then(Commands.argument("动作", StringArgumentType.word())
                    .executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "动作")))))
            .then(Commands.literal("family")
                .then(Commands.literal("accept").executes(ctx -> family(ctx.getSource(), "accept")))
                .then(Commands.literal("decline").executes(ctx -> family(ctx.getSource(), "decline")))
                .then(Commands.literal("child").executes(ctx -> family(ctx.getSource(), "child"))))
            .then(Commands.literal("ask")
                .then(Commands.argument("内容", StringArgumentType.greedyString())
                    .executes(ctx -> ask(ctx.getSource(), StringArgumentType.getString(ctx, "内容")))))
            .then(Commands.literal("task")
                .then(Commands.argument("动作", StringArgumentType.word())
                    .then(Commands.argument("数量", IntegerArgumentType.integer(1, 256))
                        .executes(ctx -> action(ctx.getSource(), StringArgumentType.getString(ctx, "动作"),
                            IntegerArgumentType.getInteger(ctx, "数量"))))))
            .then(Commands.literal("invincible").requires(source -> source.hasPermission(4))
                .executes(ctx -> invincible(ctx.getSource(), null))
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
            .then(Commands.literal("special")
                .then(Commands.literal("teleport_to_qxf1975")
                    .executes(ctx -> teleportToQxf1975(ctx.getSource()))))
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
        source.sendSuccess(() -> Component.literal("qxfMCAI v11：轻量二维毛毛龙、场景情绪、三合一实体棋盘与 AI 真实执行；按 M 打开紧凑控制台。")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("常用：summon、ask、inventory、play、gomoku、chess、mine、cave、farm、hunt、build house"), false);
        source.sendSuccess(() -> Component.literal("聊天：@龙龙 你的要求；Shift+右键龙龙也可打开27格背包。"), false);
        source.sendSuccess(() -> Component.literal("v11 固定提供 OP4 命令源；只应在私人且已备份的世界使用。")
            .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int summon(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompanionManager.summon(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("[龙龙] 主人，我来啦！这次会一起真正生活、工作和玩耍。")
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
        if ("stop".equals(type)) {
            CompanionManager.applyActions(player, java.util.List.of(AgentAction.simple("stop")));
            source.sendSuccess(() -> Component.literal("[龙龙] 已立即停止当前任务。"), false);
            return 1;
        }
        // 快捷命令也进入同一套 API 规划链；无 API、超时或漏动作时由本地计划保底。
        AiService.ask(player, actionPrompt(type, count), false);
        source.sendSuccess(() -> Component.literal("[龙龙] 已把快捷任务交给 AI 结合现场规划：" + type + " × " + count), false);
        return 1;
    }

    private static String actionPrompt(String type, int count) {
        String task = switch (type) {
            case "gather" -> "收集附近掉落物"; case "mine" -> "真正向下挖矿";
            case "find_cave" -> "向下开凿寻找天然矿洞"; case "explore" -> "探索附近安全区域";
            case "patrol" -> "巡视基地周围"; case "hunt" -> "使用合适武器清理附近怪物";
            case "chop" -> "使用斧头砍树并收集原木"; case "farm", "harvest", "plant" -> "照料附近农田";
            case "build_shelter" -> "设计并建造集中庇护所"; case "build_house" -> "观察基地并设计一座实用小房屋后真正建造";
            case "build_bridge" -> "在当前位置规划并建造一座桥"; case "equip_weapon" -> "装备最合适的武器";
            case "equip_pickaxe" -> "装备最合适的镐子"; case "place_torch" -> "在这里放置火把照明";
            case "deposit" -> "把物资整理进附近箱子"; case "fish" -> "使用钓竿钓鱼";
            case "eat" -> "从自己的物资中进食"; case "sleep" -> "安全休息";
            case "craft" -> "制作需要的基础材料"; default -> "执行动作 " + type;
        };
        return task + "，目标数量 " + count + "；请输出可执行动作并持续根据实际进度调整。";
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

    private static int ride(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = getOrSummon(source);
        boolean result = companion.mountOwner(player);
        source.sendSuccess(() -> Component.literal(result ? "已骑上龙龙；方向键控制，潜行键下马。" : "现在无法骑乘。"), false);
        return result ? 1 : 0;
    }

    private static int hide(CommandSourceStack source, Boolean requested)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        boolean hidden = requested == null ? !companion.isCompanionHidden() : requested;
        companion.setCompanionHidden(hidden);
        companion.reactToOwnerAction(hidden ? "主人把我隐藏了" : "主人让我重新出现在身边",
            hidden ? "sad" : "joy", hidden ? null : "我回来啦，主人！");
        source.sendSuccess(() -> Component.literal(hidden
            ? "龙龙已隐藏；任务与思考继续运行，使用 /mcai hide false 显示。"
            : "龙龙已重新显示并恢复跟随。"), false);
        if (!hidden) companion.setMode(AiCompanionEntity.Mode.FOLLOW);
        return 1;
    }

    private static int board(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack board = new ItemStack(ModBlocks.DRAGON_GAME_BOARD_ITEM.get());
        if (!player.getInventory().add(board)) player.drop(board, false);
        source.sendSuccess(() -> Component.literal("已领取龙龙三合一棋盘；放在任意方块上，右键打开独立棋局。"), false);
        return 1;
    }

    private static int teleportToQxf1975(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer traveler = source.getPlayerOrException();
        ServerPlayer destination = traveler.server.getPlayerList().getPlayerByName("qxf1975");
        if (destination == null) {
            source.sendFailure(Component.literal("特殊传送失败：目的地玩家 qxf1975 当前不在线。"));
            return 0;
        }
        if (traveler == destination) {
            source.sendSuccess(() -> Component.literal("你就是 qxf1975，无需传送。"), false);
            return 1;
        }
        traveler.teleportTo(destination.serverLevel(), destination.getX() + 1.0D, destination.getY(),
            destination.getZ() + 1.0D, destination.getYRot(), destination.getXRot());
        AiCompanionEntity companion = CompanionManager.find(traveler);
        if (companion != null) {
            companion.remember("主人使用特殊控制台传送到了 qxf1975 身边");
            companion.speak("主人已安全到达 qxf1975 身边。", "joy");
        }
        source.sendSuccess(() -> Component.literal("已传送到 qxf1975 身边。"), false);
        return 1;
    }

    private static int accessory(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = getOrSummon(source);
        boolean result = companion.equipAccessory(player);
        source.sendSuccess(() -> Component.literal(result ? "已把手中物品作为饰品送给龙龙。" : "请主手拿着饰品；最多四件。"), false);
        return result ? 1 : 0;
    }

    private static int gomokuStart(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        companion.startGomoku();
        source.sendSuccess(() -> Component.literal("五子棋已开局。棋面不再刷入聊天，请放置三合一棋盘并右键打开。"), false);
        return 1;
    }

    private static int gomoku(CommandSourceStack source, int x, int y) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        String result = companion.playGomoku(x, y);
        source.sendSuccess(() -> Component.literal("[龙龙] " + result + "（棋面请在实体棋盘查看）"), false);
        return 1;
    }

    private static int gomokuBoard(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        getOrSummon(source);
        source.sendSuccess(() -> Component.literal("棋面已迁移到实体棋盘：使用 /mcai board 领取并放置。"), false);
        return 1;
    }

    private static int xiangqiStart(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        companion.startXiangqi();
        source.sendSuccess(() -> Component.literal("中国象棋已开局，主人执红。请在实体棋盘界面落子。"), false);
        return 1;
    }

    private static int xiangqi(CommandSourceStack source, int fromX, int fromY, int toX, int toY)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        var result = companion.playXiangqi(fromX, fromY, toX, toY);
        source.sendSuccess(() -> Component.literal("[龙龙] " + result.message() + "（棋面请在实体棋盘查看）"), false);
        return result.accepted() ? 1 : 0;
    }

    private static int xiangqiBoard(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        getOrSummon(source);
        source.sendSuccess(() -> Component.literal("棋面已迁移到实体棋盘：使用 /mcai board 领取并放置。"), false);
        return 1;
    }

    private static int play(CommandSourceStack source, String gesture)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        boolean started = companion.startOwnerPlay(gesture);
        source.sendSuccess(() -> Component.literal(started
            ? "龙龙正在主人面前玩耍。动作：wave/dance/cheer/bow/shy/stretch/nod/look/spin/hop"
            : "龙龙正在执行任务或离主人太远，暂时不能玩耍。"), false);
        return started ? 1 : 0;
    }

    private static int family(CommandSourceStack source, String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = getOrSummon(source);
        if ("accept".equals(action)) {
            companion.acceptFamily();
            source.sendSuccess(() -> Component.literal("已明确同意组建家庭；任何时候都可用 decline 撤回。"), false);
            return 1;
        }
        if ("decline".equals(action)) {
            companion.declineFamily();
            source.sendSuccess(() -> Component.literal("已撤回家庭同意，龙龙会尊重决定。"), false);
            return 1;
        }
        boolean created = companion.createFamilyChild(player);
        source.sendSuccess(() -> Component.literal(created ? "家庭迎来了一个小龙宝宝。" : "需要好感度80、明确同意，并等待一个游戏日。"), false);
        return created ? 1 : 0;
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

    private static int invincible(CommandSourceStack source, Boolean requested)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        AiCompanionEntity companion = getOrSummon(source);
        boolean enabled = requested == null ? !companion.isCompanionInvincible() : requested;
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
        source.sendSuccess(() -> Component.literal("轻量二维皮肤=已启用（原版玩家骨骼，无YSM），API=" + (AiService.isConfigured() ? "已配置" : "未配置")), false);
        source.sendSuccess(() -> Component.literal("隐藏装备仓=已启用（工具/武器/箭不占27格物资背包）"), false);
        source.sendSuccess(() -> Component.literal("好感度=" + companion.getFavorability()
            + "，饰品=" + companion.accessorySummary() + "，五子棋=" + companion.getGomokuWins() + "胜/"
            + companion.getGomokuLosses() + "负，家庭孩子=" + companion.getChildrenCount()
            + "，显示=" + (companion.isCompanionHidden() ? "隐藏" : "可见")
            + "，家庭同意=" + (companion.hasFamilyConsent() ? "是" : "否")
            + "，象棋=" + companion.getXiangqiOwnerWins() + "主人胜/" + companion.getXiangqiLonglongWins() + "龙龙胜"), false);
        return 1;
    }

    private static AiCompanionEntity getOrSummon(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AiCompanionEntity companion = CompanionManager.find(player);
        return companion == null ? CompanionManager.summon(player) : companion;
    }
}
