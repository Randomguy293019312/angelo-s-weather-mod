package com.meteorologia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Ponte entre codigo comum (blocos) e telas do cliente. So e carregada no cliente. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openRadar(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RadarScreen(pos));
    }
}
