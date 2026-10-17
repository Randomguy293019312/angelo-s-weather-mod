package com.meteorologia.weather;

import com.meteorologia.Config;
import com.meteorologia.net.StormSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Logica do servidor: nascimento, movimento, efeitos e sincronizacao das tempestades. */
public final class StormManager {
    private StormManager() {}

    public static final String DEBRIS_TAG = "meteorologia_debris";

    private static final Map<ResourceKey<Level>, StormWorld> WORLDS = new HashMap<>();

    /** Aceleracao aplicada a entidades (blocos/tick^2) por nivel EF. */
    private static final double[] EF_POWER = {0.018, 0.030, 0.050, 0.075, 0.105, 0.140};
    /** Resistencia a explosao maxima dos blocos que o tornado consegue arrancar. */
    private static final float[] EF_BLOCK_RESISTANCE = {0.35F, 0.8F, 2.5F, 4.5F, 8.0F, 40.0F};
    /** Raio do funil no chao (blocos). */
    private static final float[] EF_RADIUS = {4F, 6F, 9F, 14F, 20F, 28F};

    public static StormWorld get(Level level) {
        return WORLDS.computeIfAbsent(level.dimension(), k -> new StormWorld());
    }

    public static void clearAllWorlds() {
        WORLDS.clear();
    }

    // =====================================================================
    //  Tick principal
    // =====================================================================
    public static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        StormWorld w = get(level);
        long time = level.getGameTime();

        if (time % 100L == 0L) {
            naturalSpawn(level, w, players);
        }

        for (Iterator<Supercell> it = w.supercells.iterator(); it.hasNext(); ) {
            if (!tickSupercell(level, w, it.next(), players)) {
                it.remove();
            }
        }
        for (Iterator<Tornado> it = w.tornadoes.iterator(); it.hasNext(); ) {
            if (!tickTornado(level, w, it.next(), players)) {
                it.remove();
            }
        }

        if (time % 20L == 0L && w.rainIntensity > 0F && !level.isRaining()) {
            w.rainIntensity = Math.max(0F, w.rainIntensity - 0.05F);
        }
        sync(level, w, players, time);
    }

    // =====================================================================
    //  Nascimento
    // =====================================================================
    private static void naturalSpawn(ServerLevel level, StormWorld w, List<ServerPlayer> players) {
        if (!Config.NATURAL_STORMS.get() || w.supercells.size() >= Config.MAX_SUPERCELLS.get()) {
            return;
        }
        double p = level.isThundering() ? 0.05 : level.isRaining() ? 0.02 : 0.002;
        p *= (0.6 + w.rainIntensity) * Config.STORM_FREQUENCY.get();
        if (level.random.nextDouble() >= p) {
            return;
        }
        ServerPlayer target = players.get(level.random.nextInt(players.size()));
        spawnApproaching(level, w, target.getX(), target.getZ(),
                1500 + level.random.nextInt(1200), randomPotential(level.random));
    }

    private static int randomPotential(RandomSource r) {
        float f = r.nextFloat();
        if (f < 0.35F) return -1;   // sem tornado
        if (f < 0.55F) return 0;
        if (f < 0.73F) return 1;
        if (f < 0.85F) return 2;
        if (f < 0.93F) return 3;
        if (f < 0.98F) return 4;
        return 5;
    }

    private static Supercell newSupercell(StormWorld w, RandomSource r, double x, double z, double vx, double vz) {
        Supercell s = new Supercell();
        s.id = w.nextId++;
        s.x = x;
        s.z = z;
        s.vx = vx;
        s.vz = vz;
        s.radius = 260F + r.nextFloat() * 220F;
        s.baseY = 170F + r.nextFloat() * 30F;
        s.topY = s.baseY + 650F + r.nextFloat() * 300F;
        s.windAngle = (float) Math.atan2(vz, vx);
        return s;
    }

    /** Supercelula que nasce longe e se aproxima do ponto (px, pz). */
    public static Supercell spawnApproaching(ServerLevel level, StormWorld w, double px, double pz,
                                             double dist, int maxEf) {
        RandomSource r = level.random;
        double ang = r.nextDouble() * Math.PI * 2.0;
        double sx = px + Math.cos(ang) * dist;
        double sz = pz + Math.sin(ang) * dist;
        double aim = (r.nextDouble() - 0.5) * dist * 0.4;
        double tx = px - Math.sin(ang) * aim;
        double tz = pz + Math.cos(ang) * aim;
        double speed = 0.25 + r.nextDouble() * 0.2;
        double dx = tx - sx;
        double dz = tz - sz;
        double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
        Supercell s = newSupercell(w, r, sx, sz, dx / len * speed, dz / len * speed);
        s.maxEf = maxEf;
        s.lifetime = (int) (dist / speed) + 5000 + r.nextInt(4000);
        w.supercells.add(s);
        return s;
    }

    /** Cria um tornado (com sua supercelula-mae) exatamente em (x, z). */
    public static Tornado spawnTornado(ServerLevel level, double x, double z, int ef) {
        StormWorld w = get(level);
        RandomSource r = level.random;
        double heading = r.nextDouble() * Math.PI * 2.0;
        double speed = 0.18 + r.nextDouble() * 0.12;
        Supercell s = newSupercell(w, r, x, z, Math.cos(heading) * speed, Math.sin(heading) * speed);
        double offAng = r.nextDouble() * Math.PI * 2.0;
        double offDist = s.radius * 0.28;
        s.x = x - Math.cos(offAng) * offDist;
        s.z = z - Math.sin(offAng) * offDist;
        s.age = 600;
        s.lifetime = 600 + 4800 + ef * 600;
        s.maxEf = ef;
        s.hasTornado = true;
        w.supercells.add(s);
        Tornado t = spawnTornadoFor(level, w, s, ef, 160, offAng, offDist);
        t.lifetime = 4200 + ef * 600;
        return t;
    }

    private static Tornado spawnTornadoFor(ServerLevel level, StormWorld w, Supercell s, int ef,
                                           int formTicks, double offAng, double offDist) {
        RandomSource r = level.random;
        Tornado t = new Tornado();
        t.id = w.nextId++;
        t.parentId = s.id;
        t.ef = Math.max(0, Math.min(5, ef));
        t.radius = EF_RADIUS[t.ef] * (0.85F + r.nextFloat() * 0.3F);
        t.formTicks = formTicks;
        t.lifetime = 2400 + t.ef * 900 + r.nextInt(2400);
        t.offAngle = offAng;
        t.offDist = offDist;
        t.x = s.x + Math.cos(offAng) * offDist;
        t.z = s.z + Math.sin(offAng) * offDist;
        t.y = groundY(level, t.x, t.z, level.getSeaLevel());
        t.height = Math.max(60F, s.baseY - (float) t.y);
        s.hasTornado = true;
        w.tornadoes.add(t);

        for (ServerPlayer p : level.players()) {
            double dx = t.x - p.getX();
            double dz = t.z - p.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 3500.0) {
                p.displayClientMessage(Component.literal("§4§l[ALERTA] Tornado EF" + t.ef + " se formando a "
                        + Compass.distance(d) + " ao " + Compass.direction(dx, dz) + "! Procure abrigo subterraneo."), false);
            }
        }
        return t;
    }

    private static double groundY(ServerLevel level, double x, double z, double fallback) {
        BlockPos bp = BlockPos.containing(x, 64, z);
        if (level.hasChunkAt(bp)) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp.getX(), bp.getZ());
        }
        return fallback;
    }

    // =====================================================================
    //  Supercelula
    // =====================================================================
    private static boolean tickSupercell(ServerLevel level, StormWorld w, Supercell s, List<ServerPlayer> players) {
        s.age++;
        float prev = s.strength;
        float grow = Math.min(1F, s.age / 1200F);
        float decay = s.age > s.lifetime - 1200 ? Math.max(0F, (s.lifetime - s.age) / 1200F) : 1F;
        s.strength = grow * decay;
        s.dStrength = s.strength - prev;
        s.x += s.vx;
        s.z += s.vz;
        if (s.age >= s.lifetime) {
            return false;
        }

        // genese do tornado
        if (!s.hasTornado && s.maxEf >= 0 && s.age > 1800 && s.age < s.lifetime - 3600
                && level.random.nextFloat() < 0.0025F && nearAnyPlayer(players, s.x, s.z, 3000.0)) {
            double offAng = level.random.nextDouble() * Math.PI * 2.0;
            spawnTornadoFor(level, w, s, s.maxEf, 600, offAng, s.radius * 0.28);
        }

        // chuva/trovoada globais quando a tempestade esta em cima de alguem
        if (s.age % 40 == 0 && s.strength > 0.3F && !level.isThundering()
                && nearAnyPlayer(players, s.x, s.z, s.radius * 1.2)) {
            level.setWeatherParameters(0, 6000, true, true);
        }

        if (s.age % 20 == 0 && s.strength > 0.5F && Config.LIGHTNING.get()) {
            lightning(level, s, players);
        }
        return true;
    }

    private static void lightning(ServerLevel level, Supercell s, List<ServerPlayer> players) {
        RandomSource r = level.random;
        double reach = s.radius * 1.3;
        for (ServerPlayer p : players) {
            double dx = p.getX() - s.x;
            double dz = p.getZ() - s.z;
            if (dx * dx + dz * dz > reach * reach || r.nextFloat() > 0.10F * s.strength) {
                continue;
            }
            double ang = r.nextDouble() * Math.PI * 2.0;
            double d = 25.0 + r.nextDouble() * 70.0;
            BlockPos col = BlockPos.containing(p.getX() + Math.cos(ang) * d, 64, p.getZ() + Math.sin(ang) * d);
            if (!level.hasChunkAt(col)) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(top));
                level.addFreshEntity(bolt);
            }
        }
    }

    // =====================================================================
    //  Tornado
    // =====================================================================
    private static boolean tickTornado(ServerLevel level, StormWorld w, Tornado t, List<ServerPlayer> players) {
        Supercell parent = w.findSupercell(t.parentId);
        final int decayTicks = 600;
        t.age++;
        if (parent == null && t.lifetime > t.age + decayTicks) {
            t.lifetime = t.age + decayTicks;
        }
        float form = Math.min(1F, t.age / (float) Math.max(1, t.formTicks));
        float decay = t.age > t.lifetime - decayTicks ? Math.max(0F, (t.lifetime - t.age) / (float) decayTicks) : 1F;
        float prev = t.strength;
        t.strength = form * decay;
        t.dStrength = t.strength - prev;
        if (t.age >= t.lifetime) {
            return false;
        }

        // movimento: acompanha a supercelula, com meandro
        if (parent != null) {
            t.offAngle += (level.random.nextDouble() - 0.5) * 0.01;
            double nx = parent.x + Math.cos(t.offAngle) * t.offDist;
            double nz = parent.z + Math.sin(t.offAngle) * t.offDist;
            t.vx = nx - t.x;
            t.vz = nz - t.z;
            t.x = nx;
            t.z = nz;
        } else {
            t.x += t.vx;
            t.z += t.vz;
        }
        BlockPos bp = BlockPos.containing(t.x, 64, t.z);
        if (level.hasChunkAt(bp)) {
            double gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp.getX(), bp.getZ());
            t.y += (gy - t.y) * 0.15;
        }
        float baseY = parent != null ? parent.baseY : (float) t.y + 140F;
        t.height = Math.max(60F, baseY - (float) t.y);

        if (t.strength > 0.08F && nearAnyPlayer(players, t.x, t.z, 400.0)) {
            affectEntities(level, t);
            if (Config.BLOCK_DAMAGE.get()) {
                damageBlocks(level, t);
            }
        }
        return true;
    }

    private static double effRadius(Tornado t) {
        return t.radius * (0.55 + 0.45 * t.strength);
    }

    private static void affectEntities(ServerLevel level, Tornado t) {
        double radius = effRadius(t);
        double reach = radius * 4.0 + 20.0;
        AABB box = new AABB(t.x - reach, t.y - 8.0, t.z - reach, t.x + reach, t.y + t.height + 10.0, t.z + reach);
        List<Entity> list = level.getEntitiesOfClass(Entity.class, box,
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.getAbilities().flying));
        long time = level.getGameTime();

        for (Entity e : list) {
            double dx = e.getX() - t.x;
            double dz = e.getZ() - t.z;
            double r = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            if (r > reach) {
                continue;
            }
            // abrigo subterraneo protege
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(e.getX()), (int) Math.floor(e.getZ()));
            if (e.getY() < surface - 4) {
                continue;
            }
            double fall = r < radius * 1.6 ? 1.0 : Math.max(0.0, 1.0 - (r - radius * 1.6) / (reach - radius * 1.6));
            double p = EF_POWER[t.ef] * t.strength * fall;
            if (e instanceof Player pl && pl.isShiftKeyDown()) {
                p *= 0.6;
            }
            double nx = dx / r;
            double nz = dz / r;
            double ax = (nz * 0.9 - nx * 0.7) * p;
            double az = (-nx * 0.9 - nz * 0.7) * p;
            double ay = (r < radius * 2.0 ? 1.15 : 0.15) * p;

            Vec3 v = e.getDeltaMovement().add(ax, ay, az);
            double hx = v.x;
            double hz = v.z;
            double hLen = Math.sqrt(hx * hx + hz * hz);
            if (hLen > 1.6) {
                hx = hx / hLen * 1.6;
                hz = hz / hLen * 1.6;
            }
            e.setDeltaMovement(hx, Math.min(v.y, 0.85), hz);
            e.hurtMarked = true;

            if (t.ef >= 1 && r < radius * 1.15 && time % 10L == 0L && e instanceof LivingEntity le) {
                le.hurt(level.damageSources().generic(), 1.0F + t.ef * 1.2F * t.strength);
            }
        }
    }

    private static void damageBlocks(ServerLevel level, Tornado t) {
        RandomSource rnd = level.random;
        double radius = effRadius(t);
        float maxRes = EF_BLOCK_RESISTANCE[t.ef];

        double samplesD = (1 + t.ef * 2) * t.strength;
        int samples = (int) samplesD;
        if (rnd.nextDouble() < samplesD - samples) {
            samples++;
        }
        if (samples <= 0) {
            return;
        }

        AABB area = new AABB(t.x - radius * 2.5, t.y - 8.0, t.z - radius * 2.5,
                t.x + radius * 2.5, t.y + t.height, t.z + radius * 2.5);
        int debris = Config.MAX_DEBRIS.get() <= 0 ? Integer.MAX_VALUE
                : level.getEntitiesOfClass(FallingBlockEntity.class, area, e -> e.getTags().contains(DEBRIS_TAG)).size();

        for (int i = 0; i < samples; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double rr = radius * (0.3 + rnd.nextDouble() * 1.4);
            int bx = (int) Math.floor(t.x + Math.cos(ang) * rr);
            int bz = (int) Math.floor(t.z + Math.sin(ang) * rr);
            if (!level.hasChunkAt(new BlockPos(bx, 64, bz))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) - 1;
            int by = top - rnd.nextInt(1 + t.ef / 2);
            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState st = level.getBlockState(pos);

            if (st.isAir() || !st.getFluidState().isEmpty()) {
                continue;
            }
            if (st.hasBlockEntity() && t.ef < 4) {
                continue;
            }
            if (st.getDestroySpeed(level, pos) < 0F) {
                continue;
            }
            if (st.getBlock().getExplosionResistance() > maxRes) {
                continue;
            }

            if (rnd.nextFloat() < 0.35F && debris < Config.MAX_DEBRIS.get() && !st.hasBlockEntity()) {
                FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, st);
                fb.addTag(DEBRIS_TAG);
                double tx = (bz - t.z);
                double tz = -(bx - t.x);
                double len = Math.max(1.0, Math.sqrt(tx * tx + tz * tz));
                double sp = 0.25 + 0.1 * t.ef;
                fb.setDeltaMovement(tx / len * sp, 0.35 + rnd.nextDouble() * 0.3, tz / len * sp);
                fb.hurtMarked = true;
                debris++;
            } else {
                level.removeBlock(pos, false);
            }
            level.sendParticles(ParticleTypes.POOF, bx + 0.5, by + 0.5, bz + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    // =====================================================================
    //  Utilidades
    // =====================================================================
    private static boolean nearAnyPlayer(List<ServerPlayer> players, double x, double z, double range) {
        double r2 = range * range;
        for (ServerPlayer p : players) {
            double dx = p.getX() - x;
            double dz = p.getZ() - z;
            if (dx * dx + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    /** 0 = sem perigo, 1 = tempestade severa por perto, 2 = tornado proximo. */
    public static int warningLevel(ServerLevel level, double x, double z) {
        StormWorld w = get(level);
        int lvl = 0;
        for (Tornado t : w.tornadoes) {
            if (t.strength < 0.15F) {
                continue;
            }
            double d = Math.hypot(t.x - x, t.z - z);
            if (d < 1500.0) {
                lvl = 2;
            } else if (d < 3000.0) {
                lvl = Math.max(lvl, 1);
            }
        }
        for (Supercell s : w.supercells) {
            if (s.maxEf >= 0 && s.strength > 0.3F && Math.hypot(s.x - x, s.z - z) < s.radius * 3.0) {
                lvl = Math.max(lvl, 1);
            }
        }
        return lvl;
    }

    /** Linhas de relatorio (usadas pelo radio e pelo comando /meteo info). */
    public static List<String> report(ServerLevel level, double px, double pz) {
        StormWorld w = get(level);
        List<String> out = new ArrayList<>();
        for (Tornado t : w.tornadoes) {
            out.add("§c[!] Tornado EF" + t.ef + " §7" + where(t.x - px, t.z - pz, t.vx, t.vz));
        }
        for (Supercell s : w.supercells) {
            String pot = s.maxEf >= 0 ? " (risco de tornado)" : "";
            out.add("§6Supercelula" + pot + " §7" + where(s.x - px, s.z - pz, s.vx, s.vz));
        }
        if (out.isEmpty()) {
            out.add("§aNenhuma tempestade severa detectada.");
        }
        if (w.rainIntensity >= 0.9F) {
            out.add("§9Chuva forte em andamento.");
        } else if (w.rainIntensity >= 0.6F) {
            out.add("§9Chuva moderada em andamento.");
        } else if (level.isRaining()) {
            out.add("§9Chuva.");
        }
        return out;
    }

    private static String where(double dx, double dz, double vx, double vz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        double speedKmh = Math.hypot(vx, vz) * 20.0 * 3.6;
        StringBuilder sb = new StringBuilder();
        sb.append("a ").append(Compass.distance(d)).append(" ao ").append(Compass.direction(dx, dz));
        if (speedKmh > 1.0) {
            sb.append(", rumo ").append(Compass.direction(vx, vz)).append(" a ").append((int) speedKmh).append(" km/h");
            double closing = -(vx * dx + vz * dz) / Math.max(1.0, d);
            if (closing > 0.01) {
                int secs = (int) (d / (closing * 20.0));
                sb.append(", chega em ~").append(Math.max(1, secs / 60)).append(" min");
            }
        }
        return sb.toString();
    }

    // =====================================================================
    //  Comandos auxiliares
    // =====================================================================
    public static void setRain(ServerLevel level, float intensity, boolean thunder) {
        StormWorld w = get(level);
        w.rainIntensity = intensity;
        if (intensity <= 0F) {
            level.setWeatherParameters(12000, 0, false, false);
        } else {
            level.setWeatherParameters(0, 12000, true, thunder);
        }
    }

    public static void clearAll(ServerLevel level) {
        StormWorld w = get(level);
        w.supercells.clear();
        w.tornadoes.clear();
        w.rainIntensity = 0F;
    }

    // =====================================================================
    //  Sincronizacao com os clientes
    // =====================================================================
    private static void sync(ServerLevel level, StormWorld w, List<ServerPlayer> players, long time) {
        boolean any = !w.supercells.isEmpty() || !w.tornadoes.isEmpty() || w.rainIntensity > 0F;
        boolean send = any ? time % 10L == 0L : (!w.sentEmpty || time % 200L == 0L);
        if (!send) {
            return;
        }
        StormSyncPayload payload = new StormSyncPayload(w.rainIntensity,
                new ArrayList<>(w.supercells), new ArrayList<>(w.tornadoes));
        for (ServerPlayer p : players) {
            PacketDistributor.sendToPlayer(p, payload);
        }
        w.sentEmpty = !any;
    }
}
