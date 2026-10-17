package com.meteorologia.client;

import com.meteorologia.Meteorologia;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Registra o shader de ray marching. Se falhar ao compilar, o mod usa particulas como plano B. */
@EventBusSubscriber(modid = Meteorologia.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModShaders {
    private ModShaders() {}

    public static ShaderInstance storm;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(Meteorologia.MODID, "storm"),
                            DefaultVertexFormat.POSITION),
                    shader -> storm = shader);
        } catch (Exception ex) {
            storm = null;
            Meteorologia.LOGGER.error("Falha ao carregar o shader de ray marching; usando particulas.", ex);
        }
    }
}
