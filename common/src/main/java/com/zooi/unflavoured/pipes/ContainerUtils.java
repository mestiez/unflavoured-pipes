package com.zooi.unflavoured.pipes;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

public class ContainerUtils {

    public static boolean transferFirstAvailableItem(Container sourceContainer, Container destinationContainer, Direction direction) {
        int[] sourceSlots = getAccessibleSlots(sourceContainer, direction);

        for (int srcSlot : sourceSlots) {
            ItemStack sourceStack = sourceContainer.getItem(srcSlot);

            // Skip empty slots or items that cannot be taken through this face
            if (sourceStack.isEmpty() || !canExtractItem(sourceContainer, destinationContainer, sourceStack, srcSlot, direction)) {
                continue;
            }

            // Use removeItem to properly remove the item from the source container
            ItemStack extractedStack = sourceContainer.removeItem(srcSlot, sourceStack.getCount());

            // Try to add the item to the destination container
            ItemStack remainingStack = transferItemStackToContainer(destinationContainer, extractedStack, direction.getOpposite());

            // If there are remaining items that couldn't be transferred, put them back into the source container
            if (!remainingStack.isEmpty()) {
                sourceContainer.setItem(srcSlot, remainingStack);
            }

            // If any items were transferred
            if (remainingStack.getCount() < extractedStack.getCount()) {
                sourceContainer.setChanged();
                destinationContainer.setChanged();
                return true;
            }
        }

        return false;
    }

    private static ItemStack transferItemStackToContainer(Container destinationContainer, ItemStack itemStack, Direction direction) {
        ItemStack remainingStack = itemStack.copy();

        int[] destSlots = getAccessibleSlots(destinationContainer, direction);

        // First, try to merge with existing stacks
        for (int destSlot : destSlots) {
            ItemStack destStack = destinationContainer.getItem(destSlot);

            if (canMergeItems(destStack, remainingStack) && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                int maxTransfer = Math.min(remainingStack.getCount(), destStack.getMaxStackSize() - destStack.getCount());
                if (maxTransfer > 0) {
                    // Use setItem to update the stack properly
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
            for (int destSlot : destSlots) {
                ItemStack destStack = destinationContainer.getItem(destSlot);

                if (destStack.isEmpty() && canInsertItem(destinationContainer, remainingStack, destSlot, direction)) {
                    int maxPlacement = Math.min(remainingStack.getCount(), remainingStack.getMaxStackSize());
                    ItemStack stackToPlace = remainingStack.copy();
                    stackToPlace.setCount(maxPlacement);

                    // Use setItem to add the item
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