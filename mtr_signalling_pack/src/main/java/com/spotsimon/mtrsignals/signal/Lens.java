package com.spotsimon.mtrsignals.signal;

/**
 * ARGB colours of the lenses. These are the same values MTR uses for its own signal lamps
 * (red -65536, orange -22016, green -16711936).
 */
public final class Lens {

	public static final int RED = 0xFFFF0000;
	public static final int YELLOW = 0xFFFFAA00;
	public static final int GREEN = 0xFF00FF00;
	public static final int WHITE = 0xFFFFFFFF;

	private Lens() {
	}
}
