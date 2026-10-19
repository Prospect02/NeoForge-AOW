package net.prospect.otherworlds.bending;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.Ability;
import net.prospect.otherworlds.bending.ability.air.AirBlastAbility;
import net.prospect.otherworlds.bending.ability.air.AirKickAbility;
import net.prospect.otherworlds.bending.ability.air.AirPunchAbility;
import net.prospect.otherworlds.bending.ability.air.TestChannelAbility;
import net.prospect.otherworlds.bending.ability.air.WindBufferAbility;
import net.prospect.otherworlds.bending.ability.air.WindJumpAbility;
import net.prospect.otherworlds.bending.ability.air.WindRunAbility;

public class ModAbilities {
    public static final ResourceKey<Registry<Ability>> ABILITY_KEY =
            ResourceKey.createRegistryKey(OtherWorlds.id("ability"));

    public static final DeferredRegister<Ability> ABILITIES =
            DeferredRegister.create(ABILITY_KEY, OtherWorlds.MODID);

    public static final Registry<Ability> REGISTRY =
            ABILITIES.makeRegistry(builder -> builder.sync(true));

    // --- Air ---
    public static final DeferredHolder<Ability, AirBlastAbility> AIR_BLAST =
            ABILITIES.register("air_blast", AirBlastAbility::new);
    public static final DeferredHolder<Ability, TestChannelAbility> TEST_CHANNEL =
            ABILITIES.register("test_channel", TestChannelAbility::new);

    public static final DeferredHolder<Ability, WindJumpAbility> WIND_JUMP =
            ABILITIES.register("wind_jump", WindJumpAbility::new);
    public static final DeferredHolder<Ability, WindRunAbility> WIND_RUN =
            ABILITIES.register("wind_run", WindRunAbility::new);
    public static final DeferredHolder<Ability, WindBufferAbility> WIND_BUFFER =
            ABILITIES.register("wind_buffer", WindBufferAbility::new);

    public static final DeferredHolder<Ability, AirPunchAbility> AIR_PUNCH =
            ABILITIES.register("air_punch", AirPunchAbility::new);
    public static final DeferredHolder<Ability, AirKickAbility> AIR_KICK =
            ABILITIES.register("air_kick", AirKickAbility::new);

    public static void register(IEventBus eventBus) {
        ABILITIES.register(eventBus);
    }
}