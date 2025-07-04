package net.tyrone.horrorofacespeke.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tyrone.horrorofacespeke.Horrorofacespeke;
import net.tyrone.horrorofacespeke.entity.custom.StalkerEntity;

public class StalkerRenderer extends MobRenderer<StalkerEntity, PlayerModel<StalkerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Horrorofacespeke.MODID, "textures/entity/acespeke.png");


    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return TEXTURE;
    }
}
