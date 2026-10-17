package com.meteorologia.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pluviometro: acumula chuva (0..7). Clique para ler; agachado + clique esvazia. */
public class RainGaugeBlock extends Block {
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 7);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public RainGaugeBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int fill = state.getValue(FILL);
        if (fill < 7 && level.isRainingAt(pos.above()) && random.nextInt(3) == 0) {
            level.setBlock(pos, state.setValue(FILL, fill + 1), 3);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(FILL) * 2;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        int fill = state.getValue(FILL);
        player.displayClientMessage(Component.literal("§bPluviometro: §f" + (fill * 12) + " mm acumulados"), true);
        if (player.isShiftKeyDown() && fill > 0) {
            level.setBlock(pos, state.setValue(FILL, 0), 3);
        }
        return InteractionResult.sidedSuccess(false);
    }
}
