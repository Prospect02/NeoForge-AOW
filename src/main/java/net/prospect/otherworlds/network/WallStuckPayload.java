package net.prospect.otherworlds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.prospect.otherworlds.OtherWorlds;

/**
 * Server to client: an entity is stuck on a wall, or got free.
 * wall is the side of the wall it is against, as Direction.get2DDataValue(). pos is the block it is stuck in.
 * Animations and wall marks read this later.
 */
public record WallStuckPayload(int entityId, boolean stuck, int wall, BlockPos pos)
        implements CustomPacketPayload {
    public static final Type<WallStuckPayload> TYPE = new Type<>(OtherWorlds.id("wall_stuck"));

    public static final StreamCodec<ByteBuf, WallStuckPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WallStuckPayload::entityId,
            ByteBufCodecs.BOOL, WallStuckPayload::stuck,
            ByteBufCodecs.VAR_INT, WallStuckPayload::wall,
            BlockPos.STREAM_CODEC, WallStuckPayload::pos,
            WallStuckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}