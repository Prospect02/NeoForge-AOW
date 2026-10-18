package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.prospect.otherworlds.OtherWorlds;

/**
 * Server to client: is the combo (or charge) accepting clicks right now?
 * ability, step and total are for the HUD later; the click blocking only needs inputOpen.
 */
public record CastStatePayload(boolean inputOpen, ResourceLocation ability, int step, int total)
        implements CustomPacketPayload {
    public static final Type<CastStatePayload> TYPE = new Type<>(OtherWorlds.id("cast_state"));

    public static final StreamCodec<ByteBuf, CastStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CastStatePayload::inputOpen,
            ResourceLocation.STREAM_CODEC, CastStatePayload::ability,
            ByteBufCodecs.VAR_INT, CastStatePayload::step,
            ByteBufCodecs.VAR_INT, CastStatePayload::total,
            CastStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}