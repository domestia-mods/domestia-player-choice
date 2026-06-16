package dpc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InfoFrameBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<InfoFrameBlock> CODEC = simpleCodec(properties -> new InfoFrameBlock(properties, false));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /*
     * The supplied models face WEST in their unrotated form. Angled pieces are
     * approximated with thin stair-step boxes so the outline/collision shape
     * follows the visible model instead of enclosing empty space.
     */
    private static final double[][] STAND_BOXES_WEST = new double[][] {
            {5.0D, 0.0D, 2.0D, 12.0D, 1.0D, 4.0D},
            {5.0D, 0.0D, 12.0D, 12.0D, 1.0D, 14.0D},

            {6.0D, 1.0D, 3.0D, 7.9D, 3.2D, 13.0D},
            {6.7D, 3.0D, 3.0D, 8.6D, 5.2D, 13.0D},
            {7.4D, 5.0D, 3.0D, 9.3D, 7.2D, 13.0D},
            {8.1D, 7.0D, 3.0D, 10.0D, 9.2D, 13.0D},
            {8.8D, 9.0D, 3.0D, 10.7D, 11.3D, 13.0D},

            {6.0D, 2.0D, 2.0D, 8.7D, 4.2D, 4.0D},
            {6.7D, 4.0D, 2.0D, 9.4D, 6.2D, 4.0D},
            {7.4D, 6.0D, 2.0D, 10.1D, 8.2D, 4.0D},
            {8.1D, 8.0D, 2.0D, 10.8D, 10.3D, 4.0D}
    };

    private static final double[][] WALL_BOXES_WEST = new double[][] {
            {15.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D},
            {14.0D, 4.0D, 2.0D, 16.0D, 12.0D, 4.0D}
    };

    private static final VoxelShape STAND_WEST = shapeForFacing(STAND_BOXES_WEST, Direction.WEST);
    private static final VoxelShape STAND_NORTH = shapeForFacing(STAND_BOXES_WEST, Direction.NORTH);
    private static final VoxelShape STAND_EAST = shapeForFacing(STAND_BOXES_WEST, Direction.EAST);
    private static final VoxelShape STAND_SOUTH = shapeForFacing(STAND_BOXES_WEST, Direction.SOUTH);

    private static final VoxelShape WALL_WEST = shapeForFacing(WALL_BOXES_WEST, Direction.WEST);
    private static final VoxelShape WALL_NORTH = shapeForFacing(WALL_BOXES_WEST, Direction.NORTH);
    private static final VoxelShape WALL_EAST = shapeForFacing(WALL_BOXES_WEST, Direction.EAST);
    private static final VoxelShape WALL_SOUTH = shapeForFacing(WALL_BOXES_WEST, Direction.SOUTH);

    private final boolean wallMounted;

    public InfoFrameBlock(BlockBehaviour.Properties properties, boolean wallMounted) {
        super(properties);
        this.wallMounted = wallMounted;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public boolean isWallMounted() {
        return this.wallMounted;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForState(state);
    }

    private static VoxelShape getShapeForState(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean wallMounted = state.getBlock() instanceof InfoFrameBlock frameBlock && frameBlock.isWallMounted();

        if (wallMounted) {
            return switch (facing) {
                case NORTH -> WALL_NORTH;
                case EAST -> WALL_EAST;
                case SOUTH -> WALL_SOUTH;
                default -> WALL_WEST;
            };
        }

        return switch (facing) {
            case NORTH -> STAND_NORTH;
            case EAST -> STAND_EAST;
            case SOUTH -> STAND_SOUTH;
            default -> STAND_WEST;
        };
    }

    private static VoxelShape shapeForFacing(double[][] boxes, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (double[] box : boxes) {
            shape = Shapes.or(shape, boxForFacingFromWest(box, facing));
        }
        return shape.optimize();
    }

    private static VoxelShape boxForFacingFromWest(double[] box, Direction facing) {
        double minX = box[0];
        double minY = box[1];
        double minZ = box[2];
        double maxX = box[3];
        double maxY = box[4];
        double maxZ = box[5];

        return switch (facing) {
            case NORTH -> Block.box(16.0D - maxZ, minY, minX, 16.0D - minZ, maxY, maxX);
            case EAST -> Block.box(16.0D - maxX, minY, 16.0D - maxZ, 16.0D - minX, maxY, 16.0D - minZ);
            case SOUTH -> Block.box(minZ, minY, 16.0D - maxX, maxZ, maxY, 16.0D - minX);
            default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        return this.handleInteraction(state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return this.handleInteraction(state, level, pos, player);
    }

    private InteractionResult handleInteraction(BlockState state, Level level, BlockPos pos, Player player) {
        if (!isPlayerOnFrontSide(pos, state.getValue(FACING), player)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            DpcScreenOpener.open();
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean isPlayerOnFrontSide(BlockPos pos, Direction facing, Player player) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * facing.getStepX() + dz * facing.getStepZ() > 0.0D;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            popResource(level, pos, new ItemStack(DpcItems.INFO_FRAME));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
