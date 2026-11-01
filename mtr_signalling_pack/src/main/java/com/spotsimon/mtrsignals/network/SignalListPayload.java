package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.MtrSignals;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: all signals of the player's dimension.
 *
 * @param open         true to open the tablet screen, false to just refresh an already open screen
 * @param hasPreselect whether {@code preselect} is meaningful
 * @param preselect    a signal to select in the list
 */
public record SignalListPayload(List<SignalInfo> signals, boolean open, boolean hasPreselect, BlockPos preselect) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SignalListPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "signal_list"));
	public static final StreamCodec<ByteBuf, SignalListPayload> STREAM_CODEC = StreamCodec.composite(
			SignalInfo.STREAM_CODEC.apply(ByteBufCodecs.list(2048)), SignalListPayload::signals,
			ByteBufCodecs.BOOL, SignalListPayload::open,
			ByteBufCodecs.BOOL, SignalListPayload::hasPreselect,
			BlockPos.STREAM_CODEC, SignalListPayload::preselect,
			SignalListPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
