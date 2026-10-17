package com.meteorologia.block;

import com.meteorologia.weather.StormManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sirene: toca sozinha quando ha tornado por perto ou quando recebe redstone. */
public class StormSirenBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public StormSirenBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean alarm = level.hasNeighborSignal(pos)
                || StormManager.warningLevel(level, pos.getX(), pos.getZ()) >= 2;
        if (alarm) {
            float pitch = (level.getGameTime() / 40L) % 2L == 0L ? 0.5F : 0.65F;
            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 6.0F, pitch);
        }
        level.scheduleTick(pos, this, 40);
    }
}
