package cn.qxf.mcai.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端到客户端的脱敏 AI 设置快照。API 密钥不是该数据结构的一部分。 */
public record AiConfigSnapshotPacket(String provider, String baseUrl, String model,
                                     boolean proactiveEnabled, boolean autonomyEnabled,
                                     String corePrompt, String taskPrompt, String autonomyPrompt,
                                     String proactiveChatPrompt) {
    public static void encode(AiConfigSnapshotPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.provider, 16);
        buffer.writeUtf(message.baseUrl, 512);
        buffer.writeUtf(message.model, 128);
        buffer.writeBoolean(message.proactiveEnabled);
        buffer.writeBoolean(message.autonomyEnabled);
        buffer.writeUtf(message.corePrompt, 8192);
        buffer.writeUtf(message.taskPrompt, 8192);
        buffer.writeUtf(message.autonomyPrompt, 8192);
        buffer.writeUtf(message.proactiveChatPrompt, 8192);
    }

    public static AiConfigSnapshotPacket decode(FriendlyByteBuf buffer) {
        return new AiConfigSnapshotPacket(buffer.readUtf(16), buffer.readUtf(512), buffer.readUtf(128),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(8192), buffer.readUtf(8192),
            buffer.readUtf(8192), buffer.readUtf(8192));
    }

    public static void handle(AiConfigSnapshotPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandlers.applyAiConfigSnapshot(message)));
        context.setPacketHandled(true);
    }
}
