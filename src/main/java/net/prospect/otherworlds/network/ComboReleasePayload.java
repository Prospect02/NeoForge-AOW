package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.prospect.otherworlds.OtherWorlds;

/** Client to server: the player let go of a combo click. left = true for left click, false for right. */
public record ComboReleasePayload(boolean left) implements CustomPacketPayload {
    public static final Type<ComboReleasePayload> TYPE = new Type<>(OtherWorlds.id("combo_release"));

    public static final StreamCodec<ByteBuf, ComboReleasePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ComboReleasePayload::new, ComboReleasePayload::left);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}