package cn.qxf.mcai.ai;

import cn.qxf.mcai.config.McAiConfig;
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
    private static final Set<String> ALLOWED_ACTIONS = Set.of("follow", "stay", "guard", "gather", "mine", "come");
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
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(executor)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public static boolean isConfigured() {
        if (McAiConfig.baseUrl().isBlank() || McAiConfig.model().isBlank()) return false;
        return McAiConfig.provider().equals("custom") || !McAiConfig.apiKey().isBlank();
    }

    public static void ask(ServerPlayer player, String prompt, boolean proactive) {
        init();
        UUID playerId = player.getUUID();
        synchronized (PENDING) {
            if (PENDING.contains(playerId)) {
                if (!proactive) player.sendSystemMessage(Component.literal("[小麦] 我还在想上一件事，稍等一下呀。")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            if (!isConfigured()) {
                if (!proactive) player.sendSystemMessage(Component.literal("[qxfMCAI] 尚未配置API。请按 K 打开菜单，在“API 设置”中填写并保存。")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }
            PENDING.add(playerId);
        }

        String contextPrompt = prompt + "\n\n当前游戏状态：" + gameContext(player);
        if (!proactive) player.sendSystemMessage(Component.literal("[小麦] 让我想想……").withStyle(ChatFormatting.DARK_GRAY));

        CompletableFuture.supplyAsync(() -> request(playerId, contextPrompt), executor)
            .whenComplete((reply, error) -> {
                if (player.getServer() == null) return;
                player.getServer().execute(() -> {
                    synchronized (PENDING) { PENDING.remove(playerId); }
                    if (error != null) {
                        if (!proactive) player.sendSystemMessage(Component.literal("[qxfMCAI] 请求失败：" + safeError(error))
                            .withStyle(ChatFormatting.RED));
                        return;
                    }
                    if (!player.isAlive()) return;
                    player.sendSystemMessage(Component.literal("<小麦> " + reply.text()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    CompanionManager.applyActions(player, reply.actions());
                });
            });
    }

    private static ParsedReply request(UUID playerId, String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", McAiConfig.model());
            body.addProperty("stream", false);
            JsonArray messages = new JsonArray();
            messages.add(messageJson("system", McAiConfig.SYSTEM_PROMPT.get()));

            Deque<Message> history = HISTORY.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
            synchronized (history) {
                for (Message old : history) messages.add(messageJson(old.role(), old.content()));
            }
            messages.add(messageJson("user", prompt));
            body.add("messages", messages);

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint()))
                .timeout(Duration.ofSeconds(McAiConfig.REQUEST_TIMEOUT_SECONDS.get()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            if (!McAiConfig.apiKey().isBlank()) builder.header("Authorization", "Bearer " + McAiConfig.apiKey());

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + "：" + readApiError(response.body()));
            }
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
        if (clean.startsWith("```")) {
            clean = clean.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            JsonObject object = JsonParser.parseString(clean).getAsJsonObject();
            String text = object.has("reply") ? object.get("reply").getAsString() : "我知道啦。";
            List<String> actions = new ArrayList<>();
            if (object.has("actions") && object.get("actions").isJsonArray()) {
                for (JsonElement element : object.getAsJsonArray("actions")) {
                    String type = null;
                    if (element.isJsonPrimitive()) type = element.getAsString();
                    else if (element.isJsonObject() && element.getAsJsonObject().has("type"))
                        type = element.getAsJsonObject().get("type").getAsString();
                    if (type != null) {
                        type = type.toLowerCase(java.util.Locale.ROOT);
                        if (ALLOWED_ACTIONS.contains(type)) actions.add(type);
                    }
                }
            }
            return new ParsedReply(text.isBlank() ? "我在呢。" : text, List.copyOf(actions));
        } catch (Exception ignored) {
            return new ParsedReply(clean.isBlank() ? "我在呢。" : clean, List.of());
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
        return "玩家=" + player.getGameProfile().getName()
            + "，维度=" + player.level().dimension().location()
            + "，生命=" + Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())
            + "，饱食度=" + player.getFoodData().getFoodLevel()
            + "，坐标=" + player.blockPosition().getX() + "," + player.blockPosition().getY() + "," + player.blockPosition().getZ();
    }

    private static String readApiError(String body) {
        try {
            JsonObject root = JsonParser.parseString(body.trim()).getAsJsonObject();
            if (root.has("error")) {
                JsonElement error = root.get("error");
                if (error.isJsonObject() && error.getAsJsonObject().has("message")) return error.getAsJsonObject().get("message").getAsString();
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
    private record ParsedReply(String text, List<String> actions) {}
}
