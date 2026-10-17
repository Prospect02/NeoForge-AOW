package net.prospect.otherworlds.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OtherWorlds.MODID);

    // Your own "Avatar Other Worlds" tab. Add a new output.accept(...) line for each new item or block.
    public static final Supplier<CreativeModeTab> CHARGED_GEMSTONE_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("charged_gemstone_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.otherworlds.charged_gemstone_items_tab"))
                    .icon(() -> new ItemStack(ModBlocks.CHARGED_GEMSTONE_ORE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CHARGED_GEMSTONE);
                        output.accept(ModItems.DEPLETED_GEMSTONE);
                        output.accept(ModBlocks.CHARGED_GEMSTONE_BLOCK);
                        output.accept(ModBlocks.DULL_GEMSTONE_BLOCK);
                        output.accept(ModBlocks.CHARGED_GEMSTONE_ORE);
                    }).build());

    // Adds your items and blocks to the vanilla creative tabs
    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.CHARGED_GEMSTONE);
            event.accept(ModItems.DEPLETED_GEMSTONE);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.CHARGED_GEMSTONE_ORE);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.CHARGED_GEMSTONE_BLOCK);
            event.accept(ModBlocks.DULL_GEMSTONE_BLOCK);
        }
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
        eventBus.addListener(ModCreativeModeTabs::addToVanillaTabs);
    }
}