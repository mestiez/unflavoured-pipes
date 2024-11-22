package com.zooi.unflavoured.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CopperPipeBlockEntity extends BlockEntity {
    public CopperPipeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(UnflavouredPipesMod.ModBlockEntityType.COPPER_PIPE, blockPos, blockState);
    }

    public static void pushItemsTick(Level level, BlockPos blockPos, BlockState blockState, CopperPipeBlockEntity pipeBlockEntity) {
        
    }
}
