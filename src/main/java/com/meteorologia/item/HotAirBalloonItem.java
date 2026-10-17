package com.meteorologia.item;

import com.meteorologia.ModRegistry;
import com.meteorologia.entity.HotAirBalloon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class HotAirBalloonItem extends Item {
    public HotAirBalloonItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        HotAirBalloon balloon = ModRegistry.BALLOON.get().create(sl);
        if (balloon == null) {
            return InteractionResult.FAIL;
        }
        balloon.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation(), 0F);
        sl.addFreshEntity(balloon);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
