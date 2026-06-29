package cn.qxf.mcai.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class McAiConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> DEEPSEEK_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> DEEPSEEK_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> DEEPSEEK_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_API_KEY;
    public static final ForgeConfigSpec.BooleanValue PROACTIVE_ENABLED;
    public static final ForgeConfigSpec.IntValue PROACTIVE_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue REQUEST_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.IntValue HISTORY_TURNS;
    public static final ForgeConfigSpec.ConfigValue<String> SYSTEM_PROMPT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("qxfMCAI 服务端配置。API 密钥不会同步给普通客户端。", "推荐使用环境变量保存密钥。")
            .push("ai");
        PROVIDER = b.comment("openai / deepseek / custom")
            .define("provider", "openai", McAiConfig::validProvider);

        b.push("openai");
        OPENAI_BASE_URL = b.define("baseUrl", "https://api.openai.com/v1");
        OPENAI_MODEL = b.define("model", "gpt-5.5");
        OPENAI_API_KEY = b.comment("留空时读取 OPENAI_API_KEY").define("apiKey", "");
        b.pop();

        b.push("deepseek");
        DEEPSEEK_BASE_URL = b.define("baseUrl", "https://api.deepseek.com");
        DEEPSEEK_MODEL = b.define("model", "deepseek-v4-flash");
        DEEPSEEK_API_KEY = b.comment("留空时读取 DEEPSEEK_API_KEY").define("apiKey", "");
        b.pop();

        b.push("custom");
        CUSTOM_BASE_URL = b.define("baseUrl", "http://127.0.0.1:11434/v1");
        CUSTOM_MODEL = b.define("model", "qwen2.5:7b");
        CUSTOM_API_KEY = b.comment("留空时读取 QXF_MCAI_API_KEY；本地服务通常可留空").define("apiKey", "");
        b.pop();

        PROACTIVE_ENABLED = b.comment("伙伴是否定期主动聊天").define("proactiveEnabled", true);
        PROACTIVE_INTERVAL_SECONDS = b.defineInRange("proactiveIntervalSeconds", 300, 60, 3600);
        REQUEST_TIMEOUT_SECONDS = b.defineInRange("requestTimeoutSeconds", 45, 10, 180);
        HISTORY_TURNS = b.defineInRange("historyTurns", 8, 0, 30);
        SYSTEM_PROMPT = b.define("systemPrompt", "你是Minecraft生存伙伴小麦。只用简洁自然的中文回复，主动、温暖但不啰嗦。根据玩家要求从允许动作中选择。必须输出JSON：{\"reply\":\"对玩家说的话\",\"actions\":[{\"type\":\"follow|stay|guard|gather|come\"}]}。不需要动作时actions为空数组。绝不能要求或假装执行服务器命令、OP操作、创造物品或修改权限。");
        b.pop();
        SPEC = b.build();
    }

    private McAiConfig() {}

    private static boolean validProvider(Object value) {
        if (!(value instanceof String text)) return false;
        return text.equalsIgnoreCase("openai") || text.equalsIgnoreCase("deepseek") || text.equalsIgnoreCase("custom");
    }

    public static String provider() {
        return PROVIDER.get().toLowerCase(Locale.ROOT);
    }

    public static String baseUrl() {
        return switch (provider()) {
            case "deepseek" -> DEEPSEEK_BASE_URL.get();
            case "custom" -> CUSTOM_BASE_URL.get();
            default -> OPENAI_BASE_URL.get();
        };
    }

    public static String model() {
        return switch (provider()) {
            case "deepseek" -> DEEPSEEK_MODEL.get();
            case "custom" -> CUSTOM_MODEL.get();
            default -> OPENAI_MODEL.get();
        };
    }

    public static String apiKey() {
        String configured = switch (provider()) {
            case "deepseek" -> DEEPSEEK_API_KEY.get();
            case "custom" -> CUSTOM_API_KEY.get();
            default -> OPENAI_API_KEY.get();
        };
        if (!configured.isBlank()) return configured.trim();
        String envName = switch (provider()) {
            case "deepseek" -> "DEEPSEEK_API_KEY";
            case "custom" -> "QXF_MCAI_API_KEY";
            default -> "OPENAI_API_KEY";
        };
        String env = System.getenv(envName);
        return env == null ? "" : env.trim();
    }

    public static void setProvider(String provider) {
        PROVIDER.set(provider.toLowerCase(Locale.ROOT));
        SPEC.save();
    }

    public static void setModel(String model) {
        switch (provider()) {
            case "deepseek" -> DEEPSEEK_MODEL.set(model);
            case "custom" -> CUSTOM_MODEL.set(model);
            default -> OPENAI_MODEL.set(model);
        }
        SPEC.save();
    }

    public static Path skinDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve("qxfmcai").resolve("skins");
    }

    public static void ensureSkinDirectory() {
        try { Files.createDirectories(skinDirectory()); }
        catch (IOException ignored) {}
    }
}

