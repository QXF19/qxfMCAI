package cn.qxf.mcai.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 有界请求凭证：取消/替换后旧回复永远不能取得执行权。 */
public final class RequestLedger {
    private final int capacity;
    private final Map<UUID, Long> active = new HashMap<>();
    private long sequence;
    public RequestLedger(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized long begin(UUID owner) {
        if (!active.containsKey(owner) && active.size() >= capacity) return -1;
        long ticket = ++sequence;
        active.put(owner, ticket);
        return ticket;
    }
    public synchronized boolean complete(UUID owner, long ticket) {
        if (!Long.valueOf(ticket).equals(active.get(owner))) return false;
        active.remove(owner);
        return true;
    }
    public synchronized boolean pending(UUID owner) { return active.containsKey(owner); }
    public synchronized void cancel(UUID owner) { active.remove(owner); }
    public synchronized void clear() { active.clear(); }
}
