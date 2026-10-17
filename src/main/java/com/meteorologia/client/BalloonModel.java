package com.meteorologia.client;

import com.meteorologia.entity.HotAirBalloon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Cesta + cordas + queimador + envelope (balao) em camadas. Textura 256x256. */
public class BalloonModel extends EntityModel<HotAirBalloon> {
    private final ModelPart root;

    public BalloonModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0F, 24F, 0F));

        // cesta de vime
        body.addOrReplaceChild("basket", CubeListBuilder.create().texOffs(0, 100)
                .addBox(-12F, -12F, -12F, 24F, 12F, 24F), PartPose.ZERO);
        // quatro cordas/postes
        CubeListBuilder pole = CubeListBuilder.create().texOffs(0, 150);
        body.addOrReplaceChild("pole1", pole.addBox(-11.5F, -30F, -11.5F, 2F, 18F, 2F), PartPose.ZERO);
        body.addOrReplaceChild("pole2", CubeListBuilder.create().texOffs(0, 150)
                .addBox(9.5F, -30F, -11.5F, 2F, 18F, 2F), PartPose.ZERO);
        body.addOrReplaceChild("pole3", CubeListBuilder.create().texOffs(0, 150)
                .addBox(-11.5F, -30F, 9.5F, 2F, 18F, 2F), PartPose.ZERO);
        body.addOrReplaceChild("pole4", CubeListBuilder.create().texOffs(0, 150)
                .addBox(9.5F, -30F, 9.5F, 2F, 18F, 2F), PartPose.ZERO);
        // queimador
        body.addOrReplaceChild("burner", CubeListBuilder.create().texOffs(100, 100)
                .addBox(-3F, -32F, -3F, 6F, 6F, 6F), PartPose.ZERO);

        // envelope: pilha de caixas formando um balao
        CubeListBuilder env = CubeListBuilder.create().texOffs(0, 0);
        body.addOrReplaceChild("skirt", env.addBox(-12F, -42F, -12F, 24F, 10F, 24F), PartPose.ZERO);
        body.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-18F, -54F, -18F, 36F, 12F, 36F), PartPose.ZERO);
        body.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-27F, -76F, -27F, 54F, 22F, 54F), PartPose.ZERO);
        body.addOrReplaceChild("upper", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-23F, -90F, -23F, 46F, 14F, 46F), PartPose.ZERO);
        body.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-14F, -98F, -14F, 28F, 8F, 28F), PartPose.ZERO);
        body.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6F, -102F, -6F, 12F, 4F, 12F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(HotAirBalloon entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // sem animacao
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
