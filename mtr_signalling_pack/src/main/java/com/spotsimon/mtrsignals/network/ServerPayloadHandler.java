package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.block.BlockCountrySignal;
import com.spotsimon.mtrsignals.item.ItemSignallingTablet;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {

	private ServerPayloadHandler() {
	}

	public static void handleSetSignal(SetSignalPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) {
				return;
			}
			// The player has to actually hold a tablet
			if (!(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ItemSignallingTablet)
					&& !(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ItemSignallingTablet)) {
				return;
			}
			final BlockPos pos = payload.pos();
			if (!ItemSignallingTablet.isInRange(player, pos)) {
				return;
			}
			final ServerLevel level = player.serverLevel();
			if (!level.hasChunkAt(pos)) {
				return;
			}
			final BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof BlockCountrySignal.CountrySignalBlockEntity signal) {
				signal.configure(SignalSystem.byOrdinal(payload.system()), payload.manual());
			}
		});
	}
}
