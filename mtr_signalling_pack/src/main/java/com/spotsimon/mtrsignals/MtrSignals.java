package com.spotsimon.mtrsignals;

import com.spotsimon.mtrsignals.block.BlockCountrySignal;
import com.spotsimon.mtrsignals.item.ItemSignallingTablet;
import com.spotsimon.mtrsignals.network.ClientPayloadHandler;
import com.spotsimon.mtrsignals.network.OpenTabletPayload;
import com.spotsimon.mtrsignals.network.ServerPayloadHandler;
import com.spotsimon.mtrsignals.network.SetSignalPayload;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

@Mod(MtrSignals.MOD_ID)
public class MtrSignals {

	public static final String MOD_ID = "mtrsignals";

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
	public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

	/**
	 * Every signal block of this mod: one single-sided and one double-sided per signalling system.
	 */
	public static final List<DeferredBlock<BlockCountrySignal>> SIGNAL_BLOCKS = new ArrayList<>();

	static {
		for (final SignalSystem system : SignalSystem.values()) {
			for (final boolean doubleSided : new boolean[]{false, true}) {
				final String name = "signal_" + system.id() + (doubleSided ? "_double" : "");
				final DeferredBlock<BlockCountrySignal> block = BLOCKS.registerBlock(
						name,
						properties -> new BlockCountrySignal(properties, system, doubleSided),
						BlockBehaviour.Properties.of().pushReaction(PushReaction.BLOCK).strength(3F)
				);
				ITEMS.registerSimpleBlockItem(block);
				SIGNAL_BLOCKS.add(block);
			}
		}
	}

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockCountrySignal.CountrySignalBlockEntity>> COUNTRY_SIGNAL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
			"country_signal",
			() -> BlockEntityType.Builder.of(
					BlockCountrySignal.CountrySignalBlockEntity::new,
					SIGNAL_BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new)
			).build(null)
	);

	/**
	 * The signal a tablet is currently linked to.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SELECTED_SIGNAL = DATA_COMPONENT_TYPES.register(
			"selected_signal",
			() -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build()
	);

	public static final DeferredItem<ItemSignallingTablet> SIGNALLING_TABLET = ITEMS.registerItem(
			"signalling_tablet",
			ItemSignallingTablet::new,
			new Item.Properties()
	);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(
			"main",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + MOD_ID))
					.icon(() -> new ItemStack(SIGNALLING_TABLET.get()))
					.displayItems((parameters, output) -> {
						output.accept(SIGNALLING_TABLET.get());
						SIGNAL_BLOCKS.forEach(block -> output.accept(block.get()));
					})
					.build()
	);

	public MtrSignals(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		BLOCK_ENTITY_TYPES.register(modEventBus);
		DATA_COMPONENT_TYPES.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);
		modEventBus.addListener(MtrSignals::registerPayloads);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(OpenTabletPayload.TYPE, OpenTabletPayload.STREAM_CODEC, ClientPayloadHandler::handleOpenTablet);
		registrar.playToServer(SetSignalPayload.TYPE, SetSignalPayload.STREAM_CODEC, ServerPayloadHandler::handleSetSignal);
	}
}
