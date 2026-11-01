package com.spotsimon.mtrsignals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.spotsimon.mtrsignals.block.BlockCountrySignal.CountrySignalBlockEntity;
import com.spotsimon.mtrsignals.signal.SignalAspect;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockSignalBase;
import org.mtr.client.IDrawing;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.RenderSignalBase;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

/**
 * Draws the lamps of a {@link CountrySignalBlockEntity} the same way MTR draws its own 4-aspect
 * signal (same lens positions, same light layer), only with the lamp pattern of the signal's
 * national system.
 * <p>
 * In automatic mode MTR's {@link RenderSignalBase} does everything (rail lookup, train detection,
 * redstone) and calls {@link #render(StoredMatrixTransformations, CountrySignalBlockEntity, ClientLevel, float, int, int, boolean)}
 * with MTR's aspect value. When the tablet forces an aspect, the signal is drawn directly so that it
 * works even if the signal is not connected to a rail.
 */
public class CountrySignalRenderer extends RenderSignalBase<CountrySignalBlockEntity> {

	private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/white.png");
	private static final int FULL_BRIGHT = 15728880;

	public CountrySignalRenderer() {
		// Same values as MTR's RenderSignalLight4Aspect: (colour indicator height, number of aspects)
		super(16, 4);
	}

	@Override
	public void render(CountrySignalBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource multiBufferSource, ClientLevel level, LocalPlayer player, float partialTick, int light, int overlay) {
		if (blockEntity.getManualAspect() < 0) {
			super.render(blockEntity, poseStack, multiBufferSource, level, player, partialTick, light, overlay);
			return;
		}

		final BlockPos blockPos = blockEntity.getBlockPos();
		final BlockState blockState = level.getBlockState(blockPos);
		if (!(blockState.getBlock() instanceof BlockSignalBase)) {
			return;
		}

		final float baseAngle = BlockSignalBase.getAngle(blockState);
		final StoredMatrixTransformations baseTransformations = new StoredMatrixTransformations(0.5 + blockPos.getX(), blockPos.getY(), 0.5 + blockPos.getZ());
		final int sides = blockEntity.isDoubleSided ? 2 : 1;
		for (int side = 0; side < sides; side++) {
			final float angle = baseAngle + side * 180;
			final StoredMatrixTransformations transformations = baseTransformations.copy();
			transformations.add(newPoseStack -> Drawing.rotateYDegrees(newPoseStack, -angle));
			render(transformations, blockEntity, level, partialTick, light, CountrySignalBlockEntity.MANUAL_BASE + blockEntity.getManualAspect(), side == 1);
		}
	}

	@Override
	protected void render(StoredMatrixTransformations transformations, CountrySignalBlockEntity blockEntity, ClientLevel level, float partialTick, int light, int aspect, boolean isBackSide) {
		final SignalSystem system = blockEntity.getSystem();
		final SignalAspect signalAspect = aspect >= CountrySignalBlockEntity.MANUAL_BASE
				? system.aspect(aspect - CountrySignalBlockEntity.MANUAL_BASE)
				: system.autoAspect(aspect);
		final long now = System.currentTimeMillis();

		for (final SignalAspect.Lamp lamp : signalAspect.lamps()) {
			if (!lamp.isLit(now)) {
				continue;
			}
			// Lens positions of MTR's 4-lens housing: 0.03125 + 0.25 per slot
			final float y = 0.03125F + 0.25F * lamp.slot();
			final int color = system.lensColor(lamp.slot());
			MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT, (poseStack, vertexConsumer, offset) -> {
				transformations.transform(poseStack, offset);
				IDrawing.drawTexture(poseStack, vertexConsumer, -0.09375F, y, -0.19375F, 0.09375F, y + 0.1875F, -0.19375F, Direction.UP, color, FULL_BRIGHT);
				poseStack.popPose();
			});
		}

		// Speed bars (ZSR/SZ) sit in the margin of the housing beside the lowest lens
		for (final SignalAspect.Bar bar : signalAspect.bars()) {
			final float x1 = bar.right() ? 0.10F : -0.18F;
			final float x2 = bar.right() ? 0.18F : -0.10F;
			final int color = bar.color();
			MainRenderer.scheduleRender(WHITE_TEXTURE, false, QueuedRenderLayer.LIGHT, (poseStack, vertexConsumer, offset) -> {
				transformations.transform(poseStack, offset);
				IDrawing.drawTexture(poseStack, vertexConsumer, x1, 0.03125F, -0.19375F, x2, 0.21875F, -0.19375F, Direction.UP, color, FULL_BRIGHT);
				poseStack.popPose();
			});
		}
	}
}
