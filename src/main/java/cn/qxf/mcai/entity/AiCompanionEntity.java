package cn.qxf.mcai.entity;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.server.CompanionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
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
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class AiCompanionEntity extends TamableAnimal implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_MODE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_INVINCIBLE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_SKIN =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_BUBBLE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EMOTION =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIVITY =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_YSM_MODEL =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_YSM_TEXTURE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);

    private final SimpleContainer inventory = new SimpleContainer(27);
    private final Deque<AgentAction> taskQueue = new ArrayDeque<>();
    private final Deque<String> memories = new ArrayDeque<>();
    private final Deque<BlockPos> buildQueue = new ArrayDeque<>();
    @Nullable private AgentAction currentTask;
    @Nullable private BlockPos miningTarget;
    @Nullable private BlockPos workTarget;
    @Nullable private BlockPos homePosition;
    @Nullable private BlockPos pendingTeleportPosition;
    @Nullable private BlockPos lastNotifiedOre;
    private int miningProgress;
    private int miningStuckTicks;
    private int workProgress;
    private int workGoal;
    private int taskTicks;
    private int bubbleTicks;
    private int dragonLevel = 1;
    private int dragonExperience;
    private int completedTasks;
    private String thought = "想和玩家一起把今天过好";
    private String longTermGoal = "建立安全、温暖的共同基地";
    private String habit = "探索者";

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
            .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MODE, Mode.PATROL.id);
        entityData.define(DATA_INVINCIBLE, true);
        entityData.define(DATA_SKIN, "white_dragon.png");
        entityData.define(DATA_BUBBLE, "");
        entityData.define(DATA_EMOTION, "curious");
        entityData.define(DATA_ACTIVITY, "正在观察世界");
        entityData.define(DATA_YSM_MODEL, "001");
        entityData.define(DATA_YSM_TEXTURE, "-");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 20, 15.0F) {
            @Override public boolean canUse() {
                return getMainHandItem().getItem() instanceof BowItem && hasArrows() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true) {
            @Override public boolean canUse() {
                return !(getMainHandItem().getItem() instanceof BowItem) && super.canUse();
            }
        });
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 5.0F, 2.0F, false) {
            @Override public boolean canUse() { return getMode() == Mode.FOLLOW && super.canUse(); }
            @Override public boolean canContinueToUse() { return getMode() == Mode.FOLLOW && super.canContinueToUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true) {
            @Override public boolean canUse() {
                return (getMode() == Mode.GUARD || getMode() == Mode.HUNT) && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % 20 == 0) CompanionManager.register(this);
        if (tickCount % 200 == 1) {
            ensureOwnEquipment();
        }
        if (bubbleTicks > 0 && --bubbleTicks == 0) entityData.set(DATA_BUBBLE, "");

        tickTaskEngine();
        switch (getMode()) {
            case MINE -> mineOreTick();
            case GATHER -> gatherNearbyItem();
            case EXPLORE, PATROL -> exploreTick();
            case HUNT -> huntTick();
            case LUMBER -> lumberTick();
            case FARM -> farmTick();
            case FISH -> fishTick();
            case BUILD -> buildTick();
            case FOLLOW -> followTick();
            case STAY -> {
                setTarget(null);
                getNavigation().stop();
            }
            default -> { }
        }
        if (McAiConfig.AUTONOMY_ENABLED.get() && currentTask == null && taskQueue.isEmpty()
            && tickCount % 600 == 0) autonomousDecision();
        if (currentTask == null && tickCount % 2400 == 0) offerIndependentSuggestion();
    }

    private void followTick() {
        if (tickCount % 10 == 0 && getOwner() instanceof ServerPlayer owner && distanceToSqr(owner) > 1024.0D)
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
    }

    public void enqueueAction(AgentAction action) {
        if (action == null || action.type().isBlank() || taskQueue.size() >= 16) return;
        if (action.type().equals("stop")) {
            taskQueue.clear();
            currentTask = null;
            buildQueue.clear();
            clearMiningTarget();
            setMode(Mode.STAY);
            speak("好，我已经立即停下来了。", "focused");
            return;
        }
        taskQueue.addLast(action);
        QxfMcAi.LOGGER.info("龙龙任务入队：type={} target={} count={} queue={}",
            action.type(), action.target(), action.count(), taskQueue.size());
    }

    private void tickTaskEngine() {
        if (currentTask == null && !taskQueue.isEmpty()) startTask(taskQueue.removeFirst());
        if (currentTask == null) return;
        taskTicks++;
        if (taskTicks > 2400) finishTask(false, "任务超时，先停下来重新想想");
    }

    private void startTask(AgentAction action) {
        currentTask = action;
        taskTicks = 0;
        workProgress = 0;
        workGoal = action.count();
        QxfMcAi.LOGGER.info("龙龙开始任务：{} × {}，位置={}", action.type(), action.count(), blockPosition());
        String type = action.type();
        switch (type) {
            case "follow" -> { setMode(Mode.FOLLOW); finishTask(true, "跟着你走"); }
            case "stay" -> { setMode(Mode.STAY); finishTask(true, "在这里等你"); }
            case "guard" -> { setMode(Mode.GUARD); equipBestWeapon(); finishTask(true, "开始警戒"); }
            case "gather" -> setWorkMode(Mode.GATHER, "收集附近物品");
            case "mine" -> { equipBestPickaxe(); setWorkMode(Mode.MINE, "向下寻找矿洞和矿石"); }
            case "find_cave" -> { equipBestPickaxe(); setWorkMode(Mode.MINE, "独立向下寻找天然矿洞"); }
            case "explore" -> setWorkMode(Mode.EXPLORE, "探索周围地形");
            case "patrol" -> setWorkMode(Mode.PATROL, "巡视基地周围");
            case "hunt" -> { equipBestWeapon(); setWorkMode(Mode.HUNT, "狩猎敌对生物"); }
            case "chop" -> { equipBestAxe(); setWorkMode(Mode.LUMBER, "寻找并砍伐树木"); }
            case "harvest", "plant", "farm" -> setWorkMode(Mode.FARM, "照料并收获农田");
            case "fish" -> startFishing();
            case "build_shelter" -> prepareBuilding(false, false);
            case "build_house" -> prepareBuilding(true, false);
            case "build_bridge" -> prepareBuilding(false, true);
            case "place_torch" -> {
                boolean done = placeTorch();
                finishTask(done, done ? "点亮这里" : "没有火把或当前位置不能放火把");
            }
            case "eat" -> {
                boolean done = eatFromInventory();
                finishTask(done, done ? "补充体力" : "背包里没有食物");
            }
            case "sleep" -> { setMode(Mode.STAY); emote("sleepy", "有点困啦，靠着你休息一会儿……"); finishTask(true, "休息"); }
            case "deposit" -> {
                boolean done = depositIntoNearbyContainer();
                finishTask(done, done ? "整理背包" : "附近没有可以存放物品的箱子");
            }
            case "equip_weapon" -> {
                boolean done = equipBestWeapon();
                finishTask(done, done ? "装备武器" : "背包里没有剑或弓");
            }
            case "equip_pickaxe" -> {
                boolean done = equipBestPickaxe();
                finishTask(done, done ? "装备镐子" : "背包里没有镐子");
            }
            case "craft" -> {
                boolean done = craftBasics();
                finishTask(done, done ? "制作基础材料" : "没有足够的原木或木板");
            }
            case "come" -> {
                if (getOwner() instanceof ServerPlayer owner) CompanionManager.come(owner, this);
                finishTask(true, "回到你身边");
            }
            case "command" -> {
                boolean done = getOwner() instanceof ServerPlayer owner
                    && CompanionManager.executeAuthorizedCommand(owner, action.command());
                finishTask(done, done ? "执行授权命令" : "命令未授权、为空或执行失败");
            }
            case "emote" -> { emote(action.target().isBlank() ? "happy" : action.target(), action.message()); finishTask(true, "互动"); }
            case "stop" -> { taskQueue.clear(); setMode(Mode.STAY); finishTask(true, "停止当前任务"); }
            default -> finishTask(false, "不认识动作 " + type);
        }
    }

    private void setWorkMode(Mode mode, String activity) {
        setMode(mode);
        setActivity(activity);
    }

    private void completeWorkUnit() {
        workProgress++;
        addExperience(3);
        if (currentTask != null && workProgress >= workGoal)
            finishTask(true, entityData.get(DATA_ACTIVITY) + "完成");
    }

    private void finishTask(boolean success, String result) {
        String finishedType = currentTask == null ? "unknown" : currentTask.type();
        if (success) {
            completedTasks++;
            addExperience(5);
            remember("完成：" + result);
            emote("proud", result + "！");
        } else {
            remember("未完成：" + result);
            emote("worried", result);
        }
        currentTask = null;
        taskTicks = 0;
        workTarget = null;
        clearMiningTarget();
        if (getMode() == Mode.GATHER || getMode() == Mode.MINE || getMode() == Mode.EXPLORE
            || getMode() == Mode.PATROL || getMode() == Mode.HUNT || getMode() == Mode.LUMBER
            || getMode() == Mode.FARM || getMode() == Mode.BUILD || getMode() == Mode.FISH)
            setMode(taskQueue.isEmpty() ? Mode.PATROL : Mode.STAY);
        notifyOwner((success ? "任务完成" : "任务失败") + "：" + result);
        QxfMcAi.LOGGER.info("龙龙任务结束：type={} success={} result={}", finishedType, success, result);
    }

    private void autonomousDecision() {
        if (!(getOwner() instanceof ServerPlayer owner)) return;
        if (getHealth() < getMaxHealth() * 0.45F && hasFood()) enqueueAction(AgentAction.simple("eat"));
        else if (owner.level().isNight() && !hasBuildingNearby())
            enqueueAction(new AgentAction("build_shelter", "", 1, "", ""));
        else switch (habit) {
            case "矿工" -> enqueueAction(new AgentAction("mine", "ores", 3, "", ""));
            case "建设者" -> enqueueAction(new AgentAction("build_shelter", "", 1, "", ""));
            case "守卫" -> enqueueAction(new AgentAction("patrol", "", 2, "", ""));
            default -> enqueueAction(new AgentAction(random.nextBoolean() ? "explore" : "find_cave", "", 1, "", ""));
        }
        thought = switch (random.nextInt(4)) {
            case 0 -> "想把基地周围变得更安全";
            case 1 -> "在盘算下一次下矿要准备什么";
            case 2 -> "希望今天能和玩家完成一件像样的事";
            default -> "好奇附近有没有没见过的地方";
        };
    }

    public void initializeIndependentAgent() {
        if (homePosition == null) homePosition = blockPosition();
        int profile = Math.floorMod(getUUID().hashCode(), 4);
        habit = switch (profile) { case 0 -> "矿工"; case 1 -> "建设者"; case 2 -> "守卫"; default -> "探索者"; };
        ensureOwnEquipment();
        if (getMainHandItem().isEmpty()) equipBestWeapon();
        remember("v4初始化：我习惯做一名" + habit + "，拥有自己的最高级工具与最高权限");
    }

    public void setHomePosition(BlockPos pos) { homePosition = pos == null ? blockPosition() : pos.immutable(); }

    private void ensureOwnEquipment() {
        ensureTool(Items.NETHERITE_SWORD, SwordItem.class);
        ensureTool(Items.NETHERITE_PICKAXE, PickaxeItem.class);
        ensureTool(Items.NETHERITE_AXE, AxeItem.class);
        ensureTool(Items.NETHERITE_SHOVEL, net.minecraft.world.item.ShovelItem.class);
        ensureTool(Items.NETHERITE_HOE, net.minecraft.world.item.HoeItem.class);
        ensureTool(Items.BOW, BowItem.class);
        ensureTool(Items.FISHING_ROD, FishingRodItem.class);
        ensureSupply(Items.ARROW, 64);
        ensureSupply(Items.COOKED_BEEF, 16);
        ensureSupply(Items.TORCH, 64);
        ensureSupply(Items.COBBLESTONE, 64);
        ensureSupply(Items.OAK_PLANKS, 64);
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        if (getItemBySlot(EquipmentSlot.CHEST).isEmpty()) setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        if (getItemBySlot(EquipmentSlot.LEGS).isEmpty()) setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        if (getItemBySlot(EquipmentSlot.FEET).isEmpty()) setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    }

    private void ensureTool(net.minecraft.world.item.Item item, Class<?> type) {
        if (type.isInstance(getMainHandItem().getItem())) return;
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (type.isInstance(inventory.getItem(i).getItem())) return;
        inventory.addItem(new ItemStack(item));
    }

    private void ensureSupply(net.minecraft.world.item.Item item, int count) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (inventory.getItem(i).is(item)) total += inventory.getItem(i).getCount();
        if (total == 0) inventory.addItem(new ItemStack(item, count));
    }

    private void offerIndependentSuggestion() {
        String suggestion = switch (habit) {
            case "矿工" -> "我想独自向下探一条矿道，发现矿洞后叫你过来，可以吗？";
            case "建设者" -> "我注意到基地还能扩建，要不要让我自己准备一座物资小屋？";
            case "守卫" -> "附近的照明和防御还不够，我建议先巡逻并清理威胁。";
            default -> "我想自己去远处探索；找到有意思的地点后会通知你并申请传送。";
        };
        thought = suggestion;
        speak(suggestion, "curious");
        notifyOwner("建议：" + suggestion);
    }

    private void notifyOwner(String message) {
        if (getOwner() instanceof ServerPlayer owner)
            owner.sendSystemMessage(Component.literal("[龙龙] " + message).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void offerTeleportToOwner(BlockPos target, String reason) {
        pendingTeleportPosition = target.immutable();
        if (!(getOwner() instanceof ServerPlayer owner)) return;
        Component allow = Component.literal(" [允许传送]").withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcai permit teleport")));
        owner.sendSystemMessage(Component.literal("[龙龙] " + reason + "，要传送到我这里吗？")
            .withStyle(ChatFormatting.GOLD).append(allow));
    }

    public boolean teleportOwnerWithPermission(ServerPlayer owner) {
        if (pendingTeleportPosition == null || level() != owner.level()) return false;
        BlockPos safe = pendingTeleportPosition.above();
        owner.teleportTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        pendingTeleportPosition = null;
        return true;
    }

    private void gatherNearbyItem() {
        if (tickCount % 5 != 0) return;
        ItemEntity item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(14.0D),
                e -> e.isAlive() && !e.getItem().isEmpty()).stream()
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (item == null) return;
        if (distanceToSqr(item) > 2.25D) { getNavigation().moveTo(item, 1.2D); return; }
        ItemStack remainder = inventory.addItem(item.getItem().copy());
        item.setItem(remainder);
        if (remainder.isEmpty()) item.discard();
        completeWorkUnit();
    }

    private void mineOreTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) return;
        if (currentTask != null && currentTask.type().equals("find_cave")) {
            findCaveTick(serverLevel);
            return;
        }
        if (!McAiConfig.MINING_ENABLED.get()) {
            clearMiningTarget();
            if (currentTask != null) finishTask(false, "服务器配置关闭了挖矿功能");
            return;
        }
        ItemStack tool = getMainHandItem();
        if (!(tool.getItem() instanceof PickaxeItem) || tool.isEmpty()) {
            equipBestPickaxe();
            if (!(getMainHandItem().getItem() instanceof PickaxeItem)) {
                finishTask(false, "背包里没有镐子");
                return;
            }
        }
        if (miningTarget == null || !isValidOre(serverLevel, miningTarget)) {
            clearMiningTarget();
            if (tickCount % 20 == 0) {
                miningTarget = findOreBelow(serverLevel);
                if (miningTarget != null && !miningTarget.equals(lastNotifiedOre)) {
                    lastNotifiedOre = miningTarget.immutable();
                    offerTeleportToOwner(miningTarget, "我独立发现了矿脉 " + serverLevel.getBlockState(miningTarget).getBlock().getName().getString()
                        + "，坐标 " + miningTarget.toShortString());
                }
            }
        }
        if (miningTarget == null) {
            if (taskTicks > 400) finishTask(false, "向下搜索后没有发现矿石");
            return;
        }
        Vec3 center = Vec3.atCenterOf(miningTarget);
        if (position().distanceToSqr(center) > 8.0D) {
            boolean moving = getNavigation().moveTo(center.x, center.y, center.z, 1.05D);
            miningProgress = 0;
            miningStuckTicks = moving ? Math.max(0, miningStuckTicks - 1) : miningStuckTicks + 2;
            if (miningStuckTicks > 30 || tickCount % 40 == 0) excavateToward(serverLevel, miningTarget);
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(center.x, center.y, center.z);
        miningProgress++;
        int breakTicks = adjustedBreakTicks(serverLevel.getBlockState(miningTarget), getMainHandItem());
        serverLevel.destroyBlockProgress(getId(), miningTarget, Math.min(9, miningProgress * 10 / breakTicks));
        swing(InteractionHand.MAIN_HAND);
        if (miningProgress >= breakTicks && breakIntoInventory(serverLevel, miningTarget, getMainHandItem())) {
            completeWorkUnit();
            clearMiningTarget();
        }
    }

    private void findCaveTick(ServerLevel level) {
        if (workTarget == null || !isCavePocket(level, workTarget)) {
            if (tickCount % 20 == 0) {
                workTarget = findCaveBelow(level);
                if (workTarget != null)
                    offerTeleportToOwner(workTarget, "我发现了地下天然空间，坐标 " + workTarget.toShortString());
            }
        }
        if (workTarget == null) {
            if (taskTicks > 600) finishTask(false, "搜索范围内没有找到天然矿洞");
            return;
        }
        BlockPos target = workTarget.immutable();
        if (target.distSqr(blockPosition()) > 10.0D) {
            getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
            if (tickCount % 20 == 0) excavateToward(level, target);
            return;
        }
        finishTask(true, "已抵达天然矿洞 " + target.toShortString());
        offerTeleportToOwner(target, "我已经安全抵达矿洞");
    }

    @Nullable
    private BlockPos findCaveBelow(ServerLevel level) {
        BlockPos origin = blockPosition();
        int radius = McAiConfig.MINING_RADIUS.get();
        int depth = Math.min(64, McAiConfig.MINING_DEPTH.get() * 2);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = -4; dy >= -depth; dy--) for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = origin.offset(dx, dy, dz);
                if (!isCavePocket(level, pos)) continue;
                double distance = pos.distSqr(origin);
                if (distance < bestDistance) { bestDistance = distance; best = pos.immutable(); }
            }
        return best;
    }

    private boolean isCavePocket(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
            || level.getBlockState(pos.below()).isAir() || level.canSeeSky(pos)) return false;
        int open = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            if (level.getBlockState(pos.relative(direction)).isAir()) open++;
        return open >= 2;
    }

    @Nullable
    private BlockPos findOreBelow(ServerLevel level) {
        int radius = McAiConfig.MINING_RADIUS.get();
        int depth = McAiConfig.MINING_DEPTH.get();
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dy = -1; dy >= -depth; dy--) {
            int horizontal = Math.min(radius, 4 + (-dy / 3));
            for (int dx = -horizontal; dx <= horizontal; dx++) for (int dz = -horizontal; dz <= horizontal; dz++) {
                BlockPos candidate = origin.offset(dx, dy, dz);
                if (!isValidOre(level, candidate) || !getMainHandItem().isCorrectToolForDrops(level.getBlockState(candidate))) continue;
                double score = candidate.distSqr(origin) + Math.abs(dy) * 0.25D;
                if (score < bestScore) { bestScore = score; best = candidate.immutable(); }
            }
        }
        return best;
    }

    private void excavateToward(ServerLevel level, BlockPos target) {
        BlockPos here = blockPosition();
        int dx = Integer.compare(target.getX(), here.getX());
        int dz = Integer.compare(target.getZ(), here.getZ());
        int dy = target.getY() < here.getY() - 2 ? -1 : 0;
        BlockPos next = here.offset(dx, dy, dz);
        BlockPos[] passage = {next, next.above()};
        for (BlockPos pos : passage) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && getMainHandItem().isCorrectToolForDrops(state)) {
                miningProgress++;
                if (miningProgress >= adjustedBreakTicks(state, getMainHandItem())) {
                    breakIntoInventory(level, pos, getMainHandItem());
                    miningProgress = 0;
                }
                swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
        getNavigation().moveTo(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D, 1.0D);
        miningStuckTicks = 0;
    }

    private boolean breakIntoInventory(ServerLevel level, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return false;
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), this, tool);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            ItemStack remainder = inventory.addItem(drop.copy());
            if (!remainder.isEmpty()) spawnAtLocation(remainder);
        }
        tool.hurtAndBreak(1, this, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        addExperience(2);
        return true;
    }

    private int adjustedBreakTicks(BlockState state, ItemStack tool) {
        float speed = Math.max(1.0F, tool.getDestroySpeed(state));
        return Math.max(4, Math.round(McAiConfig.MINING_BREAK_TICKS.get() / speed));
    }

    private boolean isValidOre(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Tags.Blocks.ORES) && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private void exploreTick() {
        if (tickCount % 80 != 0) return;
        if (workTarget == null || position().distanceToSqr(Vec3.atCenterOf(workTarget)) < 9.0D) {
            if (workTarget != null) completeWorkUnit();
            int radius = getMode() == Mode.PATROL ? 12 : 28;
            BlockPos origin = getMode() == Mode.PATROL && homePosition != null ? homePosition : blockPosition();
            BlockPos rough = origin.offset(random.nextInt(radius * 2 + 1) - radius, 0,
                random.nextInt(radius * 2 + 1) - radius);
            workTarget = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rough);
        }
        getNavigation().moveTo(workTarget.getX() + 0.5D, workTarget.getY(), workTarget.getZ() + 0.5D, 1.0D);
    }

    private void huntTick() {
        if (getTarget() != null) {
            if (distanceToSqr(getTarget()) > 64.0D && hasArrows()) equipBest(stack -> stack.getItem() instanceof BowItem);
            else equipBest(stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem);
            setActivity("与 " + getTarget().getName().getString() + " 战斗");
            if (!getTarget().isAlive()) completeWorkUnit();
        } else if (tickCount % 60 == 0) {
            Monster monster = level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24), Mob::isAlive).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (monster != null) setTarget(monster);
        }
    }

    private void lumberTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 10 != 0) return;
        if (workTarget == null || !serverLevel.getBlockState(workTarget).is(BlockTags.LOGS))
            workTarget = findNearestBlock(serverLevel, 12, state -> state.is(BlockTags.LOGS));
        if (workTarget == null) return;
        if (workTarget.distSqr(blockPosition()) > 6.0D) { getNavigation().moveTo(workTarget.getX(), workTarget.getY(), workTarget.getZ(), 1.0D); return; }
        if (!(getMainHandItem().getItem() instanceof AxeItem) && !equipBestAxe()) {
            finishTask(false, "背包里没有斧头");
            return;
        }
        if (breakIntoInventory(serverLevel, workTarget, getMainHandItem())) { completeWorkUnit(); workTarget = null; }
    }

    private void farmTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 10 != 0) return;
        if (currentTask != null && currentTask.type().equals("plant")) { plantCropTick(serverLevel); return; }
        if (workTarget == null || !(serverLevel.getBlockState(workTarget).getBlock() instanceof CropBlock crop)
            || !crop.isMaxAge(serverLevel.getBlockState(workTarget)))
            workTarget = findNearestBlock(serverLevel, 12, state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state));
        if (workTarget == null) return;
        if (workTarget.distSqr(blockPosition()) > 6.0D) { getNavigation().moveTo(workTarget.getX(), workTarget.getY(), workTarget.getZ(), 1.0D); return; }
        BlockState state = serverLevel.getBlockState(workTarget);
        CropBlock crop = (CropBlock) state.getBlock();
        for (ItemStack drop : Block.getDrops(state, serverLevel, workTarget, serverLevel.getBlockEntity(workTarget), this, ItemStack.EMPTY)) {
            ItemStack remainder = inventory.addItem(drop);
            if (!remainder.isEmpty()) spawnAtLocation(remainder);
        }
        serverLevel.setBlock(workTarget, crop.getStateForAge(0), 3);
        swing(InteractionHand.MAIN_HAND);
        completeWorkUnit();
        workTarget = null;
    }

    private void plantCropTick(ServerLevel serverLevel) {
        int seedSlot = findSeed();
        if (seedSlot < 0) { finishTask(false, "背包里没有可种植的种子"); return; }
        if (workTarget == null || !serverLevel.getBlockState(workTarget).is(Blocks.FARMLAND)
            || !serverLevel.getBlockState(workTarget.above()).isAir())
            workTarget = findPlantingSpot(serverLevel);
        if (workTarget == null) {
            finishTask(false, "附近没有空着的耕地"); return;
        }
        if (workTarget.distSqr(blockPosition()) > 8.0D) {
            getNavigation().moveTo(workTarget.getX(), workTarget.getY() + 1, workTarget.getZ(), 1.0D);
            return;
        }
        ItemStack seeds = inventory.getItem(seedSlot);
        BlockState crop = cropForSeed(seeds);
        if (crop == null) { finishTask(false, "无法识别这种种子"); return; }
        serverLevel.setBlock(workTarget.above(), crop, 3);
        seeds.shrink(1);
        swing(InteractionHand.MAIN_HAND);
        completeWorkUnit();
        workTarget = null;
    }

    @Nullable
    private BlockPos findPlantingSpot(ServerLevel level) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -4, -12), origin.offset(12, 4, 12))) {
            if (!level.getBlockState(pos).is(Blocks.FARMLAND) || !level.getBlockState(pos.above()).isAir()) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) { bestDistance = distance; best = pos.immutable(); }
        }
        return best;
    }

    private int findSeed() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.CARROT) || stack.is(Items.POTATO)) return i;
        }
        return -1;
    }

    @Nullable
    private BlockState cropForSeed(ItemStack stack) {
        if (stack.is(Items.WHEAT_SEEDS)) return Blocks.WHEAT.defaultBlockState();
        if (stack.is(Items.BEETROOT_SEEDS)) return Blocks.BEETROOTS.defaultBlockState();
        if (stack.is(Items.CARROT)) return Blocks.CARROTS.defaultBlockState();
        if (stack.is(Items.POTATO)) return Blocks.POTATOES.defaultBlockState();
        return null;
    }

    @Nullable
    private BlockPos findNearestBlock(ServerLevel level, int radius, java.util.function.Predicate<BlockState> predicate) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -6, -radius), origin.offset(radius, 8, radius))) {
            if (!predicate.test(level.getBlockState(pos))) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) { bestDistance = distance; best = pos.immutable(); }
        }
        return best;
    }

    private void prepareBuilding(boolean house, boolean bridge) {
        if (!McAiConfig.BUILDING_ENABLED.get()) { finishTask(false, "服务器关闭了建造权限"); return; }
        buildQueue.clear();
        BlockPos base = blockPosition().relative(getDirection(), 3).below();
        if (bridge) {
            Direction direction = getDirection();
            Direction side = direction.getClockWise();
            for (int length = 0; length < 12; length++) for (int width = -1; width <= 1; width++)
                buildQueue.add(base.relative(direction, length).relative(side, width));
        } else {
            int radius = house ? 3 : 2;
            int height = house ? 4 : 3;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) buildQueue.add(base.offset(x, 0, z));
            for (int y = 1; y < height; y++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    if (!(z == -radius && x == 0 && y <= 2)) buildQueue.add(base.offset(x, y, z));
                }
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) buildQueue.add(base.offset(x, height, z));
        }
        setWorkMode(Mode.BUILD, bridge ? "建造桥梁" : house ? "建造房屋" : "搭建生存庇护所");
    }

    private void buildTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 5 != 0) return;
        if (buildQueue.isEmpty()) { completeWorkUnit(); return; }
        BlockPos pos = buildQueue.peekFirst();
        if (!serverLevel.getWorldBorder().isWithinBounds(pos)) { buildQueue.removeFirst(); return; }
        if (pos.distSqr(blockPosition()) > 20.0D) { getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D); return; }
        if (!serverLevel.getBlockState(pos).canBeReplaced()) { buildQueue.removeFirst(); return; }
        int slot = findBuildingBlock();
        if (slot < 0) { finishTask(false, "背包里没有可用建筑方块"); return; }
        ItemStack stack = inventory.getItem(slot);
        Block block = ((BlockItem) stack.getItem()).getBlock();
        BlockState placement = block.defaultBlockState();
        if (!placement.canSurvive(serverLevel, pos)) { buildQueue.removeFirst(); return; }
        serverLevel.setBlock(pos, placement, 3);
        stack.shrink(1);
        inventory.setChanged();
        buildQueue.removeFirst();
        swing(InteractionHand.MAIN_HAND);
        addExperience(1);
    }

    private int findBuildingBlock() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof BlockItem blockItem)) continue;
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || stack.is(Items.COBBLESTONE)
                || stack.is(Items.STONE) || stack.is(Items.DIRT) || stack.is(Items.BRICKS)) return i;
        }
        return -1;
    }

    private boolean placeTorch() {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        int slot = findItem(Items.TORCH);
        if (slot < 0) { emote("worried", "背包里没有火把"); return false; }
        BlockPos pos = blockPosition();
        if (serverLevel.getBlockState(pos).canBeReplaced() && Blocks.TORCH.defaultBlockState().canSurvive(serverLevel, pos)) {
            serverLevel.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            inventory.getItem(slot).shrink(1);
            return true;
        }
        return false;
    }

    private void startFishing() {
        int rod = findTool(FishingRodItem.class);
        if (rod < 0) { finishTask(false, "背包里没有钓鱼竿"); return; }
        equipFromSlot(rod);
        if (!(level() instanceof ServerLevel serverLevel)) { finishTask(false, "当前世界无法钓鱼"); return; }
        workTarget = findNearestBlock(serverLevel, 16, state -> state.getFluidState().is(FluidTags.WATER));
        if (workTarget == null) { finishTask(false, "附近没有找到水面"); return; }
        setWorkMode(Mode.FISH, "在水边使用钓鱼竿");
    }

    private void fishTick() {
        if (!(level() instanceof ServerLevel serverLevel) || workTarget == null) return;
        if (workTarget.distSqr(blockPosition()) > 12.0D) {
            BlockPos stand = workTarget.relative(Direction.getNearest(
                getX() - workTarget.getX(), 0, getZ() - workTarget.getZ()));
            getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 1.0D);
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(Vec3.atCenterOf(workTarget));
        if (taskTicks % 40 == 1) swing(InteractionHand.MAIN_HAND);
        if (taskTicks < 160 || taskTicks % 120 != 0) return;
        LootTable fishing = serverLevel.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(workTarget))
            .withParameter(LootContextParams.TOOL, getMainHandItem())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.FISHING);
        for (ItemStack result : fishing.getRandomItems(params)) {
            ItemStack remainder = inventory.addItem(result);
            if (!remainder.isEmpty()) spawnAtLocation(remainder);
        }
        getMainHandItem().hurtAndBreak(1, this, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        completeWorkUnit();
    }

    private boolean eatFromInventory() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEdible()) {
                int nutrition = stack.getFoodProperties(this) == null ? 1 : stack.getFoodProperties(this).getNutrition();
                heal(Math.max(2, nutrition));
                stack.shrink(1);
                emote("happy", "这个味道不错～");
                return true;
            }
        }
        return false;
    }

    private boolean hasFood() {
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).isEdible()) return true;
        return false;
    }

    private boolean depositIntoNearbyContainer() {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-5, -2, -5), blockPosition().offset(5, 3, 5))) {
            if (!(serverLevel.getBlockEntity(pos) instanceof net.minecraft.world.Container container)) continue;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty()) continue;
                for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
                    ItemStack existing = container.getItem(slot);
                    if (existing.isEmpty() && container.canPlaceItem(slot, stack)) {
                        container.setItem(slot, stack.copy());
                        stack.setCount(0);
                    } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                        int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        existing.grow(moved);
                        stack.shrink(moved);
                    }
                }
            }
            container.setChanged();
            return true;
        }
        return false;
    }

    private boolean craftBasics() {
        int logs = findTagged(BlockTags.LOGS);
        if (logs >= 0) {
            ItemStack stack = inventory.getItem(logs);
            stack.shrink(1);
            inventory.addItem(new ItemStack(Items.OAK_PLANKS, 4));
            return true;
        }
        int planks = findTagged(BlockTags.PLANKS);
        if (planks >= 0 && inventory.getItem(planks).getCount() >= 2) {
            inventory.getItem(planks).shrink(2);
            inventory.addItem(new ItemStack(Items.STICK, 4));
            return true;
        }
        return false;
    }

    private boolean hasBuildingNearby() {
        BlockPos center = blockPosition();
        BlockPos min = center.offset(-8, 0, -8);
        BlockPos max = center.offset(8, 5, 8);
        int placed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level().getBlockState(pos);
            if ((state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.BRICKS)) && ++placed > 24) return true;
        }
        return false;
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (id, playerInventory, ignored) -> ChestMenu.threeRows(id, playerInventory, inventory),
            Component.literal("龙龙的独立背包 · 27格")));
    }

    public boolean equipBestWeapon() { return equipBest(stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof BowItem); }
    public boolean equipBestPickaxe() { return equipBest(stack -> stack.getItem() instanceof PickaxeItem); }
    public boolean equipBestAxe() { return equipBest(stack -> stack.getItem() instanceof AxeItem); }

    private boolean equipBest(java.util.function.Predicate<ItemStack> predicate) {
        int best = -1;
        int score = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!predicate.test(stack)) continue;
            int current = stack.getItem() instanceof TieredItem tiered ? tiered.getTier().getLevel() * 100 - stack.getDamageValue() : 50;
            if (current > score) { score = current; best = i; }
        }
        if (best >= 0) { equipFromSlot(best); return true; }
        return predicate.test(getMainHandItem());
    }

    private void equipFromSlot(int slot) {
        ItemStack next = inventory.removeItemNoUpdate(slot);
        ItemStack old = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, next);
        if (!old.isEmpty()) {
            ItemStack remainder = inventory.addItem(old);
            if (!remainder.isEmpty()) spawnAtLocation(remainder);
        }
    }

    private int findTool(Class<?> type) {
        for (int i = 0; i < inventory.getContainerSize(); i++) if (type.isInstance(inventory.getItem(i).getItem())) return i;
        return -1;
    }

    private int findItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(item)) return i;
        return -1;
    }

    private boolean hasArrows() { return findItem(Items.ARROW) >= 0; }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        int arrowSlot = findItem(Items.ARROW);
        if (arrowSlot < 0 || !(level() instanceof ServerLevel serverLevel)) return;
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F,
            14 - serverLevel.getDifficulty().getId() * 4);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (random.nextFloat() * 0.4F + 0.8F));
        serverLevel.addFreshEntity(arrow);
        inventory.getItem(arrowSlot).shrink(1);
        getMainHandItem().hurtAndBreak(1, this, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && (getMainHandItem().getItem() instanceof SwordItem
            || getMainHandItem().getItem() instanceof AxeItem))
            getMainHandItem().hurtAndBreak(1, this, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return hit;
    }

    private int findTagged(net.minecraft.tags.TagKey<Block> tag) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BlockItem block && block.getBlock().defaultBlockState().is(tag)) return i;
        }
        return -1;
    }

    public void speak(String text, String emotion) {
        String clean = text == null ? "" : text.trim();
        if (clean.length() > 120) clean = clean.substring(0, 120);
        entityData.set(DATA_BUBBLE, clean);
        entityData.set(DATA_EMOTION, sanitize(emotion, 16, "curious"));
        bubbleTicks = 20 * Math.max(4, Math.min(12, clean.length() / 8 + 3));
    }

    public void emote(String emotion, String message) {
        String normalized = sanitize(emotion, 16, "happy");
        entityData.set(DATA_EMOTION, normalized);
        if (message != null && !message.isBlank()) speak(message, normalized);
        swing(InteractionHand.MAIN_HAND);
    }

    public String getBubble() { return entityData.get(DATA_BUBBLE); }
    public String getEmotion() { return entityData.get(DATA_EMOTION); }
    public String getActivity() { return entityData.get(DATA_ACTIVITY); }
    public void setActivity(String activity) { entityData.set(DATA_ACTIVITY, sanitize(activity, 80, "空闲")); }
    public String getThought() { return thought; }
    public void setThought(String value) { thought = sanitize(value, 256, thought); }
    public String getLongTermGoal() { return longTermGoal; }
    public String getHabit() { return habit; }
    public int getDragonLevel() { return dragonLevel; }
    public int getDragonExperience() { return dragonExperience; }
    public int getCompletedTasks() { return completedTasks; }
    public SimpleContainer getDragonInventory() { return inventory; }

    public String describeForAi() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize() && items.size() < 10; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) items.add(stack.getHoverName().getString() + "x" + stack.getCount());
        }
        return "龙龙等级=" + dragonLevel + "，经验=" + dragonExperience + "，习惯=" + habit + "，已完成任务=" + completedTasks
            + "，模式=" + getMode().chinese + "，当前活动=" + getActivity() + "，想法=" + thought
            + "，长期目标=" + longTermGoal + "，装备=" + getMainHandItem().getHoverName().getString()
            + "，背包=" + String.join("、", items) + "，近期记忆=" + String.join("；", memories);
    }

    public void remember(String memory) {
        String clean = sanitize(memory, 180, "");
        if (clean.isBlank()) return;
        memories.addLast(clean);
        while (memories.size() > 10) memories.removeFirst();
    }

    private void addExperience(int amount) {
        dragonExperience += Math.max(0, amount);
        int needed = dragonLevel * 50;
        while (dragonExperience >= needed) {
            dragonExperience -= needed;
            dragonLevel++;
            needed = dragonLevel * 50;
            heal(4.0F);
            speak("升级到 " + dragonLevel + " 级啦！一起继续冒险吧～", "proud");
        }
    }

    public void setYsmSelection(String model, String texture) {
        entityData.set(DATA_YSM_MODEL, sanitize(model, 96, ""));
        entityData.set(DATA_YSM_TEXTURE, sanitize(texture, 96, "-"));
    }
    public String getYsmModel() { return entityData.get(DATA_YSM_MODEL); }
    public String getYsmTexture() { return entityData.get(DATA_YSM_TEXTURE); }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && isOwnedBy(player)) {
            if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) openInventory(serverPlayer);
            else {
                Mode next = switch (getMode()) {
                    case FOLLOW -> Mode.STAY;
                    case STAY -> Mode.GUARD;
                    case GUARD -> Mode.GATHER;
                    case GATHER -> Mode.MINE;
                    default -> Mode.FOLLOW;
                };
                setMode(next);
                player.displayClientMessage(Component.literal("§d[龙龙] 已切换为：" + next.chinese), true);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return isCompanionInvincible() || super.isInvulnerableTo(source);
    }

    public Mode getMode() { return Mode.fromId(entityData.get(DATA_MODE)); }
    public void setMode(Mode mode) {
        if (getMode() == Mode.MINE && mode != Mode.MINE) clearMiningTarget();
        entityData.set(DATA_MODE, mode.id);
        setOrderedToSit(mode == Mode.STAY);
        if (mode == Mode.STAY) setTarget(null);
        setActivity(mode.chinese);
    }
    public boolean isCompanionInvincible() { return entityData.get(DATA_INVINCIBLE); }
    public void setCompanionInvincible(boolean value) { entityData.set(DATA_INVINCIBLE, value); }
    public String getSkinName() { return entityData.get(DATA_SKIN); }
    public void setSkinName(String fileName) {
        if (fileName != null && fileName.matches("[A-Za-z0-9._-]+\\.png")) entityData.set(DATA_SKIN, fileName);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("McAiMode", getMode().id);
        tag.putBoolean("McAiInvincible", isCompanionInvincible());
        tag.putString("McAiSkin", getSkinName());
        tag.putString("DragonBubble", getBubble());
        tag.putString("DragonEmotion", getEmotion());
        tag.putString("DragonActivity", getActivity());
        tag.putString("DragonThought", thought);
        tag.putString("DragonGoal", longTermGoal);
        tag.putInt("DragonLevel", dragonLevel);
        tag.putInt("DragonExperience", dragonExperience);
        tag.putInt("DragonCompletedTasks", completedTasks);
        tag.putInt("DragonDataVersion", 4);
        tag.putString("DragonHabit", habit);
        if (homePosition != null) tag.putLong("DragonHome", homePosition.asLong());
        tag.putString("YsmModel", getYsmModel());
        tag.putString("YsmTexture", getYsmTexture());
        tag.put("DragonInventory", inventory.createTag());
        ListTag memoryTag = new ListTag();
        for (String memory : memories) memoryTag.add(StringTag.valueOf(memory));
        tag.put("DragonMemories", memoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int dataVersion = tag.getInt("DragonDataVersion");
        Mode savedMode = Mode.fromId(tag.getInt("McAiMode"));
        setMode(dataVersion < 4 && savedMode == Mode.FOLLOW ? Mode.PATROL : savedMode);
        setCompanionInvincible(dataVersion < 4 || tag.getBoolean("McAiInvincible"));
        if (dataVersion < 4) setSkinName("white_dragon.png");
        else if (tag.contains("McAiSkin")) setSkinName(tag.getString("McAiSkin"));
        entityData.set(DATA_BUBBLE, tag.getString("DragonBubble"));
        entityData.set(DATA_EMOTION, sanitize(tag.getString("DragonEmotion"), 16, "curious"));
        entityData.set(DATA_ACTIVITY, sanitize(tag.getString("DragonActivity"), 80, "空闲"));
        thought = sanitize(tag.getString("DragonThought"), 256, thought);
        longTermGoal = sanitize(tag.getString("DragonGoal"), 256, longTermGoal);
        dragonLevel = Math.max(1, tag.getInt("DragonLevel"));
        dragonExperience = Math.max(0, tag.getInt("DragonExperience"));
        completedTasks = Math.max(0, tag.getInt("DragonCompletedTasks"));
        habit = sanitize(tag.getString("DragonHabit"), 32, habit);
        if (tag.contains("DragonHome", Tag.TAG_LONG)) homePosition = BlockPos.of(tag.getLong("DragonHome"));
        if (dataVersion < 4) setYsmSelection("001", "-");
        else setYsmSelection(tag.getString("YsmModel"), tag.getString("YsmTexture"));
        if (tag.contains("DragonInventory", Tag.TAG_LIST)) inventory.fromTag(tag.getList("DragonInventory", Tag.TAG_COMPOUND));
        memories.clear();
        ListTag memoryTag = tag.getList("DragonMemories", Tag.TAG_STRING);
        for (int i = 0; i < memoryTag.size(); i++) remember(memoryTag.getString(i));
        initializeIndependentAgent();
    }

    private void clearMiningTarget() {
        if (miningTarget != null && level() instanceof ServerLevel serverLevel)
            serverLevel.destroyBlockProgress(getId(), miningTarget, -1);
        miningTarget = null;
        miningProgress = 0;
        miningStuckTicks = 0;
    }

    private static String sanitize(String value, int max, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String clean = value.trim().replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) { return null; }

    public enum Mode {
        FOLLOW(0, "跟随"), STAY(1, "等待"), GUARD(2, "警戒"), GATHER(3, "拾取"), MINE(4, "下矿"),
        EXPLORE(5, "探索"), PATROL(6, "巡逻"), HUNT(7, "战斗"), LUMBER(8, "伐木"), FARM(9, "耕作"),
        BUILD(10, "建造"), FISH(11, "钓鱼");
        public final int id;
        public final String chinese;
        Mode(int id, String chinese) { this.id = id; this.chinese = chinese; }
        public static Mode fromId(int id) {
            for (Mode value : values()) if (value.id == id) return value;
            return FOLLOW;
        }
    }
}
