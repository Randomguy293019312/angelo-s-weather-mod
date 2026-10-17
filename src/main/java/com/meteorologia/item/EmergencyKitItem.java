package com.meteorologia.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Kit de emergencia: protecao temporaria contra detritos e quedas. */
public class EmergencyKitItem extends Item {
    public EmergencyKitItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2400, 0));
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
