package com.spotsimon.mtrsignals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the tablet shows about one signal.
 *
 * @param name   player-given name, may be empty
 * @param system {@link com.spotsimon.mtrsignals.signal.SignalSystem} ordinal
 * @param manual index of the forced aspect, or -1 for automatic
 * @param loaded whether the signal's chunk is loaded (only loaded signals can be changed)
 */
public record SignalInfo(BlockPos pos, String name, int system, int manual, boolean loaded) {

	public static final StreamCodec<ByteBuf, SignalInfo> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SignalInfo::pos,
			ByteBufCodecs.stringUtf8(64), SignalInfo::name,
			ByteBufCodecs.VAR_INT, SignalInfo::system,
			ByteBufCodecs.VAR_INT, SignalInfo::manual,
			ByteBufCodecs.BOOL, SignalInfo::loaded,
			SignalInfo::new
	);
}
