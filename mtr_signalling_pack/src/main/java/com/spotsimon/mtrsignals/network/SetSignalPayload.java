package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.MtrSignals;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the tablet screen wants the signal at {@code pos} to use this system and forced aspect.
 *
 * @param system the {@link com.spotsimon.mtrsignals.signal.SignalSystem} ordinal
 * @param manual the forced aspect index, or -1 for automatic
 */
public record SetSignalPayload(BlockPos pos, int system, int manual) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SetSignalPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "set_signal"));
	public static final StreamCodec<ByteBuf, SetSignalPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SetSignalPayload::pos,
			ByteBufCodecs.VAR_INT, SetSignalPayload::system,
			ByteBufCodecs.VAR_INT, SetSignalPayload::manual,
			SetSignalPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
