package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.redstone.Redstone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopperPipeBlockEntity extends RandomizableContainerBlockEntity {
    public static final int TRANSFER_COOLDOWN = 15;

    private NonNullList<ItemStack> items;
    private int timer = 0;
    private int selectedDirIndex;

    public CopperPipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UnflavouredPipesMod.ModBlockEntityType.COPPER_PIPE, blockPos, blockState);
        this.items = NonNullList.withSize(4, ItemStack.EMPTY);
    }

    public static void pushItemsTick(Level world, BlockPos blockPos, BlockState state, CopperPipeBlockEntity copperPipe) {
        if (world.isClientSide())
            return;

        copperPipe.timer++;
        if (copperPipe.timer >= TRANSFER_COOLDOWN)
            copperPipe.timer = 0;
        else
            return;

        if (state.getValue(CopperPipeBlock.POWER) == 0)
            return;

        var directions = Direction.values();
        for (var dir : directions)
            handleDir(dir, world, blockPos, state, copperPipe, Flow.INCOMING);

        for (int i = 0; i < directions.length; i++) {
            if (handleDir(directions[(copperPipe.selectedDirIndex++) % directions.length], world, blockPos, state, copperPipe, Flow.OUTGOING))
                break; // we only do 1 thing at most
        }
    }

    private static boolean handleDir(Direction direction, Level world, BlockPos pipePos, BlockState pipeState, CopperPipeBlockEntity pipeBlockEntity, Flow flow) {
        var directionPos = pipePos.relative(direction);
        var directionPosCenter = directionPos.getCenter();
        var stateInDirection = world.getBlockState(directionPos);

        var didSomething = false;

        // handle container
        var targetContainer = getContainerAt(world, directionPosCenter.x, directionPosCenter.y, directionPosCenter.z);
        if (targetContainer != null) {
            if (flow == Flow.OUTGOING && isOutput(direction, world, pipePos, pipeState))
                didSomething = ContainerUtils.transferFirstAvailableItem(pipeBlockEntity, targetContainer, direction);
            else if (flow == Flow.INCOMING && isInput(direction, world, pipePos, pipeState))
                didSomething = ContainerUtils.transferFirstAvailableItem(targetContainer, pipeBlockEntity, direction);
        }

        // handle composter (!didSomething && )
        if (!didSomething && flow == Flow.OUTGOING && stateInDirection.is(Blocks.COMPOSTER)) {
            didSomething = handleComposter(world, pipeBlockEntity, stateInDirection, directionPos, didSomething);
        }

        if (!didSomething && stateInDirection.is(Blocks.END_PORTAL_FRAME) && flow == Flow.OUTGOING) {
            handleEnderFrame(world, pipeBlockEntity, stateInDirection, directionPos);
        }

        return didSomething;
    }

    private static boolean handleComposter(Level world, CopperPipeBlockEntity pipeBlockEntity, BlockState stateInDirection, BlockPos directionPos, boolean didSomething) {
        var composterState = stateInDirection;
        if (composterState.getValue(ComposterBlock.LEVEL) < 7)
            for (var stack : pipeBlockEntity.getItems()) {
                if (stack.isEmpty() || !ComposterBlock.COMPOSTABLES.containsKey(stack.getItem()))
                    continue;

                var oldLvl = composterState.getValue(ComposterBlock.LEVEL);
                composterState = ComposterBlock.insertItem(null, composterState, (ServerLevel) world, stack, directionPos);
                var newLvl = composterState.getValue(ComposterBlock.LEVEL);
                //ComposterBlock.handleFill(world, directionPos, newLvl != 8); // does nothing because it should be run clientside i think
                world.playSound(null, directionPos, newLvl > oldLvl ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                didSomething = true;
            }
        return didSomething;
    }

    private static void handleEnderFrame(Level world, CopperPipeBlockEntity pipeBlockEntity, BlockState stateInDirection, BlockPos directionPos) {
        if (stateInDirection.getValue(EndPortalFrameBlock.HAS_EYE))
            return;

        for (var itemStack : pipeBlockEntity.getItems()) {
            if (itemStack.is(Items.ENDER_EYE) && itemStack.getCount() > 0) {
                var newState = stateInDirection.setValue(EndPortalFrameBlock.HAS_EYE, true);
                world.setBlock(directionPos, newState, 2);
                world.updateNeighbourForOutputSignal(directionPos, Blocks.END_PORTAL_FRAME);
                itemStack.shrink(1);
                world.levelEvent(1503, directionPos, 0);
                BlockPattern.BlockPatternMatch blockPatternMatch = EndPortalFrameBlock.getOrCreatePortalShape().find(world, directionPos);
                if (blockPatternMatch != null) {
                    BlockPos blockPos2 = blockPatternMatch.getFrontTopLeft().offset(-3, 0, -3);

                    for (int i = 0; i < 3; ++i) {
                        for (int j = 0; j < 3; ++j) {
                            world.setBlock(blockPos2.offset(i, 0, j), Blocks.END_PORTAL.defaultBlockState(), 2);
                        }
                    }

                    world.globalLevelEvent(1038, blockPos2.offset(1, 0, 1), 0);
                }
            }
        }
    }

    public static boolean isOutput(Direction direction, Level world, BlockPos pipePos, BlockState pipeState) {
        var prop = CopperPipeBlock.getConnection(direction);
        if (pipeState.getValue(prop)) {
            var otherPos = pipePos.relative(direction);
            var otherState = world.getBlockState(otherPos);
            if (otherState.is(UnflavouredPipesMod.ModBlocks.COPPER_PIPE)) {
                return otherState.getValue(CopperPipeBlock.POWER) <= pipeState.getValue(CopperPipeBlock.POWER);
            } else {
                return pipeState.getValue(CopperPipeBlock.POWER) != Redstone.SIGNAL_MAX;
            }
        }

        return false;
    }

    public static boolean isInput(Direction direction, Level world, BlockPos pipePos, BlockState pipeState) {
        var prop = CopperPipeBlock.getConnection(direction);
        if (pipeState.getValue(prop)) {
            var otherPos = pipePos.relative(direction);
            var otherState = world.getBlockState(otherPos);
            if (otherState.is(UnflavouredPipesMod.ModBlocks.COPPER_PIPE)) {
                return false;//otherState.getValue(CopperPipeBlock.POWER) >= pipeState.getValue(CopperPipeBlock.POWER); // copper pipes should only push to each other!
            } else {
                return pipeState.getValue(CopperPipeBlock.POWER) == Redstone.SIGNAL_MAX;
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

    @Override
    public int getContainerSize() {
        return getItems().size();
    }

    @Override
    public boolean isEmpty() {
        return getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int i) {
        return getItems().get(i);
    }

    @Override
    public @NotNull ItemStack removeItem(int i, int j) {
        ItemStack itemStack = ContainerHelper.removeItem(getItems(), i, j);
        if (!itemStack.isEmpty()) {
            setChanged();
        }

        return itemStack;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> it) {
        items = it;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(getItems(), i);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        getItems().set(i, itemStack);
        if (itemStack.getCount() > getMaxStackSize())
            itemStack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        getItems().clear();
    }

    @Override
    protected Component getDefaultName() {
        return Component.empty();
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(compoundTag))
            ContainerHelper.loadAllItems(compoundTag, this.items);
        this.timer = compoundTag.getInt("Timer");
        this.selectedDirIndex = compoundTag.getInt("SelectedDirectionIndex");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (!this.trySaveLootTable(compoundTag))
            ContainerHelper.saveAllItems(compoundTag, this.items);
        compoundTag.putInt("Timer", this.timer);
        compoundTag.putInt("SelectedDirectionIndex", this.selectedDirIndex);
    }

    @Override
    public boolean canOpen(Player player) {
        return false;
    }

    private enum Flow {
        INCOMING,
        OUTGOING
    }
}
