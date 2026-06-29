package cn.qxf.mcai.client;

import cn.qxf.mcai.entity.AiCompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class AiCompanionRenderer extends MobRenderer<AiCompanionEntity, PlayerModel<AiCompanionEntity>> {
    public AiCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AiCompanionEntity entity) {
        return SkinLoader.textureFor(entity.getSkinName());
    }
}

