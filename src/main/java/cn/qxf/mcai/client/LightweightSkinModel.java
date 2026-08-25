package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * v11 轻量皮肤模型。使用原版玩家骨骼和标准 64x64 皮肤 UV，避免维护自定义 3D
 * 几何体；动作只修改六个原版肢体，渲染成本与普通玩家接近。
 */
public final class LightweightSkinModel extends PlayerModel<AiCompanionEntity> {
    private static final float BLEND_TICKS = 8.0F;
    private final Map<AiCompanionEntity, BlendState> blendStates = new WeakHashMap<>();

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

        BlendState state = blendStates.computeIfAbsent(entity, ignored -> new BlendState());
        if (!gesture.equals(state.current)) {
            state.previous = state.current;
            state.current = gesture;
            state.changedAt = age;
        }
        float linear = Mth.clamp((age - state.changedAt) / BLEND_TICKS, 0.0F, 1.0F);
        float eased = linear * linear * (3.0F - 2.0F * linear);
        if (!state.previous.isBlank() && eased < 1.0F) applyGesture(state.previous, 1.0F - eased, age, wave, slow);
        if (!state.current.isBlank()) applyGesture(state.current, eased, age, wave, slow);
        if (linear >= 1.0F) state.previous = "";
        applyEmotion(entity.getEmotion(), slow);
    }

    private void applyGesture(String gesture, float weight, float age, float wave, float slow) {
        if (weight <= 0.0F) return;
        switch (gesture) {
            case "wave" -> {
                rightArm.xRot = blend(rightArm.xRot, -2.65F + wave * 0.22F, weight);
                rightArm.zRot = blend(rightArm.zRot, 0.35F + wave * 0.22F, weight);
                head.zRot = blend(head.zRot, -0.10F, weight);
            }
            case "dance" -> {
                rightArm.zRot = blend(rightArm.zRot, 1.35F + wave * 0.25F, weight);
                leftArm.zRot = blend(leftArm.zRot, -1.35F - wave * 0.25F, weight);
                rightLeg.zRot = blend(rightLeg.zRot, -slow * 0.12F, weight);
                leftLeg.zRot = blend(leftLeg.zRot, slow * 0.12F, weight);
                body.yRot = blend(body.yRot, slow * 0.18F, weight);
            }
            case "cheer" -> {
                rightArm.xRot = blend(rightArm.xRot, -2.75F, weight);
                leftArm.xRot = blend(leftArm.xRot, -2.75F, weight);
                rightArm.zRot = blend(rightArm.zRot, 0.38F, weight);
                leftArm.zRot = blend(leftArm.zRot, -0.38F, weight);
            }
            case "bow" -> {
                body.xRot = blend(body.xRot, 0.62F, weight);
                head.xRot = blend(head.xRot, -0.18F, weight);
                rightArm.xRot = blend(rightArm.xRot, -0.20F, weight);
                leftArm.xRot = blend(leftArm.xRot, -0.20F, weight);
            }
            case "shy" -> {
                rightArm.xRot = blend(rightArm.xRot, -1.20F, weight);
                leftArm.xRot = blend(leftArm.xRot, -1.20F, weight);
                rightArm.yRot = blend(rightArm.yRot, -0.42F, weight);
                leftArm.yRot = blend(leftArm.yRot, 0.42F, weight);
                head.zRot = blend(head.zRot, 0.14F, weight);
            }
            case "stretch" -> {
                rightArm.xRot = blend(rightArm.xRot, -2.95F, weight);
                leftArm.xRot = blend(leftArm.xRot, -2.95F, weight);
                body.xRot = blend(body.xRot, -0.10F, weight);
            }
            case "nod" -> head.xRot += Mth.sin(age * 0.48F) * 0.22F * weight;
            case "look" -> head.yRot += slow * 0.32F * weight;
            case "spin" -> body.yRot = blend(body.yRot, age * 0.34F, weight);
            case "hop" -> {
                rightArm.zRot = blend(rightArm.zRot, 0.65F, weight);
                leftArm.zRot = blend(leftArm.zRot, -0.65F, weight);
                rightLeg.xRot = blend(rightLeg.xRot, Mth.abs(wave) * 0.30F, weight);
                leftLeg.xRot = blend(leftLeg.xRot, Mth.abs(wave) * 0.30F, weight);
            }
            default -> { }
        }
    }

    private static float blend(float from, float to, float weight) {
        return Mth.lerp(Mth.clamp(weight, 0.0F, 1.0F), from, to);
    }

    private void applyEmotion(String emotion, float slow) {
        switch (emotion) {
            case "joy" -> {
                rightArm.zRot += 0.10F + slow * 0.05F;
                leftArm.zRot -= 0.10F + slow * 0.05F;
                head.zRot += slow * 0.035F;
            }
            case "angry" -> {
                head.xRot += 0.12F;
                rightArm.xRot -= 0.16F;
                leftArm.xRot -= 0.16F;
            }
            case "sad" -> {
                head.xRot += 0.24F;
                rightArm.zRot -= 0.08F;
                leftArm.zRot += 0.08F;
            }
            case "happy" -> head.zRot += slow * 0.025F;
            default -> { }
        }
    }

    private static final class BlendState {
        private String current = "";
        private String previous = "";
        private float changedAt = -BLEND_TICKS;
    }
}
