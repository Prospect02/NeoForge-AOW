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

    // Endurance: base 50 points (25 hearts) and 0.5 points per second. Rolls, level and training modify these.
    public static final DeferredHolder<Attribute, Attribute> MAX_ENDURANCE = ATTRIBUTES.register("max_endurance",
            () -> new RangedAttribute("attribute.name.otherworlds.max_endurance", 50.0, 1.0, 100000.0)
                    .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> ENDURANCE_REGEN = ATTRIBUTES.register("endurance_regen",
            () -> new RangedAttribute("attribute.name.otherworlds.endurance_regen", 0.5, 0.0, 1000.0)
                    .setSyncable(true));

    // Share of bending damage that gets cut (0.25 = 25 percent). Custom Avatar armor will add to this.
    public static final DeferredHolder<Attribute, Attribute> BENDING_PROTECTION = ATTRIBUTES.register("bending_protection",
            () -> new RangedAttribute("attribute.name.otherworlds.bending_protection", 0.0, 0.0, 1.0)
                    .setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
        eventBus.addListener(ModAttributes::onAttributeModification);
    }

    // Players don't have custom attributes until we attach them here.
    // Custom NPCs that use Endurance can add the same attributes for their own entity type later,
    // or just use an EnduranceProfile instead.
    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_CHI);
        event.add(EntityType.PLAYER, CHI_REGEN);
        event.add(EntityType.PLAYER, MAX_ENDURANCE);
        event.add(EntityType.PLAYER, ENDURANCE_REGEN);
        event.add(EntityType.PLAYER, BENDING_PROTECTION);
    }
}