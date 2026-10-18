package net.prospect.otherworlds.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, OtherWorlds.MODID);

    // Base values: 100 max chi, 2 chi per second. Every player's rolls modify these.
    public static final DeferredHolder<Attribute, Attribute> MAX_CHI = ATTRIBUTES.register("max_chi",
            () -> new RangedAttribute("attribute.name.otherworlds.max_chi", 100.0, 1.0, 10000.0)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> CHI_REGEN = ATTRIBUTES.register("chi_regen",
            () -> new RangedAttribute("attribute.name.otherworlds.chi_regen", 2.0, 0.0, 1000.0)
                    .setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
        eventBus.addListener(ModAttributes::onAttributeModification);
    }

    // Players don't have custom attributes until we attach them here.
    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_CHI);
        event.add(EntityType.PLAYER, CHI_REGEN);
    }
}