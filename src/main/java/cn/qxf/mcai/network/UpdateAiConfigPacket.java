package cn.qxf.mcai.network;

import cn.qxf.mcai.config.McAiConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateAiConfigPacket(String provider, String baseUrl, String model, String apiKey,
                                   boolean clearApiKey, boolean proactiveEnabled,
                                   boolean autonomyEnabled, boolean allowFullCommands,
                                   String corePrompt, String taskPrompt, String autonomyPrompt,
                                   String proactiveChatPrompt) {
    public static void encode(UpdateAiConfigPacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.provider, 16);
        buffer.writeUtf(message.baseUrl, 512);
        buffer.writeUtf(message.model, 128);
        buffer.writeUtf(message.apiKey, 1024);
        buffer.writeBoolean(message.clearApiKey);
        buffer.writeBoolean(message.proactiveEnabled);
        buffer.writeBoolean(message.autonomyEnabled);
        buffer.writeBoolean(message.allowFullCommands);
        buffer.writeUtf(message.corePrompt, 8192);
        buffer.writeUtf(message.taskPrompt, 8192);
        buffer.writeUtf(message.autonomyPrompt, 8192);
        buffer.writeUtf(message.proactiveChatPrompt, 8192);
    }

    public static UpdateAiConfigPacket decode(FriendlyByteBuf buffer) {
        return new UpdateAiConfigPacket(
            buffer.readUtf(16), buffer.readUtf(512), buffer.readUtf(128), buffer.readUtf(1024),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readUtf(8192), buffer.readUtf(8192), buffer.readUtf(8192), buffer.readUtf(8192));
    }

    public static void handle(UpdateAiConfigPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            if (!sender.hasPermissions(4)) {
                sender.sendSystemMessage(Component.literal("[qxfMCAI] 保存API配置需要OP权限等级4。")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            try {
                McAiConfig.updateFromMenu(message.provider, message.baseUrl, message.model,
                    message.apiKey, message.clearApiKey, message.proactiveEnabled,
                    message.autonomyEnabled, message.allowFullCommands, message.corePrompt,
                    message.taskPrompt, message.autonomyPrompt, message.proactiveChatPrompt);
                sender.sendSystemMessage(Component.literal("[qxfMCAI] API配置已安全保存；密钥不会回传到客户端。")
                    .withStyle(ChatFormatting.GREEN));
            } catch (RuntimeException error) {
                sender.sendSystemMessage(Component.literal("[qxfMCAI] 配置未保存：" + error.getMessage())
                    .withStyle(ChatFormatting.RED));
            }
        });
        context.setPacketHandled(true);
    }
}
