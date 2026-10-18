package net.prospect.otherworlds.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ComboClickPayload.TYPE, ComboClickPayload.STREAM_CODEC,
                ServerPayloadHandler::onComboClick);
        registrar.playToServer(ComboReleasePayload.TYPE, ComboReleasePayload.STREAM_CODEC,
                ServerPayloadHandler::onComboRelease);
        registrar.playToClient(CastStatePayload.TYPE, CastStatePayload.STREAM_CODEC,
                ClientPayloadHandler::onCastState);
        registrar.playToClient(CastAnimationPayload.TYPE, CastAnimationPayload.STREAM_CODEC,
                ClientPayloadHandler::onCastAnimation);
    }
}