package net.prospect.otherworlds.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.AbilityCooldowns;
import net.prospect.otherworlds.bending.ability.CastTracker;
import net.prospect.otherworlds.bending.data.BendingData;
import net.prospect.otherworlds.bending.effect.ActiveEffects;
import net.prospect.otherworlds.bending.endurance.EnduranceData;
import net.prospect.otherworlds.bending.endurance.EnduranceState;
import net.prospect.otherworlds.bending.progression.ProgressionData;
import net.prospect.otherworlds.bending.progression.ProgressionState;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OtherWorlds.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BendingData>> BENDING =
            ATTACHMENT_TYPES.register("bending", () ->
                    AttachmentType.builder(() -> BendingData.DEFAULT)
                            .serialize(BendingData.CODEC)  // saved with the player
                            .copyOnDeath()                 // kept when the player dies
                            .build());

    // Progression: player level, element levels, move levels and learned moves. Saved, and kept on death
    // (hardcore mode wipes it in BendingEvents.onClone).
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ProgressionData>> PROGRESSION =
            ATTACHMENT_TYPES.register("progression", () ->
                    AttachmentType.builder(() -> ProgressionData.DEFAULT)
                            .serialize(ProgressionData.CODEC)
                            .copyOnDeath()
                            .build());

    // Not saved: move fatigue for the anti-spam XP reduction.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ProgressionState>> PROGRESSION_STATE =
            ATTACHMENT_TYPES.register("progression_state", () ->
                    AttachmentType.builder(ProgressionState::new).build());

    // Endurance: saved, and kept on death (the pool itself is refilled on respawn). Works on any living entity.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EnduranceData>> ENDURANCE =
            ATTACHMENT_TYPES.register("endurance", () ->
                    AttachmentType.builder(() -> EnduranceData.DEFAULT)
                            .serialize(EnduranceData.CODEC)
                            .copyOnDeath()
                            .build());

    // Not saved: last hit time, spar flag. Works on any living entity.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EnduranceState>> ENDURANCE_STATE =
            ATTACHMENT_TYPES.register("endurance_state", () ->
                    AttachmentType.builder(EnduranceState::new).build());

    // Not saved: cooldowns reset when the player relogs. Works on any living entity.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AbilityCooldowns>> COOLDOWNS =
            ATTACHMENT_TYPES.register("cooldowns", () ->
                    AttachmentType.builder(AbilityCooldowns::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ActiveEffects>> EFFECTS =
            ATTACHMENT_TYPES.register("effects", () ->
                    AttachmentType.builder(ActiveEffects::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CastTracker>> CASTING =
            ATTACHMENT_TYPES.register("casting", () ->
                    AttachmentType.builder(CastTracker::new).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}