package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FallbackContainerUtils implements IContainerUtils {
    public boolean transferFirstAvailableItem(Options options) {
        var sourceContainer = options.sourceContainer;
        var destinationContainer = options.destinationContainer;
        var direction = options.direction;
        var maxCount = options.maxCount;

        var sourceSlots = getAccessibleSlots(sourceContainer, direction);

        for (int srcSlot : sourceSlots) {
            var sourceStack = sourceContainer.getItem(srcSlot);

            // Skip empty slots or items that cannot be taken through this face
            if (sourceStack.isEmpty() || !canExtractItem(sourceContainer, destinationContainer, sourceStack, srcSlot, direction)) {
                continue;
            }

            // Determine the amount to transfer, respecting the maxCount limit
            var transferCount = Math.min(sourceStack.getCount(), maxCount);

            // Use removeItem to properly remove the item from the source container
            var extractedStack = sourceContainer.removeItem(srcSlot, transferCount);

            // Try to add the item to the destination container
            var remainingStack = transferItemStackToContainer(destinationContainer, extractedStack, direction.getOpposite(), transferCount);

            // If there are remaining items that couldn't be transferred, put them back into the source container
            if (!remainingStack.isEmpty()) {
                // Place the remaining items back into the source container slot
                var currentSourceStack = sourceContainer.getItem(srcSlot);
                if (currentSourceStack.isEmpty()) {
                    // If the source slot is empty, set the remaining stack directly
                    sourceContainer.setItem(srcSlot, remainingStack);
                } else {
                    // If there's already an item in the source slot, combine them
                    currentSourceStack.grow(remainingStack.getCount());
                    sourceContainer.setItem(srcSlot, currentSourceStack);
                }
            }

            // Calculate the actual number of items transferred
            int itemsTransferred = extractedStack.getCount() - remainingStack.getCount();

            // If any items were transferred
            if (itemsTransferred > 0) {
                sourceContainer.setChanged();
                destinationContainer.setChanged();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canPipeConnect(BlockState neighborState, BlockPos pos, Level world, Direction direction) {
        if (neighborState.hasBlockEntity()) {
            var be = world.getBlockEntity(pos);
            if (be != null)
                return be instanceof Container || be instanceof CopperPipeBlockEntity;
        }

        return false;
    }

    @Override
    public boolean transfer(ServerLevel world, BlockPos pipePos, BlockPos containerPos, BlockState pipeState, BlockState containerState, CopperPipeBlockEntity pipeBlockEntity, CopperPipeBlockEntity.Flow flow, Direction direction) {
        var directionPosCenter = containerPos.getCenter();
        var targetContainer = getContainerAt(world, directionPosCenter.x, directionPosCenter.y, directionPosCenter.z);
        if (targetContainer != null) {
            var options = new IContainerUtils.Options();
            options.maxCount = 1;
            options.world = world;

            if (flow == CopperPipeBlockEntity.Flow.OUTGOING && CopperPipeBlockEntity.isOutput(direction, world, pipePos, pipeState))
            {
                options.sourceContainer = pipeBlockEntity;
                options.destinationContainer = targetContainer;
                options.sourcePos = pipePos;
                options.destinationPos = containerPos;
                options.direction = direction;
                return transferFirstAvailableItem(options);
            }
            else if (flow == CopperPipeBlockEntity.Flow.INCOMING && CopperPipeBlockEntity.isInput(direction, world, pipePos, pipeState))
            {
                options.sourceContainer = targetContainer;
                options.destinationContainer = pipeBlockEntity;
                options.sourcePos = containerPos;
                options.destinationPos = pipePos;
                options.direction = direction;
                return transferFirstAvailableItem(options);
            }
        }

        return false;
    }

    // Straight up copied from hopper
    @Nullable
    private static Container getContainerAt(Level world, double x, double y, double z) {
        Container container = null;
        var blockPos = BlockPos.containing(x, y, z);
        var blockState = world.getBlockState(blockPos);
        var block = blockState.getBlock();
        if (block instanceof WorldlyContainerHolder worldlyContainerHolder) {
            container = worldlyContainerHolder.getContainer(blockState, world, blockPos);
        } else if (blockState.hasBlockEntity()) {
            var blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof Container c) {
                container = c;
                if (container instanceof ChestBlockEntity && block instanceof ChestBlock chest) {
                    container = ChestBlock.getContainer(chest, blockState, world, blockPos, true);
                }
            }
        }

        return container;
    }

    private static ItemStack transferItemStackToContainer(Container destinationContainer, ItemStack itemStack, Direction direction, int maxCount) {
        var remainingStack = itemStack.copy();

        var itemsToTransfer = Math.min(remainingStack.getCount(), maxCount);
        remainingStack.setCount(itemsToTransfer);

        var destSlots = getAccessibleSlots(destinationContainer, direction);

        // First, try to merge with existing stacks
        for (var destSlot : destSlots) {
            var destStack = destinationContainer.getItem(destSlot);

            if (canMergeItems(destStack, remainingStack) && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                var maxTransfer = Math.min(remainingStack.getCount(), destStack.getMaxStackSize() - destStack.getCount());
                if (maxTransfer > 0) {
                    // Update destination stack
                    destStack.grow(maxTransfer);
                    remainingStack.shrink(maxTransfer);
                    destinationContainer.setItem(destSlot, destStack);
                    if (remainingStack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        // Then, try to place into empty slots
        if (!remainingStack.isEmpty()) {
            for (var destSlot : destSlots) {
                var destStack = destinationContainer.getItem(destSlot);

                if (destStack.isEmpty() && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                    var maxPlacement = Math.min(remainingStack.getCount(), remainingStack.getMaxStackSize());
                    var stackToPlace = remainingStack.copy();
                    stackToPlace.setCount(maxPlacement);

                    destinationContainer.setItem(destSlot, stackToPlace);
                    remainingStack.shrink(maxPlacement);
                    if (remainingStack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return remainingStack;
    }

    private static boolean canMergeItems(ItemStack itemStack, ItemStack itemStack2) {
        return itemStack.getCount() <= itemStack.getMaxStackSize() && ItemStack.isSameItemSameTags(itemStack, itemStack2);
    }

    private static int[] getAccessibleSlots(Container container, Direction direction) {
        if (container instanceof WorldlyContainer worldlyContainer) {
            return worldlyContainer.getSlotsForFace(direction);
        } else {
            int[] slots = new int[container.getContainerSize()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }
    }

    private static boolean canExtractItem(Container src, Container dst, ItemStack stack, int slot, Direction direction) {
        if (!src.canTakeItem(dst, slot, stack))
            return false;

        if (src instanceof WorldlyContainer worldlyContainer)
            return worldlyContainer.canTakeItemThroughFace(slot, stack, direction);

        return true;
    }

    private static boolean canInsertItem(Container container, ItemStack stack, int slot, Direction direction) {
        if (!container.canPlaceItem(slot, stack)) {
            return false;
        }
        if (container instanceof WorldlyContainer worldlyContainer) {
            return worldlyContainer.canPlaceItemThroughFace(slot, stack, direction);
        }
        return true;
    }
}