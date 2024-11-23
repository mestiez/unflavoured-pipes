package com.zooi.unflavoured.pipes;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopperPipeBlockEntity extends BlockEntity implements Container {
    public static final int TRANSFER_COOLDOWN = 15;

    private final NonNullList<ItemStack> items;
    private int timer = 0;

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

        if (up && down) {
            var upPos = blockPos.relative(Direction.UP);
            var upPosCenter = upPos.getCenter();
            var downPos = blockPos.relative(Direction.DOWN);
            var downPosCenter = downPos.getCenter();
            var upContainer = getContainerAt(world, upPosCenter.x, upPosCenter.y, upPosCenter.z);
            var downContainer = getContainerAt(world, downPosCenter.x, downPosCenter.y, downPosCenter.z);

            if (upContainer != null)
                ContainerUtils.transferFirstAvailableItem(copperPipe, upContainer, Direction.UP);

            if (downContainer != null)
                ContainerUtils.transferFirstAvailableItem(downContainer, copperPipe, Direction.DOWN);

            if (world.getBlockState(upPos).is(Blocks.COMPOSTER)) {
                var composterState = world.getBlockState(upPos);
                var composter = (ComposterBlock) composterState.getBlock();
                for (var stack : copperPipe.getItems()) {
                    if (stack.isEmpty())
                        continue;

                    composterState = ComposterBlock.insertItem(null, composterState, (ServerLevel) world, stack, upPos);
                    ComposterBlock.handleFill(world, upPos, composterState.getValue(ComposterBlock.LEVEL) != 8); // does nothing because it should be run clientside i think
//                    {
//                        int i = composterState.getValue(ComposterBlock.LEVEL);
//                        if (i < 8 && ComposterBlock.COMPOSTABLES.containsKey(stack.getItem())) {
//                            if (i < 7 && !world.isClientSide) {
//                                {
//                                    float f = ComposterBlock.COMPOSTABLES.getFloat(stack.getItem());
//                                    if ((i != 0 || !(f > 0.0F)) && !(world.getRandom().nextDouble() < f)) {
//
//                                    } else {
//                                        int j = i + 1;
//                                        BlockState blockState2 = composterState.setValue(ComposterBlock.LEVEL, j);
//                                        world.setBlock(blockPos, blockState2, 3);
//                                        if (j == 7) {
//                                            world.scheduleTick(blockPos, composterState.getBlock(), 20);
//                                        }
//                                    }
//                                }
//                                stack.shrink(1);
//                            }
//                        }else if (i == 8){
//                            ComposterBlock.extractProduce((Entity)null, composterState, world, blockPos);
//                        }
//                    }
                }
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

    public NonNullList<ItemStack> getItems() {
        return items;
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
        items.clear();
    }
}
