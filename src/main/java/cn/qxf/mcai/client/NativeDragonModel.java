package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** 轻量原生白龙模型：一个实体、一套骨骼，不进行代理实体传送。 */
public final class NativeDragonModel extends HierarchicalModel<AiCompanionEntity> implements ArmedModel, HeadedModel {
    private final ModelPart root;
    private final ModelPart white;
    private final ModelPart blue;
    private final ModelPart gold;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart chest;
    private final ModelPart faceAccent;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart horns;
    private final ModelPart belt;
    private final ModelPart ornament;

    public NativeDragonModel(ModelPart root) {
        this.root = root;
        white = root.getChild("white");
        blue = root.getChild("blue");
        gold = root.getChild("gold");
        body = white.getChild("body");
        head = white.getChild("head");
        chest = blue.getChild("chest");
        faceAccent = blue.getChild("face_accent");
        leftArm = white.getChild("left_arm");
        rightArm = white.getChild("right_arm");
        leftLeg = white.getChild("left_leg");
        rightLeg = white.getChild("right_leg");
        leftWing = white.getChild("left_wing");
        rightWing = white.getChild("right_wing");
        tail1 = white.getChild("tail_1");
        tail2 = tail1.getChild("tail_2");
        horns = gold.getChild("horns");
        belt = gold.getChild("belt");
        ornament = gold.getChild("ornament");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition white = root.addOrReplaceChild("white", CubeListBuilder.create(), PartPose.ZERO);
        white.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
            .addBox(-4.0F, 0.0F, -2.2F, 8.0F, 10.0F, 4.4F, new CubeDeformation(0.15F)), PartPose.offset(0, 5, 0));
        white.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4.2F, -7.8F, -4.0F, 8.4F, 8.0F, 8.0F, new CubeDeformation(0.2F))
            .texOffs(32, 0).addBox(-2.7F, -3.1F, -6.2F, 5.4F, 3.2F, 2.5F, new CubeDeformation(0.05F))
            .texOffs(46, 0).addBox(-4.0F, -10.8F, -1.2F, 2.0F, 4.0F, 2.0F)
            .texOffs(54, 0).addBox(2.0F, -10.8F, -1.2F, 2.0F, 4.0F, 2.0F), PartPose.offset(0, 5, 0));
        white.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 18)
            .addBox(-1.5F, -1.0F, -1.6F, 3.0F, 11.0F, 3.2F, new CubeDeformation(0.08F)), PartPose.offset(5.2F, 6, 0));
        white.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 18).mirror()
            .addBox(-1.5F, -1.0F, -1.6F, 3.0F, 11.0F, 3.2F, new CubeDeformation(0.08F)), PartPose.offset(-5.2F, 6, 0));
        white.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32)
            .addBox(-1.9F, 0, -2.0F, 3.8F, 9.0F, 4.0F, new CubeDeformation(0.08F)), PartPose.offset(2.2F, 15, 0));
        white.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).mirror()
            .addBox(-1.9F, 0, -2.0F, 3.8F, 9.0F, 4.0F, new CubeDeformation(0.08F)), PartPose.offset(-2.2F, 15, 0));
        white.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 34)
            .addBox(0, -1, 0, 1.0F, 8.0F, 9.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(3.2F, 7, 1.5F, 0.18F, 0.35F, -0.35F));
        white.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 34).mirror()
            .addBox(-1, -1, 0, 1.0F, 8.0F, 9.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-3.2F, 7, 1.5F, 0.18F, -0.35F, 0.35F));
        PartDefinition tail1 = white.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(40, 20)
            .addBox(-1.6F, -1.5F, 0, 3.2F, 3.2F, 7.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0, 13, 1.8F, 0.35F, 0, 0));
        tail1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(40, 30)
            .addBox(-1.2F, -1.2F, 0, 2.4F, 2.4F, 7.0F, new CubeDeformation(-0.08F)), PartPose.offsetAndRotation(0, 0, 6.2F, 0.22F, 0, 0));

        PartDefinition blue = root.addOrReplaceChild("blue", CubeListBuilder.create(), PartPose.ZERO);
        blue.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 48)
            .addBox(-3.4F, 0, -2.55F, 6.8F, 7.5F, 0.7F, new CubeDeformation(0.02F)), PartPose.offset(0, 7, 0));
        blue.addOrReplaceChild("face_accent", CubeListBuilder.create().texOffs(28, 48)
            .addBox(-2.2F, -2.5F, -6.5F, 4.4F, 2.0F, 0.5F)
            .texOffs(40, 55).addBox(-3.0F, -6.0F, -4.35F, 1.5F, 2.0F, 0.5F)
            .texOffs(48, 55).addBox(1.5F, -6.0F, -4.35F, 1.5F, 2.0F, 0.5F), PartPose.offset(0, 5, 0));

        PartDefinition gold = root.addOrReplaceChild("gold", CubeListBuilder.create(), PartPose.ZERO);
        gold.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(40, 48)
            .addBox(-3.7F, -12.5F, -0.7F, 1.2F, 3.5F, 1.2F)
            .addBox(2.5F, -12.5F, -0.7F, 1.2F, 3.5F, 1.2F), PartPose.offset(0, 5, 0));
        gold.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 57)
            .addBox(-4.15F, 0, -2.45F, 8.3F, 1.0F, 4.9F), PartPose.offset(0, 13, 0));
        gold.addOrReplaceChild("ornament", CubeListBuilder.create().texOffs(28, 55)
            .addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0, 9, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public ModelPart root() { return root; }
    @Override public ModelPart getHead() { return head; }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack pose) {
        ModelPart arm = side == HumanoidArm.LEFT ? leftArm : rightArm;
        white.translateAndRotate(pose);
        arm.translateAndRotate(pose);
        pose.translate(side == HumanoidArm.LEFT ? 0.035F : -0.035F, 0.60F, -0.02F);
    }

    @Override
    public void setupAnim(AiCompanionEntity entity, float limbSwing, float limbAmount,
                          float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float motion = Mth.clamp((float) entity.getDeltaMovement().horizontalDistance() * 4.0F, 0, 1);
        float walk = Mth.clamp(Math.max(limbAmount, motion), 0, 1);
        float idle = Mth.sin(age * 0.075F);
        float breath = Mth.sin(age * 0.10F) * 0.045F;
        float gait = Mth.cos(limbSwing * 0.6662F);
        float bob = Math.abs(Mth.sin(limbSwing * 0.6662F)) * walk * 0.34F + breath;
        float bodyLean = walk * 0.055F;

        white.y = blue.y = gold.y = bob;
        white.xRot = blue.xRot = gold.xRot = bodyLean;
        body.zRot = gait * walk * 0.035F;
        chest.zRot = body.zRot;
        belt.zRot = body.zRot;

        head.yRot = Mth.clamp(headYaw, -65.0F, 65.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -40.0F, 45.0F) * Mth.DEG_TO_RAD + idle * 0.018F;
        faceAccent.yRot = head.yRot;
        faceAccent.xRot = head.xRot;
        horns.yRot = head.yRot;
        horns.xRot = head.xRot;

        leftLeg.xRot = gait * 1.12F * walk;
        rightLeg.xRot = -gait * 1.12F * walk;
        leftLeg.zRot = -0.025F * walk;
        rightLeg.zRot = 0.025F * walk;
        leftArm.xRot = -gait * 0.82F * walk + idle * 0.025F * (1.0F - walk);
        rightArm.xRot = gait * 0.82F * walk - idle * 0.025F * (1.0F - walk);
        leftArm.zRot = -0.055F;
        rightArm.zRot = 0.055F;

        float airborne = entity.onGround() ? 0.0F : 1.0F;
        float flap = Mth.sin(age * (airborne > 0 ? 0.58F : 0.12F));
        float wingAmplitude = 0.07F + airborne * 0.48F + walk * 0.06F;
        leftWing.zRot = -0.38F - flap * wingAmplitude;
        rightWing.zRot = 0.38F + flap * wingAmplitude;
        leftWing.yRot = 0.35F + airborne * 0.18F;
        rightWing.yRot = -0.35F - airborne * 0.18F;

        float tailWave = Mth.sin(age * 0.11F) * 0.20F + Mth.sin(limbSwing * 0.42F) * walk * 0.13F;
        tail1.yRot = tailWave;
        tail1.xRot = 0.35F + walk * 0.08F;
        tail2.yRot = Mth.sin(age * 0.11F + 0.85F) * 0.27F + tailWave * 0.35F;
        tail2.xRot = 0.22F + Mth.sin(age * 0.08F) * 0.035F;

        if (entity.isOrderedToSit()) {
            leftLeg.xRot = rightLeg.xRot = -1.2F;
            leftArm.xRot = rightArm.xRot = -0.25F;
            white.y = blue.y = gold.y = 3.2F;
            tail1.xRot = 0.78F;
            leftWing.zRot = -0.25F;
            rightWing.zRot = 0.25F;
        }
        String activity = entity.getActivity();
        if (activity.contains("挖") || activity.contains("砍") || activity.contains("收割") || activity.contains("建造")) {
            float workSwing = Mth.sin(age * 0.72F);
            rightArm.xRot = -1.30F + workSwing * 1.05F;
            rightArm.zRot = 0.12F;
            leftArm.xRot = -0.28F - workSwing * 0.18F;
            head.xRot += 0.12F;
            white.xRot = blue.xRot = gold.xRot = 0.10F;
        } else if (entity.swinging || attackTime > 0.0F) {
            float attack = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightArm.xRot = -1.65F + attack * 1.15F;
            rightArm.yRot = -0.22F + attack * 0.42F;
            leftArm.xRot = -0.42F;
            white.yRot = blue.yRot = gold.yRot = Mth.sin(attackTime * Mth.PI) * 0.10F;
        }
        ornament.visible = entity.getAccessoryCount() > 0;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
                               float red, float green, float blueValue, float alpha) {
        pose.pushPose();
        if (young) {
            pose.scale(0.58F, 0.58F, 0.58F);
            pose.translate(0, 1.15F, 0);
        }
        white.render(pose, consumer, light, overlay, 0.97F, 0.94F, 1.0F, alpha);
        blue.render(pose, consumer, light, overlay, 0.18F, 0.62F, 0.96F, alpha);
        gold.render(pose, consumer, light, overlay, 1.0F, 0.72F, 0.18F, alpha);
        pose.popPose();
    }
}
