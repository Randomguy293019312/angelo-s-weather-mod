package com.meteorologia.client;

import com.meteorologia.Meteorologia;
import com.meteorologia.entity.HotAirBalloon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BalloonRenderer extends EntityRenderer<HotAirBalloon> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Meteorologia.MODID, "hot_air_balloon"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Meteorologia.MODID, "textures/entity/hot_air_balloon.png");

    private final BalloonModel model;

    public BalloonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BalloonModel(context.bakeLayer(LAYER));
    }

    @Override
    public void render(HotAirBalloon entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer vc = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HotAirBalloon entity) {
        return TEXTURE;
    }
}
