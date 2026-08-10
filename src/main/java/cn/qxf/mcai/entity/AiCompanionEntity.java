package cn.qxf.mcai.entity;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.ai.AiService;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.pathfinder.Path;
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
    private static final EntityDataAccessor<String> DATA_BUBBLE =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EMOTION =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIVITY =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_FAVORABILITY =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACCESSORY_COUNT =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FAMILY_CONSENT =
        SynchedEntityData.defineId(AiCompanionEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer inventory = new SimpleContainer(27);
    /** 龙龙私有装备仓：不在玩家可打开的27格背包中显示。 */
    private final SimpleContainer equipmentStorage = new SimpleContainer(18);
    private final SimpleContainer accessories = new SimpleContainer(4);
    private final Deque<AgentAction> taskQueue = new ArrayDeque<>();
    private final Deque<String> memories = new ArrayDeque<>();
    private final Deque<BlockPos> buildQueue = new ArrayDeque<>();
    @Nullable private AgentAction currentTask;
    @Nullable private BlockPos miningTarget;
    @Nullable private BlockPos searchTunnelTarget;
    @Nullable private BlockPos workTarget;
    @Nullable private BlockPos homePosition;
    @Nullable private BlockPos pendingTeleportPosition;
    @Nullable private BlockPos lastNotifiedOre;
    private int miningProgress;
    private int miningStuckTicks;
    private int workProgress;
    private int workGoal;
    private int taskTicks;
    private Vec3 lastTaskPosition = Vec3.ZERO;
    private int navigationRecoveryAttempts;
    private int bubbleTicks;
    private int dragonLevel = 1;
    private int dragonExperience;
    private int completedTasks;
    private boolean starterKitGranted;
    private int centralBuildingIndex;
    private String thought = "想和玩家一起把今天过好";
    private String longTermGoal = "建立安全、温暖的共同基地";
    private String habit = "探索者";
    private final byte[] gomokuBoard = new byte[81];
    private boolean gomokuActive;
    private int gomokuWins;
    private int gomokuLosses;
    private int childrenCount;
    private long lastFamilyProposal;
    private long lastBirth;
    private boolean familyChild;

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
        entityData.define(DATA_BUBBLE, "");
        entityData.define(DATA_EMOTION, "curious");
        entityData.define(DATA_ACTIVITY, "正在观察世界");
        entityData.define(DATA_FAVORABILITY, 0);
        entityData.define(DATA_ACCESSORY_COUNT, 0);
        entityData.define(DATA_FAMILY_CONSENT, false);
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
        if (familyChild) {
            if (getMode() != Mode.FOLLOW) setMode(Mode.FOLLOW);
            return;
        }
        if (tickCount == 20) {
            level().getEntities(this, getBoundingBox().inflate(256.0D),
                entity -> entity.getPersistentData().getBoolean("QxfMcAiDragon"))
                .forEach(Entity::discard);
        }
        if (tickCount % 20 == 0) CompanionManager.register(this);
        if (tickCount % 40 == 1) {
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
            && tickCount % 2400 == 0) autonomousDecision();
        if (currentTask == null && taskQueue.isEmpty() && tickCount % 6000 == 0) offerIndependentSuggestion();
        if (tickCount % 1200 == 0) offerFamilyProposal();
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
        if (currentTask == null && taskQueue.isEmpty()) {
            startTask(action);
            QxfMcAi.LOGGER.info("龙龙立即开始任务：type={} target={} count={}",
                action.type(), action.target(), action.count());
        } else {
            taskQueue.addLast(action);
            QxfMcAi.LOGGER.info("龙龙任务入队：type={} target={} count={} queue={}",
                action.type(), action.target(), action.count(), taskQueue.size());
        }
    }

    private void tickTaskEngine() {
        if (currentTask == null && !taskQueue.isEmpty()) startTask(taskQueue.removeFirst());
        if (currentTask == null) return;
        taskTicks++;
        if (taskTicks % 600 == 0 && getOwner() instanceof ServerPlayer owner)
            AiService.reviewAgentState(owner, "任务进度", describeForAi(), true);
        if (taskTicks > 3600) finishTask(false, "长时间无新进展，已安全停止并保留已完成成果");
    }

    private void startTask(AgentAction action) {
        currentTask = action;
        taskTicks = 0;
        workProgress = 0;
        workGoal = action.count();
        lastTaskPosition = position();
        navigationRecoveryAttempts = 0;
        QxfMcAi.LOGGER.info("龙龙开始任务：{} × {}，位置={}", action.type(), action.count(), blockPosition());
        String type = action.type();
        switch (type) {
            case "follow" -> { setMode(Mode.FOLLOW); finishTask(true, "跟着你走"); }
            case "stay" -> { setMode(Mode.STAY); finishTask(true, "在这里等你"); }
            case "guard" -> { setMode(Mode.GUARD); equipBestWeapon(); finishTask(true, "开始警戒"); }
            case "gather" -> setWorkMode(Mode.GATHER, "收集附近物品");
            case "mine" -> {
                equipBestPickaxe();
                setWorkMode(Mode.MINE, "向下开凿并寻找矿石");
                prepareUndergroundSearch(false);
            }
            case "find_cave" -> {
                equipBestPickaxe();
                setWorkMode(Mode.MINE, "独立向下开凿并寻找天然矿洞");
                prepareUndergroundSearch(true);
            }
            case "explore" -> setWorkMode(Mode.EXPLORE, "探索周围地形");
            case "patrol" -> setWorkMode(Mode.PATROL, "巡视基地周围");
            case "hunt" -> { equipBestWeapon(); setWorkMode(Mode.HUNT, "狩猎敌对生物"); }
            case "chop" -> { equipBestAxe(); setWorkMode(Mode.LUMBER, "寻找并砍伐树木"); }
            case "harvest", "plant", "farm" -> prepareFarmTask(type);
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
                String command = !action.command().isBlank() ? action.command()
                    : (!action.target().isBlank() ? action.target() : action.message());
                boolean done = getOwner() instanceof ServerPlayer owner
                    && CompanionManager.executeAuthorizedCommand(owner, command);
                finishTask(done, done ? "执行最高权限命令 /" + command : "命令为空或执行时发生错误");
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
            addFavorability(McAiConfig.TASK_FAVORABILITY_GAIN.get());
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
        searchTunnelTarget = null;
        clearMiningTarget();
        if (getMode() == Mode.GATHER || getMode() == Mode.MINE || getMode() == Mode.EXPLORE
            || getMode() == Mode.PATROL || getMode() == Mode.HUNT || getMode() == Mode.LUMBER
            || getMode() == Mode.FARM || getMode() == Mode.BUILD || getMode() == Mode.FISH)
            setMode(Mode.STAY);
        notifyOwner((success ? "任务完成" : "任务失败") + "：" + result);
        QxfMcAi.LOGGER.info("龙龙任务结束：type={} success={} result={}", finishedType, success, result);
        if (getOwner() instanceof ServerPlayer owner)
            AiService.reviewAgentState(owner, success ? "任务完成" : "任务失败",
                "任务=" + finishedType + "，结果=" + result + "。" + describeForAi(), true);
    }

    private void autonomousDecision() {
        if (!(getOwner() instanceof ServerPlayer owner)) return;
        if (AiService.isConfigured()) {
            AiService.reviewAgentState(owner, "AI自主决策", describeForAi(), true);
            return;
        }
        if (getHealth() < getMaxHealth() * 0.45F && hasFood()) enqueueAction(AgentAction.simple("eat"));
        else if (owner.level().isNight())
            enqueueAction(new AgentAction("patrol", "home", 2, "", ""));
        else switch (habit) {
            case "矿工" -> enqueueAction(new AgentAction("mine", "ores", 3, "", ""));
            case "建设者" -> enqueueAction(new AgentAction("build_house", "central_base", 1, "", ""));
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

    /** 五分钟主动对话的立即本地回馈；API 随后会基于完整状态继续思考。 */
    public String proactiveLocalMessage() {
        String message;
        if (currentTask != null) {
            message = "我正在" + getActivity() + "，当前进度 " + workProgress + "/" + workGoal
                + "。我会继续行动，并让 AI 根据现场调整后续计划。";
        } else if (getHealth() < getMaxHealth() * 0.5F) {
            message = "我现在状态偏低，想先整理食物和装备，再继续帮你做事。";
        } else if (level().isNight()) {
            message = "天黑了，我想听听你今晚更想冒险、整理物资，还是安心休息。";
        } else {
            message = switch (habit) {
                case "矿工" -> "今天挖到的东西里，你最希望先补齐哪一种？我想听听你的计划。";
                case "建设者" -> "我在观察基地布局，不过这会儿只想陪你聊聊：你最喜欢怎样的基地风格？";
                case "守卫" -> "我在留意附近的危险和防御死角，不会只站在这里发呆。";
                default -> "我想看看附近还有哪些安全可达的地方，再让 AI 决定值得做什么。";
            };
        }
        thought = message;
        speak(message, "curious");
        notifyOwner(message);
        return message;
    }

    public void initializeIndependentAgent() {
        if (homePosition == null) homePosition = blockPosition();
        if (familyChild) {
            setMode(Mode.FOLLOW);
            return;
        }
        int profile = Math.floorMod(getUUID().hashCode(), 4);
        habit = switch (profile) { case 0 -> "矿工"; case 1 -> "建设者"; case 2 -> "守卫"; default -> "探索者"; };
        ensureOwnEquipment();
        grantStarterKit();
        if (getMainHandItem().isEmpty()) equipBestWeapon();
        remember("v9初始化：我习惯做一名" + habit + "，AI负责思考，我的单实体负责真正行动");
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
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        if (getItemBySlot(EquipmentSlot.CHEST).isEmpty()) setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        if (getItemBySlot(EquipmentSlot.LEGS).isEmpty()) setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        if (getItemBySlot(EquipmentSlot.FEET).isEmpty()) setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    }

    private void ensureTool(net.minecraft.world.item.Item item, Class<?> type) {
        if (type.isInstance(getMainHandItem().getItem())) return;
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++)
            if (type.isInstance(equipmentStorage.getItem(i).getItem())) return;
        equipmentStorage.addItem(new ItemStack(item));
    }

    private void grantStarterKit() {
        if (starterKitGranted) return;
        equipmentStorage.addItem(new ItemStack(Items.ARROW, 64));
        inventory.addItem(new ItemStack(Items.COOKED_BEEF, 16));
        inventory.addItem(new ItemStack(Items.TORCH, 64));
        inventory.addItem(new ItemStack(Items.COBBLESTONE, 64));
        inventory.addItem(new ItemStack(Items.COBBLESTONE, 64));
        inventory.addItem(new ItemStack(Items.COBBLESTONE, 64));
        inventory.addItem(new ItemStack(Items.OAK_PLANKS, 64));
        inventory.addItem(new ItemStack(Items.OAK_PLANKS, 64));
        inventory.addItem(new ItemStack(Items.WHEAT_SEEDS, 32));
        starterKitGranted = true;
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

    private void offerFamilyProposal() {
        if (!(getOwner() instanceof ServerPlayer owner) || familyChild || getFavorability() < 80 || completedTasks < 10) return;
        long now = level().getGameTime();
        if (now - lastFamilyProposal < 12000) return;
        lastFamilyProposal = now;
        if (!hasFamilyConsent()) {
            String proposal = "我很珍惜我们一起生活的经历。愿意和我组建家庭、以后一起孕育小龙宝宝吗？只有你明确同意才会继续。";
            speak(proposal, "happy");
            Component accept = Component.literal(" [同意组建家庭]").withStyle(style -> style
                .withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcai family accept")));
            Component decline = Component.literal(" [暂时不要]").withStyle(style -> style
                .withColor(ChatFormatting.GRAY).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcai family decline")));
            owner.sendSystemMessage(Component.literal("[龙龙] " + proposal).append(accept).append(decline));
        } else if (now - lastBirth >= 24000) {
            String proposal = "我们的家已经很温暖了。你愿意现在和我一起迎接一个小龙宝宝吗？";
            speak(proposal, "curious");
            owner.sendSystemMessage(Component.literal("[龙龙] " + proposal).append(Component.literal(" [愿意]")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcai family child")))));
        }
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
        if (item == null) {
            if (currentTask != null && taskTicks > 40) finishTask(true, workProgress > 0
                ? "拾取完成，已收集 " + workProgress + " 批物品"
                : "附近掉落物已扫描完成，当前无可拾取物品");
            return;
        }
        if (distanceToSqr(item) > 2.25D) { getNavigation().moveTo(item, 1.45D); return; }
        int before = item.getItem().getCount();
        ItemStack remainder = inventory.addItem(item.getItem().copy());
        item.setItem(remainder);
        if (remainder.isEmpty()) item.discard();
        if (remainder.getCount() < before) completeWorkUnit();
        else if (currentTask != null) finishTask(false, "27格物资背包已满，无法继续拾取");
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
            if (tickCount % 40 == 0) {
                miningTarget = findOreBelow(serverLevel);
                if (miningTarget != null && !miningTarget.equals(lastNotifiedOre)) {
                    lastNotifiedOre = miningTarget.immutable();
                    offerTeleportToOwner(miningTarget, "我独立发现了矿脉 " + serverLevel.getBlockState(miningTarget).getBlock().getName().getString()
                        + "，坐标 " + miningTarget.toShortString());
                }
            }
        }
        if (miningTarget == null) {
            if (searchTunnelTarget == null) searchTunnelTarget = createTunnelTarget();
            if (tickCount % 4 == 0) excavateToward(serverLevel, searchTunnelTarget);
            if (taskTicks > 1800) finishTask(false, "已实际向下开凿并搜索 90 秒，当前范围未发现矿石");
            return;
        }
        Vec3 center = Vec3.atCenterOf(miningTarget);
        if (position().distanceToSqr(center) > 8.0D) {
            boolean moving = !getNavigation().isDone();
            if (tickCount % 10 == 0) moving = getNavigation().moveTo(center.x, center.y, center.z, 1.35D);
            miningProgress = 0;
            miningStuckTicks = moving ? Math.max(0, miningStuckTicks - 1) : miningStuckTicks + 2;
            if (miningStuckTicks > 12 || tickCount % 12 == 0) excavateToward(serverLevel, miningTarget);
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
            if (tickCount % 40 == 0) {
                workTarget = findCaveBelow(level);
                if (workTarget != null)
                    offerTeleportToOwner(workTarget, "我发现了地下天然空间，坐标 " + workTarget.toShortString());
            }
        }
        if (workTarget == null) {
            if (searchTunnelTarget == null) searchTunnelTarget = createTunnelTarget();
            if (tickCount % 4 == 0) excavateToward(level, searchTunnelTarget);
            if (taskTicks > 2400) finishTask(false, "已向下开凿并搜索 120 秒，当前范围未找到天然矿洞");
            return;
        }
        BlockPos target = workTarget.immutable();
        if (target.distSqr(blockPosition()) > 10.0D) {
            if (tickCount % 10 == 0)
                getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.35D);
            if (tickCount % 8 == 0) excavateToward(level, target);
            return;
        }
        finishTask(true, "已抵达天然矿洞 " + target.toShortString());
        offerTeleportToOwner(target, "我已经安全抵达矿洞");
    }

    private void prepareUndergroundSearch(boolean cave) {
        searchTunnelTarget = createTunnelTarget();
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (cave) workTarget = findCaveBelow(serverLevel);
        else miningTarget = findOreBelow(serverLevel);
    }

    private BlockPos createTunnelTarget() {
        int depth = Math.max(8, McAiConfig.MINING_DEPTH.get());
        int x = random.nextBoolean() ? 10 : -10;
        int z = random.nextBoolean() ? 10 : -10;
        return blockPosition().offset(x, -depth, z);
    }

    @Nullable
    private BlockPos findCaveBelow(ServerLevel level) {
        BlockPos origin = blockPosition();
        int radius = McAiConfig.MINING_RADIUS.get();
        int depth = Math.min(64, McAiConfig.MINING_DEPTH.get() * 2);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int checked = 0;
        caveScan: for (int dy = -4; dy >= -depth; dy--) for (int dx = -radius; dx <= radius; dx += 2)
            for (int dz = -radius; dz <= radius; dz++) {
                if (++checked > 6144) break caveScan;
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
        int checked = 0;
        oreScan: for (int dy = -1; dy >= -depth; dy--) {
            int horizontal = Math.min(radius, 4 + (-dy / 3));
            for (int dx = -horizontal; dx <= horizontal; dx++) for (int dz = -horizontal; dz <= horizontal; dz++) {
                if (++checked > 8192) break oreScan;
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
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && state.getFluidState().isEmpty()
                && level.getBlockEntity(pos) == null) {
                equipBestToolFor(state);
                miningProgress++;
                if (miningProgress >= adjustedBreakTicks(state, getMainHandItem())) {
                    breakIntoInventory(level, pos, getMainHandItem());
                    miningProgress = 0;
                }
                swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
        getNavigation().moveTo(next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D, 1.35D);
        miningStuckTicks = 0;
    }

    private boolean breakIntoInventory(ServerLevel level, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !state.getFluidState().isEmpty()
            || level.getBlockEntity(pos) != null) return false;
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
        if (currentTask == null || tickCount % 10 != 0) return;
        if (workTarget != null && position().distanceToSqr(Vec3.atCenterOf(workTarget)) < 9.0D) {
            completeWorkUnit();
            if (currentTask == null) return;
            workTarget = null;
            navigationRecoveryAttempts = 0;
        }
        if (workTarget == null && !selectReachableExploreTarget()) {
            navigationRecoveryAttempts++;
            if (navigationRecoveryAttempts >= 4)
                finishTask(true, workProgress > 0 ? "已完成可达区域巡查" : "已检查附近，暂无更多安全可达路线");
            return;
        }
        if (workTarget == null) return;
        Path path = getNavigation().createPath(workTarget, 1);
        if (path == null || !path.canReach()) {
            workTarget = null;
            navigationRecoveryAttempts++;
            return;
        }
        if (position().distanceToSqr(lastTaskPosition) < 0.04D && getNavigation().isDone()) {
            if (++navigationRecoveryAttempts >= 4) workTarget = null;
        } else {
            lastTaskPosition = position();
        }
        getNavigation().moveTo(path, 1.35D);
    }

    private boolean selectReachableExploreTarget() {
        int radius = getMode() == Mode.PATROL ? 12 : 28;
        BlockPos current = blockPosition();
        BlockPos origin = getMode() == Mode.PATROL && homePosition != null
            && Math.abs(homePosition.getY() - current.getY()) <= 10 && homePosition.distSqr(current) <= 2304
            ? homePosition : current;
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = origin.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos candidate = findWalkablePosition(x, z, current.getY());
            if (candidate == null || candidate.distSqr(current) < 16.0D) continue;
            Path path = getNavigation().createPath(candidate, 1);
            if (path != null && path.canReach()) {
                workTarget = candidate;
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findWalkablePosition(int x, int z, int preferredY) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] ys = offset == 0 ? new int[]{preferredY} : new int[]{preferredY + offset, preferredY - offset};
            for (int y : ys) {
                BlockPos feet = new BlockPos(x, y, z);
                if (level().getBlockState(feet).isAir() && level().getBlockState(feet.above()).isAir()
                    && level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), Direction.UP))
                    return feet;
            }
        }
        return null;
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
            else if (currentTask != null && taskTicks > 80) finishTask(true, "巡查完成，附近没有敌对生物");
        }
    }

    private void lumberTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 4 != 0) return;
        if (workTarget == null || !serverLevel.getBlockState(workTarget).is(BlockTags.LOGS))
            workTarget = findNearestBlock(serverLevel, 12, state -> state.is(BlockTags.LOGS));
        if (workTarget == null) {
            if (currentTask != null && taskTicks > 60) finishTask(true, workProgress > 0
                ? "伐木完成，已处理 " + workProgress + " 根原木"
                : "扫描完成，附近没有可砍伐树木");
            return;
        }
        if (workTarget.distSqr(blockPosition()) > 6.0D) {
            if (tickCount % 12 == 0) getNavigation().moveTo(workTarget.getX(), workTarget.getY(), workTarget.getZ(), 1.35D);
            return;
        }
        if (!(getMainHandItem().getItem() instanceof AxeItem) && !equipBestAxe()) {
            finishTask(false, "背包里没有斧头");
            return;
        }
        if (breakIntoInventory(serverLevel, workTarget, getMainHandItem())) { completeWorkUnit(); workTarget = null; }
    }

    private void prepareFarmTask(String type) {
        setWorkMode(Mode.FARM, type.equals("plant") ? "扫描空耕地并播种" : "立即扫描附近农田");
        if (!(level() instanceof ServerLevel serverLevel)) return;
        workTarget = type.equals("plant") ? findPlantingSpot(serverLevel) : findMatureCrop(serverLevel);
        if (workTarget == null && type.equals("farm") && findSeed() >= 0)
            workTarget = findPlantingSpot(serverLevel);
        if (workTarget == null) finishFarmScan(type.equals("farm"));
    }

    private void farmTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 4 != 0 || currentTask == null) return;
        String type = currentTask.type();
        if (type.equals("plant") || (type.equals("farm") && workTarget != null
            && serverLevel.getBlockState(workTarget).is(Blocks.FARMLAND))) {
            plantCropTick(serverLevel);
            return;
        }
        if (workTarget == null || !(serverLevel.getBlockState(workTarget).getBlock() instanceof CropBlock crop)
            || !crop.isMaxAge(serverLevel.getBlockState(workTarget)))
            workTarget = findMatureCrop(serverLevel);
        if (workTarget == null) {
            if (type.equals("farm") && findSeed() >= 0) {
                workTarget = findPlantingSpot(serverLevel);
                if (workTarget != null) { plantCropTick(serverLevel); return; }
            }
            finishFarmScan(type.equals("farm"));
            return;
        }
        if (workTarget.distSqr(blockPosition()) > 6.0D) {
            if (tickCount % 12 == 0) getNavigation().moveTo(workTarget.getX(), workTarget.getY(), workTarget.getZ(), 1.35D);
            return;
        }
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

    @Nullable
    private BlockPos findMatureCrop(ServerLevel level) {
        return findNearestBlock(level, 16,
            state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state));
    }

    private void finishFarmScan(boolean scanCountsAsSuccess) {
        boolean success = workProgress > 0 || scanCountsAsSuccess;
        finishTask(success, workProgress > 0
            ? "农田扫描完成，已处理 " + workProgress + " 格"
            : "农田扫描完成，附近暂无成熟作物或可播种耕地");
    }

    private void plantCropTick(ServerLevel serverLevel) {
        int seedSlot = findSeed();
        if (seedSlot < 0) { finishFarmScan(currentTask != null && currentTask.type().equals("farm")); return; }
        if (workTarget == null || !serverLevel.getBlockState(workTarget).is(Blocks.FARMLAND)
            || !serverLevel.getBlockState(workTarget.above()).isAir())
            workTarget = findPlantingSpot(serverLevel);
        if (workTarget == null) {
            finishFarmScan(currentTask != null && currentTask.type().equals("farm")); return;
        }
        if (workTarget.distSqr(blockPosition()) > 8.0D) {
            if (tickCount % 12 == 0) getNavigation().moveTo(workTarget.getX(), workTarget.getY() + 1, workTarget.getZ(), 1.35D);
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
        String idea = currentTask == null ? "" : currentTask.target().trim();
        String normalizedIdea = idea.toLowerCase(Locale.ROOT);
        BlockPos base = bridge ? blockPosition().relative(getDirection(), 3).below() : nextCentralBuildingSite();
        if (bridge) {
            Direction direction = getDirection();
            Direction side = direction.getClockWise();
            int lengthLimit = normalizedIdea.contains("长") || normalizedIdea.contains("long") ? 18 : 12;
            for (int length = 0; length < lengthLimit; length++) for (int width = -1; width <= 1; width++)
                buildQueue.add(base.relative(direction, length).relative(side, width));
        } else {
            int radius = house ? 3 : 2;
            int height = house ? 4 : 3;
            // API 的建筑目标会落到安全、轻量的受限蓝图中，而不是被忽略或任意改动世界。
            if (normalizedIdea.contains("塔") || normalizedIdea.contains("tower") || normalizedIdea.contains("瞭望")) {
                radius = 2;
                height = 6;
            } else if (normalizedIdea.contains("仓库") || normalizedIdea.contains("storage") || normalizedIdea.contains("储物")) {
                radius = 3;
                height = 3;
            } else if (normalizedIdea.contains("农舍") || normalizedIdea.contains("farm")) {
                radius = 3;
                height = 3;
            }
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) buildQueue.add(base.offset(x, 0, z));
            for (int y = 1; y < height; y++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    if (!(z == -radius && x == 0 && y <= 2)) buildQueue.add(base.offset(x, y, z));
                }
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) buildQueue.add(base.offset(x, height, z));
        }
        String safeIdea = idea.isBlank() ? "" : "（AI 方案：" + idea.substring(0, Math.min(24, idea.length())) + "）";
        setWorkMode(Mode.BUILD, (bridge ? "建造桥梁" : house ? "在基地建筑区建造房屋" : "在基地建筑区搭建庇护所") + safeIdea);
    }

    private BlockPos nextCentralBuildingSite() {
        BlockPos center = homePosition == null ? blockPosition() : homePosition;
        int slot = centralBuildingIndex++;
        int column = slot % 4;
        int row = slot / 4;
        BlockPos rough = center.offset(10 + column * 12, 0, 10 + row * 12);
        if (level() instanceof ServerLevel serverLevel)
            return serverLevel.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rough);
        return rough;
    }

    private void buildTick() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) return;
        if (buildQueue.isEmpty()) {
            boolean needsAnother = currentTask != null && workProgress + 1 < workGoal;
            completeWorkUnit();
            if (needsAnother && currentTask != null) {
                String type = currentTask.type();
                prepareBuilding(type.equals("build_house"), type.equals("build_bridge"));
            }
            return;
        }
        BlockPos pos = buildQueue.peekFirst();
        if (!serverLevel.getWorldBorder().isWithinBounds(pos)) { buildQueue.removeFirst(); return; }
        if (pos.distSqr(blockPosition()) > 20.0D) { getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.35D); return; }
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

    public void openInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (id, playerInventory, ignored) -> ChestMenu.threeRows(id, playerInventory, inventory),
            Component.literal("龙龙的独立背包 · 27格")));
    }

    public boolean equipBestWeapon() { return equipBest(stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof BowItem); }
    public boolean equipBestPickaxe() { return equipBest(stack -> stack.getItem() instanceof PickaxeItem); }
    public boolean equipBestAxe() { return equipBest(stack -> stack.getItem() instanceof AxeItem); }

    private boolean equipBestToolFor(BlockState state) {
        int bestSlot = -1;
        float bestScore = toolScore(getMainHandItem(), state);
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++) {
            ItemStack candidate = equipmentStorage.getItem(i);
            float score = toolScore(candidate, state);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        if (bestSlot >= 0) equipFromSlot(bestSlot);
        return !getMainHandItem().isEmpty();
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return 0.0F;
        float speed = stack.getDestroySpeed(state);
        if (stack.isCorrectToolForDrops(state)) speed += 100.0F;
        return speed - stack.getDamageValue() * 0.0001F;
    }

    private boolean equipBest(java.util.function.Predicate<ItemStack> predicate) {
        int best = -1;
        int score = -1;
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++) {
            ItemStack stack = equipmentStorage.getItem(i);
            if (!predicate.test(stack)) continue;
            int current = stack.getItem() instanceof TieredItem tiered ? tiered.getTier().getLevel() * 100 - stack.getDamageValue() : 50;
            if (current > score) { score = current; best = i; }
        }
        if (best >= 0) { equipFromSlot(best); return true; }
        return predicate.test(getMainHandItem());
    }

    private void equipFromSlot(int slot) {
        ItemStack next = equipmentStorage.removeItemNoUpdate(slot);
        ItemStack old = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, next);
        if (!old.isEmpty()) {
            ItemStack remainder = isHiddenEquipment(old)
                ? equipmentStorage.addItem(old) : inventory.addItem(old);
            if (!remainder.isEmpty()) spawnAtLocation(remainder);
        }
    }

    private int findTool(Class<?> type) {
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++)
            if (type.isInstance(equipmentStorage.getItem(i).getItem())) return i;
        return -1;
    }

    private int findItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(item)) return i;
        return -1;
    }

    private int findHiddenItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++)
            if (equipmentStorage.getItem(i).is(item)) return i;
        return -1;
    }

    private boolean hasArrows() { return findHiddenItem(Items.ARROW) >= 0; }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        int arrowSlot = findHiddenItem(Items.ARROW);
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
        equipmentStorage.getItem(arrowSlot).shrink(1);
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

    private static boolean isHiddenEquipment(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem || stack.getItem() instanceof SwordItem
            || stack.getItem() instanceof BowItem || stack.getItem() instanceof FishingRodItem
            || stack.is(Items.ARROW);
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
    public int getFavorability() { return entityData.get(DATA_FAVORABILITY); }
    public void addFavorability(int amount) {
        entityData.set(DATA_FAVORABILITY, Mth.clamp(getFavorability() + amount, 0, 100));
    }
    public int getAccessoryCount() { return entityData.get(DATA_ACCESSORY_COUNT); }
    public boolean hasFamilyConsent() { return entityData.get(DATA_FAMILY_CONSENT); }
    public int getChildrenCount() { return childrenCount; }
    public int getGomokuWins() { return gomokuWins; }
    public int getGomokuLosses() { return gomokuLosses; }
    public boolean isFamilyChild() { return familyChild; }

    public boolean equipAccessory(Player player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return false;
        for (int i = 0; i < accessories.getContainerSize(); i++) {
            if (!accessories.getItem(i).isEmpty()) continue;
            accessories.setItem(i, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            entityData.set(DATA_ACCESSORY_COUNT, getAccessoryCount() + 1);
            addFavorability(McAiConfig.ACCESSORY_FAVORABILITY_GAIN.get());
            speak("谢谢你的饰品，我会好好戴着。", "happy");
            return true;
        }
        return false;
    }

    public String accessorySummary() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < accessories.getContainerSize(); i++)
            if (!accessories.getItem(i).isEmpty()) names.add(accessories.getItem(i).getHoverName().getString());
        return names.isEmpty() ? "无" : String.join("、", names);
    }

    public void acceptFamily() {
        entityData.set(DATA_FAMILY_CONSENT, true);
        lastBirth = level().getGameTime() - 24000;
        addFavorability(5);
        remember("玩家明确同意和我组建家庭");
        speak("谢谢你认真回应我。我们慢慢来，一起把家经营好。", "happy");
    }

    public void declineFamily() {
        entityData.set(DATA_FAMILY_CONSENT, false);
        speak("没关系，我尊重你的决定，我们还是最可靠的伙伴。", "curious");
    }

    public boolean createFamilyChild(ServerPlayer player) {
        long now = level().getGameTime();
        if (!hasFamilyConsent() || getFavorability() < 80 || now - lastBirth < 24000) return false;
        AiCompanionEntity child = ModEntities.AI_COMPANION.get().create(player.serverLevel());
        if (child == null) return false;
        child.tame(player);
        child.familyChild = true;
        child.setBaby(true);
        child.setCustomName(Component.literal("小龙宝宝 " + (childrenCount + 1)));
        child.setCustomNameVisible(true);
        child.setCompanionInvincible(true);
        child.setMode(Mode.FOLLOW);
        child.moveTo(getX() + 0.8D, getY(), getZ() + 0.8D, getYRot(), 0);
        player.serverLevel().addFreshEntity(child);
        childrenCount++;
        lastBirth = now;
        addFavorability(5);
        remember("家庭迎来了第" + childrenCount + "个小龙宝宝");
        speak("欢迎来到我们的家，小家伙。我们一起照顾它吧！", "proud");
        return true;
    }

    public void startGomoku() {
        java.util.Arrays.fill(gomokuBoard, (byte) 0);
        gomokuActive = true;
        speak("五子棋开局！使用 /mcai gomoku 下 x y，坐标为0到8。", "focused");
    }

    public String playGomoku(int x, int y) {
        if (x < 0 || x > 8 || y < 0 || y > 8) return "坐标必须是0到8";
        if (!gomokuActive) startGomoku();
        int index = y * 9 + x;
        if (gomokuBoard[index] != 0) return "这个位置已经有棋子了";
        gomokuBoard[index] = 1;
        if (hasFive((byte) 1)) {
            gomokuWins++;
            gomokuActive = false;
            addFavorability(2);
            return "你赢了！这局很漂亮";
        }
        int move = chooseGomokuMove();
        if (move < 0) { gomokuActive = false; return "棋盘满了，这局平局"; }
        gomokuBoard[move] = 2;
        int mx = move % 9, my = move / 9;
        if (hasFive((byte) 2)) {
            gomokuLosses++;
            gomokuActive = false;
            return "我下在 " + mx + "," + my + "，这局我赢啦";
        }
        return "我下在 " + mx + "," + my + "，轮到你";
    }

    private int chooseGomokuMove() {
        for (byte side : new byte[]{2, 1}) for (int i = 0; i < 81; i++) if (gomokuBoard[i] == 0) {
            gomokuBoard[i] = side;
            boolean wins = hasFive(side);
            gomokuBoard[i] = 0;
            if (wins) return i;
        }
        if (gomokuBoard[40] == 0) return 40;
        int start = random.nextInt(81);
        for (int n = 0; n < 81; n++) { int i = (start + n) % 81; if (gomokuBoard[i] == 0) return i; }
        return -1;
    }

    private boolean hasFive(byte side) {
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
        for (int y = 0; y < 9; y++) for (int x = 0; x < 9; x++) for (int[] d : dirs) {
            int endX = x + d[0] * 4, endY = y + d[1] * 4;
            if (endX < 0 || endX >= 9 || endY < 0 || endY >= 9) continue;
            boolean ok = true;
            for (int n = 0; n < 5; n++) if (gomokuBoard[(y + d[1]*n)*9 + x + d[0]*n] != side) { ok = false; break; }
            if (ok) return true;
        }
        return false;
    }

    public String describeForAi() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize() && items.size() < 10; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) items.add(stack.getHoverName().getString() + "x" + stack.getCount());
        }
        List<String> equipment = new ArrayList<>();
        for (int i = 0; i < equipmentStorage.getContainerSize(); i++) {
            ItemStack stack = equipmentStorage.getItem(i);
            if (!stack.isEmpty()) equipment.add(stack.getHoverName().getString() + "x" + stack.getCount());
        }
        return "龙龙等级=" + dragonLevel + "，经验=" + dragonExperience + "，习惯=" + habit + "，已完成任务=" + completedTasks
            + "，模式=" + getMode().chinese + "，当前活动=" + getActivity() + "，想法=" + thought
            + "，长期目标=" + longTermGoal + "，装备=" + getMainHandItem().getHoverName().getString()
            + "，隐藏装备仓=" + String.join("、", equipment) + "，27格背包=" + String.join("、", items)
            + "，好感度=" + getFavorability() + "，饰品=" + accessorySummary()
            + "，家庭孩子=" + childrenCount + "，五子棋=" + gomokuWins + "胜/" + gomokuLosses + "负"
            + "，近期记忆=" + String.join("；", memories);
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

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && isOwnedBy(player)) {
            if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) openInventory(serverPlayer);
            else if (player.isSprinting() && getPassengers().isEmpty()) {
                player.startRiding(this, true);
                player.displayClientMessage(Component.literal("§b[龙龙] 已骑乘；按潜行键下马"), true);
            }
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

    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        move.accept(passenger, getX(), getY() + getBbHeight() + 0.15D, getZ());
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
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("McAiMode", getMode().id);
        tag.putBoolean("McAiInvincible", isCompanionInvincible());
        tag.putString("DragonBubble", getBubble());
        tag.putString("DragonEmotion", getEmotion());
        tag.putString("DragonActivity", getActivity());
        tag.putString("DragonThought", thought);
        tag.putString("DragonGoal", longTermGoal);
        tag.putInt("DragonLevel", dragonLevel);
        tag.putInt("DragonExperience", dragonExperience);
        tag.putInt("DragonCompletedTasks", completedTasks);
        tag.putInt("DragonFavorability", getFavorability());
        tag.putBoolean("DragonFamilyConsent", hasFamilyConsent());
        tag.putInt("DragonChildren", childrenCount);
        tag.putLong("DragonFamilyProposal", lastFamilyProposal);
        tag.putLong("DragonLastBirth", lastBirth);
        tag.putBoolean("DragonFamilyChild", familyChild);
        tag.putInt("DragonGomokuWins", gomokuWins);
        tag.putInt("DragonGomokuLosses", gomokuLosses);
        tag.putBoolean("DragonGomokuActive", gomokuActive);
        tag.putByteArray("DragonGomokuBoard", gomokuBoard);
        tag.put("DragonAccessories", accessories.createTag());
        tag.putBoolean("DragonStarterKitGranted", starterKitGranted);
        tag.putInt("DragonCentralBuildingIndex", centralBuildingIndex);
        tag.putInt("DragonDataVersion", 9);
        tag.putString("DragonHabit", habit);
        if (homePosition != null) tag.putLong("DragonHome", homePosition.asLong());
        tag.put("DragonInventory", inventory.createTag());
        tag.put("DragonEquipmentStorage", equipmentStorage.createTag());
        ListTag memoryTag = new ListTag();
        for (String memory : memories) memoryTag.add(StringTag.valueOf(memory));
        tag.put("DragonMemories", memoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int dataVersion = tag.getInt("DragonDataVersion");
        if (dataVersion < 7) {
            setInvisible(false);
            setCustomNameVisible(true);
        }
        Mode savedMode = Mode.fromId(tag.getInt("McAiMode"));
        setMode(dataVersion < 4 && savedMode == Mode.FOLLOW ? Mode.PATROL : savedMode);
        setCompanionInvincible(dataVersion < 4 || tag.getBoolean("McAiInvincible"));
        entityData.set(DATA_BUBBLE, tag.getString("DragonBubble"));
        entityData.set(DATA_EMOTION, sanitize(tag.getString("DragonEmotion"), 16, "curious"));
        entityData.set(DATA_ACTIVITY, sanitize(tag.getString("DragonActivity"), 80, "空闲"));
        thought = sanitize(tag.getString("DragonThought"), 256, thought);
        longTermGoal = sanitize(tag.getString("DragonGoal"), 256, longTermGoal);
        dragonLevel = Math.max(1, tag.getInt("DragonLevel"));
        dragonExperience = Math.max(0, tag.getInt("DragonExperience"));
        completedTasks = Math.max(0, tag.getInt("DragonCompletedTasks"));
        int storedFavorability = tag.getInt("DragonFavorability");
        int migratedTaskFavorability = (int) Math.min(100L,
            (long) completedTasks * McAiConfig.TASK_FAVORABILITY_GAIN.get());
        int savedFavorability = dataVersion < 7
            ? Math.max(storedFavorability, migratedTaskFavorability)
            : storedFavorability;
        entityData.set(DATA_FAVORABILITY, Mth.clamp(savedFavorability, 0, 100));
        entityData.set(DATA_FAMILY_CONSENT, tag.getBoolean("DragonFamilyConsent"));
        childrenCount = Math.max(0, tag.getInt("DragonChildren"));
        lastFamilyProposal = tag.getLong("DragonFamilyProposal");
        lastBirth = tag.getLong("DragonLastBirth");
        familyChild = tag.getBoolean("DragonFamilyChild");
        gomokuWins = Math.max(0, tag.getInt("DragonGomokuWins"));
        gomokuLosses = Math.max(0, tag.getInt("DragonGomokuLosses"));
        gomokuActive = tag.getBoolean("DragonGomokuActive");
        byte[] savedBoard = tag.getByteArray("DragonGomokuBoard");
        if (savedBoard.length == 81) System.arraycopy(savedBoard, 0, gomokuBoard, 0, 81);
        if (tag.contains("DragonAccessories", Tag.TAG_LIST))
            accessories.fromTag(tag.getList("DragonAccessories", Tag.TAG_COMPOUND));
        int accessoryCount = 0;
        for (int i = 0; i < accessories.getContainerSize(); i++)
            if (!accessories.getItem(i).isEmpty()) accessoryCount++;
        entityData.set(DATA_ACCESSORY_COUNT, accessoryCount);
        starterKitGranted = dataVersion < 5 || tag.getBoolean("DragonStarterKitGranted");
        centralBuildingIndex = Math.max(0, tag.getInt("DragonCentralBuildingIndex"));
        habit = sanitize(tag.getString("DragonHabit"), 32, habit);
        if (tag.contains("DragonHome", Tag.TAG_LONG)) homePosition = BlockPos.of(tag.getLong("DragonHome"));
        if (tag.contains("DragonInventory", Tag.TAG_LIST)) inventory.fromTag(tag.getList("DragonInventory", Tag.TAG_COMPOUND));
        if (tag.contains("DragonEquipmentStorage", Tag.TAG_LIST))
            equipmentStorage.fromTag(tag.getList("DragonEquipmentStorage", Tag.TAG_COMPOUND));
        if (dataVersion < 5) moveVisibleEquipmentToHidden();
        memories.clear();
        ListTag memoryTag = tag.getList("DragonMemories", Tag.TAG_STRING);
        for (int i = 0; i < memoryTag.size(); i++) remember(memoryTag.getString(i));
        initializeIndependentAgent();
    }

    private void moveVisibleEquipmentToHidden() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !isHiddenEquipment(stack)) continue;
            ItemStack moved = inventory.removeItemNoUpdate(i);
            ItemStack remainder = equipmentStorage.addItem(moved);
            if (!remainder.isEmpty()) inventory.setItem(i, remainder);
        }
        inventory.setChanged();
        equipmentStorage.setChanged();
    }

    private void clearMiningTarget() {
        if (miningTarget != null && level() instanceof ServerLevel serverLevel)
            serverLevel.destroyBlockProgress(getId(), miningTarget, -1);
        miningTarget = null;
        miningProgress = 0;
        miningStuckTicks = 0;
        searchTunnelTarget = null;
    }

    private static String sanitize(String value, int max, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String clean = value.trim().replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (!(otherParent instanceof AiCompanionEntity other) || !hasFamilyConsent() || !other.hasFamilyConsent()) return null;
        AiCompanionEntity child = ModEntities.AI_COMPANION.get().create(level);
        if (child != null) {
            child.familyChild = true;
            child.setOwnerUUID(getOwnerUUID());
            child.setCustomName(Component.literal("小龙宝宝"));
            child.setCompanionInvincible(true);
        }
        return child;
    }

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
