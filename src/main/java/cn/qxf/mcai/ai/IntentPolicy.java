package cn.qxf.mcai.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 本地意图边界不受模型回复及可编辑提示词覆盖。 */
public final class IntentPolicy {
    private IntentPolicy() {}
    public static String control(String prompt) {
        String text = prompt == null ? "" : prompt.trim().replaceAll("[。！!，, ]+$", "");
        if (Set.of("停", "停下", "停止", "立即停止", "停止任务", "取消任务", "别做了", "停止建造",
                "不要再建", "别建了", "别挖了", "先停下").contains(text)) return "stop";
        if (Set.of("暂停", "暂停任务", "先暂停", "等一下").contains(text)) return "pause";
        if (Set.of("继续", "继续任务", "恢复任务", "继续干活").contains(text)) return "resume";
        if (Set.of("跟着我", "跟随我", "跟随", "陪我走").contains(text)) return "follow";
        if (Set.of("过来", "回来", "回到我身边", "来我这").contains(text)) return "come";
        if (Set.of("原地等", "在这等", "待在这", "不要跟", "别跟").contains(text)) return "stay";
        return "";
    }

    public static boolean isConversation(String prompt) {
        String text = prompt == null ? "" : prompt.trim();
        if (text.startsWith("/")) return false;
        if (!control(text).isEmpty()) return false;
        if (text.matches(".*(不要|不得|禁止|别再|不准|别去|不需要|停止建|别挖|别建|别砍|只聊天|只是聊|暂时不).*")) return true;
        // 明确委托疑问句仍然是任务，“这里有铁矿吗”等询问信息则只聊天。
        if (text.matches(".*(能不能|可以|能否|能).*帮我.*")) return false;
        if (text.matches(".*(请|麻烦你|替我).*(建造|建房|挖矿|找矿|收集|砍树|种地|巡逻).*")) return false;
        return text.matches(".*(怎么|如何|为什么|觉得|建议|适合|喜欢|有没有|是否|吗|？|\\?).*");
    }

    public static boolean requestsTask(String text, List<AgentAction> local) {
        return !isConversation(text) && (!local.isEmpty() || text.matches("^(请|帮我|替我|麻烦你|去|把).+"));
    }

    public static List<AgentAction> resolve(List<AgentAction> planned, List<AgentAction> required, boolean task) {
        if (!task) return List.of();
        // 命令保持玩家逐字授权，不采用模型改写的命令或自行提权。
        if (required.stream().anyMatch(a -> a.type().equals("command"))) return required;
        boolean building = required.stream().anyMatch(a -> a.type().startsWith("build_"));
        List<AgentAction> result = new ArrayList<>();
        for (AgentAction action : planned) {
            if (action.type().equals("command") || action.type().equals("stop")
                    || (action.type().startsWith("build_") && !building)) continue;
            if (result.size() < 8 && !result.contains(action)) result.add(action);
        }
        for (AgentAction action : required) {
            boolean covered = result.stream().anyMatch(a -> a.type().equals(action.type()));
            if (!covered) {
                if (result.size() == 8) result.remove(result.size() - 1);
                result.add(action);
            }
        }
        return List.copyOf(result);
    }
}
