package com.spotsimon.mtrsignals.signal;

import java.util.List;

/**
 * One indication of a signal: a set of lit lenses. A lens is addressed by its slot in the
 * MTR 4-lens housing, 0 = bottom lens ... 3 = top lens.
 */
public final class SignalAspect {

	private final String id;
	private final List<Lamp> lamps;

	public SignalAspect(String id, Lamp... lamps) {
		this.id = id;
		this.lamps = List.of(lamps);
	}

	public String id() {
		return id;
	}

	public List<Lamp> lamps() {
		return lamps;
	}

	public static Lamp on(int slot) {
		return new Lamp(slot, false);
	}

	public static Lamp flash(int slot) {
		return new Lamp(slot, true);
	}

	public record Lamp(int slot, boolean flashing) {
	}
}
