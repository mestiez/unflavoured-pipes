package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopperPipeBlockEntity extends BlockEntity {
    public CopperPipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UnflavouredPipesMod.ModBlockEntiiyTyoe.COPPER_PIPE, blockPos, blockState);
    }
}
