package com.spotsimon.mtrsignals.client;

import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client-only entry points. Common code only passes primitives/plain objects in here, so no screen
 * class is touched when a dedicated server loads the network handlers.
 */
public final class ClientScreens {

	private ClientScreens() {
	}

	public static void openSignalTablet(BlockPos pos, int systemOrdinal, int manualAspect) {
		Minecraft.getInstance().setScreen(new SignalTabletScreen(pos, SignalSystem.byOrdinal(systemOrdinal), manualAspect));
	}
}
