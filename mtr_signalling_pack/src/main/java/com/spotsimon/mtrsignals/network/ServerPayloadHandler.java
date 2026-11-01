package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.block.BlockCountrySignal;
import com.spotsimon.mtrsignals.item.ItemSignallingTablet;
import com.spotsimon.mtrsignals.registry.SignalRegistry;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ServerPayloadHandler {

	private static final int MAX_LISTED = 2048;

	private ServerPayloadHandler() {
	}

	public static void handleRequestList(RequestListPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player && isHoldingTablet(player)) {
				sendList(player, false, null);
			}
		});
	}

	public static void handleApply(ApplyPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player) || !isHoldingTablet(player)) {
				return;
			}
			final ServerLevel level = player.serverLevel();
			int processed = 0;
			for (final BlockPos pos : payload.targets()) {
				if (processed++ >= ApplyPayload.MAX_TARGETS) {
					break;
				}
				if (!level.hasChunkAt(pos)) {
					continue;
				}
				final BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof BlockCountrySignal.CountrySignalBlockEntity signal) {
					final SignalSystem newSystem = payload.system() >= 0 ? SignalSystem.byOrdinal(payload.system()) : signal.getSystem();
					final int newManual;
					if (payload.manual() == -2) {
						newManual = newSystem == signal.getSystem() ? signal.getManualAspect() : -1;
					} else {
						newManual = payload.manual();
					}
					signal.configure(newSystem, newManual);
				}
			}
			sendList(player, false, null);
		});
	}

	public static void handleRename(RenamePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player && isHoldingTablet(player)) {
				SignalRegistry.get(player.serverLevel()).rename(payload.pos(), payload.name());
				sendList(player, false, null);
			}
		});
	}

	public static boolean isHoldingTablet(ServerPlayer player) {
		return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ItemSignallingTablet
				|| player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ItemSignallingTablet;
	}

	/**
	 * Sends every known signal of the player's dimension, nearest first.
	 *
	 * @param open      true to open the tablet screen, false to refresh an open one
	 * @param preselect a signal to select in the list, or null
	 */
	public static void sendList(ServerPlayer player, boolean open, BlockPos preselect) {
		final ServerLevel level = player.serverLevel();
		final List<SignalInfo> infos = new ArrayList<>();
		for (final Map.Entry<BlockPos, SignalRegistry.Entry> mapEntry : SignalRegistry.get(level).getEntries().entrySet()) {
			final BlockPos pos = mapEntry.getKey();
			final SignalRegistry.Entry entry = mapEntry.getValue();
			final int manual = entry.manualId.isEmpty() ? -1 : entry.system.indexOfAspect(entry.manualId);
			infos.add(new SignalInfo(pos, entry.name, entry.system.ordinal(), manual, level.hasChunkAt(pos)));
		}
		final BlockPos playerPos = player.blockPosition();
		infos.sort(Comparator.comparingDouble(info -> info.pos().distSqr(playerPos)));
		final List<SignalInfo> limited = infos.size() > MAX_LISTED ? new ArrayList<>(infos.subList(0, MAX_LISTED)) : infos;
		PacketDistributor.sendToPlayer(player, new SignalListPayload(limited, open, preselect != null, preselect == null ? BlockPos.ZERO : preselect));
	}
}
