package dpc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class InfoStandItem extends Item {
	public InfoStandItem(Properties properties) {
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
		BlockPos lowerPos = context.getClickedPos().relative(clickedFace);
		BlockPos upperPos = lowerPos.above();

		if (!level.getBlockState(lowerPos).isAir() || !level.getBlockState(upperPos).isAir()) {
			return InteractionResult.FAIL;
		}

		Direction facing = wallMounted
				? clickedFace
				: context.getHorizontalDirection().getOpposite();

		if (!hasSupport(level, lowerPos, facing, wallMounted)) {
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide()) {
			InfoStandBlock block = wallMounted
					? DpcBlocks.INFO_WALL_STAND
					: DpcBlocks.INFO_STAND;

			BlockState lowerState = block.defaultBlockState()
					.setValue(InfoStandBlock.FACING, facing)
					.setValue(InfoStandBlock.HALF, DoubleBlockHalf.LOWER);
			BlockState upperState = block.defaultBlockState()
					.setValue(InfoStandBlock.FACING, facing)
					.setValue(InfoStandBlock.HALF, DoubleBlockHalf.UPPER);

			level.setBlock(lowerPos, lowerState, 3);
			level.setBlock(upperPos, upperState, 3);

			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
		}

		return InteractionResult.SUCCESS;
	}

	private static boolean hasSupport(Level level, BlockPos lowerPos, Direction facing, boolean wallMounted) {
		if (wallMounted) {
			BlockPos supportPos = lowerPos.relative(facing.getOpposite());
			return hasPhysicalSupport(level, supportPos);
		}

		BlockPos floorPos = lowerPos.below();
		return hasPhysicalSupport(level, floorPos);
	}

	private static boolean hasPhysicalSupport(Level level, BlockPos supportPos) {
		BlockState supportState = level.getBlockState(supportPos);
		return !supportState.isAir() && !supportState.getCollisionShape(level, supportPos).isEmpty();
	}
}
