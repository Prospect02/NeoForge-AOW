package net.prospect.otherworlds.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OtherWorlds.MODID);

    // --- Gemstones ---
    public static final DeferredItem<Item> CHARGED_GEMSTONE = ITEMS.registerSimpleItem("charged_gemstone");
    public static final DeferredItem<Item> DEPLETED_GEMSTONE = ITEMS.registerSimpleItem("depleted_gemstone");

    // --- Scrolls (Phase 5c) ---
    // --- Spawn eggs (Phase 10b) ---

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
