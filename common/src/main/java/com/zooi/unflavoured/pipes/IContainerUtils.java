package com.zooi.unflavoured.pipes;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;

public interface IContainerUtils{
    boolean transferFirstAvailableItem(Container sourceContainer, Container destinationContainer, Direction direction, int maxCount);
}
