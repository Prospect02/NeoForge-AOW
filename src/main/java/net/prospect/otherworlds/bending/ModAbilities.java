package net.prospect.otherworlds.bending;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.Ability;
import net.prospect.otherworlds.bending.ability.air.AirBlastAbility;

public class ModAbilities {
    public static final ResourceKey<Registry<Ability>> ABILITY_KEY =
            ResourceKey.createRegistryKey(OtherWorlds.id("ability"));

    public static final DeferredRegister<Ability> ABILITIES =
            DeferredRegister.create(ABILITY_KEY, OtherWorlds.MODID);

    public static final Registry<Ability> REGISTRY =
            ABILITIES.makeRegistry(builder -> builder.sync(true));

    // Abilities are added here from Phase 3 onward, one line each.

    public static final DeferredHolder<Ability, AirBlastAbility> AIR_BLAST =
            ABILITIES.register("air_blast", AirBlastAbility::new);

    public static void register(IEventBus eventBus) {
        ABILITIES.register(eventBus);
    }
}