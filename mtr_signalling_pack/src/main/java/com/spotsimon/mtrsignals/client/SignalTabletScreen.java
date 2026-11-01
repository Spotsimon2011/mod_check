package com.spotsimon.mtrsignals.client;

import com.spotsimon.mtrsignals.network.ApplyPayload;
import com.spotsimon.mtrsignals.network.RenamePayload;
import com.spotsimon.mtrsignals.network.RequestListPayload;
import com.spotsimon.mtrsignals.network.SignalInfo;
import com.spotsimon.mtrsignals.signal.SignalAspect;
import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * The Signalling Tablet screen.
 * <ul>
 *     <li>Left: every signal of the dimension. Click rows to select them (Shift-click selects a range).</li>
 *     <li>Right: pick a signalling system and an aspect (or Automatic). It is applied to all selected signals.</li>
 * </ul>
 * Styled after MTR's dashboard: flat dark panels, a blue-grey title bar and flat buttons.
 */
public class SignalTabletScreen extends Screen {

	private static final int WIDTH = 480;
	private static final int HEIGHT = 256;
	private static final int SIGNAL_ROW = 22;
	private static final int SIGNAL_ROWS = 8;
	private static final int ASPECT_ROW = 18;
	private static final int ASPECT_ROWS = 6;
	private static final int REFRESH_TICKS = 60;

	private static final int COLOR_DIM = 0xB0000000;
	private static final int COLOR_PANEL = 0xFF1D262E;
	private static final int COLOR_TITLE = 0xFF2F4457;
	private static final int COLOR_BORDER = 0xFF405569;
	private static final int COLOR_ROW = 0xFF26323D;
	private static final int COLOR_ROW_HOVER = 0xFF33444F;
	private static final int COLOR_ROW_DISABLED = 0xFF1A2128;
	private static final int COLOR_SELECTED = 0xFF2F5F8A;
	private static final int COLOR_BUTTON = 0xFF2A3743;
	private static final int COLOR_BUTTON_HOVER = 0xFF3A4D5F;
	private static final int COLOR_BUTTON_SELECTED = 0xFF3E7CB1;
	private static final int COLOR_TEXT = 0xFFFFFFFF;
	private static final int COLOR_TEXT_DIM = 0xFF9FB1C1;
	private static final int COLOR_TEXT_DISABLED = 0xFF5E6B77;
	private static final int COLOR_CHECK = 0xFF6CC3FF;

	private List<SignalInfo> allSignals;
	private final List<SignalInfo> shownSignals = new ArrayList<>();
	private final Set<BlockPos> selected = new LinkedHashSet<>();

	private SignalSystem targetSystem = SignalSystem.POLAND;
	/**
	 * Aspect shown as active in the aspect list: an index, -1 = automatic, -2 = none (selection is mixed).
	 */
	private int targetManual = -1;

	private String filterText = "";
	private String nameText = "";
	private Component message = Component.empty();
	private long messageUntil;
	private int lastClickedIndex = -1;
	private int ticks;

	private int left;
	private int top;
	private int signalScroll;
	private int aspectScroll;
	private EditBox filterBox;
	private EditBox nameBox;

	public SignalTabletScreen(List<SignalInfo> signals, BlockPos preselect) {
		super(Component.translatable("gui.mtrsignals.tablet.title"));
		allSignals = new ArrayList<>(signals);
		refilter();
		if (preselect != null) {
			for (int i = 0; i < shownSignals.size(); i++) {
				final SignalInfo info = shownSignals.get(i);
				if (info.pos().equals(preselect) && info.loaded()) {
					selected.add(info.pos());
					signalScroll = Math.max(0, i - 2);
					break;
				}
			}
		}
		refreshTarget();
		refreshNameFromSelection();
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;

		filterBox = new EditBox(font, left + 10, top + 36, 124, 16, Component.translatable("gui.mtrsignals.tablet.filter"));
		filterBox.setMaxLength(32);
		filterBox.setHint(Component.translatable("gui.mtrsignals.tablet.filter"));
		filterBox.setValue(filterText);
		filterBox.setResponder(text -> {
			filterText = text;
			refilter();
		});
		addRenderableWidget(filterBox);

		addRenderableWidget(new FlatButton(left + 137, top + 36, 40, 16, Component.translatable("gui.mtrsignals.tablet.all"), () -> false, this::selectAll));
		addRenderableWidget(new FlatButton(left + 180, top + 36, 40, 16, Component.translatable("gui.mtrsignals.tablet.none"), () -> false, this::selectNone));

		nameBox = new EditBox(font, left + 10, top + 236, 160, 16, Component.translatable("gui.mtrsignals.tablet.name"));
		nameBox.setMaxLength(32);
		nameBox.setHint(Component.translatable("gui.mtrsignals.tablet.name"));
		nameBox.setValue(nameText);
		nameBox.setResponder(text -> nameText = text);
		addRenderableWidget(nameBox);
		addRenderableWidget(new FlatButton(left + 173, top + 236, 57, 16, Component.translatable("gui.mtrsignals.tablet.rename"), () -> false, this::rename));

		final SignalSystem[] systems = SignalSystem.values();
		for (int i = 0; i < systems.length; i++) {
			final SignalSystem system = systems[i];
			final int x = left + 246 + (i % 2) * 115;
			final int y = top + 50 + (i / 2) * 18;
			addRenderableWidget(new FlatButton(x, y, 109, 16, Component.translatable(system.translationKey()), () -> targetSystem == system, () -> applySystem(system)));
		}
	}

	/**
	 * Called with a fresh list from the server (after a change, or periodically).
	 */
	public void updateSignals(List<SignalInfo> signals) {
		allSignals = new ArrayList<>(signals);
		final Set<BlockPos> loaded = new LinkedHashSet<>();
		for (final SignalInfo info : allSignals) {
			if (info.loaded()) {
				loaded.add(info.pos());
			}
		}
		selected.retainAll(loaded);
		refilter();
		refreshTarget();
	}

	private void refilter() {
		shownSignals.clear();
		final String needle = filterText.strip().toLowerCase(Locale.ROOT);
		for (final SignalInfo info : allSignals) {
			if (needle.isEmpty() || searchText(info).contains(needle)) {
				shownSignals.add(info);
			}
		}
		signalScroll = Math.max(0, Math.min(signalScroll, Math.max(0, shownSignals.size() - SIGNAL_ROWS)));
	}

	private static String searchText(SignalInfo info) {
		final SignalSystem system = SignalSystem.byOrdinal(info.system());
		return (info.name() + " " + info.pos().getX() + " " + info.pos().getY() + " " + info.pos().getZ() + " "
				+ Component.translatable(system.translationKey()).getString()).toLowerCase(Locale.ROOT);
	}

	private SignalInfo find(BlockPos pos) {
		for (final SignalInfo info : allSignals) {
			if (info.pos().equals(pos)) {
				return info;
			}
		}
		return null;
	}

	/**
	 * Make the system and aspect lists show the state of the selected signals.
	 */
	private void refreshTarget() {
		SignalInfo first = null;
		boolean mixedSystem = false;
		boolean mixedAspect = false;
		for (final BlockPos pos : selected) {
			final SignalInfo info = find(pos);
			if (info == null) {
				continue;
			}
			if (first == null) {
				first = info;
			} else {
				mixedSystem |= info.system() != first.system();
				mixedAspect |= info.manual() != first.manual();
			}
		}
		if (first != null) {
			targetSystem = SignalSystem.byOrdinal(first.system());
			targetManual = mixedSystem || mixedAspect ? -2 : first.manual();
		}
	}

	private void refreshNameFromSelection() {
		String name = "";
		if (selected.size() == 1) {
			final SignalInfo info = find(selected.iterator().next());
			name = info == null ? "" : info.name();
		}
		nameText = name;
		if (nameBox != null) {
			nameBox.setValue(name);
		}
	}

	private void selectionChanged() {
		refreshTarget();
		refreshNameFromSelection();
	}

	private void selectAll() {
		selected.clear();
		for (final SignalInfo info : shownSignals) {
			if (info.loaded()) {
				selected.add(info.pos());
			}
		}
		selectionChanged();
	}

	private void selectNone() {
		selected.clear();
		selectionChanged();
	}

	private void clickSignal(int index) {
		final SignalInfo info = shownSignals.get(index);
		if (!info.loaded()) {
			return;
		}
		if (hasShiftDown() && lastClickedIndex >= 0 && lastClickedIndex < shownSignals.size()) {
			final int from = Math.min(lastClickedIndex, index);
			final int to = Math.max(lastClickedIndex, index);
			for (int i = from; i <= to; i++) {
				if (shownSignals.get(i).loaded()) {
					selected.add(shownSignals.get(i).pos());
				}
			}
		} else if (!selected.remove(info.pos())) {
			selected.add(info.pos());
		}
		lastClickedIndex = index;
		selectionChanged();
	}

	private void applySystem(SignalSystem system) {
		targetSystem = system;
		targetManual = -1;
		sendApply(system.ordinal(), -1);
	}

	private void applyAspect(int manual) {
		targetManual = manual;
		sendApply(targetSystem.ordinal(), manual);
	}

	private void sendApply(int system, int manual) {
		if (selected.isEmpty()) {
			showMessage(Component.translatable("gui.mtrsignals.tablet.select_first"));
			return;
		}
		PacketDistributor.sendToServer(new ApplyPayload(new ArrayList<>(selected), system, manual));
	}

	private void rename() {
		if (selected.size() != 1) {
			showMessage(Component.translatable("gui.mtrsignals.tablet.select_one"));
			return;
		}
		PacketDistributor.sendToServer(new RenamePayload(selected.iterator().next(), nameText));
	}

	private void showMessage(Component text) {
		message = text;
		messageUntil = System.currentTimeMillis() + 3000;
	}

	@Override
	public void tick() {
		super.tick();
		if (++ticks % REFRESH_TICKS == 0) {
			PacketDistributor.sendToServer(new RequestListPayload());
		}
	}

	// ---- input

	private boolean inSignalList(double mouseX, double mouseY) {
		return mouseX >= left + 10 && mouseX < left + 230 && mouseY >= top + 58 && mouseY < top + 58 + SIGNAL_ROWS * SIGNAL_ROW;
	}

	private boolean inAspectList(double mouseX, double mouseY) {
		return mouseX >= left + 246 && mouseX < left + 470 && mouseY >= top + 122 && mouseY < top + 122 + ASPECT_ROWS * ASPECT_ROW;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (button != 0) {
			return false;
		}
		if (inSignalList(mouseX, mouseY)) {
			final int index = signalScroll + (int) ((mouseY - (top + 58)) / SIGNAL_ROW);
			if (index < shownSignals.size()) {
				clickSignal(index);
			}
			return true;
		}
		if (inAspectList(mouseX, mouseY)) {
			final int index = aspectScroll + (int) ((mouseY - (top + 122)) / ASPECT_ROW);
			if (index < targetSystem.aspectCount() + 1) {
				applyAspect(index - 1);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		final int step = scrollY > 0 ? -2 : 2;
		if (inSignalList(mouseX, mouseY)) {
			signalScroll = Math.max(0, Math.min(Math.max(0, shownSignals.size() - SIGNAL_ROWS), signalScroll + step));
			return true;
		}
		if (inAspectList(mouseX, mouseY)) {
			aspectScroll = Math.max(0, Math.min(Math.max(0, targetSystem.aspectCount() + 1 - ASPECT_ROWS), aspectScroll + step));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- drawing

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(0, 0, width, height, COLOR_DIM);
		guiGraphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, COLOR_BORDER);
		guiGraphics.fill(left, top, left + WIDTH, top + HEIGHT, COLOR_PANEL);
		guiGraphics.fill(left, top, left + WIDTH, top + 28, COLOR_TITLE);
		// panels behind the two lists
		guiGraphics.fill(left + 9, top + 57, left + 231, top + 58 + SIGNAL_ROWS * SIGNAL_ROW + 1, COLOR_BORDER);
		guiGraphics.fill(left + 245, top + 121, left + 471, top + 122 + ASPECT_ROWS * ASPECT_ROW + 1, COLOR_BORDER);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		final Font font = Minecraft.getInstance().font;

		guiGraphics.drawString(font, title, left + 10, top + 6, COLOR_TEXT, false);
		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.subtitle", allSignals.size()), left + 10, top + 17, COLOR_TEXT_DIM, false);
		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.system"), left + 246, top + 38, COLOR_TEXT_DIM, false);
		guiGraphics.drawString(font, Component.translatable("gui.mtrsignals.tablet.aspect"), left + 246, top + 108, COLOR_TEXT_DIM, false);

		drawSignalList(guiGraphics, font, mouseX, mouseY);
		drawAspectList(guiGraphics, font, mouseX, mouseY);

		final boolean showMessage = System.currentTimeMillis() < messageUntil;
		final Component status = showMessage ? message : Component.translatable("gui.mtrsignals.tablet.selected", selected.size(), allSignals.size());
		guiGraphics.drawString(font, status, left + 246, top + 240, showMessage ? 0xFFFFC65C : COLOR_TEXT_DIM, false);
	}

	private void drawSignalList(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
		final BlockPos playerPos = Minecraft.getInstance().player == null ? BlockPos.ZERO : Minecraft.getInstance().player.blockPosition();
		final int x = left + 10;
		for (int row = 0; row < SIGNAL_ROWS; row++) {
			final int index = signalScroll + row;
			if (index >= shownSignals.size()) {
				break;
			}
			final SignalInfo info = shownSignals.get(index);
			final int y = top + 58 + row * SIGNAL_ROW;
			final boolean isSelected = selected.contains(info.pos());
			final boolean hover = inSignalList(mouseX, mouseY) && mouseY >= y && mouseY < y + SIGNAL_ROW;
			guiGraphics.fill(x, y, x + 220, y + SIGNAL_ROW - 1, !info.loaded() ? COLOR_ROW_DISABLED : isSelected ? COLOR_SELECTED : hover ? COLOR_ROW_HOVER : COLOR_ROW);

			// checkbox
			guiGraphics.renderOutline(x + 5, y + 5, 10, 10, info.loaded() ? COLOR_TEXT_DIM : COLOR_TEXT_DISABLED);
			if (isSelected) {
				guiGraphics.fill(x + 7, y + 7, x + 13, y + 13, COLOR_CHECK);
			}

			final SignalSystem system = SignalSystem.byOrdinal(info.system());
			final BlockPos pos = info.pos();
			final String posText = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			final String first = info.name().isEmpty() ? posText : info.name() + "  (" + posText + ")";
			final String aspectText = info.manual() >= 0
					? Component.translatable(system.aspectTranslationKey(info.manual())).getString()
					: Component.translatable("gui.mtrsignals.tablet.automatic_short").getString();
			final int distance = (int) Math.sqrt(pos.distSqr(playerPos));
			String second = distance + " m  |  " + Component.translatable(system.translationKey()).getString() + "  |  " + aspectText;
			if (!info.loaded()) {
				second = Component.translatable("gui.mtrsignals.tablet.not_loaded").getString() + "  |  " + distance + " m";
			}
			final int textWidth = 220 - 24;
			guiGraphics.drawString(font, font.plainSubstrByWidth(first, textWidth), x + 20, y + 3, info.loaded() ? COLOR_TEXT : COLOR_TEXT_DISABLED, false);
			guiGraphics.drawString(font, font.plainSubstrByWidth(second, textWidth), x + 20, y + 12, info.loaded() ? COLOR_TEXT_DIM : COLOR_TEXT_DISABLED, false);
		}
		drawScrollbar(guiGraphics, left + 228, top + 58, SIGNAL_ROWS * SIGNAL_ROW, shownSignals.size(), SIGNAL_ROWS, signalScroll);
	}

	private void drawAspectList(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
		final int x = left + 246;
		final int count = targetSystem.aspectCount() + 1;
		aspectScroll = Math.max(0, Math.min(aspectScroll, Math.max(0, count - ASPECT_ROWS)));
		for (int row = 0; row < ASPECT_ROWS; row++) {
			final int index = aspectScroll + row;
			if (index >= count) {
				break;
			}
			final int aspectIndex = index - 1;
			final int y = top + 122 + row * ASPECT_ROW;
			final boolean active = targetManual == aspectIndex;
			final boolean hover = inAspectList(mouseX, mouseY) && mouseY >= y && mouseY < y + ASPECT_ROW;
			guiGraphics.fill(x, y, x + 224, y + ASPECT_ROW - 1, active ? COLOR_BUTTON_SELECTED : hover ? COLOR_BUTTON_HOVER : COLOR_BUTTON);

			final Component label = aspectIndex < 0
					? Component.translatable("gui.mtrsignals.tablet.automatic")
					: Component.translatable(targetSystem.aspectTranslationKey(aspectIndex));
			guiGraphics.drawString(font, font.plainSubstrByWidth(label.getString(), 224 - 60), x + 5, y + 5, COLOR_TEXT, false);

			if (aspectIndex >= 0) {
				drawAspectDots(guiGraphics, targetSystem, targetSystem.aspect(aspectIndex), x + 224 - 6, y + 4);
			}
		}
		drawScrollbar(guiGraphics, left + 468, top + 122, ASPECT_ROWS * ASPECT_ROW, count, ASPECT_ROWS, aspectScroll);
	}

	/**
	 * Tiny preview of an aspect: one square per lit lens, right-aligned at {@code rightX}.
	 */
	private static void drawAspectDots(GuiGraphics guiGraphics, SignalSystem system, SignalAspect aspect, int rightX, int y) {
		int x = rightX;
		for (final SignalAspect.Lamp lamp : aspect.lamps()) {
			final int color = system.lensColor(lamp.slot());
			x -= 9;
			guiGraphics.fill(x, y, x + 7, y + 7, lamp.flash() == SignalAspect.Flash.STEADY ? color : (color & 0x00FFFFFF) | 0xA0000000);
			if (lamp.flash() != SignalAspect.Flash.STEADY) {
				guiGraphics.renderOutline(x, y, 7, 7, color);
			}
		}
		for (final SignalAspect.Bar bar : aspect.bars()) {
			x -= 4;
			guiGraphics.fill(x, y, x + 2, y + 7, bar.color());
		}
	}

	private static void drawScrollbar(GuiGraphics guiGraphics, int x, int y, int height, int count, int rows, int scroll) {
		if (count <= rows) {
			return;
		}
		final int barHeight = Math.max(10, height * rows / count);
		final int barY = y + (height - barHeight) * scroll / (count - rows);
		guiGraphics.fill(x, barY, x + 2, barY + barHeight, COLOR_TEXT_DIM);
	}

	private static class FlatButton extends Button {

		private final BooleanSupplier selected;

		FlatButton(int x, int y, int width, int height, Component message, BooleanSupplier selected, Runnable action) {
			super(x, y, width, height, message, button -> action.run(), DEFAULT_NARRATION);
			this.selected = selected;
		}

		@Override
		public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			final int background = selected.getAsBoolean() ? COLOR_BUTTON_SELECTED : isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
			guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
			final Font font = Minecraft.getInstance().font;
			// keep long labels inside the button
			final String text = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 8);
			guiGraphics.drawString(font, text, getX() + 5, getY() + (getHeight() - 8) / 2, COLOR_TEXT, false);
		}
	}
}
