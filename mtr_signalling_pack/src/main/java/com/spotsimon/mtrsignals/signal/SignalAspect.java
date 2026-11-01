package com.spotsimon.mtrsignals.signal;

import java.util.List;

/**
 * One indication of a signal: a set of lit lenses, plus optional "bars" (the yellow/green speed
 * bands that ZSR/SZ signals show next to the lower lens).
 * <p>
 * A lens is addressed by its slot in the MTR 4-lens housing, 0 = bottom lens ... 3 = top lens.
 */
public final class SignalAspect {

	private final String id;
	private final List<Lamp> lamps;
	private final List<Bar> bars;

	public SignalAspect(String id, Lamp... lamps) {
		this(id, List.of(lamps), List.of());
	}

	private SignalAspect(String id, List<Lamp> lamps, List<Bar> bars) {
		this.id = id;
		this.lamps = lamps;
		this.bars = bars;
	}

	/**
	 * @return a copy of this aspect that also lights the given bars
	 */
	public SignalAspect withBars(Bar... newBars) {
		return new SignalAspect(id, lamps, List.of(newBars));
	}

	public String id() {
		return id;
	}

	public List<Lamp> lamps() {
		return lamps;
	}

	public List<Bar> bars() {
		return bars;
	}

	public static Lamp on(int slot) {
		return new Lamp(slot, Flash.STEADY);
	}

	/**
	 * Slow flashing (1 flash per second).
	 */
	public static Lamp flash(int slot) {
		return new Lamp(slot, Flash.SLOW);
	}

	/**
	 * Fast flashing (2 flashes per second).
	 */
	public static Lamp fast(int slot) {
		return new Lamp(slot, Flash.FAST);
	}

	public enum Flash {
		STEADY, SLOW, FAST;

		public boolean isLit(long timeMillis) {
			return switch (this) {
				case STEADY -> true;
				case SLOW -> (timeMillis / 500) % 2 == 0;
				case FAST -> (timeMillis / 250) % 2 == 0;
			};
		}
	}

	public record Lamp(int slot, Flash flash) {

		public boolean isLit(long timeMillis) {
			return flash.isLit(timeMillis);
		}
	}

	/**
	 * A speed band beside the lowest lens.
	 *
	 * @param right true for the right side, false for the left side
	 * @param color ARGB colour (see {@link Lens})
	 */
	public record Bar(boolean right, int color) {
	}
}
