package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.MtrSignals;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: open the Signalling Tablet screen for the signal at {@code pos}.
 *
 * @param system the signal's {@link com.spotsimon.mtrsignals.signal.SignalSystem} ordinal
 * @param manual the forced aspect index, or -1 for automatic
 */
public record OpenTabletPayload(BlockPos pos, int system, int manual) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<OpenTabletPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "open_tablet"));
	public static final StreamCodec<ByteBuf, OpenTabletPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenTabletPayload::pos,
			ByteBufCodecs.VAR_INT, OpenTabletPayload::system,
			ByteBufCodecs.VAR_INT, OpenTabletPayload::manual,
			OpenTabletPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
