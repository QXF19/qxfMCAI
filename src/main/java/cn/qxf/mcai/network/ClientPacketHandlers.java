package cn.qxf.mcai.network;

import cn.qxf.mcai.client.AiControlScreen;
import net.minecraft.client.Minecraft;
import cn.qxf.mcai.client.GameBoardScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    static void applyAiConfigSnapshot(AiConfigSnapshotPacket message) {
        if (Minecraft.getInstance().screen instanceof AiControlScreen screen)
            screen.applyServerSnapshot(message);
    }

    static void openGameBoard(OpenGameBoardPacket message) {
        if (Minecraft.getInstance().screen instanceof GameBoardScreen screen
            && screen.isSameBoard(message.pos())) screen.applySnapshot(message);
        else Minecraft.getInstance().setScreen(new GameBoardScreen(message));
    }
}
