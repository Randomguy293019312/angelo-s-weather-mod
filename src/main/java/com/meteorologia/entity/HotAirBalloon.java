package com.meteorologia.entity;

import com.meteorologia.ModRegistry;
import com.meteorologia.weather.StormManager;
import com.meteorologia.weather.WindModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Balao de ar quente. W = acender o queimador (sobe), S = ventilar (desce).
 * O balao derrapa com o vento da altitude em que estiver: o vento gira com a
 * altura, entao voce "dirige" subindo e descendo. Tornados puxam o balao!
 */
public class HotAirBalloon extends Entity {
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BURNING =
            SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.BOOLEAN);

    public static final int MAX_FUEL = 3600;          // 3 minutos de queimador
    private static final double WIND_TO_SPEED = 0.0035; // km/h -> blocos/tick

    public HotAirBalloon(EntityType<? extends HotAirBalloon> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_BURNING, false);
    }

    public int getFuel() {
        return entityData.get(DATA_FUEL);
    }

    public void setFuel(int fuel) {
        entityData.set(DATA_FUEL, Math.max(0, Math.min(MAX_FUEL, fuel)));
    }

    public boolean isBurning() {
        return entityData.get(DATA_BURNING);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getInt("Fuel"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuel", getFuel());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        double off = 0.0;
        if (getPassengers().size() > 1) {
            off = getPassengers().indexOf(passenger) == 0 ? -0.45 : 0.45;
        }
        callback.accept(passenger, getX() + off, getY() + 0.25, getZ());
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModRegistry.BALLOON_ITEM.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModRegistry.PROPANE_TANK.get())) {
            if (!level().isClientSide && getFuel() < MAX_FUEL) {
                setFuel(getFuel() + 1800);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (getPassengers().size() < 2) {
            if (!level().isClientSide) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && !isRemoved()) {
            boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
            if (!creative) {
                spawnAtLocation(ModRegistry.BALLOON_ITEM.get());
            }
            discard();
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientEffects();
        } else {
            serverTick();
        }
    }

    private void clientEffects() {
        if (isBurning()) {
            level().addParticle(ParticleTypes.FLAME,
                    getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 1.9, getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.04, 0.0);
            if (tickCount % 20 == 0) {
                level().playLocalSound(getX(), getY() + 2.0, getZ(), SoundEvents.FIRE_AMBIENT,
                        SoundSource.AMBIENT, 0.8F, 0.8F, false);
            }
        }
    }

    private void serverTick() {
        ServerLevel sl = (ServerLevel) level();

        boolean burning = false;
        boolean vent = false;
        if (!getPassengers().isEmpty() && getPassengers().get(0) instanceof LivingEntity rider) {
            burning = rider.zza > 0.1F && getFuel() > 0;
            vent = rider.zza < -0.1F;
        }
        if (burning) {
            setFuel(getFuel() - 1);
        }
        if (burning != isBurning()) {
            entityData.set(DATA_BURNING, burning);
        }

        Vec3 v = getDeltaMovement();

        // vertical: queimador sobe, valvula desce, senao esfria devagar
        double targetY = burning ? 0.14 : vent ? -0.16 : -0.015;
        double vy = v.y + (targetY - v.y) * 0.08;
        if (getY() > sl.getMaxBuildHeight() - 4) {
            vy = Math.min(vy, -0.05);
        }

        // horizontal: acompanha o vento da altitude atual
        double[] wind = WindModel.wind(StormManager.get(sl), sl.getGameTime(), getX(), getY(), getZ());
        double vx = v.x + (wind[0] * WIND_TO_SPEED - v.x) * 0.03;
        double vz = v.z + (wind[1] * WIND_TO_SPEED - v.z) * 0.03;

        setDeltaMovement(vx, vy, vz);
        move(MoverType.SELF, getDeltaMovement());

        Vec3 after = getDeltaMovement();
        if (horizontalCollision) {
            after = new Vec3(0.0, after.y, 0.0);
        }
        if (verticalCollision) {
            after = new Vec3(after.x, 0.0, after.z);
        }
        setDeltaMovement(after);
    }
}
