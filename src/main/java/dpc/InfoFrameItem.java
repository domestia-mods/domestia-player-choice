package dpc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class InfoFrameItem extends Item {
    public InfoFrameItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();

        if (clickedFace == Direction.DOWN) {
            return InteractionResult.FAIL;
        }

        boolean wallMounted = clickedFace.getAxis().isHorizontal();
        BlockPos placePos = context.getClickedPos().relative(clickedFace);

        if (!level.getBlockState(placePos).isAir()) {
            return InteractionResult.FAIL;
        }

        Direction facing = wallMounted
                ? clickedFace
                : context.getHorizontalDirection().getOpposite();

        if (!hasSupport(level, placePos, facing, wallMounted)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            InfoFrameBlock block = wallMounted
                    ? DpcBlocks.INFO_WALL_FRAME
                    : DpcBlocks.INFO_FRAME;

            BlockState state = block.defaultBlockState().setValue(InfoFrameBlock.FACING, facing);
            level.setBlock(placePos, state, 3);

            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean hasSupport(Level level, BlockPos placePos, Direction facing, boolean wallMounted) {
        BlockPos supportPos = wallMounted
                ? placePos.relative(facing.getOpposite())
                : placePos.below();
        BlockState supportState = level.getBlockState(supportPos);
        return !supportState.isAir() && !supportState.getCollisionShape(level, supportPos).isEmpty();
    }
}
