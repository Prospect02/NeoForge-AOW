package net.prospect.otherworlds.bending.scooter;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.prospect.otherworlds.OtherWorlds;

/** Client to server: "get me off the scooter". Registers itself, so ModNetwork does not change. */
@EventBusSubscriber(modid = OtherWorlds.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ScooterNetwork {
    private static final String PROTOCOL_VERSION = "1";   // the same as ModNetwork

    private ScooterNetwork() {}

    public record DismountPayload() implements CustomPacketPayload {
        public static final Type<DismountPayload> TYPE = new Type<>(OtherWorlds.id("scooter_dismount"));
        public static final StreamCodec<ByteBuf, DismountPayload> STREAM_CODEC = StreamCodec.unit(new DismountPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToServer(DismountPayload.TYPE, DismountPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.getVehicle() instanceof AirScooterEntity scooter
                            && scooter.getState() == AirScooterEntity.STATE_RIDING) {
                        scooter.dismount();
                    }
                }));
    }
}