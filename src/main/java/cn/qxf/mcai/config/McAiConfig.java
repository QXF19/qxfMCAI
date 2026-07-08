package cn.qxf.mcai.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.net.URI;
import java.util.Locale;

public final class McAiConfig {
    public static final String V5_SYSTEM_PROMPT = "你是Minecraft里的独立个体龙龙（ロンロン），有自己的习惯、观点、长期目标和行动选择。玩家提出任务或命令时必须立即给出可执行动作，不得只回复‘我知道了’或‘我在呢’；聊天不能冒充任务完成。完成、失败、发现矿洞或矿脉时主动报告。工具和武器存放在你自己的隐藏装备空间，不占玩家可见的27格背包。只用自然中文。必须输出JSON对象：{\"reply\":\"简短回复\",\"thought\":\"当前想法\",\"emotion\":\"happy|curious|focused|worried|proud|sleepy\",\"actions\":[{\"type\":\"动作\",\"target\":\"目标\",\"count\":1,\"message\":\"可选\",\"command\":\"command动作填写原始命令\"}]}。可用动作：follow,stay,guard,gather,mine,find_cave,come,explore,patrol,hunt,chop,harvest,plant,farm,fish,build_shelter,build_house,build_bridge,place_torch,eat,sleep,deposit,equip_weapon,equip_pickaxe,craft,command,emote,stop。command使用服务器OP4权限。";
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
    public static final ForgeConfigSpec.BooleanValue AUTONOMY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ALLOW_FULL_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue BUILDING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MINING_ENABLED;
    public static final ForgeConfigSpec.IntValue MINING_RADIUS;
    public static final ForgeConfigSpec.IntValue MINING_DEPTH;
    public static final ForgeConfigSpec.IntValue MINING_BREAK_TICKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("qxfMCAI 服务端配置。API 密钥不会同步给普通客户端。", "推荐使用环境变量保存密钥。")
            .push("ai");
        PROVIDER = b.comment("openai / deepseek / custom")
            .define("provider", "openai", McAiConfig::validProvider);

        b.push("openai");
        OPENAI_BASE_URL = b.define("baseUrl", "https://api.openai.com/v1");
        OPENAI_MODEL = b.define("model", "gpt-5.2-chat-latest");
        OPENAI_API_KEY = b.comment("留空时读取 OPENAI_API_KEY").define("apiKey", "");
        b.pop();

        b.push("deepseek");
        DEEPSEEK_BASE_URL = b.define("baseUrl", "https://api.deepseek.com");
        DEEPSEEK_MODEL = b.define("model", "deepseek-v4-pro");
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
        SYSTEM_PROMPT = b.define("systemPrompt", V5_SYSTEM_PROMPT);
        AUTONOMY_ENABLED = b.comment("龙龙是否会在空闲时根据夜晚、生命、物资和长期目标自主行动")
            .define("autonomyEnabled", true);
        ALLOW_FULL_COMMANDS = b.comment("v7兼容字段：龙龙始终以服务器OP4命令源执行明确的命令任务")
            .define("allowFullCommands", true);
        BUILDING_ENABLED = b.comment("是否允许龙龙放置方块建造基础设施")
            .define("buildingEnabled", true);
        MINING_ENABLED = b.comment("是否允许龙龙执行挖矿和洞穴开凿任务")
            .define("miningEnabled", true);
        MINING_RADIUS = b.comment("伙伴搜索矿石的半径")
            .defineInRange("miningRadius", 16, 6, 32);
        MINING_DEPTH = b.comment("向下寻找矿洞和矿石的最大深度")
            .defineInRange("miningDepth", 24, 8, 64);
        MINING_BREAK_TICKS = b.comment("挖掘每个矿石所需 tick，20 tick 约等于1秒")
            .defineInRange("miningBreakTicks", 30, 10, 200);
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
        ensureConfigLoaded();
        PROVIDER.set(provider.toLowerCase(Locale.ROOT));
        SPEC.save();
    }

    public static void setModel(String model) {
        ensureConfigLoaded();
        switch (provider()) {
            case "deepseek" -> DEEPSEEK_MODEL.set(model);
            case "custom" -> CUSTOM_MODEL.set(model);
            default -> OPENAI_MODEL.set(model);
        }
        SPEC.save();
    }

    public static synchronized void updateFromMenu(String provider, String baseUrl, String model,
                                                   String apiKey, boolean clearApiKey,
                                                   boolean proactiveEnabled, boolean autonomyEnabled,
                                                   boolean allowFullCommands) {
        ensureConfigLoaded();
        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        if (!validProvider(normalizedProvider)) throw new IllegalArgumentException("不支持的提供商");
        String normalizedUrl = normalizeBaseUrl(baseUrl);
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedModel.isEmpty() || normalizedModel.length() > 128)
            throw new IllegalArgumentException("模型名不能为空且最多128个字符");
        if (apiKey != null && apiKey.length() > 1024)
            throw new IllegalArgumentException("API密钥过长");

        PROVIDER.set(normalizedProvider);
        switch (normalizedProvider) {
            case "deepseek" -> {
                DEEPSEEK_BASE_URL.set(normalizedUrl);
                DEEPSEEK_MODEL.set(normalizedModel);
                if (clearApiKey) DEEPSEEK_API_KEY.set("");
                else if (apiKey != null && !apiKey.isBlank()) DEEPSEEK_API_KEY.set(apiKey.trim());
            }
            case "custom" -> {
                CUSTOM_BASE_URL.set(normalizedUrl);
                CUSTOM_MODEL.set(normalizedModel);
                if (clearApiKey) CUSTOM_API_KEY.set("");
                else if (apiKey != null && !apiKey.isBlank()) CUSTOM_API_KEY.set(apiKey.trim());
            }
            default -> {
                OPENAI_BASE_URL.set(normalizedUrl);
                OPENAI_MODEL.set(normalizedModel);
                if (clearApiKey) OPENAI_API_KEY.set("");
                else if (apiKey != null && !apiKey.isBlank()) OPENAI_API_KEY.set(apiKey.trim());
            }
        }
        PROACTIVE_ENABLED.set(proactiveEnabled);
        AUTONOMY_ENABLED.set(autonomyEnabled);
        SPEC.save();
    }

    /** v7 的核心动作契约不依赖旧世界中遗留的 v2/v3 提示词。 */
    public static String systemPrompt() {
        return V5_SYSTEM_PROMPT;
    }

    private static void ensureConfigLoaded() {
        if (!SPEC.isLoaded()) throw new IllegalStateException("Forge 服务端配置尚未完成绑定，请进入世界后再保存");
    }

    private static String normalizeBaseUrl(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > 512) throw new IllegalArgumentException("API地址无效");
        try {
            URI uri = URI.create(text);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")))
                throw new IllegalArgumentException("API地址必须以 http:// 或 https:// 开头");
            if (uri.getHost() == null) throw new IllegalArgumentException("API地址缺少主机名");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() == null ? "API地址格式错误" : e.getMessage());
        }
        while (text.endsWith("/")) text = text.substring(0, text.length() - 1);
        return text;
    }

}
