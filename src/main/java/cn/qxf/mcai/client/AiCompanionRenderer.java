package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** v11 原版玩家骨骼 + 用户提供的 64x64 二维毛毛龙皮肤。 */
public final class AiCompanionRenderer extends MobRenderer<AiCompanionEntity, LightweightSkinModel> {
    private static final ResourceLocation SKIN = new ResourceLocation("qxfmcai", "textures/entity/longlong.png");

    public AiCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new LightweightSkinModel(context.bakeLayer(ClientEvents.SKIN_LAYER)), 0.45F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override public ResourceLocation getTextureLocation(AiCompanionEntity entity) { return SKIN; }

    @Override
    protected void renderNameTag(AiCompanionEntity entity, Component name, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        super.renderNameTag(entity, name, pose, buffers, light);
        String bubble = entity.getBubble();
        if (bubble.isBlank()) return;
        String face = switch (entity.getEmotion()) {
            case "joy" -> "ヾ(≧▽≦*)o";
            case "angry" -> "( ` ω ´ )";
            case "sad" -> "(｡•́︿•̀｡)";
            case "happy" -> "(≧▽≦)";
            case "focused" -> "( •̀ ω •́ )✧";
            case "worried" -> "(・_・;)";
            case "proud" -> "(￣▽￣)ゞ✧";
            case "sleepy" -> "(－ω－) zzZ";
            default -> "ฅ(•ㅅ•❀)ฅ";
        };
        Font font = Minecraft.getInstance().font;
        Component line = Component.literal(face + " " + bubble + "  §8[" + entity.getActivity() + "]");
        pose.pushPose();
        pose.translate(0, entity.getBbHeight() + 0.55F, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        var lines = font.split(line, 200);
        float y = -lines.size() * 5.0F;
        for (var text : lines) {
            font.drawInBatch(text, -font.width(text) / 2.0F, y, 0xFFFFE8, false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0x990B1020, light);
            y += 10;
        }
        pose.popPose();
    }
}
