package com.spotsimon.mtrsignals.client;

import com.spotsimon.mtrsignals.MtrSignals;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MtrSignals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MtrSignalsClient {

	private MtrSignalsClient() {
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(MtrSignals.COUNTRY_SIGNAL_BLOCK_ENTITY.get(), context -> new CountrySignalRenderer());
	}
}
