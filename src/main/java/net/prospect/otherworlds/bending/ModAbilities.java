package net.prospect.otherworlds.bending;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.Ability;
import net.prospect.otherworlds.bending.ability.air.AirBulletAbility;
import net.prospect.otherworlds.bending.ability.air.AirKickAbility;
import net.prospect.otherworlds.bending.ability.air.AirPunchAbility;
import net.prospect.otherworlds.bending.ability.air.AirScooterAbility;
import net.prospect.otherworlds.bending.ability.air.BreathOfWindAbility;
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
    public static final DeferredHolder<Ability, WindJumpAbility> WIND_JUMP =
            ABILITIES.register("wind_jump", WindJumpAbility::new);
    public static final DeferredHolder<Ability, WindRunAbility> WIND_RUN =
            ABILITIES.register("wind_run", WindRunAbility::new);
    public static final DeferredHolder<Ability, WindBufferAbility> WIND_BUFFER =
            ABILITIES.register("wind_buffer", WindBufferAbility::new);

    public static final DeferredHolder<Ability, BreathOfWindAbility> BREATH_OF_WIND =
            ABILITIES.register("breath_of_wind", BreathOfWindAbility::new);

    public static final DeferredHolder<Ability, AirPunchAbility> AIR_PUNCH =
            ABILITIES.register("air_punch", AirPunchAbility::new);
    public static final DeferredHolder<Ability, AirKickAbility> AIR_KICK =
            ABILITIES.register("air_kick", AirKickAbility::new);
    public static final DeferredHolder<Ability, AirBulletAbility> AIR_BULLET =
            ABILITIES.register("air_bullet", AirBulletAbility::new);

    public static final DeferredHolder<Ability, AirScooterAbility> AIR_SCOOTER =
            ABILITIES.register("air_scooter", AirScooterAbility::new);

    public static void register(IEventBus eventBus) {
        ABILITIES.register(eventBus);
    }
}