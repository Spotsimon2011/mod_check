# MTR Signalling Pack

An add-on for **Minecraft Transit Railway 4.1.0-beta.2** (NeoForge, Minecraft 1.21.1).

- Light signals using the aspect systems of **Poland (PKP)**, **Slovakia (ŽSR)**, **Czechia (SŽ)** and **Germany (Ks)**
- Each system comes as a single-sided and a double-sided block (8 blocks)
- The **Signalling Tablet**: pick a signal, choose its system and force an aspect

> **Status: not compiled or tested.** It was written in a sandbox that could not reach Minecraft/NeoForge
> downloads, so it has never been built or launched. It was checked against MTR's real class signatures,
> but expect to fix a compile error or two on the first build. See "If the build fails" below.

## How it fits MTR

The signals are real MTR signals: they extend MTR's own `BlockSignalLightBase`, use MTR's 4-aspect housing
models, connect to rails with the normal MTR signal connectors, and are drawn through MTR's own light renderer
(same lens positions). In *Automatic* mode MTR decides what the signal shows from train detection, and this mod
translates that into the aspect of the signal's country:

| MTR state        | Poland | Slovakia   | Czechia       | Germany      |
|------------------|--------|------------|---------------|--------------|
| occupied (stop)  | S1     | Stoj       | Stůj          | Hp 0         |
| just cleared     | S5     | Výstraha   | Výstraha      | Ks 2         |
| cleared recently | S3     | Očakávaj 40| Očekávejte 40 | Ks 1 blinking|
| free (clear)     | S2     | Voľno      | Volno         | Ks 1         |

Extra aspects only available by forcing them with the tablet: PL S4, S10; SK/CZ "40 km/h and clear / and caution";
DE Ks 1 + Zs 3.

The lamp layouts are **simplified** so every country fits MTR's 4-lens housing (the German Zs 3 speed indicator is
shown as a white lamp, for example). Edit `signal/SignalSystem.java` to change any pattern.

## Signalling Tablet

- **Right-click a signal** with the tablet: selects it and opens the screen.
- **Right-click in the air** (or on any other block): reopens the screen for the selected signal (works up to 128 blocks away, if loaded).
- In the screen: choose the **signalling system** (changing the system resets to Automatic), then either
  **Automatic** or a specific **aspect**. Changes apply instantly to the signal.
- A forced aspect is drawn even if the signal is not connected to a rail. While an aspect is forced, MTR's
  redstone input/output for that signal is paused; set it back to Automatic to restore it.

The item uses MTR's dashboard frame with a signal glyph on top. Everything is in the *MTR Signalling Pack* creative tab.

## Getting the jar without installing anything (GitHub Actions)

`.github/workflows/build.yml` builds the jar on GitHub's servers.

1. Put this project folder (`mtr_signalling_pack/`) into your `mod_check` repo and copy
   `.github/workflows/build.yml` to `<repo root>/.github/workflows/build-signalling-pack.yml`
   (or push the project as its own repo with the MTR jar in `libs/`).
2. Push. Open the repo's **Actions** tab, open the "Build Signalling Pack" run, download the **mtrsignals-jar** artifact.
3. If the run fails with a compile error, the log shows exactly which line; send it to Claude to fix.

## Building locally

1. Install **JDK 21**.
2. Get the real MTR jar. The one in the `mod_check` repo is a Git LFS pointer; run `git lfs pull` there or use your own copy.
   Copy it into `libs/` (any `*.jar` in that folder is used).
3. `./gradlew build` (Windows: `gradlew.bat build`). The mod jar is in `build/libs/`.
4. Put that jar and the MTR jar in your `mods` folder (NeoForge 21.1.x for Minecraft 1.21.1).

Dev run: `./gradlew runClient`.

### If the build fails

- **Plugin / NeoForge version not found**: `build.gradle` pins `net.neoforged.moddev 2.0.78` and `gradle.properties`
  pins `neo_version=21.1.172`. Use the newest 2.0.x plugin and any 21.1.x NeoForge release.
- **Compile errors**: the likely spots are the NeoForge event / registry API names (`MtrSignals.java`,
  `MtrSignalsClient.java`) if your NeoForge 21.1 build differs slightly from what this was written against.

## Credits / licence

MIT. The block models, housing textures and lamp rendering come from Minecraft Transit Railway (MIT, by Jonathan Ho
and contributors); the item icons are derived from MTR's textures.
