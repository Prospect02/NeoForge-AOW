package net.prospect.otherworlds.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;

/** Sound events. The actual audio for each is set up in assets/otherworlds/sounds.json. */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, OtherWorlds.MODID);

    /** Something is thrown into a wall hard enough to get stuck. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WALL_IMPACT = sound("wall_impact");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(OtherWorlds.id(name)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}