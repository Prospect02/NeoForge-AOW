package net.prospect.otherworlds.bending.scooter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.network.CastAnimationPayload;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public final class AirScooterEvents {
    private AirScooterEvents() {}

    /** The rider sits above the ball, so low ceilings would put their head in a block. That should not hurt. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL) && event.getEntity().getVehicle() instanceof AirScooterEntity) {
            event.setCanceled(true);
        }
    }

    /**
     * Crouching is vanilla's way off every vehicle, and here it should do nothing (Jump is the way off).
     * So a dismount while crouching is blocked, unless the scooter itself is doing it. Everything else still works:
     * running out of chi, dying, logging out and so on.
     */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide()) return;   // the server decides. Cancelling on the client would desync.
        if (!event.isDismounting()) return;
        if (!(event.getEntityBeingMounted() instanceof AirScooterEntity scooter)) return;

        Entity who = event.getEntityMounting();
        if (scooter.getState() != AirScooterEntity.STATE_RIDING) return;
        if (scooter.isDismountAllowed()) return;
        if (!who.isAlive() || !who.isShiftKeyDown()) return;

        event.setCanceled(true);
    }

    /** Someone walks into range of a rider: they were not there when the ride animation started, so send it now. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (event.getTarget() instanceof LivingEntity rider
                && rider.getVehicle() instanceof AirScooterEntity scooter
                && scooter.getState() == AirScooterEntity.STATE_RIDING) {
            PacketDistributor.sendToPlayer(viewer, new CastAnimationPayload(rider.getId(),
                    OtherWorlds.id(AirScooterEntity.ANIM_RIDE), true));
        }
    }
}