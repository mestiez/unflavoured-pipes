package com.zooi.unflavoured.pipes.fabric;

import com.zooi.unflavoured.pipes.IContainerUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

public final class UnflavouredPipesMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.containerUtils = new FabricContainerUtils();
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.init();
    }

    private static class FabricContainerUtils implements IContainerUtils {
        @Override
        public boolean transferFirstAvailableItem(Container sourceContainer, Container destinationContainer, Direction direction, int maxCount) {
            var source = InventoryStorage.of(sourceContainer, direction);
            var dest = InventoryStorage.of(destinationContainer, direction.getOpposite());

            if (!source.supportsExtraction())
                return false;
            if (!dest.supportsInsertion())
                return false;

            var transferred = false;

            try (var transaction = Transaction.openOuter()) {
                for (var view : source.nonEmptyViews())
                {
                    if (view.isResourceBlank())
                        continue;

                    var resource = view.getResource();

                    var extracted = view.extract(resource, Math.min(view.getAmount(), maxCount), transaction);
                    var inserted = dest.insert(resource, extracted, transaction);
                    transferred = inserted != 0;
                    if (transferred){
                        transaction.commit();
                        return transferred;
                    }
                }
            }

            return transferred;
        }
    }
}
