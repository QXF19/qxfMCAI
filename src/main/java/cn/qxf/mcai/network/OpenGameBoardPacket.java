package cn.qxf.mcai.network;

import cn.qxf.mcai.entity.AiCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenGameBoardPacket(
    BlockPos pos, int selectedGame, String message,
    byte[] gomokuBoard, boolean gomokuActive, int gomokuWins, int gomokuLosses,
    String xiangqiBoard, boolean xiangqiActive, int xiangqiOwnerWins, int xiangqiLonglongWins,
    byte[] goBoard, boolean goActive, int goOwnerWins, int goLonglongWins,
    int goOwnerCaptures, int goLonglongCaptures) {

    public static OpenGameBoardPacket from(BlockPos pos, int selectedGame, String message,
                                           AiCompanionEntity companion) {
        return new OpenGameBoardPacket(pos.immutable(), selectedGame, message,
            companion.gomokuBoardSnapshot(), companion.isGomokuActive(),
            companion.getGomokuWins(), companion.getGomokuLosses(),
            companion.xiangqiBoardSnapshot(), companion.isXiangqiActive(),
            companion.getXiangqiOwnerWins(), companion.getXiangqiLonglongWins(),
            companion.goBoardSnapshot(), companion.isGoActive(),
            companion.getGoOwnerWins(), companion.getGoLonglongWins(),
            companion.getGoOwnerCaptures(), companion.getGoLonglongCaptures());
    }

    public static void encode(OpenGameBoardPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeVarInt(message.selectedGame);
        buffer.writeUtf(message.message, 512);
        buffer.writeByteArray(message.gomokuBoard);
        buffer.writeBoolean(message.gomokuActive);
        buffer.writeVarInt(message.gomokuWins); buffer.writeVarInt(message.gomokuLosses);
        buffer.writeUtf(message.xiangqiBoard, 128);
        buffer.writeBoolean(message.xiangqiActive);
        buffer.writeVarInt(message.xiangqiOwnerWins); buffer.writeVarInt(message.xiangqiLonglongWins);
        buffer.writeByteArray(message.goBoard);
        buffer.writeBoolean(message.goActive);
        buffer.writeVarInt(message.goOwnerWins); buffer.writeVarInt(message.goLonglongWins);
        buffer.writeVarInt(message.goOwnerCaptures); buffer.writeVarInt(message.goLonglongCaptures);
    }

    public static OpenGameBoardPacket decode(FriendlyByteBuf buffer) {
        return new OpenGameBoardPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf(512),
            buffer.readByteArray(128), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readUtf(128), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readByteArray(256), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(OpenGameBoardPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandlers.openGameBoard(message)));
        context.setPacketHandled(true);
    }
}
