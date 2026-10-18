package net.prospect.otherworlds.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.prospect.otherworlds.bending.ability.AbilityManager;
import net.prospect.otherworlds.bending.ability.Click;

public class ServerPayloadHandler {
    public static void onComboClick(ComboClickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AbilityManager.input(player, payload.left() ? Click.LEFT : Click.RIGHT);
            }
        });
    }
}