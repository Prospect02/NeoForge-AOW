package net.prospect.otherworlds.bending;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.data.BendingData;
import net.prospect.otherworlds.registry.ModAttachments;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class BendingEvents {

    // TEMPORARY TEST: replace this method in part B.
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BendingData data = player.getData(ModAttachments.BENDING);

        // Temporary rolls (-1..1) so you can see them being saved
        if (!data.rollsAssigned()) {
            var random = player.getRandom();
            data = data.withRolls(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f);
            player.setData(ModAttachments.BENDING, data);
        }

        OtherWorlds.LOGGER.info("[TEST] {}: chi={}, maxChiRoll={}, regenRoll={}, assigned={}",
                player.getName().getString(), data.chi(), data.maxChiRoll(),
                data.regenRoll(), data.rollsAssigned());
    }
}