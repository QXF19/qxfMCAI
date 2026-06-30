package cn.qxf.mcai.entity;

import cn.qxf.mcai.server.CompanionManager;
import cn.qxf.mcai.config.McAiConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.Comparator;

public class AiCompanionEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_MODE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_INVINCIBLE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_SKIN =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    @Nullable private BlockPos miningTarget;
    private int miningProgress;
    private int miningStuckTicks;

    public AiCompanionEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MODE, Mode.FOLLOW.id);
        entityData.define(DATA_INVINCIBLE, false);
        entityData.define(DATA_SKIN, "companion.png");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % 20 == 0) CompanionManager.register(this);

        if (getMode() == Mode.MINE) {
            mineOreTick();
            return;
        }
        if (tickCount % 10 != 0) return;

        Mode mode = getMode();
        if (mode == Mode.STAY) {
            setTarget(null);
            getNavigation().stop();
        } else if (mode == Mode.GATHER) {
            gatherNearbyItem();
        }

        if (mode == Mode.FOLLOW && getOwner() instanceof ServerPlayer owner && distanceToSqr(owner) > 1024.0D) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    private void gatherNearbyItem() {
        if (!(getOwner() instanceof ServerPlayer owner)) return;
        ItemEntity item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(12.0D),
                e -> e.isAlive() && !e.getItem().isEmpty()).stream()
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (item == null) return;
        if (distanceToSqr(item) > 2.25D) {
            getNavigation().moveTo(item, 1.2D);
            return;
        }
        ItemStack stack = item.getItem();
        owner.getInventory().add(stack);
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && isOwnedBy(player)) {
            Mode next = switch (getMode()) {
                case FOLLOW -> Mode.STAY;
                case STAY -> Mode.GUARD;
                case GUARD -> Mode.GATHER;
                case GATHER -> Mode.MINE;
                case MINE -> Mode.FOLLOW;
            };
            setMode(next);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[小麦] 已切换为：" + next.chinese), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isCompanionInvincible() || super.isInvulnerableTo(source);
    }

    public Mode getMode() {
        return Mode.fromId(entityData.get(DATA_MODE));
    }

    public void setMode(Mode mode) {
        if (getMode() == Mode.MINE && mode != Mode.MINE) clearMiningTarget();
        entityData.set(DATA_MODE, mode.id);
        setOrderedToSit(mode != Mode.FOLLOW);
        if (mode == Mode.STAY) setTarget(null);
    }

    public boolean isCompanionInvincible() {
        return entityData.get(DATA_INVINCIBLE);
    }

    public void setCompanionInvincible(boolean value) {
        entityData.set(DATA_INVINCIBLE, value);
    }

    public String getSkinName() {
        return entityData.get(DATA_SKIN);
    }

    public void setSkinName(String fileName) {
        if (fileName != null && fileName.matches("[A-Za-z0-9._-]+\\.png")) entityData.set(DATA_SKIN, fileName);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("McAiMode", getMode().id);
        tag.putBoolean("McAiInvincible", isCompanionInvincible());
        tag.putString("McAiSkin", getSkinName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMode(Mode.fromId(tag.getInt("McAiMode")));
        setCompanionInvincible(tag.getBoolean("McAiInvincible"));
        if (tag.contains("McAiSkin")) setSkinName(tag.getString("McAiSkin"));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    public enum Mode {
        FOLLOW(0, "跟随"), STAY(1, "等待"), GUARD(2, "警戒"), GATHER(3, "拾取"), MINE(4, "挖矿");
        public final int id;
        public final String chinese;
        Mode(int id, String chinese) { this.id = id; this.chinese = chinese; }
        public static Mode fromId(int id) {
            for (Mode value : values()) if (value.id == id) return value;
            return FOLLOW;
        }
    }

    private void mineOreTick() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (!McAiConfig.MINING_ENABLED.get()
            || !serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            clearMiningTarget();
            return;
        }

        if (miningTarget == null || !isValidOre(serverLevel, miningTarget)
            || miningTarget.distSqr(blockPosition()) > McAiConfig.MINING_RADIUS.get() * McAiConfig.MINING_RADIUS.get() * 4.0D) {
            clearMiningTarget();
            if (tickCount % 20 == 0) miningTarget = findNearestOre(serverLevel);
        }
        if (miningTarget == null) return;

        Vec3 center = Vec3.atCenterOf(miningTarget);
        double distance = position().distanceToSqr(center);
        if (distance > 8.0D) {
            boolean moving = getNavigation().moveTo(center.x, center.y, center.z, 1.1D);
            miningProgress = 0;
            miningStuckTicks = moving ? 0 : miningStuckTicks + 1;
            if (miningStuckTicks > 100) clearMiningTarget();
            return;
        }

        getNavigation().stop();
        getLookControl().setLookAt(center.x, center.y, center.z);
        miningStuckTicks = 0;
        miningProgress++;
        int breakTicks = McAiConfig.MINING_BREAK_TICKS.get();
        int crack = Math.min(9, miningProgress * 10 / breakTicks);
        serverLevel.destroyBlockProgress(getId(), miningTarget, crack);
        if (miningProgress < breakTicks) return;

        BlockState state = serverLevel.getBlockState(miningTarget);
        if (isValidOre(serverLevel, miningTarget)) {
            Block.dropResources(state, serverLevel, miningTarget, serverLevel.getBlockEntity(miningTarget),
                this, new ItemStack(Items.DIAMOND_PICKAXE));
            serverLevel.setBlock(miningTarget, Blocks.AIR.defaultBlockState(), 3);
            serverLevel.levelEvent(2001, miningTarget, Block.getId(state));
        }
        clearMiningTarget();
    }

    @Nullable
    private BlockPos findNearestOre(ServerLevel level) {
        int radius = McAiConfig.MINING_RADIUS.get();
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -Math.min(6, radius); dy <= Math.min(6, radius); dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!isValidOre(level, candidate)) continue;
                    double distance = candidate.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isValidOre(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Tags.Blocks.ORES) && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private void clearMiningTarget() {
        if (miningTarget != null && level() instanceof ServerLevel serverLevel)
            serverLevel.destroyBlockProgress(getId(), miningTarget, -1);
        miningTarget = null;
        miningProgress = 0;
        miningStuckTicks = 0;
    }
}
