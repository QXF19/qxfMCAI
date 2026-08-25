package cn.qxf.mcai.event;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.block.ModBlocks;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.server.CompanionManager;
import cn.qxf.mcai.server.McAiCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = QxfMcAi.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {
    private static final Map<UUID, Long> LAST_PROACTIVE = new ConcurrentHashMap<>();

    private ForgeEvents() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        McAiCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String prompt = extractPrompt(event.getRawText());
        if (prompt != null && !prompt.isBlank()) AiService.ask(event.getPlayer(), prompt, false);
    }

    private static String extractPrompt(String raw) {
        String text = raw.trim();
        String[] prefixes = {"U：", "U:", "U ", "u：", "u:", "u ",
            "@龙龙 ", "@龙龙，", "@龙龙,", "龙龙 ", "龙龙，", "龙龙,",
            "@龍龍 ", "龍龍，", "龍龍,"};
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) return text.substring(prefix.length()).trim();
        }
        if (text.equalsIgnoreCase("U") || text.equals("@龙龙") || text.equals("龙龙") || text.equals("龍龍"))
            return "主人在叫你。请自然称呼主人，结合自己的当前想法主动回应，并询问或建议一件可以真正执行的事情。";
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!McAiConfig.PROACTIVE_ENABLED.get() || player.tickCount % 20 != 0) return;
        var companion = CompanionManager.find(player);
        if (companion == null) return;
        long now = System.currentTimeMillis();
        long interval = McAiConfig.PROACTIVE_INTERVAL_SECONDS.get() * 1000L;
        long last = LAST_PROACTIVE.computeIfAbsent(player.getUUID(), ignored -> now);
        if (now - last < interval) return;
        LAST_PROACTIVE.put(player.getUUID(), now);
        if (AiService.isConfigured())
            AiService.ask(player, "请根据记忆、目标和当前环境主动和主人聊天。只说一次，只聊天和提建议，不得自主建造或执行任务。", true);
        else companion.proactiveLocalMessage();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_PROACTIVE.put(player.getUUID(), System.currentTimeMillis());
            player.sendSystemMessage(Component.literal("[qxfMCAI v11.1] 轻量二维龙龙与四合一实体棋桌已就绪；按 M 打开控制台。")
                .withStyle(ChatFormatting.AQUA));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_PROACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBoardPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getPlacedBlock().is(ModBlocks.DRAGON_GAME_BOARD.get())) return;
        var companion = CompanionManager.find(player);
        if (companion != null)
            companion.reactToOwnerAction("主人放置了属于我们的实体棋盘", "happy", "棋盘摆好啦，主人想先玩哪一种？");
    }

    @SubscribeEvent
    public static void onBoardBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !event.getState().is(ModBlocks.DRAGON_GAME_BOARD.get())) return;
        var companion = CompanionManager.find(player);
        if (companion != null)
            companion.reactToOwnerAction("主人拆掉了属于我们的实体棋盘", "sad", "棋盘被收起来了……下次再陪主人玩。");
    }
}
