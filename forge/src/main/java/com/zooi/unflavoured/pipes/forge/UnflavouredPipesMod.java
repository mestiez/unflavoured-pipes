package com.zooi.unflavoured.pipes.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;

@Mod(com.zooi.unflavoured.pipes.UnflavouredPipesMod.MOD_ID)
public final class UnflavouredPipesMod {
    public static final Capability<IItemHandler> ITEM_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});

    public UnflavouredPipesMod() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(com.zooi.unflavoured.pipes.UnflavouredPipesMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        com.zooi.unflavoured.pipes.UnflavouredPipesMod.init();
    }
}
