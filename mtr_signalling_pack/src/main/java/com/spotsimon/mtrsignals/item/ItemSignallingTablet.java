package com.spotsimon.mtrsignals.item;

import com.spotsimon.mtrsignals.MtrSignals;
import com.spotsimon.mtrsignals.block.BlockCountrySignal;
import com.spotsimon.mtrsignals.network.OpenTabletPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Signalling Tablet.
 * <ul>
 *     <li>Right-click a signal from this mod: select it and open the tablet screen for it.</li>
 *     <li>Right-click in the air: reopen the screen for the selected signal (works from a distance
 *     as long as the signal is loaded and within {@link #MAX_DISTANCE} blocks).</li>
 * </ul>
 */
public class ItemSignallingTablet extends Item {

	public static final double MAX_DISTANCE = 128;

	public ItemSignallingTablet(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level level = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		if (!(level.getBlockState(pos).getBlock() instanceof BlockCountrySignal)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
			context.getItemInHand().set(MtrSignals.SELECTED_SIGNAL.get(), pos);
			if (!openScreen(player, pos)) {
				player.displayClientMessage(Component.translatable("message.mtrsignals.tablet.not_found"), true);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			final BlockPos pos = stack.get(MtrSignals.SELECTED_SIGNAL.get());
			if (pos == null) {
				serverPlayer.displayClientMessage(Component.translatable("message.mtrsignals.tablet.no_selection"), true);
			} else if (!isInRange(serverPlayer, pos)) {
				serverPlayer.displayClientMessage(Component.translatable("message.mtrsignals.tablet.too_far"), true);
			} else if (!openScreen(serverPlayer, pos)) {
				serverPlayer.displayClientMessage(Component.translatable("message.mtrsignals.tablet.not_found"), true);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		final BlockPos pos = stack.get(MtrSignals.SELECTED_SIGNAL.get());
		if (pos == null) {
			tooltipComponents.add(Component.translatable("tooltip.mtrsignals.tablet.hint"));
		} else {
			tooltipComponents.add(Component.translatable("tooltip.mtrsignals.tablet.selected", pos.getX(), pos.getY(), pos.getZ()));
		}
	}

	public static boolean isInRange(ServerPlayer player, BlockPos pos) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_DISTANCE * MAX_DISTANCE;
	}

	/**
	 * @return false if there is no loaded signal of this mod at the position
	 */
	public static boolean openScreen(ServerPlayer player, BlockPos pos) {
		final ServerLevel level = player.serverLevel();
		if (!level.hasChunkAt(pos)) {
			return false;
		}
		final BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BlockCountrySignal.CountrySignalBlockEntity signal) {
			PacketDistributor.sendToPlayer(player, new OpenTabletPayload(pos, signal.getSystem().ordinal(), signal.getManualAspect()));
			return true;
		}
		return false;
	}
}
