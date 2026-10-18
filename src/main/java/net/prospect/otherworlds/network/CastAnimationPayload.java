package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.prospect.otherworlds.OtherWorlds;

/** Server to client: start (play = true) or stop (play = false) a move's animation on an entity. */
public record CastAnimationPayload(int entityId, ResourceLocation animation, boolean play)
        implements CustomPacketPayload {
    public static final Type<CastAnimationPayload> TYPE = new Type<>(OtherWorlds.id("cast_animation"));

    public static final StreamCodec<ByteBuf, CastAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastAnimationPayload::entityId,
            ResourceLocation.STREAM_CODEC, CastAnimationPayload::animation,
            ByteBufCodecs.BOOL, CastAnimationPayload::play,
            CastAnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}