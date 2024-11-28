package com.zooi.unflavoured.pipes;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

public class FallbackContainerUtils implements IContainerUtils {
    public boolean transferFirstAvailableItem(Container sourceContainer, Container destinationContainer, Direction direction, int maxCount) {
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