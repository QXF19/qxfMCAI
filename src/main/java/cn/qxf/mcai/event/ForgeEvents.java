package cn.qxf.mcai.event;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.server.CompanionManager;
import cn.qxf.mcai.server.McAiCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
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
        String[] prefixes = {"@小麦 ", "@小麦，", "@小麦,", "小麦 ", "小麦，", "小麦,"};
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) return text.substring(prefix.length()).trim();
        }
        if (text.equals("@小麦") || text.equals("小麦")) return "玩家在叫你，请主动回应并询问需要什么帮助。";
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!McAiConfig.PROACTIVE_ENABLED.get() || player.tickCount % 20 != 0) return;
        if (CompanionManager.find(player) == null || !AiService.isConfigured()) return;
        long now = System.currentTimeMillis();
        long interval = McAiConfig.PROACTIVE_INTERVAL_SECONDS.get() * 1000L;
        long last = LAST_PROACTIVE.computeIfAbsent(player.getUUID(), ignored -> now);
        if (now - last < interval) return;
        LAST_PROACTIVE.put(player.getUUID(), now);
        AiService.ask(player, "请根据当前状态主动和玩家说一两句贴心、有用的话；必要时可建议下一步生存目标。", true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_PROACTIVE.put(player.getUUID(), System.currentTimeMillis());
            player.sendSystemMessage(Component.literal("[qxfMCAI] 按 M 打开AI伙伴菜单；输入“@小麦 你好”开始聊天。")
                .withStyle(ChatFormatting.AQUA));
        }
    }
}
