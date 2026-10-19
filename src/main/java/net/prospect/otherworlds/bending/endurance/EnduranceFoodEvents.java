package net.prospect.otherworlds.bending.endurance;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.prospect.otherworlds.OtherWorlds;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class EnduranceFoodEvents {

    /** Fires when something finishes eating or drinking. */
    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        EnduranceHandler.onEat(entity, event.getItem());
    }
}