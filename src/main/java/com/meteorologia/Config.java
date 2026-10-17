package com.meteorologia;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private Config() {}

    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // ---- servidor / mundo
    public static final ModConfigSpec.BooleanValue NATURAL_STORMS;
    public static final ModConfigSpec.DoubleValue STORM_FREQUENCY;
    public static final ModConfigSpec.IntValue MAX_SUPERCELLS;
    public static final ModConfigSpec.BooleanValue BLOCK_DAMAGE;
    public static final ModConfigSpec.IntValue MAX_DEBRIS;
    public static final ModConfigSpec.BooleanValue LIGHTNING;

    // ---- cliente / graficos
    public static final ModConfigSpec.BooleanValue ENABLE_SHADER;
    public static final ModConfigSpec.IntValue SHADER_STEPS;
    public static final ModConfigSpec.BooleanValue HEAVY_FOG;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.push("tempestades");
        NATURAL_STORMS = common
                .comment("Supercelulas e tornados surgem naturalmente quando chove/troveja.")
                .define("tempestadesNaturais", true);
        STORM_FREQUENCY = common
                .comment("Multiplicador da frequencia de tempestades naturais (0 = nunca, 1 = normal).")
                .defineInRange("frequencia", 1.0D, 0.0D, 10.0D);
        MAX_SUPERCELLS = common
                .comment("Maximo de supercelulas simultaneas.")
                .defineInRange("maxSupercelulas", 2, 1, 8);
        BLOCK_DAMAGE = common
                .comment("Tornados destroem blocos (depende da intensidade EF).")
                .define("tornadoDestroiBlocos", true);
        MAX_DEBRIS = common
                .comment("Maximo de blocos voando (detritos) por tornado, para evitar lag.")
                .defineInRange("maxDetritos", 40, 0, 400);
        LIGHTNING = common
                .comment("Supercelulas lancam raios perto dos jogadores.")
                .define("raios", true);
        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("graficos");
        ENABLE_SHADER = client
                .comment("Liga o ray marching de tornados/nuvens. Desligue se tiver problemas (ou use shaders do Iris).")
                .define("rayMarching", true);
        SHADER_STEPS = client
                .comment("Qualidade do ray marching (passos por pixel). 24 = rapido, 48 = normal, 96 = bonito.")
                .defineInRange("passos", 48, 16, 120);
        HEAVY_FOG = client
                .comment("Neblina densa durante chuva forte / perto de tempestades.")
                .define("neblinaTempestade", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }
}
