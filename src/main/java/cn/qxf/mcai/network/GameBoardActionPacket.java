package cn.qxf.mcai.network;

import cn.qxf.mcai.block.ModBlocks;
import cn.qxf.mcai.server.CompanionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 棋盘点击请求。服务端验证距离、方块与所有权后才修改棋局。 */
public record GameBoardActionPacket(BlockPos pos, int game, int action,
                                    int fromX, int fromY, int toX, int toY) {
    public static final int START = 0;
    public static final int MOVE = 1;
    public static final int PASS = 2;
    public static final int CHI = 3;
    public static final int PENG = 4;
    public static final int GANG = 5;
    public static final int HU = 6;

    public static void encode(GameBoardActionPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos); buffer.writeVarInt(message.game); buffer.writeVarInt(message.action);
        buffer.writeVarInt(message.fromX); buffer.writeVarInt(message.fromY);
        buffer.writeVarInt(message.toX); buffer.writeVarInt(message.toY);
    }

    public static GameBoardActionPacket decode(FriendlyByteBuf buffer) {
        return new GameBoardActionPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(GameBoardActionPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> process(context.getSender(), message));
        context.setPacketHandled(true);
    }

    private static void process(ServerPlayer player, GameBoardActionPacket packet) {
        if (player == null || !player.serverLevel().getBlockState(packet.pos).is(ModBlocks.DRAGON_GAME_BOARD.get())
            || player.distanceToSqr(Vec3.atCenterOf(packet.pos)) > 64.0D) return;
        var companion = CompanionManager.find(player);
        if (companion == null) {
            player.sendSystemMessage(Component.literal("[龙龙棋盘] 请先召唤龙龙。"));
            return;
        }
        String result = "棋局已更新";
        if (packet.game == 0) {
            if (packet.action == START) { companion.startGomoku(); result = "五子棋已重新开局，主人执黑"; }
            else if (packet.action == MOVE) result = companion.playGomoku(packet.toX, packet.toY);
        } else if (packet.game == 1) {
            if (packet.action == START) { companion.startXiangqi(); result = "中国象棋已重新开局，主人执红"; }
            else if (packet.action == MOVE)
                result = companion.playXiangqi(packet.fromX, packet.fromY, packet.toX, packet.toY).message();
        } else if (packet.game == 2) {
            if (packet.action == START) { companion.startGo(); result = "13×13围棋已重新开局，主人执黑"; }
            else if (packet.action == PASS) result = companion.passGo().message();
            else if (packet.action == MOVE) result = companion.playGo(packet.toX, packet.toY).message();
        } else if (packet.game == 3) {
            if (packet.action == START) { companion.startMahjong(); result = "四人麻将已重新开局，主人先打牌"; }
            else if (packet.action == MOVE) result = companion.playMahjong(packet.toX).message();
            else if (packet.action == PASS) result = companion.passMahjong().message();
            else if (packet.action == CHI) result = companion.claimMahjong(cn.qxf.mcai.game.MahjongGame.CLAIM_CHI).message();
            else if (packet.action == PENG) result = companion.claimMahjong(cn.qxf.mcai.game.MahjongGame.CLAIM_PENG).message();
            else if (packet.action == GANG) result = companion.claimMahjong(cn.qxf.mcai.game.MahjongGame.CLAIM_GANG).message();
            else if (packet.action == HU) result = companion.claimMahjong(cn.qxf.mcai.game.MahjongGame.CLAIM_HU).message();
        }
        ModNetwork.openGameBoard(player, packet.pos, Math.max(0, Math.min(3, packet.game)), result);
    }
}
