package cn.qxf.mcai.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/** 菜单按秒轮询；服务端限制每人最多每十 tick 回复一次。 */
public record RequestAgentStatusPacket() {
    private static final Map<UUID, Integer> LAST_REPLY_TICK = new ConcurrentHashMap<>();

    public static void encode(RequestAgentStatusPacket ignored, FriendlyByteBuf buf) {}
    public static RequestAgentStatusPacket decode(FriendlyByteBuf buf) { return new RequestAgentStatusPacket(); }

    public static void handle(RequestAgentStatusPacket ignored, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            int now = player.server.getTickCount();
            Integer last = LAST_REPLY_TICK.get(player.getUUID());
            if (last != null && now - last >= 0 && now - last < 10) return;
            LAST_REPLY_TICK.put(player.getUUID(), now);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), AgentStatusPacket.from(player));
        });
        context.setPacketHandled(true);
    }

    public static void clearPlayer(UUID playerId) { LAST_REPLY_TICK.remove(playerId); }
    public static void clear() { LAST_REPLY_TICK.clear(); }
}
