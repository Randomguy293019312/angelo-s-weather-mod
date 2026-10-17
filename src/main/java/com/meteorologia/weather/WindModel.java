package com.meteorologia.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Modelo simples (mas coerente) de vento, pressao, temperatura e umidade. */
public final class WindModel {
    private WindModel() {}

    /** Velocidade maxima do vento por nivel EF (km/h). */
    public static final double[] EF_KMH = {110, 150, 190, 240, 300, 390};

    /** Vento (para onde sopra) em km/h no ponto dado: {x, z}. */
    public static double[] wind(StormWorld w, long gameTime, double x, double y, double z) {
        double ang = 2.2 + 0.7 * Math.sin(gameTime / 7000.0) + (y - 63.0) * 0.004;
        double spd = (10.0 + 8.0 * Math.sin(gameTime / 3100.0 + 1.3)) * (1.0 + Math.max(0.0, y - 63.0) * 0.005);
        double wx = Math.cos(ang) * spd;
        double wz = Math.sin(ang) * spd;
        if (w == null) {
            return new double[] {wx, wz};
        }

        // entrada de ar (inflow) na direcao das supercelulas
        for (Supercell s : w.supercells) {
            double dx = s.x - x;
            double dz = s.z - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            double lim = s.radius * 1.8;
            if (d < lim && d > 1.0) {
                double f = 1.0 - d / lim;
                double in = 28.0 * s.strength * f * f;
                wx += dx / d * in;
                wz += dz / d * in;
            }
        }

        // vortice de Rankine para cada tornado (anti-horario visto de cima)
        for (Tornado t : w.tornadoes) {
            if (y < t.y - 15.0 || y > t.y + t.height + 30.0) {
                continue;
            }
            double dx = x - t.x;
            double dz = z - t.z;
            double r = Math.sqrt(dx * dx + dz * dz);
            double core = Math.max(4.0, t.radius * (0.6 + 0.4 * t.strength) * 1.3);
            double lim = core * 9.0;
            if (r > lim || r < 0.01) {
                continue;
            }
            double v = EF_KMH[Math.max(0, Math.min(5, t.ef))] * t.strength;
            double prof = r < core ? r / core : Math.pow(core / r, 0.85);
            double cut = r > lim * 0.6 ? 1.0 - (r - lim * 0.6) / (lim * 0.4) : 1.0;
            v *= prof * cut;
            double tx = dz / r;
            double tz = -dx / r;
            double ix = -dx / r;
            double iz = -dz / r;
            wx += v * (tx * 0.88 + ix * 0.47);
            wz += v * (tz * 0.88 + iz * 0.47);
        }
        return new double[] {wx, wz};
    }

    public static double speed(double[] wind) {
        return Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1]);
    }

    /** "23 km/h (de SO)" */
    public static String describeWind(double[] wind) {
        double s = speed(wind);
        if (s < 1.0) {
            return "calmo";
        }
        return String.format("%d km/h (de %s)", (int) Math.round(s), Compass.direction(-wind[0], -wind[1]));
    }

    public static double pressureHpa(Level level, StormWorld w, BlockPos pos) {
        double p = 1013.25 - (pos.getY() - 63.0) * 0.12;
        if (level.isRaining()) {
            p -= 6.0;
        }
        if (level.isThundering()) {
            p -= 8.0;
        }
        if (w != null) {
            for (Supercell s : w.supercells) {
                double dx = pos.getX() - s.x;
                double dz = pos.getZ() - s.z;
                double q = (dx * dx + dz * dz) / (s.radius * s.radius * 2.25);
                p -= 12.0 * s.strength * Math.exp(-q);
            }
            for (Tornado t : w.tornadoes) {
                double dx = pos.getX() - t.x;
                double dz = pos.getZ() - t.z;
                double core = Math.max(6.0, t.radius * 3.0);
                double q = (dx * dx + dz * dz) / (core * core);
                p -= (t.ef + 1) * 7.0 * t.strength * Math.exp(-q);
            }
        }
        return p;
    }

    public static double temperatureC(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        double base = biome.getBaseTemperature();
        double t = base <= 0.15 ? -8.0 + base * 50.0 : base * 24.0 - 0.5;
        t = Math.min(t, 44.0);
        double dayFrac = (level.getDayTime() % 24000L) / 24000.0;
        t += 4.0 * Math.cos((dayFrac - 0.33) * Math.PI * 2.0);
        t -= Math.max(0, pos.getY() - 63) * 0.01;
        if (level.isRaining()) {
            t -= 3.0;
        }
        if (level.isThundering()) {
            t -= 2.0;
        }
        return t;
    }

    public static double humidityPct(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        double h = 45.0 + (biome.hasPrecipitation() ? 10.0 : -20.0);
        double dayFrac = (level.getDayTime() % 24000L) / 24000.0;
        h -= 8.0 * Math.cos((dayFrac - 0.33) * Math.PI * 2.0);
        if (level.isRaining()) {
            h += 30.0;
        }
        if (level.isThundering()) {
            h += 10.0;
        }
        return Math.max(8.0, Math.min(100.0, h));
    }
}
