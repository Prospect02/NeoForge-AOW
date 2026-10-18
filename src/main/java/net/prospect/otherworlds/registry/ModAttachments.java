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

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OtherWorlds.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BendingData>> BENDING =
            ATTACHMENT_TYPES.register("bending", () ->
                    AttachmentType.builder(() -> BendingData.DEFAULT)
                            .serialize(BendingData.CODEC)  // saved with the player
                            .copyOnDeath()                 // kept when the player dies
                            .build());

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