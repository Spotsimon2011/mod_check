package com.spotsimon.mtrsignals.client;

import com.spotsimon.mtrsignals.signal.SignalAspect;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import com.spotsimon.mtrsignals.network.SetSignalPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Signalling Tablet screen: pick the signalling system of the selected signal and force one of
 * its aspects (or leave it on automatic, where MTR's train detection decides).
 * <p>
 * Styled after MTR's dashboard: flat dark panels, a blue-grey title bar and flat buttons.
 */
public class SignalTabletScreen extends Screen {

	private static final int WIDTH = 356;
	private static final int HEIGHT = 216;
	private static final int ROW = 22;

	private static final int COLOR_DIM = 0xB0000000;
	private static final int COLOR_PANEL = 0xFF1D262E;
	private static final int COLOR_TITLE = 0xFF2F4457;
	private static final int COLOR_BORDER = 0xFF405569;
	private static final int COLOR_BUTTON = 0xFF2A3743;
	private static final int COLOR_BUTTON_HOVER = 0xFF3A4D5F;
	private static final int COLOR_BUTTON_SELECTED = 0xFF3E7CB1;
	private static final int COLOR_TEXT = 0xFFFFFFFF;
	private static final int COLOR_TEXT_DIM = 0xFF9FB1C1;
	private static final int COLOR_HOUSING = 0xFF0B0F13;

	private final BlockPos signalPos;
	private SignalSystem system;
	private int manualAspect;
	private int left;
	private int top;

	public SignalTabletScreen(BlockPos signalPos, SignalSystem system, int manualAspect) {
		super(Component.translatable("gui.mtrsignals.tablet.title"));
		this.signalPos = signalPos;
		this.system = system;
		this.manualAspect = manualAspect;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;

		// Left column: signalling systems
		int y = top + 52;
		for (final SignalSystem option : SignalSystem.values()) {
			addRenderableWidget(new FlatButton(left + 10, y, 100, ROW - 2, Component.translatable(option.translationKey()), option == system, () -> selectSystem(option)));
			y += ROW;
		}

		// Middle column: automatic + the aspects of the selected system
		y = top + 52;
		addRenderableWidget(new FlatButton(left + 120, y, 176, ROW - 2, Component.translatable("gui.mtrsignals.tablet.automatic"), manualAspect < 0, () -> selectAspect(-1)));
		y += ROW;
		for (int i = 0; i < system.aspectCount(); i++) {
			final int index = i;
			addRenderableWidget(new FlatButton(left + 120, y, 176, ROW - 2, Component.translatable(system.aspectTranslationKey(i)), manualAspect == i, () -> selectAspect(index)));
			y += ROW;
		}
	}

	private void selectSystem(SignalSystem newSystem) {
		if (newSystem == system) {
			return;
		}
		system = newSystem;
		manualAspect = -1;
		sendToServer();
		rebuildWidgets();
	}

	private void selectAspect(int index) {
		manualAspect = index;
		sendToServer();
		rebuildWidgets();
	}

	private void sendToServer() {
		PacketDistributor.sendToServer(new SetSignalPayload(signalPos, system.ordinal(), manualAspect));
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(0, 0, width, height, COLOR_DIM);
		guiGraphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, COLOR_BORDER);
		guiGraphics.fill(left, top, left + WIDTH, top + HEIGHT, COLOR_PANEL);
		guiGraphics.fill(left, top, left + WIDTH, top + 28, COLOR_TITLE);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		final Font font = Minecraft.getInstance().font;

		guiGraphics.drawString(font, title, left + 10, top + 6, COLOR_TEXT, false);
		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.position", signalPos.getX(), signalPos.getY(), signalPos.getZ()), left + 10, top + 17, COLOR_TEXT_DIM, false);

		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.system"), left + 10, top + 38, COLOR_TEXT_DIM, false);
		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.aspect"), left + 120, top + 38, COLOR_TEXT_DIM, false);

		drawPreview(guiGraphics, left + 306, top + 40);
	}

	/**
	 * A small copy of the signal head showing what the selected aspect looks like.
	 */
	private void drawPreview(GuiGraphics guiGraphics, int x, int y) {
		final int lensSize = 30;
		final int gap = 6;
		final int pad = 5;
		final int width = lensSize + pad * 2;
		final int height = lensSize * 4 + gap * 3 + pad * 2;
		guiGraphics.fill(x, y, x + width, y + height, COLOR_HOUSING);
		guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, COLOR_BORDER);

		final boolean flashOn = (System.currentTimeMillis() / 500) % 2 == 0;
		final SignalAspect aspect = manualAspect >= 0 ? system.aspect(manualAspect) : null;
		for (int slot = 0; slot < 4; slot++) {
			final int centerX = x + width / 2;
			// slot 3 (top lens) is drawn first
			final int centerY = y + pad + (3 - slot) * (lensSize + gap) + lensSize / 2;
			boolean lit = false;
			if (aspect != null) {
				for (final SignalAspect.Lamp lamp : aspect.lamps()) {
					if (lamp.slot() == slot && (!lamp.flashing() || flashOn)) {
						lit = true;
					}
				}
			}
			final int lensColor = system.lensColor(slot);
			// unlit lenses keep a faint tint so the layout is still readable
			drawDisc(guiGraphics, centerX, centerY, lensSize / 2, lit ? lensColor : (lensColor & 0x00FFFFFF) | 0x40000000);
		}
	}

	private static void drawDisc(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			final int halfWidth = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
			guiGraphics.fill(centerX - halfWidth, centerY + dy, centerX + halfWidth, centerY + dy + 1, color);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class FlatButton extends Button {

		private final boolean selected;

		FlatButton(int x, int y, int width, int height, Component message, boolean selected, Runnable action) {
			super(x, y, width, height, message, button -> action.run(), DEFAULT_NARRATION);
			this.selected = selected;
		}

		@Override
		public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			final int background = selected ? COLOR_BUTTON_SELECTED : isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
			guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
			final Font font = Minecraft.getInstance().font;
			// keep long labels inside the button
			final String text = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 8);
			guiGraphics.drawString(font, text, getX() + 5, getY() + (getHeight() - 8) / 2, COLOR_TEXT, false);
		}
	}
}
