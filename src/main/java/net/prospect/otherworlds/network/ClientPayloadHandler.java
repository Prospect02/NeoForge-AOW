package net.prospect.otherworlds.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.prospect.otherworlds.client.ClientAnimations;
import net.prospect.otherworlds.client.ClientCastState;

public class ClientPayloadHandler {
    public static void onCastState(CastStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientCastState.set(payload.inputOpen(), payload.ability(), payload.step(), payload.total()));
    }

    public static void onCastAnimation(CastAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.play()) ClientAnimations.play(payload.entityId(), payload.animation());
            else ClientAnimations.stop(payload.entityId());
        });
    }
}