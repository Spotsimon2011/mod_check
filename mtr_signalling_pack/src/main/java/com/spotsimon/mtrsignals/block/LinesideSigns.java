package com.spotsimon.mtrsignals.block;

import java.util.ArrayList;
import java.util.List;

/**
 * The ZSR-style lineside signs. The sign ids are used for block names, models, textures and
 * translations; the asset generator script uses the same list.
 */
public final class LinesideSigns {

	/**
	 * Speeds in tens of km/h, as shown on the boards (8 = 80 km/h).
	 */
	public static final int[] SPEED_VALUES = {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16};

	public static final List<String> NAMES;

	static {
		final List<String> names = new ArrayList<>();
		for (final int value : SPEED_VALUES) {
			names.add("sign_speed_" + value);
		}
		for (final int value : SPEED_VALUES) {
			names.add("sign_advance_" + value);
		}
		names.add("sign_temp_start");
		names.add("sign_temp_end");
		names.add("sign_stop_mark");
		names.add("sign_warning_mark");
		names.add("sign_distant_marker_arrows");
		names.add("sign_distant_marker_cross");
		names.add("sign_approach_4");
		names.add("sign_approach_3");
		names.add("sign_approach_2");
		names.add("sign_approach_1");
		names.add("sign_group_signal");
		NAMES = List.copyOf(names);
	}

	private LinesideSigns() {
	}
}
