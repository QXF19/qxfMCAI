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
import net.minecraft.server.level.ServerPlayer;

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

public final class AiService {
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
        "follow", "stay", "guard", "gather", "mine", "find_cave", "come", "explore", "patrol", "hunt", "chop",
        "harvest", "plant", "farm", "fish", "build_shelter", "build_house", "build_bridge", "place_torch",
        "eat", "sleep", "deposit", "equip_weapon", "equip_pickaxe", "craft", "command", "emote", "stop");
    private static final Set<UUID> PENDING = new HashSet<>();
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
        if (!proactive) {
            AiCompanionEntity relationship = CompanionManager.find(player);
            // 聊天只发展已建立的关系；召唤必须由 /mcai summon 或明确任务触发。
            if (relationship != null) {
                relationship.addFavorability(McAiConfig.CHAT_FAVORABILITY_GAIN.get());
                relationship.remember("玩家主动和我交流：" + prompt);
            }
        }
        // 任务意图先在服务端确定性执行，API 只负责补充人格化回复和复杂计划。
        // 这样即使模型只说“我知道了”、返回了坏 JSON 或网络很慢，任务也不会丢失。
        List<AgentAction> immediateActions = inferActionsLocally(prompt);
        if (!immediateActions.isEmpty()) {
            CompanionManager.applyActions(player, immediateActions);
            if (!proactive) player.sendSystemMessage(Component.literal("[龙龙] 已立即开始执行：" +
                    immediateActions.stream().map(AgentAction::type).reduce((a, b) -> a + " → " + b).orElse("任务"))
                .withStyle(ChatFormatting.GREEN));
        }
        synchronized (PENDING) {
            if (PENDING.contains(playerId)) {
                if (!proactive && immediateActions.isEmpty()) player.sendSystemMessage(Component.literal("[龙龙] 我正在认真回应上一句话；新任务仍可直接用菜单或命令下达。")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            if (!isConfigured()) {
                if (!proactive) player.sendSystemMessage(Component.literal(immediateActions.isEmpty()
                        ? "[qxfMCAI] 尚未配置API；按 M 填写后可聊天。菜单、命令和可识别任务仍可离线执行。"
                        : "[qxfMCAI] API 未配置，但任务引擎已经离线开始执行。")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }
            PENDING.add(playerId);
        }

        String contextPrompt = prompt + "\n\n当前游戏状态：" + gameContext(player);
        if (!proactive) player.sendSystemMessage(Component.literal("[龙龙] 让我观察一下，再决定怎么真正做……")
            .withStyle(ChatFormatting.DARK_GRAY));

        CompletableFuture.supplyAsync(() -> request(playerId, contextPrompt), executor)
            .whenComplete((reply, error) -> {
                if (player.getServer() == null) return;
                player.getServer().execute(() -> {
                    synchronized (PENDING) { PENDING.remove(playerId); }
                    if (error != null) {
                        if (!proactive) player.sendSystemMessage(Component.literal("[qxfMCAI] 聊天请求失败：" + safeError(error)
                                + (immediateActions.isEmpty() ? "" : "；已识别的任务仍在执行。"))
                            .withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!player.isAlive()) return;
                    player.sendSystemMessage(Component.literal("<龙龙> " + reply.text()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    AiCompanionEntity companion = CompanionManager.find(player);
                    if (companion != null) {
                        companion.speak(reply.text(), reply.emotion());
                        companion.setThought(reply.thought());
                        companion.remember("玩家说：" + prompt);
                        companion.remember("龙龙回应：" + reply.text());
                    }
                    if (immediateActions.isEmpty()) {
                        List<AgentAction> resolvedActions = reply.actions().isEmpty()
                            ? inferActionsLocally(prompt) : reply.actions();
                        // 模型返回的动作不得绕过“明确召唤”的边界。
                        if (companion != null) {
                            CompanionManager.applyActions(player, resolvedActions);
                        } else if (!resolvedActions.isEmpty() && !proactive) {
                            player.sendSystemMessage(Component.literal(
                                "[qxfMCAI] 龙龙尚未召唤，AI 规划的动作未执行。请先使用 /mcai summon。")
                                .withStyle(ChatFormatting.YELLOW));
                        }
                        if (resolvedActions.isEmpty())
                            QxfMcAi.LOGGER.info("龙龙将本次输入识别为纯聊天：{}", prompt);
                    }
                });
            });
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
            String thought = object.has("thought") ? object.get("thought").getAsString() : "在认真理解玩家的需要";
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

    private static List<AgentAction> inferActionsLocally(String prompt) {
        String text = prompt == null ? "" : prompt.trim().toLowerCase(java.util.Locale.ROOT);
        List<AgentAction> actions = new ArrayList<>();
        if (text.contains("找矿洞") || text.contains("寻找矿洞") || text.contains("天然矿洞") || text.contains("洞穴"))
            actions.add(AgentAction.simple("find_cave"));
        else if (text.contains("挖矿") || text.contains("矿石") || text.contains("下矿") || text.contains("采矿"))
            actions.add(new AgentAction("mine", "ores", numberHint(text, 3), "", ""));
        if (text.contains("砍树") || text.contains("伐木")) actions.add(new AgentAction("chop", "logs", numberHint(text, 4), "", ""));
        if (text.contains("建房") || text.contains("房子") || text.contains("小屋")) actions.add(AgentAction.simple("build_house"));
        else if (text.contains("庇护所")) actions.add(AgentAction.simple("build_shelter"));
        else if (text.contains("造桥") || text.contains("建桥")) actions.add(AgentAction.simple("build_bridge"));
        if (text.contains("种地") || text.contains("农田") || text.contains("收割") || text.contains("农作"))
            actions.add(new AgentAction("farm", "crops", numberHint(text, 3), "", ""));
        if (text.contains("打怪") || text.contains("战斗") || text.contains("清理怪")) actions.add(new AgentAction("hunt", "monsters", 3, "", ""));
        if (text.contains("探索")) actions.add(AgentAction.simple("explore"));
        if (text.contains("巡逻")) actions.add(new AgentAction("patrol", "home", 2, "", ""));
        if (text.contains("钓鱼")) actions.add(AgentAction.simple("fish"));
        if (text.contains("整理") || text.contains("放进箱子")) actions.add(AgentAction.simple("deposit"));
        if (text.contains("跟着我") || text.contains("跟随我")) actions.add(AgentAction.simple("follow"));
        if (text.contains("停下") || text.contains("停止任务")) actions.add(AgentAction.simple("stop"));
        String command = extractCommand(text);
        if (!command.isBlank()) actions.add(new AgentAction("command", "", 1, "", command));
        return List.copyOf(actions);
    }

    private static String extractCommand(String text) {
        String[] markers = {"执行命令", "运行命令", "使用命令", "输入命令"};
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index < 0) continue;
            String command = text.substring(index + marker.length()).trim();
            while (command.startsWith(":" ) || command.startsWith("：") || command.startsWith("/"))
                command = command.substring(1).trim();
            return command;
        }
        if (text.startsWith("/")) return text.substring(1).trim();
        return "";
    }

    private static int numberHint(String text, int fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,3})").matcher(text);
        if (!matcher.find()) return fallback;
        try { return Math.max(1, Math.min(64, Integer.parseInt(matcher.group(1)))); }
        catch (NumberFormatException ignored) { return fallback; }
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
        return "玩家=" + player.getGameProfile().getName() + "，维度=" + player.level().dimension().location()
            + "，生命=" + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())
            + "，饱食度=" + player.getFoodData().getFoodLevel() + "，坐标=" + player.blockPosition().getX()
            + "," + player.blockPosition().getY() + "," + player.blockPosition().getZ()
            + (companion == null ? "，龙龙尚未召唤" : "。" + companion.describeForAi());
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
