package cn.qxf.mcai.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/** A validated, bounded instruction produced by the language model. */
public record AgentAction(String type, String target, int count, String message, String command) {
    public AgentAction {
        type = safe(type, 32).toLowerCase(Locale.ROOT);
        target = safe(target, 128);
        count = Math.max(1, Math.min(count, 256));
        message = safe(message, 256);
        command = safe(command, 512);
    }

    public static AgentAction simple(String type) {
        return new AgentAction(type, "", 1, "", "");
    }

    public static AgentAction fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) return simple(element.getAsString());
        if (!element.isJsonObject()) return simple("");
        JsonObject object = element.getAsJsonObject();
        return new AgentAction(
            string(object, "type"), string(object, "target"), integer(object, "count", 1),
            string(object, "message"), string(object, "command"));
    }

    private static String string(JsonObject object, String key) {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : ""; }
        catch (Exception ignored) { return ""; }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
