package com.spotsimon.mtrsignals.item;

import com.spotsimon.mtrsignals.MtrSignals;
import com.spotsimon.mtrsignals.block.BlockCountrySignal;
import com.spotsimon.mtrsignals.network.ServerPayloadHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

import java.util.List;

/**
 * The Signalling Tablet.
 * <ul>
 *     <li>Use it (right-click in the air or on any block) to open the list of all signals in the current
 *     dimension. Select one or several, then change their system or aspect.</li>
 *     <li>Right-click a signal from this mod: also pre-selects that signal in the list.</li>
 * </ul>
 */
public class ItemSignallingTablet extends Item {

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
			ServerPayloadHandler.sendList(player, true, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			ServerPayloadHandler.sendList(serverPlayer, true, stack.get(MtrSignals.SELECTED_SIGNAL.get()));
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("tooltip.mtrsignals.tablet.hint"));
		final BlockPos pos = stack.get(MtrSignals.SELECTED_SIGNAL.get());
		if (pos != null) {
			tooltipComponents.add(Component.translatable("tooltip.mtrsignals.tablet.last", pos.getX(), pos.getY(), pos.getZ()));
		}
	}
}
