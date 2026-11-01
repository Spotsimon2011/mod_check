package com.spotsimon.mtrsignals.signal;

import static com.spotsimon.mtrsignals.signal.SignalAspect.flash;
import static com.spotsimon.mtrsignals.signal.SignalAspect.on;

import java.util.List;

/**
 * The supported national aspect systems.
 * <p>
 * Every system fits the 4-lens MTR housing. {@code lenses} gives the colour of each lens,
 * bottom (slot 0) to top (slot 3). The four "auto" indexes map MTR's automatic, train-driven
 * signal state onto an aspect of the system:
 * <ul>
 *     <li>stop    - MTR aspect 1 (block occupied)</li>
 *     <li>caution - MTR aspect 2 (just cleared; next signal is at stop)</li>
 *     <li>warning - MTR aspect 3 (cleared a moment ago; reduced speed expected)</li>
 *     <li>clear   - MTR aspect 0 (free)</li>
 * </ul>
 * The lens layouts are simplified approximations of the real signals so that they fit the
 * 4-lens housing; adjust them here if you want a different look.
 */
public enum SignalSystem {

	POLAND("pl", new int[]{Lens.RED, Lens.YELLOW, Lens.GREEN, Lens.YELLOW},
			0, 4, 2, 1,
			new SignalAspect("s1", on(0)),
			new SignalAspect("s2", on(2)),
			new SignalAspect("s3", flash(2)),
			new SignalAspect("s4", flash(1)),
			new SignalAspect("s5", on(1)),
			new SignalAspect("s10", on(2), on(3))),

	SLOVAKIA("sk", new int[]{Lens.YELLOW, Lens.RED, Lens.GREEN, Lens.YELLOW},
			0, 2, 5, 1,
			new SignalAspect("stoj", on(1)),
			new SignalAspect("volno", on(2)),
			new SignalAspect("vystraha", on(3)),
			new SignalAspect("rychlost_40_volno", on(0), on(2)),
			new SignalAspect("rychlost_40_vystraha", on(0), on(3)),
			new SignalAspect("ocakavaj_40", flash(3))),

	CZECHIA("cz", new int[]{Lens.YELLOW, Lens.RED, Lens.GREEN, Lens.YELLOW},
			0, 2, 5, 1,
			new SignalAspect("stuj", on(1)),
			new SignalAspect("volno", on(2)),
			new SignalAspect("vystraha", on(3)),
			new SignalAspect("rychlost_40_volno", on(0), on(2)),
			new SignalAspect("rychlost_40_vystraha", on(0), on(3)),
			new SignalAspect("ocekavejte_40", flash(3))),

	GERMANY("de", new int[]{Lens.RED, Lens.YELLOW, Lens.GREEN, Lens.WHITE},
			0, 3, 2, 1,
			new SignalAspect("hp0", on(0)),
			new SignalAspect("ks1", on(2)),
			new SignalAspect("ks1_blink", flash(2)),
			new SignalAspect("ks2", on(1)),
			new SignalAspect("ks1_zs3", on(2), on(3)));

	private final String id;
	private final int[] lenses;
	private final int stopIndex;
	private final int cautionIndex;
	private final int warningIndex;
	private final int clearIndex;
	private final List<SignalAspect> aspects;

	SignalSystem(String id, int[] lenses, int stopIndex, int cautionIndex, int warningIndex, int clearIndex, SignalAspect... aspects) {
		this.id = id;
		this.lenses = lenses;
		this.stopIndex = stopIndex;
		this.cautionIndex = cautionIndex;
		this.warningIndex = warningIndex;
		this.clearIndex = clearIndex;
		this.aspects = List.of(aspects);
	}

	public String id() {
		return id;
	}

	public int lensColor(int slot) {
		return lenses[Math.max(0, Math.min(lenses.length - 1, slot))];
	}

	public int aspectCount() {
		return aspects.size();
	}

	public SignalAspect aspect(int index) {
		return aspects.get(Math.max(0, Math.min(aspects.size() - 1, index)));
	}

	/**
	 * @param mtrAspect the value MTR computes for a signal: 1 = stop, 2 = caution, 3 = warning, anything else = clear
	 */
	public SignalAspect autoAspect(int mtrAspect) {
		return switch (mtrAspect) {
			case 1 -> aspect(stopIndex);
			case 2 -> aspect(cautionIndex);
			case 3 -> aspect(warningIndex);
			default -> aspect(clearIndex);
		};
	}

	public String translationKey() {
		return "system.mtrsignals." + id;
	}

	public String aspectTranslationKey(int index) {
		return "aspect.mtrsignals." + id + "." + aspect(index).id();
	}

	public static SignalSystem byOrdinal(int ordinal) {
		final SignalSystem[] values = values();
		return values[Math.max(0, Math.min(values.length - 1, ordinal))];
	}

	public static SignalSystem byId(String id) {
		for (final SignalSystem system : values()) {
			if (system.id.equals(id)) {
				return system;
			}
		}
		return POLAND;
	}
}
