package net.prospect.otherworlds.bending;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.AbilityManager;
import net.prospect.otherworlds.bending.ability.EndReason;
import net.prospect.otherworlds.bending.chi.ChiHandler;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class BendingEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ChiHandler.initialize(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ChiHandler.onRespawn(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ChiHandler.refreshAttributes(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ChiHandler.tick(player);
        AbilityManager.tick(player);
        AbilityManager.sync(player);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0) AbilityManager.onHurt(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        AbilityManager.stop(event.getEntity(), EndReason.CANCELLED);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbilityManager.stop(player, EndReason.CANCELLED);
    }
}