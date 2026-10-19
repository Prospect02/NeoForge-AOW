package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.prospect.otherworlds.OtherWorlds;

/** Server to client: lock or unlock the local player's camera (stuck on a wall, and later other holds). */
public record CameraLockPayload(boolean locked) implements CustomPacketPayload {
    public static final Type<CameraLockPayload> TYPE = new Type<>(OtherWorlds.id("camera_lock"));

    public static final StreamCodec<ByteBuf, CameraLockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CameraLockPayload::locked,
            CameraLockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}