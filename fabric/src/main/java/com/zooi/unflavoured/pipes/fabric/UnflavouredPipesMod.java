package com.zooi.unflavoured.pipes.fabric;

import net.fabricmc.api.ModInitializer;

public final class UnflavouredPipesMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.init();
    }
}
