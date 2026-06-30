package cn.qxf.mcai.client;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.entity.AiCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class DragonFurryLayer extends RenderLayer<AiCompanionEntity, PlayerModel<AiCompanionEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        new ResourceLocation(QxfMcAi.MOD_ID, "dragon_furry"), "main");
    private final ModelPart ears;
    private final ModelPart tail;

    public DragonFurryLayer(RenderLayerParent<AiCompanionEntity, PlayerModel<AiCompanionEntity>> parent,
                            EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(LAYER);
        ears = root.getChild("ears");
        tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("ears", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4.0F, -11.0F, -1.0F, 2.0F, 4.0F, 1.0F)
            .texOffs(6, 0).addBox(2.0F, -11.0F, -1.0F, 2.0F, 4.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-1.5F, 7.0F, 1.5F, 3.0F, 3.0F, 9.0F), PartPose.rotation(0.45F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AiCompanionEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        ResourceLocation texture = SkinLoader.textureFor(entity.getSkinName());
        VertexConsumer vertex = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        ears.render(pose, vertex, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        tail.yRot = (float) Math.sin((entity.tickCount + partialTick) * 0.12F) * 0.35F;
        tail.render(pose, vertex, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
