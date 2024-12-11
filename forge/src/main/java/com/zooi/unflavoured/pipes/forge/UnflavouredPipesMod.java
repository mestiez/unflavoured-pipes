package com.zooi.unflavoured.pipes.forge;

import com.zooi.unflavoured.pipes.CopperPipeBlockEntity;
import com.zooi.unflavoured.pipes.IContainerUtils;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod(com.zooi.unflavoured.pipes.UnflavouredPipesMod.MOD_ID)
public final class UnflavouredPipesMod {
    public static final Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.get(new CapabilityToken<>() {
    });

    public UnflavouredPipesMod() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(com.zooi.unflavoured.pipes.UnflavouredPipesMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.containerUtils = new ForgeContainerUtils();
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.init();
    }

    private class ForgeContainerUtils implements IContainerUtils {
        @Override
        public boolean transferFirstAvailableItem(Options options) {

            var sourceBlockEntity = options.world.getBlockEntity(options.sourcePos);
            var destBlockEntity = options.world.getBlockEntity(options.destinationPos);
            var direction = options.direction;

            if (sourceBlockEntity == null || destBlockEntity == null)
                return false;

            var sourceCap = sourceBlockEntity.getCapability(ITEM_HANDLER, direction);
            var destCap = destBlockEntity.getCapability(ITEM_HANDLER, direction.getOpposite());

            if (!sourceCap.isPresent() || !destCap.isPresent())
                return false;

            var maybeSourceHandler = sourceCap.resolve();
            var maybeDestinationHandler = destCap.resolve();

            if (maybeSourceHandler.isEmpty() || maybeDestinationHandler.isEmpty())
                return false;

            var sourceHandler = maybeSourceHandler.get();
            var destinationHandler = maybeDestinationHandler.get();

            boolean transferred = false;
            int remaining = options.maxCount;

            for (int srcIndex = 0; srcIndex < sourceHandler.getSlots(); srcIndex++) {
                if (remaining <= 0)
                    break;

                var sourceStack = sourceHandler.getStackInSlot(srcIndex).copy();
                if (sourceStack.isEmpty())
                    continue;

                var transferable = Math.min(sourceStack.getCount(), remaining);
                var toTransfer = sourceStack.copy();
                toTransfer.setCount(transferable);
                var remainder = ItemHandlerHelper.insertItem(destinationHandler, toTransfer, false);
                var insertedAmount = transferable - remainder.getCount();

                if (insertedAmount > 0) {
                    sourceHandler.extractItem(srcIndex, insertedAmount, false);
                    transferred = true;
                    remaining -= insertedAmount;
                }
            }

            return transferred;
        }

        @Override
        public boolean canPipeConnect(BlockState neighborState, BlockPos pos, Level world, Direction direction) {
            if (neighborState.is(Blocks.END_PORTAL_FRAME) || neighborState.is(Blocks.COMPOSTER))
                return true;

            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ITEM_HANDLER);
                return cap.isPresent();
            }

            return false;
        }

        @Override
        public boolean transfer(ServerLevel world, BlockPos pipePos, BlockPos containerPos, BlockState pipeState, BlockState containerState, CopperPipeBlockEntity pipeBlockEntity, CopperPipeBlockEntity.Flow flow, Direction direction) {
            var containerEntity = world.getBlockEntity(containerPos);

            if (containerEntity != null) {
                var cap = containerEntity.getCapability(ITEM_HANDLER);
                if (cap.isPresent()) {
                    var options = new IContainerUtils.Options();
                    options.maxCount = 1;
                    options.world = world;

                    if (flow == CopperPipeBlockEntity.Flow.OUTGOING && CopperPipeBlockEntity.isOutput(direction, world, pipePos, pipeState)) {
                        options.sourcePos = pipePos;
                        options.destinationPos = containerPos;
                        options.direction = direction;
                        return transferFirstAvailableItem(options);
                    } else if (flow == CopperPipeBlockEntity.Flow.INCOMING && CopperPipeBlockEntity.isInput(direction, world, pipePos, pipeState)) {
                        options.sourcePos = containerPos;
                        options.destinationPos = pipePos;
                        options.direction = direction;
                        return transferFirstAvailableItem(options);
                    }
                }
            }

            return false;
        }
    }
}
