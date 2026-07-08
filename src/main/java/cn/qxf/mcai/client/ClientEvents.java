package cn.qxf.mcai.client;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.entity.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = QxfMcAi.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    public static final ModelLayerLocation DRAGON_LAYER = new ModelLayerLocation(
        new ResourceLocation(QxfMcAi.MOD_ID, "native_dragon"), "main");
    public static final KeyMapping OPEN_MENU = new KeyMapping(
        "key.qxfmcai.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.qxfmcai");

    private ClientEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AI_COMPANION.get(), AiCompanionRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DRAGON_LAYER, NativeDragonModel::createBodyLayer);
    }

    @Mod.EventBusSubscriber(modid = QxfMcAi.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_MENU.consumeClick()) {
                if (minecraft.player != null) minecraft.setScreen(new AiControlScreen());
            }
        }
    }
}
