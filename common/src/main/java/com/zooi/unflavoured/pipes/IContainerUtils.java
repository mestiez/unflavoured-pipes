package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IContainerUtils {

    boolean transferFirstAvailableItem(Options options);

    boolean canPipeConnect(BlockState neighborState, BlockPos pos, Level world, Direction direction);

    boolean transfer(ServerLevel world, BlockPos pipePos, BlockPos containerPos, BlockState pipeState, BlockState containerState, CopperPipeBlockEntity pipeBlockEntity, CopperPipeBlockEntity.Flow flow, Direction direction);

    public class Options {
        @Nullable
        public Container sourceContainer, destinationContainer;
        public BlockPos sourcePos, destinationPos;
        public ServerLevel world;
        public Direction direction;
        public int maxCount;
    }
}
