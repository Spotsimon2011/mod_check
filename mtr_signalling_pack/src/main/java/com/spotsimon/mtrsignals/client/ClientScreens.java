package com.spotsimon.mtrsignals.client;

import com.spotsimon.mtrsignals.network.SignalInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Client-only entry points. Common code only passes plain objects in here, so no screen
 * class is touched when a dedicated server loads the network handlers.
 */
public final class ClientScreens {

	private ClientScreens() {
	}

	/**
	 * @param open      true to open the tablet screen, false to refresh the one that is already open
	 * @param preselect a signal to select in the list, or null
	 */
	public static void handleSignalList(List<SignalInfo> signals, boolean open, BlockPos preselect) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (open) {
			minecraft.setScreen(new SignalTabletScreen(signals, preselect));
		} else if (minecraft.screen instanceof SignalTabletScreen screen) {
			screen.updateSignals(signals);
		}
	}
}
