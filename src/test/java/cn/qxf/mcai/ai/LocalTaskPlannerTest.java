package cn.qxf.mcai.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalTaskPlannerTest {
    @Test void bridgeIntentWinsOverGenericBuildingWord() {
        List<AgentAction> actions = LocalTaskPlanner.plan("在这里建造一座桥");
        assertEquals("build_bridge", actions.get(0).type());
        assertFalse(has(actions, "build_house"));
    }

    @Test void explicitNoBuildingIsNeverConvertedIntoBuilding() {
        assertFalse(has(LocalTaskPlanner.plan("只聊天，不得自主建造或执行任务"), "build_house"));
        assertFalse(has(LocalTaskPlanner.plan("不要建庇护所"), "build_shelter"));
    }

    @Test void apiBuildingIdeaIsPreservedAsTarget() {
        List<AgentAction> actions = LocalTaskPlanner.plan("观察基地并设计一个集中小仓库");
        assertEquals("build_house", actions.get(0).type());
        assertTrue(actions.get(0).target().contains("仓库"));
    }

    @Test void chineseCountAndTaskSynonymAreUnderstood() {
        AgentAction action = LocalTaskPlanner.plan("砍五棵树并收集原木").get(0);
        assertEquals("chop", action.type());
        assertEquals(5, action.count());
    }

    @Test void caveAndOreTasksStayDistinct() {
        assertEquals("find_cave", LocalTaskPlanner.plan("向下找天然矿洞").get(0).type());
        assertEquals("mine", LocalTaskPlanner.plan("找三块铁矿").get(0).type());
    }

    @Test void commandPreservesOriginalCaseAndStripsSlash() {
        AgentAction action = LocalTaskPlanner.plan("执行命令 /Gamerule doDaylightCycle false").get(0);
        assertEquals("command", action.type());
        assertEquals("Gamerule doDaylightCycle false", action.command());
    }

    private static boolean has(List<AgentAction> actions, String type) {
        return actions.stream().anyMatch(action -> action.type().equals(type));
    }
}
