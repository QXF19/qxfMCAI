package cn.qxf.mcai.ai;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IntentPolicyTest {
    @Test void questionsDoNotExecuteEvenIfTheyContainTaskKeywords() {
        for (String text : List.of("这里有铁矿吗？", "你觉得应该建房子吗？", "不要建房子，只聊天")) {
            assertTrue(IntentPolicy.isConversation(text));
            assertTrue(LocalTaskPlanner.plan(text).isEmpty());
        }
        assertFalse(IntentPolicy.isConversation("能不能帮我挖矿？"));
        assertEquals("mine", LocalTaskPlanner.plan("能不能帮我挖矿？").get(0).type());
    }

    @Test void modelCannotInventCommandsOrBuildings() {
        var planned = List.of(AgentAction.simple("command"), AgentAction.simple("build_house"), AgentAction.simple("mine"));
        var result = IntentPolicy.resolve(planned, List.of(AgentAction.simple("mine")), true);
        assertEquals(List.of("mine"), result.stream().map(AgentAction::type).toList());
        assertTrue(IntentPolicy.resolve(planned, List.of(), false).isEmpty());
        var explicit = new AgentAction("command", "", 1, "", "time set day");
        assertEquals(List.of(explicit), IntentPolicy.resolve(planned, List.of(explicit), true));
    }

    @Test void canceledOrReplacedRequestsCannotCommit() {
        var ledger = new RequestLedger(1);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        long old = ledger.begin(a);
        assertEquals(-1, ledger.begin(b));
        long replacement = ledger.begin(a);
        assertFalse(ledger.complete(a, old));
        ledger.cancel(a);
        assertFalse(ledger.complete(a, replacement));
        assertTrue(ledger.complete(b, ledger.begin(b)));
    }
}
