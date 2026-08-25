package cn.qxf.mcai.ai;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.server.CompanionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AiService {
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
        "follow", "stay", "guard", "gather", "mine", "find_cave", "come", "explore", "patrol", "hunt", "chop",
        "harvest", "plant", "farm", "fish", "build_shelter", "build_house", "build_bridge", "place_torch",
        "eat", "sleep", "deposit", "equip_weapon", "equip_pickaxe", "craft", "command", "emote", "stop");
    private static final Set<UUID> PENDING = new HashSet<>();
    private static final Set<UUID> BACKGROUND_PENDING = new HashSet<>();
    private static final java.util.Map<UUID, Deque<Message>> HISTORY = new java.util.concurrent.ConcurrentHashMap<>();
    private static ExecutorService executor;
    private static HttpClient client;

    private AiService() {}

    public static synchronized void init() {
        if (executor != null) return;
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "qxfMCAI-API");
            thread.setDaemon(true);
            return thread;
        });
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).executor(executor)
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public static boolean isConfigured() {
        if (McAiConfig.baseUrl().isBlank() || McAiConfig.model().isBlank()) return false;
        return McAiConfig.provider().equals("custom") || !McAiConfig.apiKey().isBlank();
    }

    public static void ask(ServerPlayer player, String prompt, boolean proactive) {
        init();
        UUID playerId = player.getUUID();
        AiCompanionEntity initialRelationship = proactive ? null : CompanionManager.find(player);
        boolean relationshipExisted = initialRelationship != null;
        if (!proactive) {
            // 聊天只发展已建立的关系；召唤必须由 /mcai summon 或明确任务触发。
            if (initialRelationship != null) {
                initialRelationship.addFavorability(McAiConfig.CHAT_FAVORABILITY_GAIN.get());
                initialRelationship.reactToOwnerWords(prompt);
            }
        }
        // API 优先生成计划，本地规划器只负责验证玩家意图并在模型漏动作、坏 JSON 或超时时保底。
        // 主动聊天是物理隔离的纯对话通道。提示词中的“不得建造/不得执行”也绝不能
        // 被关键词保底误识别为玩家下达了建造或命令任务。
        List<AgentAction> localFallbackActions = proactive ? List.of() : LocalTaskPlanner.plan(prompt);
        boolean taskRequest = !proactive && !localFallbackActions.isEmpty();
        if (taskRequest && !isConfigured()) {
            CompanionManager.applyActions(player, localFallbackActions);
            if (!proactive) player.sendSystemMessage(Component.literal("[龙龙] API 不可用，已启用安全保底并立即执行："
                    + LocalTaskPlanner.summary(localFallbackActions)).withStyle(ChatFormatting.YELLOW));
            return;
        }
        synchronized (PENDING) {
            if (PENDING.contains(playerId)) {
                if (taskRequest) {
                    CompanionManager.applyActions(player, localFallbackActions);
                    if (!proactive) player.sendSystemMessage(Component.literal("[龙龙] AI 正在处理上一次思考，本次任务已用本地保底执行："
                        + LocalTaskPlanner.summary(localFallbackActions)).withStyle(ChatFormatting.YELLOW));
                } else if (!proactive) player.sendSystemMessage(Component.literal("[龙龙] 我正在认真回应上一句话。")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            if (!isConfigured()) {
                if (!proactive) offlineChat(player, prompt);
                return;
            }
            PENDING.add(playerId);
        }

        String modePrompt = proactive ? McAiConfig.proactiveChatPrompt()
            : taskRequest ? McAiConfig.taskPrompt() : "【对话】结合现场和记忆自然回应，不要虚构已执行任务。";
        String contextPrompt = prompt + "\n\n" + modePrompt + "\n\n当前游戏状态：" + gameContext(player);
        if (!proactive) player.sendSystemMessage(Component.literal(taskRequest
                ? "[龙龙] AI 正在结合现场生成可执行计划……"
                : "[龙龙] 让我观察一下，再认真回应……").withStyle(ChatFormatting.DARK_GRAY));

        MinecraftServer server = player.getServer();
        if (server == null) {
            synchronized (PENDING) { PENDING.remove(playerId); }
            return;
        }
        CompletableFuture<ParsedReply> requestFuture = CompletableFuture.supplyAsync(() -> request(playerId, contextPrompt), executor);
        if (taskRequest) requestFuture = requestFuture.orTimeout(Math.min(15, McAiConfig.REQUEST_TIMEOUT_SECONDS.get()), TimeUnit.SECONDS);
        requestFuture
            .whenComplete((reply, error) -> {
                server.execute(() -> {
                    synchronized (PENDING) { PENDING.remove(playerId); }
                    ServerPlayer livePlayer = server.getPlayerList().getPlayer(playerId);
                    if (livePlayer == null) return;
                    if (error != null) {
                        if (taskRequest && !proactive) {
                            CompanionManager.applyActions(livePlayer, localFallbackActions);
                            livePlayer.sendSystemMessage(Component.literal("[qxfMCAI] AI 规划超时或失败，已无缝切换本地执行："
                                + LocalTaskPlanner.summary(localFallbackActions)).withStyle(ChatFormatting.YELLOW));
                        } else if (proactive) {
                            AiCompanionEntity companion = CompanionManager.find(livePlayer);
                            if (companion != null) companion.proactiveLocalMessage();
                        } else livePlayer.sendSystemMessage(Component.literal("[qxfMCAI] 聊天请求失败：" + safeError(error))
                            .withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!livePlayer.isAlive()) return;
                    livePlayer.sendSystemMessage(Component.literal("AI·龙龙：" + reply.text()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    AiCompanionEntity companion = CompanionManager.find(livePlayer);
                    // 玩家明确交付了任务时，任务本身就是召唤许可；不能让一份有效的 API 计划
                    // 因为龙龙尚未生成而静默丢失。普通聊天仍不会隐式召唤。
                    if (taskRequest && companion == null) companion = CompanionManager.summon(livePlayer);
                    if (companion != null) {
                        if (!proactive && !relationshipExisted) companion.reactToOwnerWords(prompt);
                        companion.speak(reply.text(), reply.emotion());
                        companion.setThought(reply.thought());
                        companion.remember("龙龙回应：" + reply.text());
                    }
                    List<AgentAction> resolvedActions = proactive ? List.of()
                        : mergeRequiredActions(reply.actions(), localFallbackActions, taskRequest);
                    if (companion != null) {
                        CompanionManager.applyActions(livePlayer, resolvedActions);
                        if (taskRequest && !resolvedActions.isEmpty())
                            livePlayer.sendSystemMessage(Component.literal("[龙龙] AI 计划已下发：" + LocalTaskPlanner.summary(resolvedActions))
                                .withStyle(ChatFormatting.GREEN));
                    } else if (!resolvedActions.isEmpty() && !proactive) {
                        livePlayer.sendSystemMessage(Component.literal(
                            "[qxfMCAI] 当前无法生成龙龙，AI 规划已保留，请检查实体生成空间。")
                            .withStyle(ChatFormatting.YELLOW));
                    }
                    if (resolvedActions.isEmpty()) QxfMcAi.LOGGER.info("龙龙将本次输入识别为纯聊天：{}", prompt);
                });
            });
    }

    private static void offlineChat(ServerPlayer player, String prompt) {
        AiCompanionEntity companion = CompanionManager.find(player);
        String reply = prompt.contains("怎么") || prompt.contains("建议")
            ? "主人，API 暂时不可用，但我仍会观察当前状态。可以直接交给我挖矿、建造、农田或战斗任务。"
            : "主人，我在呢。API 断开时我仍能真正执行生存任务，等连接恢复后会继续完整思考。";
        player.sendSystemMessage(Component.literal("AI·龙龙：" + reply).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (companion != null) companion.speak(reply, "curious");
    }

    /** 保留 API 的行动顺序，同时保证玩家明确要求的任务类型没有被模型漏掉。 */
    private static List<AgentAction> mergeRequiredActions(List<AgentAction> planned,
                                                           List<AgentAction> required,
                                                           boolean taskRequest) {
        if (!taskRequest) return planned.stream().limit(8).toList();
        List<AgentAction> merged = new ArrayList<>(planned.stream().limit(8).toList());
        for (AgentAction fallback : required) {
            boolean covered = merged.stream().anyMatch(action -> action.type().equals(fallback.type()));
            if (!covered && merged.size() < 8) merged.add(fallback);
        }
        return List.copyOf(merged);
    }

    /** 执行期间的低频 AI 观察环：进度、失败和完成都会回到同一个智能体上下文。 */
    public static void reviewAgentState(ServerPlayer player, String phase, String details, boolean allowActions) {
        if (!isConfigured() || player.getServer() == null) return;
        init();
        UUID playerId = player.getUUID();
        synchronized (BACKGROUND_PENDING) {
            if (BACKGROUND_PENDING.contains(playerId)) return;
            synchronized (PENDING) { if (PENDING.contains(playerId)) return; }
            BACKGROUND_PENDING.add(playerId);
        }
        MinecraftServer server = player.getServer();
        String prompt = "【智能体运行阶段：" + phase + "】\n" + details + "\n"
            + ("AI自主决策".equals(phase) ? McAiConfig.autonomyPrompt() : McAiConfig.taskPrompt())
            + "\n请基于真实进度更新想法。只有需要调整计划时才输出 actions。";
        CompletableFuture.supplyAsync(() -> request(playerId, prompt), executor)
            .orTimeout(Math.min(20, McAiConfig.REQUEST_TIMEOUT_SECONDS.get()), TimeUnit.SECONDS)
            .whenComplete((reply, error) -> server.execute(() -> {
                synchronized (BACKGROUND_PENDING) { BACKGROUND_PENDING.remove(playerId); }
                ServerPlayer livePlayer = server.getPlayerList().getPlayer(playerId);
                if (livePlayer == null) return;
                if (error != null) {
                    QxfMcAi.LOGGER.warn("龙龙 AI 运行阶段回顾失败：phase={} error={}", phase, safeError(error));
                    return;
                }
                AiCompanionEntity companion = CompanionManager.find(livePlayer);
                if (companion == null) return;
                companion.setThought(reply.thought());
                companion.remember(phase + "：" + reply.text());
                if (!reply.text().isBlank()) companion.speak(reply.text(), reply.emotion());
                List<AgentAction> adjustments = allowActions ? reply.actions().stream().limit(4).toList() : List.of();
                if (!adjustments.isEmpty()) {
                    CompanionManager.applyActions(livePlayer, adjustments);
                    livePlayer.sendSystemMessage(Component.literal("[龙龙·AI调整] " + reply.text())
                        .withStyle(ChatFormatting.AQUA));
                } else if (!"任务进度".equals(phase)) {
                    livePlayer.sendSystemMessage(Component.literal("<龙龙> " + reply.text())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }));
    }

    private static ParsedReply request(UUID playerId, String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", McAiConfig.model());
            body.addProperty("stream", false);
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            body.add("response_format", responseFormat);
            JsonArray messages = new JsonArray();
            messages.add(messageJson("system", McAiConfig.systemPrompt()));
            Deque<Message> history = HISTORY.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
            synchronized (history) {
                for (Message old : history) messages.add(messageJson(old.role(), old.content()));
            }
            messages.add(messageJson("user", prompt));
            body.add("messages", messages);

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint()))
                .timeout(Duration.ofSeconds(McAiConfig.REQUEST_TIMEOUT_SECONDS.get()))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            if (!McAiConfig.apiKey().isBlank()) builder.header("Authorization", "Bearer " + McAiConfig.apiKey());
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("HTTP " + response.statusCode() + "：" + readApiError(response.body()));
            JsonObject root = JsonParser.parseString(response.body().trim()).getAsJsonObject();
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
            ParsedReply parsed = parseReply(content);
            remember(history, prompt, parsed.text());
            return parsed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("请求已中断");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
        }
    }

    private static JsonObject messageJson(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static ParsedReply parseReply(String raw) {
        String clean = raw.trim();
        if (clean.startsWith("```")) clean = clean.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try {
            JsonObject object = JsonParser.parseString(clean).getAsJsonObject();
            String text = object.has("reply") ? object.get("reply").getAsString() : "我知道啦。";
            String thought = object.has("thought") ? object.get("thought").getAsString() : "在认真理解主人的需要";
            String emotion = object.has("emotion") ? object.get("emotion").getAsString() : "curious";
            List<AgentAction> actions = new ArrayList<>();
            if (object.has("actions") && object.get("actions").isJsonArray()) {
                for (JsonElement element : object.getAsJsonArray("actions")) {
                    AgentAction action = AgentAction.fromJson(element);
                    if (ALLOWED_ACTIONS.contains(action.type())) actions.add(action);
                }
            }
            return new ParsedReply(text.isBlank() ? "我在呢。" : text, thought, emotion, List.copyOf(actions));
        } catch (Exception ignored) {
            return new ParsedReply(clean.isBlank() ? "我在呢。" : clean, "正在理解这句话", "curious", List.of());
        }
    }

    private static void remember(Deque<Message> history, String user, String assistant) {
        int maxMessages = McAiConfig.HISTORY_TURNS.get() * 2;
        if (maxMessages <= 0) return;
        synchronized (history) {
            history.addLast(new Message("user", user));
            history.addLast(new Message("assistant", assistant));
            while (history.size() > maxMessages) history.removeFirst();
        }
    }

    private static String endpoint() {
        String base = McAiConfig.baseUrl().trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }

    private static String gameContext(ServerPlayer player) {
        AiCompanionEntity companion = CompanionManager.find(player);
        return "主人账号=" + player.getGameProfile().getName() + "，维度=" + player.level().dimension().location()
            + "，生命=" + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())
            + "，饱食度=" + player.getFoodData().getFoodLevel() + "，坐标=" + player.blockPosition().getX()
            + "," + player.blockPosition().getY() + "," + player.blockPosition().getZ()
            + "。场景=" + analyzeScene(player)
            + (companion == null ? "，龙龙尚未召唤" : "。" + companion.describeForAi());
    }

    private static String analyzeScene(ServerPlayer player) {
        var level = player.serverLevel();
        long time = Math.floorMod(level.getDayTime(), 24_000L);
        String period = time < 1_000 ? "清晨" : time < 6_000 ? "上午" : time < 12_000 ? "下午"
            : time < 13_000 ? "黄昏" : time < 23_000 ? "夜晚" : "黎明";
        String biome = level.getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.location().toString()).orElse("未知生物群系");
        String weather = level.isThundering() ? "雷暴" : level.isRaining() ? "下雨" : "晴朗";
        int light = level.getMaxLocalRawBrightness(player.blockPosition());
        String below = level.getBlockState(player.blockPosition().below()).getBlock().getName().getString();

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(12.0D));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, player));
        String looking = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
            ? level.getBlockState(blockHit.getBlockPos()).getBlock().getName().getString() : "远处";

        List<Entity> nearby = level.getEntities(player, new AABB(player.blockPosition()).inflate(16.0D),
            entity -> entity.isAlive());
        long hostiles = nearby.stream().filter(Monster.class::isInstance).count();
        long animals = nearby.stream().filter(Animal.class::isInstance).count();
        long drops = nearby.stream().filter(ItemEntity.class::isInstance).count();
        return biome + "，" + period + "，" + weather + "，亮度=" + light + "，脚下=" + below
            + "，主人正看向=" + looking + "，16格内敌对生物=" + hostiles + "、动物=" + animals
            + "、掉落物=" + drops;
    }

    private static String readApiError(String body) {
        try {
            JsonObject root = JsonParser.parseString(body.trim()).getAsJsonObject();
            if (root.has("error")) {
                JsonElement error = root.get("error");
                if (error.isJsonObject() && error.getAsJsonObject().has("message"))
                    return error.getAsJsonObject().get("message").getAsString();
                return error.toString();
            }
        } catch (Exception ignored) {}
        return body == null || body.isBlank() ? "服务没有返回错误说明" : body.substring(0, Math.min(200, body.length()));
    }

    private static String safeError(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private record Message(String role, String content) {}
    private record ParsedReply(String text, String thought, String emotion, List<AgentAction> actions) {}
}
