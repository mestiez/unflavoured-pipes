package com.zooi.unflavoured.pipes;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class UnflavouredPipesMod {
    public static final String MOD_ID = "unflavoured_pipes";
    public static final Supplier<RegistrarManager> MANAGER = Suppliers.memoize(() -> RegistrarManager.get(MOD_ID));
    public static IContainerUtils containerUtils = new FallbackContainerUtils();

    public static class ModBlocks {
        public static final Block COPPER_PIPE = new CopperPipeBlock(Properties.copy(Blocks.COPPER_BLOCK)
                .strength(2.0F, 5.0F).isRedstoneConductor((s, l, p) -> false).noOcclusion().isSuffocating((state, getter, pos) -> false));
    }

    public static class ModItems {
        public static final Item COPPER_PIPE = new BlockItem(ModBlocks.COPPER_PIPE, new Item.Properties().stacksTo(64).arch$tab(CreativeModeTabs.REDSTONE_BLOCKS));
    }

    public static class ModBlockEntityType {
        public static final BlockEntityType<CopperPipeBlockEntity> COPPER_PIPE = BlockEntityType.Builder.of(CopperPipeBlockEntity::new, ModBlocks.COPPER_PIPE).build(null);
    }

    public static ResourceLocation getId(String s) {
        return new ResourceLocation(MOD_ID, s);
    }

    public static void init() {
        var items = MANAGER.get().get(Registries.ITEM);
        var blocks = MANAGER.get().get(Registries.BLOCK);
        var blockEntities = MANAGER.get().get(Registries.BLOCK_ENTITY_TYPE);

        blocks.register(getId("copper_pipe"), () -> ModBlocks.COPPER_PIPE);
        items.register(getId("copper_pipe"), () -> ModItems.COPPER_PIPE);
        blockEntities.register(getId("copper_pipe"), () -> ModBlockEntityType.COPPER_PIPE);
    }
}
