package cn.qxf.mcai.compat;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.entity.AiCompanionEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 龙龙的轻量可视化桥。
 *
 * <p>任务、挖矿和建造仍由 qxfMCAI 的核心执行；可见身体使用车万女仆实体，
 * 因而直接获得 YSM 3D 渲染、好感度、饰品栏、骑乘和棋类记录。桥只同步位置、
 * 名称与气泡，不复制车万女仆的大型玩法系统，也不会改动玩家模型。</p>
 */
public final class MaidVisualBridge {
    private static final String BRAIN_UUID = "QxfMcAiBrain";
    private static final String MAID_UUID = "QxfMcAiMaid";
    private static final String DRAGON_MARK = "QxfMcAiDragon";

    private MaidVisualBridge() {}

    public static EntityMaid ensure(AiCompanionEntity brain) {
        if (!(brain.level() instanceof ServerLevel level)) return null;
        EntityMaid maid = find(brain);
        if (maid != null) return maid;

        maid = new EntityMaid(level);
        if (brain.getOwner() instanceof ServerPlayer owner) maid.tame(owner);
        maid.setCustomName(Component.literal("龙龙·ロンロン"));
        maid.setCustomNameVisible(true);
        maid.setPersistenceRequired();
        maid.setNoAi(true);
        maid.noPhysics = true;
        maid.setEntityInvulnerable(brain.isCompanionInvincible());
        maid.setRideable(true);
        maid.setPickup(true);
        maid.setIsYsmModel(true);
        maid.setYsmModel("001", "bailong", Component.literal("白龙"));
        maid.getPersistentData().putBoolean(DRAGON_MARK, true);
        maid.getPersistentData().putUUID(BRAIN_UUID, brain.getUUID());
        brain.getPersistentData().putUUID(MAID_UUID, maid.getUUID());
        maid.moveTo(brain.getX(), brain.getY(), brain.getZ(), brain.getYRot(), brain.getXRot());
        level.addFreshEntity(maid);
        QxfMcAi.LOGGER.info("已为龙龙创建 YSM 女仆身体：{} -> {}", brain.getUUID(), maid.getUUID());
        return maid;
    }

    @Nullable
    public static EntityMaid find(AiCompanionEntity brain) {
        if (!(brain.level() instanceof ServerLevel level)) return null;
        if (brain.getPersistentData().hasUUID(MAID_UUID)) {
            Entity linked = level.getEntity(brain.getPersistentData().getUUID(MAID_UUID));
            if (linked instanceof EntityMaid maid && maid.isAlive()) return maid;
        }
        return level.getEntitiesOfClass(EntityMaid.class, brain.getBoundingBox().inflate(256.0D), maid ->
            maid.getPersistentData().getBoolean(DRAGON_MARK)
                && maid.getPersistentData().hasUUID(BRAIN_UUID)
                && brain.getUUID().equals(maid.getPersistentData().getUUID(BRAIN_UUID)))
            .stream().findFirst().orElse(null);
    }

    public static void tick(AiCompanionEntity brain) {
        if (brain.level().isClientSide) return;
        // 读取存档时给同区块实体留出加载时间，避免先加载核心后重复创建身体。
        EntityMaid maid = find(brain);
        if (maid == null && brain.tickCount >= 20) maid = ensure(brain);
        if (maid == null) return;

        brain.setInvisible(true);
        brain.setCustomNameVisible(false);
        maid.setNoAi(true);
        maid.noPhysics = true;
        maid.setEntityInvulnerable(brain.isCompanionInvincible());
        if (maid.getVehicle() == null && maid.getControllingPassenger() == null) {
            double distance = maid.distanceToSqr(brain);
            if (distance > 64.0D || brain.tickCount % 5 == 0) {
                maid.teleportTo(brain.getX(), brain.getY(), brain.getZ());
                maid.setYRot(brain.getYRot());
                maid.setXRot(brain.getXRot());
            }
        }
    }

    public static void speak(AiCompanionEntity brain, String text) {
        EntityMaid maid = find(brain);
        if (maid != null && !text.isBlank()) maid.getChatBubbleManager().addTextChatBubble(text);
    }

    public static boolean openMaidMenu(AiCompanionEntity brain, ServerPlayer player) {
        EntityMaid maid = ensure(brain);
        return maid != null && maid.openMaidGui(player);
    }

    public static int favorability(AiCompanionEntity brain) {
        EntityMaid maid = find(brain);
        return maid == null ? 0 : maid.getFavorability();
    }

    public static void setModel(AiCompanionEntity brain, String model, String texture) {
        EntityMaid maid = find(brain);
        if (maid == null && brain.tickCount > 0) maid = ensure(brain);
        if (maid == null) return;
        String modelId = model == null || model.isBlank() ? "001" : model;
        String textureId = texture == null || texture.isBlank() || "-".equals(texture) ? "bailong" : texture;
        maid.setIsYsmModel(true);
        maid.setYsmModel(modelId, textureId, Component.literal("白龙"));
    }

    public static int gomokuWins(AiCompanionEntity brain) {
        EntityMaid maid = find(brain);
        return maid == null ? 0 : maid.getGameRecordManager().getGomokuWinCount();
    }

    public static void discard(AiCompanionEntity brain) {
        EntityMaid maid = find(brain);
        if (maid != null) maid.discard();
        brain.getPersistentData().remove(MAID_UUID);
    }
}
