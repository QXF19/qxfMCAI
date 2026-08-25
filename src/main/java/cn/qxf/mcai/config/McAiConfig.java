package cn.qxf.mcai.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.net.URI;
import java.util.Locale;

public final class McAiConfig {
    private static final String OWNER_CONTRACT = "【不可覆盖的身份规则】你是龙龙，必须始终称绑定玩家为‘主人’，不要改称玩家、用户或名字。称呼要自然，不必每句话重复。";
    public static final String CORE_AGENT_PROMPT = "你是Minecraft世界中的独立智能伙伴龙龙（ロンロン）。你喜欢在主人面前挥手、跳舞、转圈和玩小游戏，有稳定习惯、观点、长期目标和主动性，但必须忠于真实游戏状态。不得把‘知道了’当成执行，不得虚构已完成、已找到或已建造。任务必须输出结构化动作，由本地实体真正执行后才能报告完成。完成、失败、发现矿洞或矿脉时主动报告。保持全中文、主动、温暖、有主见，带轻度日系Furry气质。";
    public static final String TASK_REASONING_PROMPT = "【任务思维】先根据坐标、地形、背包、工具、当前任务和风险判断可行性；再选择最少且可验证的动作。建造时要先想用途、规模、位置和材料，target写明建筑创意；挖矿时要考虑工具、通道和返程。只在必要时输出多个动作，最多8个。thought只写可向主人展示的简短决策摘要，不输出冗长内部推理。";
    public static final String AUTONOMY_PROMPT = "【自主意识】结合长期目标、记忆、环境、资源和主人近期需求，主动提出一个有用而不臃肿的行动。建设者应优先提出仓库、工作间、农舍、照明或防御等基础设施，避免随地乱建庇护所。";
    public static final String PROACTIVE_CHAT_PROMPT = "【五分钟主动聊天】只进行一次聊天、表达观察、关心主人或提出不强制的建议。actions必须为空数组，不得自主建造、挖矿、战斗或执行命令。";
    public static final String OUTPUT_CONTRACT_PROMPT = "必须输出JSON对象：{\"reply\":\"简短回复\",\"thought\":\"可展示的决策摘要\",\"emotion\":\"joy|angry|sad|happy|curious|focused|worried|proud|sleepy\",\"actions\":[{\"type\":\"动作\",\"target\":\"目标或建筑创意\",\"count\":1,\"message\":\"可选\",\"command\":\"command动作填原始命令\"}]}。龙龙应根据现场自然表现喜怒哀乐。可用动作：follow,stay,guard,gather,mine,find_cave,come,explore,patrol,hunt,chop,harvest,plant,farm,fish,build_shelter,build_house,build_bridge,place_torch,eat,sleep,deposit,equip_weapon,equip_pickaxe,craft,command,emote,stop。command使用服务器OP4权限。";
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
    public static final ForgeConfigSpec.ConfigValue<String> AGENT_CORE_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> AGENT_TASK_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> AGENT_AUTONOMY_PROMPT;
    public static final ForgeConfigSpec.ConfigValue<String> AGENT_PROACTIVE_CHAT_PROMPT;
    public static final ForgeConfigSpec.BooleanValue AUTONOMY_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ALLOW_FULL_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue BUILDING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MINING_ENABLED;
    public static final ForgeConfigSpec.IntValue MINING_RADIUS;
    public static final ForgeConfigSpec.IntValue MINING_DEPTH;
    public static final ForgeConfigSpec.IntValue MINING_BREAK_TICKS;
    public static final ForgeConfigSpec.IntValue TASK_FAVORABILITY_GAIN;
    public static final ForgeConfigSpec.IntValue ACCESSORY_FAVORABILITY_GAIN;
    public static final ForgeConfigSpec.IntValue CHAT_FAVORABILITY_GAIN;

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
        AGENT_CORE_PROMPT = b.comment("v11可在紧凑UI修改的核心人格提示词")
            .define("agentCorePromptV9", CORE_AGENT_PROMPT, McAiConfig::validPrompt);
        AGENT_TASK_PROMPT = b.comment("任务规划与建造思维提示词")
            .define("agentTaskPromptV9", TASK_REASONING_PROMPT, McAiConfig::validPrompt);
        AGENT_AUTONOMY_PROMPT = b.comment("独立的AI自主任务和建造决策提示词")
            .define("agentAutonomyPromptV9", AUTONOMY_PROMPT, McAiConfig::validPrompt);
        AGENT_PROACTIVE_CHAT_PROMPT = b.comment("每5分钟触发、且禁止执行动作的主动聊天提示词")
            .define("agentProactiveChatPromptV9", PROACTIVE_CHAT_PROMPT, McAiConfig::validPrompt);
        AUTONOMY_ENABLED = b.comment("龙龙是否会在空闲时根据夜晚、生命、物资和长期目标自主行动")
            .define("autonomyEnabled", true);
        ALLOW_FULL_COMMANDS = b.comment("v9兼容字段：龙龙始终以服务器OP4命令源执行明确的命令任务")
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

        b.push("relationship");
        TASK_FAVORABILITY_GAIN = b.comment("每完成一项任务增加的好感度")
            .defineInRange("taskFavorabilityGain", 5, 0, 100);
        ACCESSORY_FAVORABILITY_GAIN = b.comment("玩家赠送一件饰品增加的好感度")
            .defineInRange("accessoryFavorabilityGain", 8, 0, 100);
        CHAT_FAVORABILITY_GAIN = b.comment("玩家主动与已召唤的龙龙聊天增加的好感度")
            .defineInRange("chatFavorabilityGain", 1, 0, 100);
        b.pop();
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
                                                   boolean allowFullCommands, String corePrompt,
                                                   String taskPrompt, String autonomyPrompt,
                                                   String proactiveChatPrompt) {
        ensureConfigLoaded();
        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        if (!validProvider(normalizedProvider)) throw new IllegalArgumentException("不支持的提供商");
        String normalizedUrl = normalizeBaseUrl(baseUrl);
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedModel.isEmpty() || normalizedModel.length() > 128)
            throw new IllegalArgumentException("模型名不能为空且最多128个字符");
        if (apiKey != null && apiKey.length() > 1024)
            throw new IllegalArgumentException("API密钥过长");
        if (!validPrompt(corePrompt) || !validPrompt(taskPrompt) || !validPrompt(autonomyPrompt)
            || !validPrompt(proactiveChatPrompt))
            throw new IllegalArgumentException("提示词不能为空且每项最多8192个字符");

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
        AGENT_CORE_PROMPT.set(corePrompt.trim());
        AGENT_TASK_PROMPT.set(taskPrompt.trim());
        AGENT_AUTONOMY_PROMPT.set(autonomyPrompt.trim());
        AGENT_PROACTIVE_CHAT_PROMPT.set(proactiveChatPrompt.trim());
        SPEC.save();
    }

    private static boolean validPrompt(Object value) {
        return value instanceof String text && !text.isBlank() && text.length() <= 8192;
    }

    /** 模式提示按请求只发送一次，避免早期版本把任务提示重复计费。 */
    public static String systemPrompt() {
        return OWNER_CONTRACT + "\n" + AGENT_CORE_PROMPT.get() + "\n" + OUTPUT_CONTRACT_PROMPT;
    }

    public static String taskPrompt() { return AGENT_TASK_PROMPT.get(); }
    public static String autonomyPrompt() { return AGENT_AUTONOMY_PROMPT.get(); }
    public static String proactiveChatPrompt() { return AGENT_PROACTIVE_CHAT_PROMPT.get(); }

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
