package net.prospect.otherworlds.bending.endurance;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.damage.ModDamageTypes;
import net.prospect.otherworlds.registry.ModAttributes;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class EnduranceEvents {

    /** Before armor is worked out: lets the config turn down how much vanilla armor counts against bending. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(ModDamageTypes.BENDING)) return;
        double factor = Config.BENDING_VANILLA_ARMOR_FACTOR.get();
        if (factor >= 1.0) return;
        event.addReductionModifier(DamageContainer.Reduction.ARMOR,
                (container, reduction) -> (float) (reduction * factor));
    }

    /** After armor and enchantments: bending protection, then Endurance, then whatever is left reaches hearts. */
    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel)) return;

        float amount = event.getNewDamage();
        if (amount <= 0.0f) return;


        if (event.getSource().is(ModDamageTypes.BENDING)) {
            AttributeInstance protection = entity.getAttribute(ModAttributes.BENDING_PROTECTION);
            if (protection != null) {
                double cut = Mth.clamp(protection.getValue(), 0.0, Config.MAX_BENDING_PROTECTION.get());
                amount *= (float) (1.0 - cut);
            }
        }

        event.setNewDamage(EnduranceHandler.absorb(entity, event.getSource(), amount));
    }
}