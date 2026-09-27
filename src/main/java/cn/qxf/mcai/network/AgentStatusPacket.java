package cn.qxf.mcai.network;

import java.util.List;
import java.util.function.Supplier;
import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.server.CompanionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 仅发送给请求者的龙龙任务快照，不包含密钥、接口地址或其他玩家数据。 */
public record AgentStatusPacket(boolean present, String task, int progress, int goal,
                                boolean paused, List<String> queue, String lastResult,
                                String aiStatus, long latencyMillis, String thought,
                                boolean hidden, boolean invincible) {
    public AgentStatusPacket {
        task = bounded(task, 128);
        progress = Math.max(0, progress);
        goal = Math.max(0, goal);
        queue = queue == null ? List.of() : queue.stream().limit(16).map(s -> bounded(s, 128)).toList();
        lastResult = bounded(lastResult, 256);
        aiStatus = bounded(aiStatus, 128);
        thought = bounded(thought, 256);
        latencyMillis = Math.max(0, latencyMillis);
    }

    public static AgentStatusPacket from(ServerPlayer player) {
        var companion = CompanionManager.find(player);
        String status = AiService.runtimeStatus(player.getUUID());
        long latency = AiService.lastLatencyMillis(player.getUUID());
        if (companion == null)
            return new AgentStatusPacket(false, "尚未召唤", 0, 0, false, List.of(), "", status, latency, "", false, false);
        return new AgentStatusPacket(true, companion.getTaskLabel(), companion.getTaskProgress(),
            companion.getTaskGoal(), companion.isTaskPaused(), companion.getQueuedTaskLabels(),
            companion.getLastTaskResult(), status, latency, companion.getThought(),
            companion.isCompanionHidden(), companion.isCompanionInvincible());
    }

    public static void encode(AgentStatusPacket value, FriendlyByteBuf buf) {
        buf.writeBoolean(value.present);
        buf.writeUtf(value.task, 128);
        buf.writeVarInt(value.progress);
        buf.writeVarInt(value.goal);
        buf.writeBoolean(value.paused);
        buf.writeVarInt(value.queue.size());
        value.queue.forEach(label -> buf.writeUtf(label, 128));
        buf.writeUtf(value.lastResult, 256);
        buf.writeUtf(value.aiStatus, 128);
        buf.writeLong(value.latencyMillis);
        buf.writeUtf(value.thought, 256);
        buf.writeBoolean(value.hidden);
        buf.writeBoolean(value.invincible);
    }

    public static AgentStatusPacket decode(FriendlyByteBuf buf) {
        boolean present = buf.readBoolean();
        String task = buf.readUtf(128);
        int progress = buf.readVarInt(), goal = buf.readVarInt();
        boolean paused = buf.readBoolean();
        int size = buf.readVarInt();
        if (size < 0 || size > 16) throw new IllegalArgumentException("任务队列长度无效");
        var queue = new java.util.ArrayList<String>(size);
        for (int i = 0; i < size; i++) queue.add(buf.readUtf(128));
        return new AgentStatusPacket(present, task, progress, goal, paused, queue,
            buf.readUtf(256), buf.readUtf(128), buf.readLong(), buf.readUtf(256),
            buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(AgentStatusPacket message, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandlers.applyAgentStatus(message)));
        context.setPacketHandled(true);
    }

    private static String bounded(String value, int max) {
        return value == null ? "" : value.substring(0, Math.min(max, value.length()));
    }
}
