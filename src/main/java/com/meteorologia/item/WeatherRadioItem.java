package com.meteorologia.item;

import com.meteorologia.weather.StormManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Radio: clique direito = previsao; na hotbar avisa sozinho de tornados proximos. */
public class WeatherRadioItem extends Item {
    public WeatherRadioItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            player.displayClientMessage(Component.literal("§b== Radio Meteorologico =="), false);
            for (String line : StormManager.report(sl, player.getX(), player.getZ())) {
                player.displayClientMessage(Component.literal(line), false);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (slot < 0 || slot > 8 || sl.getGameTime() % 100L != 0L) {
            return;
        }
        int lvl = StormManager.warningLevel(sl, player.getX(), player.getZ());
        if (lvl >= 2) {
            player.displayClientMessage(Component.literal("§4§l[ALERTA DE TORNADO] Procure abrigo subterraneo!"), true);
            sl.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 0.6F);
        } else if (lvl == 1) {
            player.displayClientMessage(Component.literal("§6[!] Tempestade severa nas proximidades."), true);
        }
    }
}
