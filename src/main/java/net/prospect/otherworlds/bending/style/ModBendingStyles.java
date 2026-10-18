package net.prospect.otherworlds.bending.style;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;

public class ModBendingStyles {
    public static final ResourceKey<Registry<BendingStyle>> STYLE_KEY =
            ResourceKey.createRegistryKey(OtherWorlds.id("bending_style"));

    public static final DeferredRegister<BendingStyle> STYLES =
            DeferredRegister.create(STYLE_KEY, OtherWorlds.MODID);

    // Creates the custom registry. sync(true) sends it to clients so they match the server.
    public static final Registry<BendingStyle> REGISTRY =
            STYLES.makeRegistry(builder -> builder.sync(true));

    // --- Main styles ---
    public static final DeferredHolder<BendingStyle, BendingStyle> AIR =
            STYLES.register("air", () -> new BendingStyle(0xE8E2C0));
    public static final DeferredHolder<BendingStyle, BendingStyle> WATER =
            STYLES.register("water", () -> new BendingStyle(0x3F8FD2));
    public static final DeferredHolder<BendingStyle, BendingStyle> EARTH =
            STYLES.register("earth", () -> new BendingStyle(0x8B5A2B));
    public static final DeferredHolder<BendingStyle, BendingStyle> FIRE =
            STYLES.register("fire", () -> new BendingStyle(0xE8501A));

    // --- Earth sub-styles ---
    public static final DeferredHolder<BendingStyle, BendingStyle> METAL =
            STYLES.register("metal", () -> new BendingStyle(EARTH, 0xB0B0B0));
    public static final DeferredHolder<BendingStyle, BendingStyle> SAND =
            STYLES.register("sand", () -> new BendingStyle(EARTH, 0xD9C27A));
    public static final DeferredHolder<BendingStyle, BendingStyle> LAVA =
            STYLES.register("lava", () -> new BendingStyle(EARTH, 0xC8340E));
    public static final DeferredHolder<BendingStyle, BendingStyle> SEISMIC_VISION =
            STYLES.register("seismic_vision", () -> new BendingStyle(EARTH, 0x7FC8A9));

    // --- Water sub-styles ---
    public static final DeferredHolder<BendingStyle, BendingStyle> BLOOD =
            STYLES.register("blood", () -> new BendingStyle(WATER, 0x8B0000));
    public static final DeferredHolder<BendingStyle, BendingStyle> ICE =
            STYLES.register("ice", () -> new BendingStyle(WATER, 0xBFE6F5));

    // --- Fire sub-styles ---
    public static final DeferredHolder<BendingStyle, BendingStyle> LIGHTNING =
            STYLES.register("lightning", () -> new BendingStyle(FIRE, 0x9FC8FF));
    public static final DeferredHolder<BendingStyle, BendingStyle> COMBUSTION =
            STYLES.register("combustion", () -> new BendingStyle(FIRE, 0xFFB000));

    public static void register(IEventBus eventBus) {
        STYLES.register(eventBus);
    }
}