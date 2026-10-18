package net.prospect.otherworlds.bending;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.chi.ChiHandler;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class BendingEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ChiHandler.initialize(player);

        OtherWorlds.LOGGER.info("[TEST] chi={} / max={} (regen {}/s)",
                ChiHandler.round2(ChiHandler.getChi(player)), ChiHandler.getMaxChi(player), ChiHandler.getRegen(player));
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

        // TEMPORARY TEST: log every 5 seconds. Delete after the checkpoint.
        if (player.tickCount % 100 == 0) {
            OtherWorlds.LOGGER.info("[TEST] chi={} / max={} (regen {}/s)",
                    ChiHandler.getChi(player), ChiHandler.getMaxChi(player), ChiHandler.getRegen(player));
        }
    }
}