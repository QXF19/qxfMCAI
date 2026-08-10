package cn.qxf.mcai.network;

import cn.qxf.mcai.config.McAiConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** 请求不含任何数据；服务端只返回可公开的设置，永不返回 API 密钥。 */
public record RequestAiConfigPacket() {
    public static void encode(RequestAiConfigPacket ignored, FriendlyByteBuf buffer) {}

    public static RequestAiConfigPacket decode(FriendlyByteBuf buffer) { return new RequestAiConfigPacket(); }

    public static void handle(RequestAiConfigPacket ignored, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !sender.hasPermissions(4)) return;
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new AiConfigSnapshotPacket(
                McAiConfig.provider(), McAiConfig.baseUrl(), McAiConfig.model(),
                McAiConfig.PROACTIVE_ENABLED.get(), McAiConfig.AUTONOMY_ENABLED.get(),
                McAiConfig.AGENT_CORE_PROMPT.get(), McAiConfig.AGENT_TASK_PROMPT.get(),
                McAiConfig.AGENT_AUTONOMY_PROMPT.get(), McAiConfig.AGENT_PROACTIVE_CHAT_PROMPT.get()));
        });
        context.setPacketHandled(true);
    }
}
