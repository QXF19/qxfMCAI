package cn.qxf.mcai.network;

import cn.qxf.mcai.QxfMcAi;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import cn.qxf.mcai.server.CompanionManager;

public final class ModNetwork {
    private static final String PROTOCOL = "12";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(QxfMcAi.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static boolean registered;

    private ModNetwork() {}

    public static synchronized void register() {
        if (registered) return;
        int id = 0;
        CHANNEL.registerMessage(id, UpdateAiConfigPacket.class,
            UpdateAiConfigPacket::encode,
            UpdateAiConfigPacket::decode,
            UpdateAiConfigPacket::handle);
        id++;
        CHANNEL.registerMessage(id, RequestAiConfigPacket.class,
            RequestAiConfigPacket::encode,
            RequestAiConfigPacket::decode,
            RequestAiConfigPacket::handle);
        id++;
        CHANNEL.registerMessage(id, AiConfigSnapshotPacket.class,
            AiConfigSnapshotPacket::encode,
            AiConfigSnapshotPacket::decode,
            AiConfigSnapshotPacket::handle);
        id++;
        CHANNEL.registerMessage(id, OpenGameBoardPacket.class,
            OpenGameBoardPacket::encode,
            OpenGameBoardPacket::decode,
            OpenGameBoardPacket::handle);
        id++;
        CHANNEL.registerMessage(id, GameBoardActionPacket.class,
            GameBoardActionPacket::encode,
            GameBoardActionPacket::decode,
            GameBoardActionPacket::handle);
        registered = true;
    }

    public static void openGameBoard(ServerPlayer player, BlockPos pos, int selectedGame, String message) {
        var companion = CompanionManager.find(player);
        if (companion == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            OpenGameBoardPacket.from(pos, selectedGame, message, companion));
    }
}
