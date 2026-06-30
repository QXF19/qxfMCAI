package cn.qxf.mcai.server;

import cn.qxf.mcai.ai.AgentAction;
import cn.qxf.mcai.config.McAiConfig;
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
        companion.setCustomName(Component.literal("龙龙·ロンロン"));
        companion.setCustomNameVisible(true);
        companion.setMode(AiCompanionEntity.Mode.FOLLOW);
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
        AiCompanionEntity companion = find(player);
        if (companion == null) companion = summon(player);
        for (AgentAction action : actions) companion.enqueueAction(action);
    }

    public static boolean executeAuthorizedCommand(ServerPlayer player, String rawCommand) {
        if (!McAiConfig.ALLOW_FULL_COMMANDS.get() || !player.hasPermissions(4)) {
            player.sendSystemMessage(Component.literal("[龙龙] 全命令权限未在菜单中由 OP4 开启。"));
            return false;
        }
        String command = rawCommand == null ? "" : rawCommand.trim();
        while (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank() || command.length() > 512) return false;
        int result = player.server.getCommands().performPrefixedCommand(
            player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
        player.sendSystemMessage(Component.literal("[龙龙] 已执行授权命令：/" + command));
        return result > 0;
    }
}
