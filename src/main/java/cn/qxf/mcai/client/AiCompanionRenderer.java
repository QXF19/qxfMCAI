package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.minecraft.resources.ResourceLocation;

public final class AiCompanionRenderer extends MobRenderer<AiCompanionEntity, PlayerModel<AiCompanionEntity>> {
    public AiCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new DragonFurryLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(AiCompanionEntity entity) {
        return SkinLoader.textureFor(entity.getSkinName());
    }

    @Override
    protected void renderNameTag(AiCompanionEntity entity, Component name, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        super.renderNameTag(entity, name, pose, buffers, light);
        String bubble = entity.getBubble();
        if (bubble.isBlank()) return;
        String face = switch (entity.getEmotion()) {
            case "happy" -> "(≧▽≦)";
            case "focused" -> "( •̀ ω •́ )✧";
            case "worried" -> "(｡•́︿•̀｡)";
            case "proud" -> "(๑•̀ㅂ•́)و✧";
            case "sleepy" -> "(－ω－) zzZ";
            default -> "ฅ(•ㅅ•❀)ฅ";
        };
        Font font = Minecraft.getInstance().font;
        Component line = Component.literal(face + " " + bubble + "  §8[" + entity.getActivity() + "]");
        pose.pushPose();
        pose.translate(0.0F, entity.getBbHeight() + 0.72F, 0.0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        var lines = font.split(line, 200);
        float y = -lines.size() * 5.0F;
        int background = 0x990B1020;
        for (var text : lines) {
            float x = -font.width(text) / 2.0F;
            font.drawInBatch(text, x, y, 0xFFFFD8, false, matrix, buffers,
                Font.DisplayMode.NORMAL, background, light);
            y += 10.0F;
        }
        pose.popPose();
    }
}
