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
import net.minecraft.world.LockCode;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopperPipeBlockEntity extends RandomizableContainerBlockEntity {
    public static final int TRANSFER_COOLDOWN = 15;

    private NonNullList<ItemStack> items;
    private int timer = 0;
    private LockCode lockKey;

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

        var up = state.getValue(CopperPipeBlock.UP);
        var down = state.getValue(CopperPipeBlock.DOWN);
        var east = state.getValue(CopperPipeBlock.EAST);
        var west = state.getValue(CopperPipeBlock.WEST);
        var north = state.getValue(CopperPipeBlock.NORTH);
        var south = state.getValue(CopperPipeBlock.SOUTH);

        var upPos = blockPos.relative(Direction.UP);
        var upPosCenter = upPos.getCenter();
        var downPos = blockPos.relative(Direction.DOWN);
        var downPosCenter = downPos.getCenter();

        if (up) {
            var upContainer = getContainerAt(world, upPosCenter.x, upPosCenter.y, upPosCenter.z);
            if (upContainer != null)
                ContainerUtils.transferFirstAvailableItem(copperPipe, upContainer, Direction.UP);
        }

        if (down) {
            var downContainer = getContainerAt(world, downPosCenter.x, downPosCenter.y, downPosCenter.z);
            if (downContainer != null)
                ContainerUtils.transferFirstAvailableItem(downContainer, copperPipe, Direction.DOWN);
        }

        if (world.getBlockState(upPos).is(Blocks.COMPOSTER)) {
            var composterState = world.getBlockState(upPos);
            var composter = (ComposterBlock) composterState.getBlock();
            if (composterState.getValue(ComposterBlock.LEVEL) < 7)
                for (var stack : copperPipe.getItems()) {
                    if (stack.isEmpty())
                        continue;

                    var oldLvl = composterState.getValue(ComposterBlock.LEVEL);
                    composterState = ComposterBlock.insertItem(null, composterState, (ServerLevel) world, stack, upPos);
                    var newLvl = composterState.getValue(ComposterBlock.LEVEL);
                    ComposterBlock.handleFill(world, upPos, newLvl != 8); // does nothing because it should be run clientside i think

                    world.playSound((Player) null, blockPos, newLvl > oldLvl ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
        }
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

        if (container == null) {
            var list = world.getEntities((Entity) null, new AABB(x - .5f, y - .5f, z - .5f, x + .5f, y + .5f, z + .5f), EntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list.isEmpty()) {
                container = (Container) list.get(world.random.nextInt(list.size()));
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
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (!this.trySaveLootTable(compoundTag))
            ContainerHelper.saveAllItems(compoundTag, this.items);
        compoundTag.putInt("Timer", this.timer);
    }
}
