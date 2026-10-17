package com.meteorologia.item;

import com.meteorologia.block.SensorBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Medidor portatil: vento, pressao, temperatura e umidade onde voce esta. */
public class HandheldMeterItem extends Item {
    public HandheldMeterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            player.displayClientMessage(Component.literal(SensorBlock.fullReading(sl, player.blockPosition())), false);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
