package com.zooi.unflavoured.pipes.fabric.client;

import com.zooi.unflavoured.pipes.UnflavouredPipesMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public final class UnflavouredPipesModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        BlockRenderLayerMap.INSTANCE.putBlock(UnflavouredPipesMod.ModBlocks.COPPER_PIPE, RenderType.cutout());
    }
}
