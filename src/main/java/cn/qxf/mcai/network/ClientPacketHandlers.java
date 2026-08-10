package cn.qxf.mcai.network;

import cn.qxf.mcai.client.AiControlScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    static void applyAiConfigSnapshot(AiConfigSnapshotPacket message) {
        if (Minecraft.getInstance().screen instanceof AiControlScreen screen)
            screen.applyServerSnapshot(message);
    }
}
