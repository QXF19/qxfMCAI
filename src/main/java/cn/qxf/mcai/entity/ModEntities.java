package cn.qxf.mcai.entity;

import cn.qxf.mcai.QxfMcAi;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, QxfMcAi.MOD_ID);

    public static final RegistryObject<EntityType<AiCompanionEntity>> AI_COMPANION = REGISTER.register(
        "ai_companion",
        () -> EntityType.Builder.<AiCompanionEntity>of(AiCompanionEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .updateInterval(2)
            .build(QxfMcAi.MOD_ID + ":ai_companion")
    );

    private ModEntities() {}

    @Mod.EventBusSubscriber(modid = QxfMcAi.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        @SubscribeEvent
        public static void register(EntityAttributeCreationEvent event) {
            event.put(AI_COMPANION.get(), AiCompanionEntity.createAttributes().build());
        }
    }
}
