package com.meteorologia;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Meteorologia.MODID)
public class Meteorologia {
    public static final String MODID = "meteorologia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Meteorologia(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }
}
