---
name: run-mystical-index
description: Launch the Mystical Index Fabric mod in a dev Minecraft client (or dedicated server) via Gradle/Loom, and confirm from the log that the mod actually registered. Use when asked to run, start, boot, or screenshot the mod, or to verify a change works in the real game rather than only compiling.
---

# Running Mystical Index

Fabric mod for **Minecraft 1.20.1**. `gradlew runClient` builds the mod, patches a
dev Minecraft, and opens a real game window. There is no test suite — the client
*is* the way to verify a change.

## Stack

| Piece | Version | Where it's set |
|---|---|---|
| Minecraft | 1.20.1 | `gradle.properties` → `minecraft_version` |
| Fabric Loader | 0.15.10 | `gradle.properties` → `loader_version` |
| Fabric API | 0.92.1+1.20.1 | `gradle.properties` → `fabric_version` |
| Fabric Loom | 1.5.8 (`1.5-SNAPSHOT`) | `build.gradle` plugin block |
| Gradle | 8.5 (wrapper) | `gradle/wrapper/` |
| Java target | 17 | `build.gradle` → `options.release = 17` |

Runtime deps pulled in automatically: MidnightLib (config UI) and REI (recipe
viewer, `modRuntimeOnly`). REI means the in-game recipe screen is available for
checking the mod's recipes.

## Launch

Use **JDK 17**, not the machine default. `java -version` here reports 21, but the
build targets 17 and Loom 1.5 is a 1.20.1-era toolchain — pinning 17 keeps the
compiler, Loom, and the game on the version the project was built against.

```bash
JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot' ./gradlew.bat runClient --console=plain
```

PowerShell form:

```bash
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot'; .\gradlew.bat runClient --console=plain
```

Run it **in the background** — `runClient` does not exit until the player closes
the game, so a foreground call will block until timeout. Then watch the log
file for boot markers rather than polling blindly.

`--console=plain` matters: without it Gradle emits ANSI progress redraws that
bury the game log in escape codes.

While `runClient` is executing, Gradle holds the project lock for the whole
session — a second gradle invocation (even `compileJava`) blocks until the game
window closes. Close the client before building.

Other Loom tasks: `runServer` (headless dedicated server), `build` (jar into
`build/libs/`), `genSources` (decompile MC for navigating vanilla code).

## Confirming it actually booted

Grep the launch output for these, in order. Compiling is not booting, and
"no errors" is not proof the window opened.

| Marker | Means |
|---|---|
| `> Task :compileJava` | mod source compiled |
| `Loading Minecraft 1.20.1 with Fabric Loader 0.15.10` | game handing off to Loader |
| `Loading 65 mods:` … `- mystical_index 1.20.1-1.2` | **the mod itself loaded** |
| `(mystical_index) Registering items/Mod Blocks/recipes` | mod entrypoint ran |
| `Sound engine started` | title screen is up — boot complete |

Then confirm the window exists:

```bash
Get-Process java | Where-Object { $_.MainWindowTitle -ne '' } | Select-Object Id, MainWindowTitle
```

Expect `Minecraft* 1.20.1`. Cold start is roughly 2 minutes to title screen with
warm caches; a first-ever run additionally downloads assets and mappings.

### Log noise that is NOT a failure

- `Could not authorize you against Realms server: ... Failed to parse into SignedJWT: FabricMC`
  — the dev launch uses an offline account. Always appears; ignore.
- `Shader rendertype_entity_translucent_emissive could not find sampler named Sampler2`
  — vanilla 1.20.1 warning, unrelated to this mod.

## When it crashes

`runClient` exiting non-zero **mid-session is a crash, not a clean quit** —
closing the window normally exits 0. Gradle only reports
`finished with non-zero exit value -1`, which says nothing useful; the real
cause is in the crash report:

```bash
ls -t run/crash-reports/ | head -3
```

Reports are suffixed `-client` or `-server`. The integrated server ticks the
world, so gameplay-logic crashes (inventory, block entities, ticking) usually
land in `-server` with `Description: Exception in server tick loop`.

Because the mod's logic runs on the server thread inside a world, **a clean boot
to the title screen does not exercise it at all.** To actually test a change,
load a world and use the item or block you touched.

## Verifying vanilla behaviour without decompiling

Mod bugs here are usually a wrong assumption about a vanilla method. `genSources` is slow;
reading the bytecode is seconds and is authoritative. The remapped jar lives at
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/…/*.jar`:

```bash
unzip -o -q "$JAR" "net/minecraft/item/Item.class" && javap -c -p net/minecraft/item/Item.class
```

Two that have already bitten this codebase:

- `Item.toString()` returns `Registries.ITEM.getId(this).getPath()` — **no namespace**. Never
  persist it as an id; use `Registries.ITEM.getId(item).toString()`.
- `DefaultedList.clear()` refills every slot with the default element and **keeps its size**
  (it does not empty the list), so `size()` stays valid afterwards.
- `SimpleInventory.setStack()` calls `markDirty()` on every write. Two inventories that sync to
  each other in `markDirty` will recurse — see the guard in `SimpleBookInventory`.

## Game directory

`run/` is the working directory for the dev client — saves, `options.txt`,
`config/`, and `logs/latest.log` live there. It is **gitignored**, so it is
scratch state: safe to delete to get a clean profile, and world saves there are
not tracked.

## Gotchas

- **Mixins and access widener.** `mystical_index.mixins.json` and
  `mysticalindex.accesswidener` are applied at launch. Errors in either surface
  as a hard crash during startup with `Mixin apply ... failed` — before the
  title screen, so a green compile tells you nothing about them.
- **`gradle.properties` drives the MC version.** Changing `minecraft_version`
  requires matching `yarn_mappings` and `fabric_version` or the build fails to
  resolve.
- **Version bumps invalidate the Loom cache**, making the next run a long one.
