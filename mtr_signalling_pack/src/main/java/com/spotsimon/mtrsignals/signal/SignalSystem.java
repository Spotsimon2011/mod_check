package com.spotsimon.mtrsignals.signal;

import static com.spotsimon.mtrsignals.signal.SignalAspect.fast;
import static com.spotsimon.mtrsignals.signal.SignalAspect.flash;
import static com.spotsimon.mtrsignals.signal.SignalAspect.on;

import java.util.ArrayList;
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
 * Aspects are stored by id (not by index) in save files, so this list can grow without breaking worlds.
 * The lens layouts are simplified approximations of the real signals so that they fit the 4-lens housing.
 */
public enum SignalSystem {

	/**
	 * PKP semaphore. Only the aspects whose lamp pattern could be confirmed are included.
	 */
	POLAND("pl", new int[]{Lens.RED, Lens.YELLOW, Lens.GREEN, Lens.YELLOW},
			0, 4, 2, 1,
			new SignalAspect("s1", on(0)),
			new SignalAspect("s2", on(2)),
			new SignalAspect("s3", flash(2)),
			new SignalAspect("s4", flash(1)),
			new SignalAspect("s5", on(1))),

	/**
	 * ZSR. Upper lens (slots 1-3) shows red/green/yellow, the lower lens (slot 0) is yellow; the
	 * 60/80/100 km/h indications are the yellow/green bars next to the lower lens.
	 */
	SLOVAKIA("sk", new int[]{Lens.YELLOW, Lens.RED, Lens.GREEN, Lens.YELLOW},
			0, 1, 3, 2,
			eastAspects()),

	/**
	 * SZ. Same signal system as the ZSR.
	 */
	CZECHIA("cz", new int[]{Lens.YELLOW, Lens.RED, Lens.GREEN, Lens.YELLOW},
			0, 1, 3, 2,
			eastAspects()),

	/**
	 * DB Ks signals.
	 */
	GERMANY("de", new int[]{Lens.RED, Lens.YELLOW, Lens.GREEN, Lens.WHITE},
			0, 3, 2, 1,
			new SignalAspect("hp0", on(0)),
			new SignalAspect("ks1", on(2)),
			new SignalAspect("ks1_blink", flash(2)),
			new SignalAspect("ks2", on(1)),
			new SignalAspect("ks1_zs3", on(2), on(3)),
			new SignalAspect("zs1", on(0), flash(3))),

	/**
	 * DB H/V signals (main signals Hp 0/1/2 and distant signal Vr 0).
	 */
	GERMANY_HV("dehv", new int[]{Lens.RED, Lens.YELLOW, Lens.GREEN, Lens.YELLOW},
			0, 3, 2, 1,
			new SignalAspect("hp0", on(0)),
			new SignalAspect("hp1", on(2)),
			new SignalAspect("hp2", on(2), on(1)),
			new SignalAspect("vr0", on(1), on(3)));

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

	/**
	 * The 31 main-signal aspects of the ZSR/SZ light signals (ZSR rulebook, aspects 1-31):
	 * stoj, vystraha, volno, the four "ocakavaj" (expect) flashing aspects, and for each of the
	 * speeds 40/60/80/100 km/h the six combinations with volno/vystraha/ocakavaj.
	 */
	private static SignalAspect[] eastAspects() {
		final List<SignalAspect> list = new ArrayList<>();
		list.add(new SignalAspect("stoj", on(1)));
		list.add(new SignalAspect("vystraha", on(3)));
		list.add(new SignalAspect("volno", on(2)));
		list.add(new SignalAspect("ocakavaj_40", flash(3)));
		list.add(new SignalAspect("ocakavaj_60", fast(3)));
		list.add(new SignalAspect("ocakavaj_80", flash(2)));
		list.add(new SignalAspect("ocakavaj_100", fast(2)));

		final int[] speeds = {40, 60, 80, 100};
		final String[] nexts = {"volno", "vystraha", "ocakavaj_40", "ocakavaj_60", "ocakavaj_80", "ocakavaj_100"};
		for (final int speed : speeds) {
			for (final String next : nexts) {
				final SignalAspect.Lamp upper = switch (next) {
					case "volno" -> on(2);
					case "vystraha" -> on(3);
					case "ocakavaj_40" -> flash(3);
					case "ocakavaj_60" -> fast(3);
					case "ocakavaj_80" -> flash(2);
					default -> fast(2);
				};
				final SignalAspect aspect = new SignalAspect("r" + speed + "_" + next, on(0), upper);
				list.add(switch (speed) {
					case 60 -> aspect.withBars(new SignalAspect.Bar(false, Lens.YELLOW));
					case 80 -> aspect.withBars(new SignalAspect.Bar(false, Lens.GREEN));
					case 100 -> aspect.withBars(new SignalAspect.Bar(false, Lens.GREEN), new SignalAspect.Bar(true, Lens.GREEN));
					default -> aspect;
				});
			}
		}
		return list.toArray(new SignalAspect[0]);
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
	 * @return the index of the aspect with this id, or -1 if there is none
	 */
	public int indexOfAspect(String aspectId) {
		for (int i = 0; i < aspects.size(); i++) {
			if (aspects.get(i).id().equals(aspectId)) {
				return i;
			}
		}
		return -1;
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
