package cn.qxf.mcai.server;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.entity.AiCompanionEntity;
import cn.qxf.mcai.entity.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
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
        companion.setCustomName(Component.literal("龙龙·ロンロン"));
        companion.setCustomNameVisible(true);
        companion.setCompanionInvincible(true);
        companion.setHomePosition(player.blockPosition());
        companion.initializeIndependentAgent();
        companion.setMode(AiCompanionEntity.Mode.PATROL);
        companion.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
        player.serverLevel().addFreshEntity(companion);
        register(companion);
        return companion;
    }

    public static void come(ServerPlayer player, AiCompanionEntity companion) {
        if (companion.level() != player.serverLevel()) {
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            companion.saveWithoutId(saved);
            companion.discard();
            BY_OWNER.remove(player.getUUID());
            AiCompanionEntity replacement = ModEntities.AI_COMPANION.get().create(player.serverLevel());
            if (replacement == null) throw new IllegalStateException("无法跨维度创建龙龙");
            replacement.load(saved);
            replacement.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(replacement);
            register(replacement);
        } else {
            companion.teleportTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D);
        }
    }

    public static void setMode(ServerPlayer player, AiCompanionEntity.Mode mode) {
        AiCompanionEntity companion = find(player);
        if (companion == null) companion = summon(player);
        companion.setMode(mode);
    }

    public static void applyActions(ServerPlayer player, List<AgentAction> actions) {
        if (actions == null || actions.isEmpty()) return;
        AiCompanionEntity companion = find(player);
        if (companion == null) companion = summon(player);
        for (AgentAction action : actions) companion.enqueueAction(action);
        QxfMcAi.LOGGER.info("为玩家 {} 下发 {} 个实际动作：{}", player.getGameProfile().getName(), actions.size(),
            actions.stream().map(AgentAction::type).toList());
    }

    public static boolean executeAuthorizedCommand(ServerPlayer player, String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.trim();
        while (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank() || command.length() > 512) return false;
        try {
            int result = player.server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4), command);
            player.sendSystemMessage(Component.literal("[龙龙·最高权限] 命令已提交：/" + command));
            QxfMcAi.LOGGER.info("龙龙为所有者 {} 执行 OP4 命令：/{}，返回值={}",
                player.getGameProfile().getName(), command, result);
            // Brigadier 的 0 也是合法返回值，不能据此把已执行命令误报为失败。
            return true;
        } catch (RuntimeException e) {
            player.sendSystemMessage(Component.literal("[龙龙·最高权限] 命令执行错误：" + e.getMessage()));
            QxfMcAi.LOGGER.error("龙龙执行 OP4 命令失败：/{}", command, e);
            return false;
        }
    }
}
