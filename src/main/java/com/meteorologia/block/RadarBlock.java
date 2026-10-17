package com.meteorologia.block;

import com.meteorologia.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Radar meteorologico: clique direito abre a tela do radar. */
public class RadarBlock extends Block {
    public RadarBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (level.isClientSide) {
            ClientHooks.openRadar(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
