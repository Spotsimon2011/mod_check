package com.spotsimon.mtrsignals.registry;

import com.spotsimon.mtrsignals.signal.SignalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers every signal of this mod in a dimension (position, name, system, forced aspect), also
 * for chunks that are not loaded. The Signalling Tablet lists signals from here.
 * <p>
 * The block entity of a loaded signal is the source of truth for system and aspect; this registry
 * just mirrors it so unloaded signals can still be listed.
 */
public class SignalRegistry extends SavedData {

	private static final String DATA_NAME = "mtrsignals_registry";
	private static final String KEY_SIGNALS = "signals";
	private static final int MAX_NAME_LENGTH = 32;

	private final Map<BlockPos, Entry> entries = new LinkedHashMap<>();

	public static SignalRegistry get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SignalRegistry::new, SignalRegistry::load, null), DATA_NAME);
	}

	public static SignalRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
		final SignalRegistry registry = new SignalRegistry();
		final ListTag list = tag.getList(KEY_SIGNALS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			final CompoundTag entryTag = list.getCompound(i);
			registry.entries.put(BlockPos.of(entryTag.getLong("pos")), new Entry(
					entryTag.getString("name"),
					SignalSystem.byId(entryTag.getString("system")),
					entryTag.getString("manual")
			));
		}
		return registry;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		final ListTag list = new ListTag();
		entries.forEach((pos, entry) -> {
			final CompoundTag entryTag = new CompoundTag();
			entryTag.putLong("pos", pos.asLong());
			entryTag.putString("name", entry.name);
			entryTag.putString("system", entry.system.id());
			entryTag.putString("manual", entry.manualId);
			list.add(entryTag);
		});
		tag.put(KEY_SIGNALS, list);
		return tag;
	}

	/**
	 * Called by a loaded signal to mirror its state. Keeps the name if the signal is already known.
	 */
	public void sync(BlockPos pos, SignalSystem system, String manualId) {
		final BlockPos key = pos.immutable();
		final Entry old = entries.get(key);
		if (old == null) {
			entries.put(key, new Entry("", system, manualId));
			setDirty();
		} else if (old.system != system || !old.manualId.equals(manualId)) {
			entries.put(key, new Entry(old.name, system, manualId));
			setDirty();
		}
	}

	public void remove(BlockPos pos) {
		if (entries.remove(pos) != null) {
			setDirty();
		}
	}

	public void rename(BlockPos pos, String name) {
		final Entry old = entries.get(pos);
		if (old != null) {
			final String cleaned = name.strip();
			entries.put(pos.immutable(), new Entry(cleaned.length() > MAX_NAME_LENGTH ? cleaned.substring(0, MAX_NAME_LENGTH) : cleaned, old.system, old.manualId));
			setDirty();
		}
	}

	public boolean contains(BlockPos pos) {
		return entries.containsKey(pos);
	}

	public Map<BlockPos, Entry> getEntries() {
		return entries;
	}

	public static final class Entry {

		public final String name;
		public final SignalSystem system;
		/**
		 * Id of the forced aspect, or an empty string for automatic.
		 */
		public final String manualId;

		public Entry(String name, SignalSystem system, String manualId) {
			this.name = name;
			this.system = system;
			this.manualId = manualId;
		}
	}
}
