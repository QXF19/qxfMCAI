package cn.qxf.mcai.server;

import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.entity.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CompanionManager {
    private static final Map<UUID, UUID> BY_OWNER = new ConcurrentHashMap<>();

    private CompanionManager() {}

    public static void register(AiCompanionEntity entity) {
        if (entity.getOwnerUUID() != null) BY_OWNER.put(entity.getOwnerUUID(), entity.getUUID());
    }

    public static AiCompanionEntity find(ServerPlayer player) {
        UUID entityId = BY_OWNER.get(player.getUUID());
        if (entityId != null) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity entity = level.getEntity(entityId);
                if (entity instanceof AiCompanionEntity companion && companion.isAlive()) return companion;
            }
        }
        AiCompanionEntity nearby = player.serverLevel().getEntitiesOfClass(AiCompanionEntity.class,
            player.getBoundingBox().inflate(256.0D), e -> e.isOwnedBy(player)).stream().findFirst().orElse(null);
        if (nearby != null) register(nearby);
        return nearby;
    }

    public static AiCompanionEntity summon(ServerPlayer player) {
        AiCompanionEntity existing = find(player);
        if (existing != null) {
            come(player, existing);
            return existing;
        }
        AiCompanionEntity companion = ModEntities.AI_COMPANION.get().create(player.serverLevel());
        if (companion == null) throw new IllegalStateException("无法创建AI伙伴实体");
        companion.tame(player);
        companion.setCustomName(Component.literal("AI伙伴·小麦"));
        companion.setCustomNameVisible(true);
        companion.setMode(AiCompanionEntity.Mode.FOLLOW);
        companion.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
        player.serverLevel().addFreshEntity(companion);
        register(companion);
        return companion;
    }

    public static void come(ServerPlayer player, AiCompanionEntity companion) {
        if (companion.level() != player.serverLevel()) {
            String skin = companion.getSkinName();
            boolean invincible = companion.isCompanionInvincible();
            AiCompanionEntity.Mode mode = companion.getMode();
            companion.discard();
            BY_OWNER.remove(player.getUUID());
            AiCompanionEntity replacement = summon(player);
            replacement.setSkinName(skin);
            replacement.setCompanionInvincible(invincible);
            replacement.setMode(mode);
        } else {
            companion.teleportTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D);
        }
    }

    public static void setMode(ServerPlayer player, AiCompanionEntity.Mode mode) {
        AiCompanionEntity companion = find(player);
        if (companion == null) companion = summon(player);
        companion.setMode(mode);
    }

    public static void applyActions(ServerPlayer player, List<String> actions) {
        for (String raw : actions) {
            String action = raw.toLowerCase(Locale.ROOT);
            switch (action) {
                case "follow" -> setMode(player, AiCompanionEntity.Mode.FOLLOW);
                case "stay" -> setMode(player, AiCompanionEntity.Mode.STAY);
                case "guard" -> setMode(player, AiCompanionEntity.Mode.GUARD);
                case "gather" -> setMode(player, AiCompanionEntity.Mode.GATHER);
                case "mine" -> setMode(player, AiCompanionEntity.Mode.MINE);
                case "come" -> {
                    AiCompanionEntity companion = find(player);
                    if (companion != null) come(player, companion);
                }
                default -> { /* 严格忽略模型产生的未知或危险动作 */ }
            }
        }
    }
}
