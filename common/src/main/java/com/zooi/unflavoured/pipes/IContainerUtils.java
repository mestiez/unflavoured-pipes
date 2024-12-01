package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IContainerUtils {
    boolean transferFirstAvailableItem(Options options);

    public class Options {
        public Container sourceContainer, destinationContainer;
        public BlockPos sourcePos, destinationPos;
        public ServerLevel world;
        public Direction direction;
        public int maxCount;
    }
}
