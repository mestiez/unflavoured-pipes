package com.zooi.unflavoured.pipes;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

public class ContainerUtils {

    /**
     * Transfers the first available item from sourceContainer to destinationContainer, considering direction for WorldlyContainers.
     * Merging is taken into account, including max stack sizes, NBT tags, etc.
     *
     * @param sourceContainer      The container to transfer items from.
     * @param destinationContainer The container to transfer items to.
     * @param direction            The direction from which the transfer is happening.
     * @return True if an item was transferred, false otherwise.
     */
    public static boolean transferFirstAvailableItem(Container sourceContainer, Container destinationContainer, Direction direction) {
        // Get the slots accessible from the source container's face
        int[] sourceSlots = getAccessibleSlots(sourceContainer, direction);

        for (int srcSlot : sourceSlots) {
            var sourceStack = sourceContainer.getItem(srcSlot);

            // Skip empty slots or items that cannot be taken through this face
            if (sourceStack.isEmpty() || !canExtractItem(sourceContainer, destinationContainer, sourceStack, srcSlot, direction)) {
                continue;
            }

            // Attempt to transfer the item stack
            var remainingStack = transferItemStack(sourceContainer, srcSlot, destinationContainer, direction.getOpposite(), sourceStack);

            // Update source container slot
            sourceContainer.setItem(srcSlot, remainingStack);

            // If items were transferred
            if (remainingStack.isEmpty() || remainingStack.getCount() < sourceStack.getCount()) {
                sourceContainer.setChanged();
                destinationContainer.setChanged();
                return true;
            }
        }

        return false;
    }

    /**
     * Transfers as much as possible of the given itemStack from sourceContainer to destinationContainer.
     * Merges with existing stacks and respects max stack sizes and slot restrictions.
     *
     * @param sourceContainer      The container the item stack is from.
     * @param srcSlot              The slot in the source container.
     * @param destinationContainer The container to transfer the item stack to.
     * @param direction            The direction into which the transfer is happening (used for WorldlyContainers).
     * @param itemStack            The item stack to transfer.
     * @return The remaining item stack that couldn't be transferred.
     */
    private static ItemStack transferItemStack(Container sourceContainer, int srcSlot, Container destinationContainer, Direction direction, ItemStack itemStack) {
        var remainingStack = itemStack.copy();

        // Get the slots accessible from the destination container's face
        var destSlots = getAccessibleSlots(destinationContainer, direction);

        // First, try to merge with existing stacks
        for (var destSlot : destSlots) {
            ItemStack destStack = destinationContainer.getItem(destSlot);

            if (canMergeItems(destStack, remainingStack) && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                var maxTransfer = Math.min(remainingStack.getCount(), destStack.getMaxStackSize() - destStack.getCount());
                if (maxTransfer > 0) {
                    destStack.grow(maxTransfer);
                    remainingStack.shrink(maxTransfer);
                    destinationContainer.setItem(destSlot, destStack);
                    if (remainingStack.isEmpty())
                        return ItemStack.EMPTY;
                }
            }
        }

        // Then, place into empty slots
        for (var destSlot : destSlots) {
            ItemStack destStack = destinationContainer.getItem(destSlot);

            if (destStack.isEmpty() && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                int maxPlacement = Math.min(remainingStack.getCount(), remainingStack.getMaxStackSize());
                var stackToPlace = remainingStack.copy();
                stackToPlace.setCount(maxPlacement);

                destinationContainer.setItem(destSlot, stackToPlace);
                remainingStack.shrink(maxPlacement);

                if (remainingStack.isEmpty())
                    return ItemStack.EMPTY;
            }
        }

        return remainingStack;
    }

    private static boolean canMergeItems(ItemStack itemStack, ItemStack itemStack2) {
        return itemStack.getCount() <= itemStack.getMaxStackSize() && ItemStack.isSameItemSameTags(itemStack, itemStack2);
    }

    /**
     * Retrieves the accessible slots for the container from the given direction.
     *
     * @param container The container.
     * @param direction The direction.
     * @return An array of accessible slot indices.
     */
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

    /**
     * Checks if an item can be extracted from the container from the given slot and direction.
     *
     * @param src       The container.
     * @param stack     The item stack.
     * @param slot      The slot index.
     * @param direction The extraction direction.
     * @return True if the item can be extracted, false otherwise.
     */
    private static boolean canExtractItem(Container src, Container dst, ItemStack stack, int slot, Direction direction) {
        if (!src.canTakeItem(dst, slot, stack))
            return false;

        if (src instanceof WorldlyContainer worldlyContainer)
            return worldlyContainer.canTakeItemThroughFace(slot, stack, direction);

        return true;
    }

    /**
     * Checks if an item can be inserted into the container at the given slot and direction.
     *
     * @param container The container.
     * @param stack     The item stack.
     * @param slot      The slot index.
     * @param direction The insertion direction.
     * @return True if the item can be inserted, false otherwise.
     */
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