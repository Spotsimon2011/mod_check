package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.MtrSignals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: send me the signal list again.
 */
public record RequestListPayload() implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RequestListPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "request_list"));
	public static final StreamCodec<ByteBuf, RequestListPayload> STREAM_CODEC = StreamCodec.unit(new RequestListPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
