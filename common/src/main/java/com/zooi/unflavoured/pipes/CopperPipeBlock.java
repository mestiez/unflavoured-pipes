package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopperPipeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty JOINT = BooleanProperty.create("joint");

    public CopperPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(getStateDefinition().any().setValue(ENABLED, true).setValue(WATERLOGGED, false).setValue(JOINT, true).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public BooleanProperty getConnection(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var fluidState = context.getLevel().getFluidState(pos);
        var state = this.defaultBlockState();
        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            var neighborState = world.getBlockState(neighborPos);
            var connected = this.canConnectTo(neighborState, neighborPos, world);
            state = state.setValue(getConnection(direction), connected);
        }
        var straight = isStraight(state);
        if (straight) {
            if (state.getValue(SOUTH) && !state.getValue(NORTH)) state = state.setValue(NORTH, true);
            if (state.getValue(NORTH) && !state.getValue(SOUTH)) state = state.setValue(SOUTH, true);

            if (state.getValue(WEST) && !state.getValue(EAST)) state = state.setValue(EAST, true);
            if (state.getValue(EAST) && !state.getValue(WEST)) state = state.setValue(WEST, true);

            if (state.getValue(UP) && !state.getValue(DOWN)) state = state.setValue(DOWN, true);
            if (state.getValue(DOWN) && !state.getValue(UP)) state = state.setValue(UP, true);
        }
        state = state.setValue(JOINT, !straight).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (world.isClientSide) return;

        var stateChanged = false;

        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            var neighborState = world.getBlockState(neighborPos);
            var connected = this.canConnectTo(neighborState, neighborPos, world);
            var connectionProperty = getConnection(direction);

            if (state.getValue(connectionProperty) != connected) {
                state = state.setValue(connectionProperty, connected);
                stateChanged = true;
            }
        }

        if (stateChanged) {
            var straight = isStraight(state);

            if (straight) {
                if (state.getValue(SOUTH) && !state.getValue(NORTH)) state = state.setValue(NORTH, true);
                if (state.getValue(NORTH) && !state.getValue(SOUTH)) state = state.setValue(SOUTH, true);

                if (state.getValue(WEST) && !state.getValue(EAST)) state = state.setValue(EAST, true);
                if (state.getValue(EAST) && !state.getValue(WEST)) state = state.setValue(WEST, true);

                if (state.getValue(UP) && !state.getValue(DOWN)) state = state.setValue(DOWN, true);
                if (state.getValue(DOWN) && !state.getValue(UP)) state = state.setValue(UP, true);
            }

            state = state.setValue(JOINT, !straight);
            world.setBlock(pos, state, 2);
            this.updateNeighbors(world, pos, state);
        }

        super.neighborChanged(state, world, pos, blockIn, fromPos, isMoving);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }


    private void updateNeighbors(Level world, BlockPos pos, BlockState state) {
        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            var neighborState = world.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof CopperPipeBlock) {
                var opposite = direction.getOpposite();
                var neighborProperty = getConnection(opposite);
                var neighborConnected = this.canConnectTo(state, pos, world);

                if (neighborState.getValue(neighborProperty) != neighborConnected) {
                    var updatedNeighborState = neighborState.setValue(neighborProperty, neighborConnected);
                    world.setBlock(neighborPos, updatedNeighborState, 2);
                }
            }
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        this.updateNeighbors(world, pos, state);
    }

    public boolean isStraight(BlockState state) {
        var up = state.getValue(UP);
        var down = state.getValue(DOWN);
        var east = state.getValue(EAST);
        var west = state.getValue(WEST);
        var north = state.getValue(NORTH);
        var south = state.getValue(SOUTH);

        // only one side is active, we extend
        if (up && !(down || east || west || north || south)) return true;
        if (down && !(up || east || west || north || south)) return true;
        if (east && !(up || down || west || north || south)) return true;
        if (west && !(up || down || east || north || south)) return true;
        if (north && !(up || down || east || west || south)) return true;
        if (south && !(up || down || east || west || north)) return true;

        // at leat two sides are active, are the others not?
        if (up && down && !(east || west || north || south)) return true;

        if (east && west && !(up || down || north || south)) return true;

        if (north && south && !(up || down || east || west)) return true;

        return false;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED, WATERLOGGED, JOINT, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, UnflavouredPipesMod.ModBlockEntityType.COPPER_PIPE, CopperPipeBlockEntity::pushItemsTick);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return getInteractionShape(blockState, blockGetter, blockPos);
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState state, BlockGetter blockGetter, BlockPos blockPos) {
        var shape = Block.box(5, 5, 5, 11, 11, 11); // center block always present
        // TODO should cache
        if (state.getValue(NORTH)) shape = Shapes.or(shape, Block.box(5, 5, 0, 11, 11, 8));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, Block.box(5, 5, 8, 11, 11, 16));
        if (state.getValue(UP))    shape = Shapes.or(shape, Block.box(5, 8, 5, 11, 16, 11));
        if (state.getValue(DOWN))  shape = Shapes.or(shape, Block.box(5, 0, 5, 11, 8, 11));
        if (state.getValue(WEST))  shape = Shapes.or(shape, Block.box(0, 5, 5, 8, 11, 11));
        if (state.getValue(EAST))  shape = Shapes.or(shape, Block.box(8, 5, 5, 16, 11, 11));

        return shape;
    }

    @Override
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2) {
        if (blockState.getValue(WATERLOGGED)) {
            levelAccessor.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
        }

        return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
    }

    public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CopperPipeBlockEntity(blockPos, blockState);
    }

    private boolean canConnectTo(BlockState neighborState, BlockPos pos, Level world) {
        var b = neighborState.getBlock();
        if (neighborState.hasBlockEntity()) {
            var be = world.getBlockEntity(pos);
            if (be != null) {
                return be instanceof Container || be instanceof CopperPipeBlockEntity;
            }
        }
        return b instanceof CopperPipeBlock;
    }
}
