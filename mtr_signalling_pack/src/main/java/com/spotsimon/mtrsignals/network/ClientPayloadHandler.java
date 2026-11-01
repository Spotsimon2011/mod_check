package com.spotsimon.mtrsignals.network;

import com.spotsimon.mtrsignals.client.ClientScreens;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Only ever invoked on the client. It deliberately mentions no Screen/Minecraft types, they live in
 * {@link ClientScreens}.
 */
public final class ClientPayloadHandler {

	private ClientPayloadHandler() {
	}

	public static void handleSignalList(SignalListPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientScreens.handleSignalList(payload.signals(), payload.open(), payload.hasPreselect() ? payload.preselect() : null));
	}
}
