package net.prospect.otherworlds.network;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.prospect.otherworlds.client.ClientAnimations;
import net.prospect.otherworlds.client.ClientCameraLock;
import net.prospect.otherworlds.client.ClientCastState;
import net.prospect.otherworlds.client.ClientStuckEntities;

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

    public static void onCameraLock(CameraLockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCameraLock.set(payload.locked()));
    }

    public static void onWallStuck(WallStuckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStuckEntities.set(payload.entityId(), payload.stuck(),
                Direction.from2DDataValue(payload.wall()), payload.pos()));
    }
}