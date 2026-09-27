package cn.qxf.mcai.ai;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.server.CompanionManager;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** API 决策与游戏线程分离；回复必须持有有效请求凭证才能落地。 */
public final class AiService {
    private static final Set<String> ALLOWED = Set.of("follow", "stay", "guard", "gather", "mine", "find_cave",
        "come", "explore", "patrol", "hunt", "chop", "harvest", "plant", "farm", "fish", "build_shelter",
        "build_house", "build_bridge", "place_torch", "eat", "sleep", "deposit", "equip_weapon", "equip_pickaxe",
        "craft", "command", "emote", "stop");
    private static final RequestLedger REQUESTS = new RequestLedger(4);
    private static final Map<UUID, CompletableFuture<?>> TRANSPORT = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Message>> HISTORY = new ConcurrentHashMap<>();
    private static final Map<UUID, String> STATUS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LATENCY = new ConcurrentHashMap<>();
    private static HttpClient client;
    private AiService() {}

    public static synchronized void init() {
        if (client == null) client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    public static boolean isConfigured() {
        return McAiConfig.SPEC.isLoaded() && !McAiConfig.baseUrl().isBlank() && !McAiConfig.model().isBlank()
            && (McAiConfig.provider().equals("custom") || !McAiConfig.apiKey().isBlank());
    }
    public static String runtimeStatus(UUID owner) {
        return STATUS.getOrDefault(owner, isConfigured() ? "待命 · API 已配置" : "离线 · 本地任务可用");
    }
    public static long lastLatencyMillis(UUID owner) { return LATENCY.getOrDefault(owner, 0L); }
    public static void cancelPending(UUID owner) {
        REQUESTS.cancel(owner);
        var future = TRANSPORT.remove(owner);
        if (future != null) future.cancel(true);
        STATUS.put(owner, "已取消旧思考 · 待命");
    }
    public static void clearPlayer(UUID owner) {
        cancelPending(owner);
        HISTORY.remove(owner); STATUS.remove(owner); LATENCY.remove(owner);
    }
    public static synchronized void shutdown() {
        REQUESTS.clear();
        TRANSPORT.values().forEach(f -> f.cancel(true));
        TRANSPORT.clear(); HISTORY.clear(); STATUS.clear(); LATENCY.clear(); client = null;
    }

    public static void ask(ServerPlayer player, String prompt, boolean proactive) {
        if (player.getServer() == null || !player.isAlive()) return;
        prompt = bounded(prompt, 512).trim();
        if (prompt.isEmpty()) return;
        String control = proactive ? "" : IntentPolicy.control(prompt);
        if (!control.isEmpty()) { immediateControl(player, control); return; }
        UUID owner = player.getUUID();
        if (proactive && REQUESTS.pending(owner)) return;
        if (!proactive && REQUESTS.pending(owner)) cancelPending(owner);
        AiCompanionEntity companion = CompanionManager.find(player);
        if (!proactive && companion != null) {
            companion.addFavorability(McAiConfig.CHAT_FAVORABILITY_GAIN.get());
            companion.reactToOwnerWords(prompt);
        }
        List<AgentAction> fallback = proactive ? List.of() : LocalTaskPlanner.plan(prompt);
        boolean task = !proactive && IntentPolicy.requestsTask(prompt, fallback);
        if (!isConfigured()) { offline(player, fallback, task, proactive); return; }
        String mode = proactive ? McAiConfig.proactiveChatPrompt() : task ? McAiConfig.taskPrompt()
            : "【仅对话】回答主人的问题，actions必须为空数组，不执行任何任务。";
        String context = prompt + "\n" + mode + "\n现场：" + gameContext(player);
        if (!proactive) tell(player, task ? "正在观察现场并规划，主人可随时暂停或取消。" : "正在结合现场回应主人……", ChatFormatting.DARK_GRAY);
        submit(player, prompt, context, fallback, task, proactive, "", -1);
    }

    private static void immediateControl(ServerPlayer player, String control) {
        cancelPending(player.getUUID());
        AiCompanionEntity companion = CompanionManager.find(player);
        if (control.equals("pause") || control.equals("resume")) {
            if (companion == null) { tell(player, "尚未召唤龙龙，没有可暂停的任务。", ChatFormatting.YELLOW); return; }
            companion.setTaskPaused(control.equals("pause"));
            tell(player, control.equals("pause") ? "已暂停行动，保留当前进度和队列。" : "已恢复任务。", ChatFormatting.AQUA);
            return;
        }
        if (control.equals("stop") && companion == null) {
            tell(player, "已取消待处理的 AI 请求。", ChatFormatting.AQUA); return;
        }
        CompanionManager.applyActions(player, List.of(AgentAction.simple(control)));
        tell(player, "已立即执行：" + LocalTaskPlanner.summary(List.of(AgentAction.simple(control))), ChatFormatting.AQUA);
    }

    /** 复盘更新想法；只有空闲自主决策才允许生成有限行动，完成回顾不重复任务。 */
    public static void reviewAgentState(ServerPlayer player, String phase, String details, boolean allowActions) {
        if (!isConfigured() || player.getServer() == null || REQUESTS.pending(player.getUUID())) return;
        var companion = CompanionManager.find(player);
        if (companion == null || companion.isTaskPaused()) return;
        boolean autonomous = phase.equals("AI自主决策") && allowActions;
        String context = "【" + phase + "】" + bounded(details, 2200) + "\n"
            + (autonomous ? McAiConfig.autonomyPrompt() : McAiConfig.taskPrompt())
            + (autonomous ? "\n仅选择一项能独立完成的非建造行动；可以空动作提出建议。不得执行命令或新建建筑。"
                : "\n只根据真实结果提出一个简短建议，不声称新任务已完成。actions必须为空数组。");
        submit(player, "", context, List.of(), false, true, phase, companion.getTaskRevision());
    }

    private static void submit(ServerPlayer player, String userText, String context, List<AgentAction> fallback,
                               boolean task, boolean quiet, String phase, long revision) {
        init();
        UUID owner = player.getUUID();
        long ticket = REQUESTS.begin(owner);
        if (ticket < 0) {
            STATUS.put(owner, "API繁忙 · 本地保底");
            if (phase.isEmpty()) offline(player, fallback, task, quiet);
            return;
        }
        var server = player.server;
        var dimension = player.level().dimension();
        var relationship = CompanionManager.find(player);
        UUID companionId = relationship == null ? null : relationship.getUUID();
        long started = System.nanoTime();
        STATUS.put(owner, task ? "AI规划中" : quiet ? "观察与复盘中" : "正在回应");
        int timeout = McAiConfig.REQUEST_TIMEOUT_SECONDS.get();
        // 所有配置、游戏现场和历史在服务端线程快照；网络线程只处理字节与 JSON。
        JsonObject body = new JsonObject();
        body.addProperty("model", McAiConfig.model()); body.addProperty("stream", false);
        JsonArray messages = new JsonArray();
        messages.add(message("system", McAiConfig.systemPrompt()));
        for (Message old : HISTORY.getOrDefault(owner, new ArrayDeque<>())) messages.add(message(old.role, old.content));
        messages.add(message("user", context)); body.add("messages", messages);
        CompletableFuture<HttpResponse<String>> wire;
        try {
            String base = McAiConfig.baseUrl().trim().replaceAll("/+$", "");
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base.endsWith("/chat/completions") ? base : base + "/chat/completions"))
                .timeout(Duration.ofSeconds(timeout)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            String key = McAiConfig.apiKey();
            if (!key.isBlank()) request.header("Authorization", "Bearer " + key);
            wire = client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (RuntimeException error) {
            REQUESTS.complete(owner, ticket); STATUS.put(owner, "接口配置无效");
            if (phase.isEmpty()) offline(player, fallback, task, quiet);
            return;
        }
        TRANSPORT.put(owner, wire);
        CompletableFuture<ParsedReply> parsed = wire.thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("HTTP " + response.statusCode());
            if (response.body().length() > 131072) throw new IllegalStateException("响应过长");
            var root = JsonParser.parseString(response.body()).getAsJsonObject();
            return parseReply(root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString());
        }).orTimeout(timeout, TimeUnit.SECONDS);
        parsed.whenComplete((reply, error) -> {
            if (error != null) wire.cancel(true);
            server.execute(() -> {
                if (!REQUESTS.complete(owner, ticket)) return;
                TRANSPORT.remove(owner, wire);
                LATENCY.put(owner, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                ServerPlayer live = server.getPlayerList().getPlayer(owner);
                if (live != player || !live.isAlive() || live.level().dimension() != dimension) {
                    STATUS.put(owner, "现场已改变 · 丢弃旧计划"); return;
                }
                var current = CompanionManager.find(live);
                if (companionId != null && (current == null || !companionId.equals(current.getUUID()))) {
                    STATUS.put(owner, "伙伴已改变 · 丢弃旧计划"); return;
                }
                if (revision >= 0 && (current == null || current.getTaskRevision() != revision || current.isTaskPaused())) {
                    STATUS.put(owner, "任务已更新 · 丢弃旧复盘"); return;
                }
                if (error != null) {
                    STATUS.put(owner, "API失败或超时 · 可重试");
                    QxfMcAi.LOGGER.warn("龙龙 AI 请求失败（不记录响应正文）：{}", error.getClass().getSimpleName());
                    if (phase.isEmpty()) offline(live, fallback, task, quiet);
                    return;
                }
                STATUS.put(owner, "已回应 · 待命");
                boolean autonomous = phase.equals("AI自主决策");
                List<AgentAction> actions = IntentPolicy.resolve(reply.actions, fallback, task || autonomous);
                if (autonomous) actions = actions.stream().filter(a ->
                    !a.type().startsWith("build_") && !a.type().equals("command") && !a.type().equals("stop"))
                    .limit(1).toList();
                if (!actions.isEmpty()) CompanionManager.applyActions(live, actions);
                if (!actions.isEmpty() && autonomous) STATUS.put(owner, "已启动自主行动：" + LocalTaskPlanner.summary(actions));
                current = CompanionManager.find(live);
                if (current != null) {
                    current.speak(reply.text, reply.emotion); current.setThought(reply.thought);
                    current.remember("龙龙：" + bounded(reply.text, 200));
                }
                if (!phase.equals("任务进度") && !reply.text.isBlank())
                    live.sendSystemMessage(Component.literal("AI·龙龙：" + reply.text).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (task) tell(live, actions.isEmpty() ? "本次没有有效执行计划，请说明目标与数量。"
                    : "已提交任务队列：" + LocalTaskPlanner.summary(actions), actions.isEmpty() ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
                if (!userText.isBlank() && !quiet) remember(owner, userText, reply.text);
            });
        });
    }

    private static void offline(ServerPlayer player, List<AgentAction> actions, boolean task, boolean proactive) {
        var companion = CompanionManager.find(player);
        if (task && !actions.isEmpty()) {
            CompanionManager.applyActions(player, actions);
            tell(player, "API暂不可用，已提交本地任务：" + LocalTaskPlanner.summary(actions), ChatFormatting.YELLOW);
        } else if (proactive && companion != null) companion.proactiveLocalMessage();
        else tell(player, task ? "API暂不可用，这项复杂任务还不能生成有效计划。主人可重试或明确目标。"
            : "主人，我在。API暂不可用，仍可跟随、暂停，或执行明确的挖矿、伐木等任务。", ChatFormatting.LIGHT_PURPLE);
    }
    private static void tell(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[龙龙] " + text).withStyle(color));
    }
    private static JsonObject message(String role, String text) {
        JsonObject value = new JsonObject(); value.addProperty("role", role); value.addProperty("content", text); return value;
    }
    static ParsedReply parseReply(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try {
            JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            var actions = new ArrayList<AgentAction>();
            if (object.has("actions") && object.get("actions").isJsonArray()) {
                for (var value : object.getAsJsonArray("actions")) {
                    AgentAction action = AgentAction.fromJson(value);
                    if (actions.size() < 8 && ALLOWED.contains(action.type())) actions.add(action);
                }
            }
            return new ParsedReply(field(object, "reply", "主人，我在。", 1000),
                field(object, "thought", "正在观察现场", 256), field(object, "emotion", "curious", 32), List.copyOf(actions));
        } catch (RuntimeException ignored) {
            return new ParsedReply(text.startsWith("{") ? "主人，计划格式有误，已检查可执行的保底任务。"
                : bounded(text.isBlank() ? "主人，我在。" : text, 1000), "正在理解主人的要求", "curious", List.of());
        }
    }
    private static String field(JsonObject o, String key, String fallback, int max) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? bounded(o.get(key).getAsString(), max) : fallback;
    }
    private static String bounded(String text, int max) { return text == null ? "" : text.substring(0, Math.min(max, text.length())); }
    private static void remember(UUID owner, String user, String reply) {
        int limit = Math.min(12, McAiConfig.HISTORY_TURNS.get() * 2);
        var history = HISTORY.computeIfAbsent(owner, id -> new ArrayDeque<>());
        history.addLast(new Message("user", bounded(user, 512)));
        history.addLast(new Message("assistant", bounded(reply, 700)));
        while (history.size() > limit) history.removeFirst();
    }
    private static String gameContext(ServerPlayer player) {
        var companion = CompanionManager.find(player);
        var level = player.serverLevel();
        List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(16), Entity::isAlive);
        HitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(12)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        String looking = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
            ? level.getBlockState(block.getBlockPos()).getBlock().getName().getString() : "远处";
        return "主人=" + player.getGameProfile().getName() + "；坐标=" + player.blockPosition().toShortString()
            + "；维度=" + level.dimension().location() + "；生命=" + Math.round(player.getHealth()) + "；饱食=" + player.getFoodData().getFoodLevel()
            + "；" + (level.isNight() ? "夜晚" : "白天") + "；" + (level.isRaining() ? "下雨" : "晴朗")
            + "；群系=" + level.getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("未知")
            + "；视线=" + looking + "；敌怪=" + nearby.stream().filter(Monster.class::isInstance).count()
            + "；动物=" + nearby.stream().filter(Animal.class::isInstance).count() + "；掉落物=" + nearby.stream().filter(ItemEntity.class::isInstance).count()
            + (companion == null ? "；龙龙尚未召唤" : "；" + bounded(companion.describeForAi(), 2200));
    }
    private record Message(String role, String content) {}
    record ParsedReply(String text, String thought, String emotion, List<AgentAction> actions) {}
}
