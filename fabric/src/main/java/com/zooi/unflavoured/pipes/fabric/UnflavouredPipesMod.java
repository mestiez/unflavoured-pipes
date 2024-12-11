package com.zooi.unflavoured.pipes.fabric;

import com.zooi.unflavoured.pipes.CopperPipeBlockEntity;
import com.zooi.unflavoured.pipes.IContainerUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
        public boolean transferFirstAvailableItem(Options options) {
            var source = ItemStorage.SIDED.find(options.world, options.sourcePos, options.direction);
            var dest = ItemStorage.SIDED.find(options.world, options.destinationPos, options.direction.getOpposite());

            if (!source.supportsExtraction())
                return false;
            if (!dest.supportsInsertion())
                return false;

            var transferred = false;

            try (var transaction = Transaction.openOuter()) {
                for (var view : source.nonEmptyViews()) {
                    if (view.isResourceBlank())
                        continue;

                    var resource = view.getResource();

                    var extracted = view.extract(resource, Math.min(view.getAmount(), options.maxCount), transaction);
                    var inserted = dest.insert(resource, extracted, transaction);
                    transferred = inserted != 0;
                    if (transferred) {
                        transaction.commit();
                        return transferred;
                    }
                }
            }

            return transferred;
        }

        @Override
        public boolean canPipeConnect(BlockState neighborState, BlockPos pos, Level world, Direction direction) {
            if (neighborState.is(Blocks.END_PORTAL_FRAME) || neighborState.is(Blocks.COMPOSTER))
                return true;

            var storage = ItemStorage.SIDED.find(world, pos, direction);
            return storage != null && (storage.supportsExtraction() || storage.supportsInsertion());
        }

        @Override
        public boolean transfer(ServerLevel world, BlockPos pipePos, BlockPos containerPos, BlockState pipeState, BlockState containerState, CopperPipeBlockEntity pipeBlockEntity, CopperPipeBlockEntity.Flow flow, Direction direction) {
            var storage = ItemStorage.SIDED.find(world, containerPos, direction);

            if (storage != null && (storage.supportsInsertion() || storage.supportsExtraction())) {
                var options = new IContainerUtils.Options();
                options.maxCount = 1;
                options.world = world;

                if (flow == CopperPipeBlockEntity.Flow.OUTGOING && CopperPipeBlockEntity.isOutput(direction, world, pipePos, pipeState)) {
                    options.sourceContainer = pipeBlockEntity;
                    options.destinationContainer = null;
                    options.sourcePos = pipePos;
                    options.destinationPos = containerPos;
                    options.direction = direction;
                    return transferFirstAvailableItem(options);
                } else if (flow == CopperPipeBlockEntity.Flow.INCOMING && CopperPipeBlockEntity.isInput(direction, world, pipePos, pipeState)) {
                    options.sourceContainer = null;
                    options.destinationContainer = pipeBlockEntity;
                    options.sourcePos = containerPos;
                    options.destinationPos = pipePos;
                    options.direction = direction;
                    return transferFirstAvailableItem(options);
                }
            }

            return false;
        }
    }
}
