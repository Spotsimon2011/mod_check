package com.spotsimon.mtrsignals.block;

import com.spotsimon.mtrsignals.MtrSignals;
import com.spotsimon.mtrsignals.item.ItemSignallingTablet;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.mtr.block.BlockSignalBase;
import org.mtr.block.BlockSignalLightBase;

/**
 * A light signal that uses MTR's own signal logic (rail connection, train detection, redstone,
 * housing models) but shows the aspects of a national signalling system.
 * <p>
 * The shape values (3, 16) are the same ones MTR passes for its 4-aspect signal.
 */
public class BlockCountrySignal extends BlockSignalLightBase {

	private final SignalSystem defaultSystem;
	private final boolean doubleSided;

	public BlockCountrySignal(BlockBehaviour.Properties properties, SignalSystem defaultSystem, boolean doubleSided) {
		super(properties, 3, 16);
		this.defaultSystem = defaultSystem;
		this.doubleSided = doubleSided;
	}

	public SignalSystem getDefaultSystem() {
		return defaultSystem;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CountrySignalBlockEntity(pos, state);
	}

	/**
	 * Let the Signalling Tablet's own use handler run instead of any default block interaction.
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (stack.getItem() instanceof ItemSignallingTablet) {
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	public static class CountrySignalBlockEntity extends BlockSignalBase.BlockEntityBase {

		/**
		 * Values at or above this are "forced aspect" markers passed to the renderer
		 * (value - MANUAL_BASE = index of the forced aspect).
		 */
		public static final int MANUAL_BASE = 100;
		private static final String KEY_SYSTEM = "mtrsignals_system";
		private static final String KEY_MANUAL = "mtrsignals_manual";

		private SignalSystem system;
		private int manualAspect = -1;

		public CountrySignalBlockEntity(BlockPos pos, BlockState state) {
			super(MtrSignals.COUNTRY_SIGNAL_BLOCK_ENTITY.get(), isDoubleSided(state), pos, state);
			system = state.getBlock() instanceof BlockCountrySignal block ? block.getDefaultSystem() : SignalSystem.POLAND;
		}

		private static boolean isDoubleSided(BlockState state) {
			return state.getBlock() instanceof BlockCountrySignal block && block.isDoubleSided();
		}

		public SignalSystem getSystem() {
			return system;
		}

		/**
		 * @return the index of the forced aspect, or -1 when the signal is automatic
		 */
		public int getManualAspect() {
			return manualAspect;
		}

		/**
		 * Server-side: change the system and forced aspect, then sync to clients.
		 */
		public void configure(SignalSystem newSystem, int newManualAspect) {
			system = newSystem;
			manualAspect = newManualAspect >= 0 && newManualAspect < newSystem.aspectCount() ? newManualAspect : -1;
			setChanged();
			final Level level = getLevel();
			if (level != null && !level.isClientSide()) {
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
			}
		}

		/**
		 * MTR calls this every frame on the client to decide what the signal shows. Keep MTR's
		 * timing bookkeeping running, but report a forced aspect when the tablet has set one.
		 */
		@Override
		public int getActualAspect(boolean occupied, boolean isBackSide) {
			final int automaticAspect = super.getActualAspect(occupied, isBackSide);
			return manualAspect >= 0 ? MANUAL_BASE + manualAspect : automaticAspect;
		}

		@Override
		protected void readNbt(CompoundTag compoundTag) {
			super.readNbt(compoundTag);
			if (compoundTag.contains(KEY_SYSTEM)) {
				system = SignalSystem.byId(compoundTag.getString(KEY_SYSTEM));
			}
			manualAspect = compoundTag.contains(KEY_MANUAL) ? compoundTag.getInt(KEY_MANUAL) : -1;
			if (manualAspect >= system.aspectCount()) {
				manualAspect = -1;
			}
		}

		@Override
		protected void writeNbt(CompoundTag compoundTag) {
			super.writeNbt(compoundTag);
			compoundTag.putString(KEY_SYSTEM, system.id());
			compoundTag.putInt(KEY_MANUAL, manualAspect);
		}
	}
}
