package cn.qxf.mcai.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不依赖网络的中文任务规划器。
 * 常见生存任务在服务端转成安全保底动作；正常情况下仍由 API 优先规划。
 */
public final class LocalTaskPlanner {
    private static final Pattern ARABIC_NUMBER = Pattern.compile("(\\d{1,3})");
    private static final Map<Character, Integer> CHINESE_NUMBERS = Map.ofEntries(
        Map.entry('一', 1), Map.entry('二', 2), Map.entry('两', 2), Map.entry('三', 3),
        Map.entry('四', 4), Map.entry('五', 5), Map.entry('六', 6), Map.entry('七', 7),
        Map.entry('八', 8), Map.entry('九', 9), Map.entry('十', 10));

    private LocalTaskPlanner() {}

    public static List<AgentAction> plan(String prompt) {
        String raw = prompt == null ? "" : prompt.trim();
        String text = raw.toLowerCase(Locale.ROOT);
        LinkedHashMap<String, AgentAction> actions = new LinkedHashMap<>();

        String command = extractCommand(raw);
        if (!command.isBlank()) {
            add(actions, new AgentAction("command", "", 1, "", command));
            return List.copyOf(actions.values());
        }

        if (containsAny(text, "停下", "立即停止", "停止任务", "取消任务", "别做了"))
            add(actions, AgentAction.simple("stop"));
        if (containsAny(text, "原地等", "在这等", "待在这", "不要跟", "别跟"))
            add(actions, AgentAction.simple("stay"));
        else if (containsAny(text, "跟着我", "跟随我", "陪我走", "和我一起走"))
            add(actions, AgentAction.simple("follow"));
        if (containsAny(text, "过来", "回到我身边", "来我这", "到我这里"))
            add(actions, AgentAction.simple("come"));
        if (containsAny(text, "守卫", "警戒", "保护我", "保护基地"))
            add(actions, AgentAction.simple("guard"));

        if (containsAny(text, "找矿洞", "寻找矿洞", "找洞穴", "天然矿洞", "地下空间", "找地下洞"))
            add(actions, AgentAction.simple("find_cave"));
        else if (containsAny(text, "挖矿", "采矿", "下矿", "找矿", "矿石", "铁矿", "煤矿", "金矿",
                "钻石", "红石", "青金石", "铜矿", "绿宝石"))
            add(actions, new AgentAction("mine", "ores", numberHint(text, 3), "", ""));

        if (containsAny(text, "砍树", "伐木", "收集木头", "收集原木", "弄点木头"))
            add(actions, new AgentAction("chop", "logs", numberHint(text, 4), "", ""));
        boolean forbidsBuilding = containsAny(text, "不要建", "别建", "禁止建", "不准建", "不得建",
            "不得自主建", "停止建造", "只聊天");
        if (!forbidsBuilding) {
            if (containsAny(text, "造桥", "建桥", "搭桥", "建造一座桥"))
                add(actions, new AgentAction("build_bridge", raw, 1, "", ""));
            else if (containsAny(text, "庇护所", "避难所"))
                add(actions, new AgentAction("build_shelter", raw, 1, "", ""));
            else if (containsAny(text, "建房", "造房", "房子", "小屋", "住处", "仓库", "农舍",
                    "工作间", "瞭望塔", "基础设施", "建造"))
                add(actions, new AgentAction("build_house", raw, 1, "", ""));
        }

        if (containsAny(text, "种地", "农田", "耕种", "收割", "收获庄稼", "播种", "农作"))
            add(actions, new AgentAction("farm", "crops", numberHint(text, 3), "", ""));
        if (containsAny(text, "打怪", "战斗", "清理怪", "杀怪", "消灭敌人", "狩猎"))
            add(actions, new AgentAction("hunt", "monsters", numberHint(text, 3), "", ""));
        if (containsAny(text, "探索", "去看看附近", "去远处看看"))
            add(actions, AgentAction.simple("explore"));
        if (containsAny(text, "巡逻", "巡视", "检查基地"))
            add(actions, new AgentAction("patrol", "home", numberHint(text, 2), "", ""));
        if (containsAny(text, "钓鱼", "去钓鱼")) add(actions, AgentAction.simple("fish"));
        if (containsAny(text, "捡东西", "拾取", "收集掉落物", "捡起附近"))
            add(actions, new AgentAction("gather", "drops", numberHint(text, 8), "", ""));
        if (containsAny(text, "放火把", "插火把", "点亮这里", "照明"))
            add(actions, AgentAction.simple("place_torch"));
        if (containsAny(text, "整理背包", "放进箱子", "存进箱子", "卸货"))
            add(actions, AgentAction.simple("deposit"));
        if (containsAny(text, "换武器", "装备武器", "拿剑", "拿弓"))
            add(actions, AgentAction.simple("equip_weapon"));
        if (containsAny(text, "换镐子", "装备镐子", "拿镐子"))
            add(actions, AgentAction.simple("equip_pickaxe"));
        if (containsAny(text, "吃东西", "补充体力", "吃点食物")) add(actions, AgentAction.simple("eat"));
        if (containsAny(text, "睡觉", "休息一会")) add(actions, AgentAction.simple("sleep"));
        if (containsAny(text, "制作基础材料", "合成木板", "合成木棍")) add(actions, AgentAction.simple("craft"));

        return List.copyOf(actions.values());
    }

    public static String summary(List<AgentAction> actions) {
        List<String> names = new ArrayList<>();
        for (AgentAction action : actions) names.add(switch (action.type()) {
            case "follow" -> "跟随"; case "stay" -> "原地等待"; case "guard" -> "守卫";
            case "gather" -> "拾取物品"; case "mine" -> "挖矿"; case "find_cave" -> "寻找矿洞";
            case "come" -> "回到身边"; case "explore" -> "探索"; case "patrol" -> "巡逻";
            case "hunt" -> "战斗"; case "chop" -> "伐木"; case "farm" -> "照料农田";
            case "fish" -> "钓鱼"; case "build_shelter" -> "建庇护所"; case "build_house" -> "建房";
            case "build_bridge" -> "建桥"; case "place_torch" -> "放火把"; case "deposit" -> "整理入箱";
            case "equip_weapon" -> "装备武器"; case "equip_pickaxe" -> "装备镐子";
            case "command" -> "执行命令"; case "stop" -> "停止任务"; default -> action.type();
        });
        return String.join(" → ", names);
    }

    private static void add(LinkedHashMap<String, AgentAction> actions, AgentAction action) {
        actions.putIfAbsent(action.type(), action);
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private static String extractCommand(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        String[] markers = {"执行命令", "运行命令", "使用命令", "输入命令"};
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index < 0) continue;
            String command = raw.substring(index + marker.length()).trim();
            while (command.startsWith(":") || command.startsWith("：") || command.startsWith("/"))
                command = command.substring(1).trim();
            return command;
        }
        return raw.startsWith("/") ? raw.substring(1).trim() : "";
    }

    private static int numberHint(String text, int fallback) {
        Matcher matcher = ARABIC_NUMBER.matcher(text);
        if (matcher.find()) {
            try { return Math.max(1, Math.min(64, Integer.parseInt(matcher.group(1)))); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        for (int i = 0; i < text.length(); i++) {
            Integer value = CHINESE_NUMBERS.get(text.charAt(i));
            if (value != null) return value;
        }
        return fallback;
    }
}
