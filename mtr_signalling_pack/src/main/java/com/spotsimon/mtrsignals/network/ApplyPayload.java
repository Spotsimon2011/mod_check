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
 * Client to server: change these signals.
 *
 * @param system {@link com.spotsimon.mtrsignals.signal.SignalSystem} ordinal, or -1 to keep each signal's system
 * @param manual index of the forced aspect, -1 for automatic, or -2 to keep each signal's aspect
 *               (a system change always resets to automatic when the aspect is kept)
 */
public record ApplyPayload(List<BlockPos> targets, int system, int manual) implements CustomPacketPayload {

	public static final int MAX_TARGETS = 1024;
	public static final CustomPacketPayload.Type<ApplyPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MtrSignals.MOD_ID, "apply"));
	public static final StreamCodec<ByteBuf, ApplyPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TARGETS)), ApplyPayload::targets,
			ByteBufCodecs.VAR_INT, ApplyPayload::system,
			ByteBufCodecs.VAR_INT, ApplyPayload::manual,
			ApplyPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
