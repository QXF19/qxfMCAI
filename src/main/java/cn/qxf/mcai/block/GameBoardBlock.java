package cn.qxf.mcai.block;

import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.server.CompanionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GameBoardBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public GameBoardBlock() {
        super(BlockBehaviour.Properties.of().strength(1.2F).sound(SoundType.WOOD).noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var companion = CompanionManager.find(serverPlayer);
            if (companion == null) {
                serverPlayer.sendSystemMessage(Component.literal("[龙龙棋盘] 请先召唤龙龙。"));
            } else {
                ModNetwork.openGameBoard(serverPlayer, pos, 0, "选择五子棋、中国象棋或围棋");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
