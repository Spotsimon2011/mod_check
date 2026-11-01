package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.MtrSignals;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: give a signal a name.
 */
public record RenamePayload(BlockPos pos, String name) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<RenamePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "rename"));
	public static final StreamCodec<ByteBuf, RenamePayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, RenamePayload::pos,
			ByteBufCodecs.stringUtf8(64), RenamePayload::name,
			RenamePayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
