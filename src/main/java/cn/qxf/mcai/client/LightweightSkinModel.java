package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * v10 轻量皮肤模型。使用原版玩家骨骼和标准 64x64 皮肤 UV，避免维护自定义 3D
 * 几何体；动作只修改六个原版肢体，渲染成本与普通玩家接近。
 */
public final class LightweightSkinModel extends PlayerModel<AiCompanionEntity> {
    public LightweightSkinModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }

    @Override
    public void setupAnim(AiCompanionEntity entity, float limbSwing, float limbAmount,
                          float age, float headYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbAmount, age, headYaw, headPitch);
        String gesture = entity.getGesture();
        String activity = entity.getActivity();
        float wave = Mth.sin(age * 0.62F);
        float slow = Mth.sin(age * 0.16F);

        if (entity.isOrderedToSit() || "sit".equals(gesture)) {
            crouching = true;
            body.xRot = 0.18F;
            rightLeg.xRot = -1.15F;
            leftLeg.xRot = -1.15F;
            rightLeg.yRot = -0.28F;
            leftLeg.yRot = 0.28F;
            rightArm.xRot = -0.18F;
            leftArm.xRot = -0.18F;
        }

        if (activity.contains("挖") || activity.contains("砍") || activity.contains("收割")
            || activity.contains("建造")) {
            rightArm.xRot = -1.45F + wave * 1.05F;
            rightArm.yRot = -0.18F;
            leftArm.xRot = -0.35F - wave * 0.16F;
            head.xRot += 0.12F;
            body.xRot = 0.08F;
        } else if (activity.contains("钓鱼")) {
            rightArm.xRot = -1.15F + slow * 0.08F;
            leftArm.xRot = -0.85F - slow * 0.08F;
            rightArm.yRot = -0.18F;
            leftArm.yRot = 0.25F;
        }

        switch (gesture) {
            case "wave" -> {
                rightArm.xRot = -2.65F + wave * 0.22F;
                rightArm.zRot = 0.35F + wave * 0.22F;
                head.zRot = -0.10F;
            }
            case "dance" -> {
                rightArm.zRot = 1.35F + wave * 0.25F;
                leftArm.zRot = -1.35F - wave * 0.25F;
                rightLeg.zRot = -slow * 0.12F;
                leftLeg.zRot = slow * 0.12F;
                body.yRot = slow * 0.18F;
            }
            case "cheer" -> {
                rightArm.xRot = leftArm.xRot = -2.75F;
                rightArm.zRot = 0.38F;
                leftArm.zRot = -0.38F;
            }
            case "bow" -> {
                body.xRot = 0.62F;
                head.xRot = -0.18F;
                rightArm.xRot = leftArm.xRot = -0.20F;
            }
            case "shy" -> {
                rightArm.xRot = leftArm.xRot = -1.20F;
                rightArm.yRot = -0.42F;
                leftArm.yRot = 0.42F;
                head.zRot = 0.14F;
            }
            case "stretch" -> {
                rightArm.xRot = leftArm.xRot = -2.95F;
                body.xRot = -0.10F;
            }
            case "nod" -> head.xRot += Mth.sin(age * 0.48F) * 0.22F;
            case "look" -> head.yRot += slow * 0.32F;
            case "spin" -> body.yRot = age * 0.34F;
            case "hop" -> {
                rightArm.zRot = 0.65F;
                leftArm.zRot = -0.65F;
                rightLeg.xRot = leftLeg.xRot = Mth.abs(wave) * 0.30F;
            }
            default -> { }
        }
    }
}
