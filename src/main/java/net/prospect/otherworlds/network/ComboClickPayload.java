package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.prospect.otherworlds.OtherWorlds;

/** Client to server: the player clicked during a cast. left = true for left click, false for right. */
public record ComboClickPayload(boolean left) implements CustomPacketPayload {
    public static final Type<ComboClickPayload> TYPE = new Type<>(OtherWorlds.id("combo_click"));

    public static final StreamCodec<ByteBuf, ComboClickPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ComboClickPayload::new, ComboClickPayload::left);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}