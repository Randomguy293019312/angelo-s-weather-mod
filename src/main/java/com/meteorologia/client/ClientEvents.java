package com.meteorologia.client;

import com.meteorologia.Config;
import com.meteorologia.Meteorologia;
import com.meteorologia.weather.Tornado;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Chuva local, neblina de tempestade, trovao de tornado e plano B com particulas. */
@EventBusSubscriber(modid = Meteorologia.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    private static ClientLevel lastLevel;
    private static float baseRain;
    private static float lastRainWritten = -1F;
    private static float localIntensity;
    private static float smoothedFog;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            ClientStormData.reset();
            lastLevel = null;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        ClientStormData.tick();

        if (level != lastLevel) {
            lastLevel = level;
            lastRainWritten = -1F;
            baseRain = level.getRainLevel(1.0F);
            smoothedFog = 0F;
            ClientStormData.reset();
        }

        // ---- chuva "local": sobrescreve o nivel de chuva do cliente, preservando o do servidor
        float current = level.getRainLevel(1.0F);
        if (lastRainWritten < 0F || Math.abs(current - lastRainWritten) > 0.002F) {
            baseRain = current; // o servidor mudou o clima
        }
        float target = Math.max(ClientStormData.localIntensity(player.getX(), player.getZ()), ClientStormData.rain());
        localIntensity += (target - localIntensity) * 0.02F;
        level.setRainLevel(Math.max(baseRain, localIntensity));
        lastRainWritten = level.getRainLevel(1.0F);
        smoothedFog = Math.max(localIntensity, baseRain * 0.4F);

        tornadoAmbience(mc, level, player);
    }

    private static void tornadoAmbience(Minecraft mc, ClientLevel level, LocalPlayer player) {
        double el = ClientStormData.elapsedTicks();
        for (Tornado t : ClientStormData.tornadoes()) {
            float str = ClientStormData.strength(t, el);
            if (str < 0.1F) {
                continue;
            }
            double tx = t.x + t.vx * el;
            double tz = t.z + t.vz * el;
            double dx = tx - player.getX();
            double dz = tz - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            // rugido: trovao grave vindo da direcao do tornado
            if (dist < 400.0 && level.getGameTime() % 40L == 0L) {
                double k = Math.min(dist, 14.0) / Math.max(dist, 1.0);
                float vol = (float) Mth.clamp(2.5 - dist / 180.0, 0.2, 2.5) * str;
                level.playLocalSound(player.getX() + dx * k, player.getY(), player.getZ() + dz * k,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, vol, 0.5F, false);
            }

            // plano B: se o shader nao carregou (ou esta desligado), faz o funil com particulas
            if ((ModShaders.storm == null || !Config.ENABLE_SHADER.get()) && dist < 300.0) {
                for (int i = 0; i < 10; i++) {
                    double hn = level.random.nextDouble();
                    double h = hn * Math.min(t.height, 90.0);
                    double r = t.radius * (0.5 + 2.5 * hn * hn) * (0.6 + 0.6 * level.random.nextDouble());
                    double a = level.random.nextDouble() * Math.PI * 2.0;
                    level.addParticle(hn < 0.15 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                            tx + Math.cos(a) * r, t.y + h, tz + Math.sin(a) * r,
                            Math.sin(a) * 0.3, 0.2, -Math.cos(a) * 0.3);
                }
            }
        }
    }

    // ---- neblina densa durante tempestades
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!Config.HEAVY_FOG.get() || smoothedFog < 0.05F) {
            return;
        }
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float target = Mth.lerp(Math.min(1F, smoothedFog), far, Math.min(far, 70F));
        event.setFarPlaneDistance(target);
        event.setNearPlaneDistance(Mth.lerp(Math.min(1F, smoothedFog), event.getNearPlaneDistance(), 0F));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (smoothedFog < 0.05F) {
            return;
        }
        float k = 1F - 0.45F * Math.min(1F, smoothedFog);
        event.setRed(event.getRed() * k);
        event.setGreen(event.getGreen() * k);
        event.setBlue(event.getBlue() * k);
    }
}
