# Create: Display Link Plus: Notes

Reference: `reference/create-1.21.1-6.0.10.jar` (Create 6.0.10, git `ac0c444d`). It is a compiled jar, so it was
decompiled with Vineflower into `reference/create-src/` (Create) and `reference/ponder-src/` (catnip GUI classes from
the bundled Ponder jar). Those folders are read-only reference material; the mod lives in the project root.

Create 6.0.10 requires NeoForge `[21.1.219,)`, MC `1.21.1`, Flywheel `[1.0.0,2.0)`, Ponder `[1.0.82,)`.
Bundled (jar-in-jar): Registrate `MC1.21-1.3.0+67`, Flywheel `1.0.6`, Ponder `1.0.82+mc1.21.1`.

## Classes (package `com.simibubi.create.content.redstone.displayLink`)

| Class | Role |
|---|---|
| `DisplayLinkBlock` | Block. `useWithoutItem` opens `DisplayLinkScreen` client-side (refuses if `targetOffset == ZERO`). Static `notifyGatherers` / `sendToGatherers` find adjacent links facing a source (only links with `activeSource != null`). `neighborChanged` handles `POWERED` and calls `onNoLongerPowered`. |
| `DisplayLinkBlockEntity` | Extends `LinkWithBulbBlockEntity` (-> `SmartBlockEntity`). All link state lives here (see below). |
| `DisplayLinkScreen` | Client GUI. Reads everything from the BE it was given. `onClose()` builds a source tag and sends `DisplayLinkConfigurationPacket`. |
| `DisplayLinkConfigurationPacket` | `BlockEntityConfigurationPacket` (range 20, no spectator/adventure). `applySettings` sets `targetLine`, source + config, then `updateGatheredData()` + `notifyUpdate()`. |
| `DisplayLinkContext` | `(level, blockEntity)`. Sources/targets read source pos, target pos and `sourceConfig()` through the BE. |
| `ClickToLinkBlockItem` / `DisplayLinkBlockItem` | Item. Right-click target stores `CLICK_TO_LINK_DATA` (pos + dim). Placing writes `TargetOffset` into `BLOCK_ENTITY_DATA`. Max distance = `displayLinkRange` server config (default 64). Sneak-use clears. |
| `LinkBulbRenderer` | Only draws the bulb glow (pulse). No link line / target rendering, so multiple targets need no render changes. |
| `api.behaviour.display.DisplaySource` / `DisplayTarget` | Registry-backed singletons; all per-link state comes from the context's BE. |

## BE state (`DisplayLinkBlockEntity`)

- `protected BlockPos targetOffset` (relative to the link)
- `public DisplaySource activeSource`
- `private CompoundTag sourceConfig` (getter/setter public)
- `public DisplayTarget activeTarget` (client gets it via `TargetType` in the update tag)
- `public int targetLine`
- `public int refreshTicks`
- Source position = block the link faces into (`worldPosition.relative(getDirection())`), or the first linked factory
  panel. Shared by every entry, which is exactly what we want.

## Runtime

- `tick()`: server, non-virtual, `activeSource != null` -> `refreshTicks++`; at `getPassiveRefreshTicks()` (default 100)
  and `shouldPassiveReset()` -> `tickSource()`.
- `tickSource()`: resets `refreshTicks`; if not `POWERED`, `updateGatheredData()`.
- `updateGatheredData()`: returns early if source/target unloaded; resolves `DisplayTarget.get(level, targetPos)`,
  drops `activeSource` if no longer offered by the source block, then `activeSource.transferData(ctx, activeTarget, targetLine)`, pulse + `sendData()`.
- Outside callers: `notifyGatherers` (source block changed -> `tickSource`), `sendToGatherers` (item throughput /
  accumulated count / redstone power sources mutate `be.getSourceConfig()`), `DisplayBoardTarget` reads
  `context.blockEntity().activeSource`, `NixieTubeDisplaySource` reads `context.blockEntity().activeTarget`,
  ComputerCraft peripheral reads `activeTarget` / `getSourceConfig()`.

## Save / sync format

`write(tag, registries, clientPacket)` and `writeSafe` both write `TargetOffset`, `TargetLine`, and `Source` (= config
copy + `Id`). Client packets add `TargetType`. `read` mirrors this. `transform()` rotates `targetOffset` on contraptions.
Sync is NBT through `SmartBlockEntity` (`sendData()` -> `blockChanged`), no StreamCodec.

## Target lines

Line dropdown is a `ScrollInput` over `target.provideStats(ctx).maxRows()`; label via `target.getLineOptionText(i)`.
Multi-line targets reserve lines in the target BE's persistent data keyed by the **link's BlockPos**
(`DisplayTarget.reserve/isReserved`). Entries of the same link therefore don't block each other: if two entries of one
link overlap lines on the same display they will overwrite each other. Known limitation; users pick non-overlapping lines.

## Access

Fields are protected/private but Mixin `@Shadow` handles that, so no access transformer needed. `DisplayLinkScreen` fields
are package-private (`sources`, `sourceTypeSelector`, `targetLineSelector`, `configWidgets`) and `blockEntity` is private.

## Chosen design: Approach A (mixin into the existing Display Link)

"Swap" technique: entry 0 is Create's own fields (vanilla data untouched). Extra entries are stored in a list on the BE.
To run any Create logic for entry *i*, its fields are swapped into the BE, the unmodified Create code runs, and the
fields are swapped back. Every source/target/context path therefore works per entry without re-implementing anything.

- `updateGatheredData` is wrapped (`@WrapMethod`): runs entry 0, then each extra entry (swapped, try/catch per entry).
- `tick`: passive refresh of entry 0 only refreshes entry 0; extra entries keep their own `refreshTicks` and refresh on
  their own source's interval.
- `onNoLongerPowered`, `sendToGatherers`, `notifyGatherers`: extended to cover extra entries.
- NBT: extra entries go in a new key `create_displaylinkplus:Entries` (same per-entry layout as Create's keys). Create ignores
  it, so removing the addon just drops back to entry 0.
- `transform`: rotates extra entries' offsets too.
- Screen: the vanilla `DisplayLinkScreen` is reused per entry. For extra entries it is opened on a client-only
  "view" BE (a `DisplayLinkBlockEntity` subclass whose fields hold that entry, with `getSourcePosition()` delegating to
  the real link). A mixin adds entry tabs (left of the window) plus "+" / "−" buttons in the empty left strip, and
  reroutes the close packet to our own `ConfigureEntryPacket` for extra entries.
- Adding a target to an existing link: right-click a display with the Display Link item (normal selection), then
  right-click an existing Display Link with that item -> adds a new entry pointing at that display. "+" in the GUI adds an
  entry on the same display, next line.

Why A over B: the swap technique means very few injection points and zero copied source/target logic, and players keep
using the normal Display Link block.

## Implementation map

| File | What it does |
|---|---|
| `link/LinkEntry` | One extra entry; NBT uses Create's key names. |
| `link/MultiLink` | Duck interface on the BE: entry list, `mdl$runWithEntry` (swap), `mdl$snapshot`, `mdl$removeEntry` (removing 0 promotes 1). |
| `link/EntryOps` | Shared client/server mutations: apply GUI result (copy of Create's `applySettings`), add-copy on next line, cap check. |
| `link/DisplayLinkEntryView` | Client stand-in BE for editing an extra entry in Create's own screen. |
| `mixin/DisplayLinkBlockEntityMixin` | Swap logic, `@WrapMethod updateGatheredData`, per-entry passive refresh, signal reset, NBT, contraption transform. |
| `mixin/DisplayLinkBlockMixin` | `notifyGatherers` / `sendToGatherers` reach extra entries. |
| `mixin/client/DisplayLinkScreenMixin` | Tabs left of the window, add (+) / remove (trash) in the left strip, routes extra-entry saves to `ConfigureEntryPacket`. |
| `network/*` | `ConfigureEntryPacket`, `ModifyEntriesPacket` (NeoForge payloads, Create's validation: range 20, no spectator/adventure). |
| `AddTargetHandler` | Display Link item with a selected display + right-click an existing link = new entry for that display. |
| `link/LabelTemplate` + `mixin/SingleLineDisplaySourceMixin` | Label templates: a label containing `{}` gets the value in place of `{}` ("We have {} bread"). Hooks `provideText`, `provideFlapDisplayText` and `loadFlapDisplayLayout` (one full-width "alphabet" board section). Labels without `{}` are untouched. |
| `mixin/client/SingleLineDisplaySourceClientMixin` | Label box: tooltip hint about `{}`, max length 32 -> 64. |
| `client/TargetOverlay` + `client/EntryColors` | Looking at a link with wrench / Display Link / clipboard / goggles: outline + line + "entry · line" label per target (Catnip `Outliner`, labels drawn in `RenderLevelStageEvent`). Tab numbers use the same colors. |
| `mixin/DisplayTargetMixin` | Per-entry line reservation: `reserve` also stores `DlpEntry<line>` = entry index; `isReserved` treats a sibling entry's reservation like another link's. Stale when the owner index is gone or no longer targets that display. |
| `link/EntryOps.conflicts / displayOf` | Entries starting on the same line of the same display (board controller aware): red "!" + tooltip on tabs. "+" picks the next unused line. |
| `link/LabelTemplate` | Placeholder engine `{field:format}`: `{}`, `{k}`, `{,}`, `{.N}`; list rows `{name}`, `{count}`, `{count:k}`. Formats the first number in the value so units stay. |
| `mixin/ValueListDisplaySourceMixin` + `FluidListDisplaySourceMixin` + `link/RowFormat` | List row format (config key `RowFormat`), one full-width board section. Rows are built as Components so item names stay translatable on servers. |
| `mixin/client/ValueListConfigMixin` | Row format text box (second config line; right half for the scoreboard). |
| `mixin/DisplayLinkClipboardMixin` | `ClipboardCloneable` on the Display Link BE, key `DisplayLink`. Paste keeps each existing entry's display; extra copied entries are added with their relative offset (range checked). |
| `client/ponder/*` + `assets/.../ponder/multi_link.nbt` | Ponder scene on the Display Link. Schematic from `tools/make_ponder_schematic.py`, scene text synced to lang by `tools/sync_ponder_lang.py` (Ponder reads `<modid>.ponder.multi_link.text_N`). |
| `DlpConfig` | Server config `maxEntriesPerLink` (default 8, max 8 because 8 tabs fill the window height). |
| `gametest/MultiLinkGameTests` | Server-side tests, run with `./gradlew runGameTestServer`. |

## Build / run

Gradle 8.14.3 needs a Java 21 JDK (`JAVA_HOME`); the system default here is Java 26, which Gradle can't run on.
Create comes from `maven.createmod.net` (`com.simibubi.create:create-1.21.1:6.0.10-281`, the full jar). The
`extractCreateLibs` task unpacks its jar-in-jar Flywheel, Ponder/Catnip and Registrate into `build/createLibs/` for
compiling (Registrate isn't on a public Maven). For runs, the Create jar is copied into `run/mods/` so FML loads it,
and its jar-in-jar deps, like a real install.

`reference/` (Create jar, decompiled source, plan, icon source images) is local only and git-ignored: Create's assets
are All Rights Reserved and must not end up in the public repo. To decompile again: Vineflower on the Create jar.

## Verified so far (2026-11-01)

- `./gradlew build` passes.
- `./gradlew runGameTestServer`: 6/6 pass. Covers: every entry writes to its own sign/line with its own config; save/load
  round trip keeps Create's keys and all entries, client sync tag carries `TargetType`; extra entries refresh on their
  own 100-tick timer with entry 0 sourceless; removing entry 0 promotes entry 1; add-copy picks next line, clamps to the
  sign's 4 rows, stops at the cap of 8; a missing target doesn't break other entries.
- Label templates: 8/8 GameTests pass, adding: `{}` is replaced on signs (every occurrence), plain labels keep
  "label value", Display Boards get a single section with the filled text. Client starts with both template mixins applied.
- User tested the multi-entry GUI in-game: works.

Label template notes: only single-line sources (`SingleLineDisplaySource` subclasses) have a label; list sources
(Warehouse Summary, item/fluid lists) don't. If the addon is removed, Create shows the raw label, e.g. "We have {} bread 64".
- `./gradlew runClient` starts with Create 6.0.10 and the screen mixin applies (no mixin errors in the log).

- 2026-11-01, v1.0.0 feature set: 13/13 GameTests (adds number formats, free-line "+", conflicts, list stops at a
  sibling entry's line [checked to fail without the fix], list row formats on signs + one board section for the list
  base and the fluid list, clipboard copy/paste). Scripted client run (temporary hook, not committed) confirmed: target
  overlay renders, tabs colored with "!" on conflicts and the conflict tooltip, row format box shows, a scoreboard list
  sign reads "Iron: 56.8k / Bread: 1.2k", the Ponder scene compiles and plays without errors.

Release: renamed to Create: Display Link Plus (`create_displaylinkplus`, package `io.github.rick007110.displaylinkplus`),
author Rick007110, LGPL-3.0 (LICENSE + COPYING). Icon (`tools/make_icon.py`, 256x256) is composed from the user-supplied
`reference/icon/` images (Create logo background + Display Link render) plus a drawn "+" badge. Those two images are
Create assets (All Rights Reserved); everything else (schematic, textures) is original. NBT key is now `create_displaylinkplus:Entries`
(worlds saved with the pre-release `multidisplaylink:Entries` key lose their extra entries).

## Not yet verified (needs in-game testing)

- Clipboard copy/paste by hand, Display Board looks of templates and row formats, Ponder at a normal resolution.

- Label templates in-game (label box tooltip, longer labels, Display Board look).
- Adding a display by right-clicking an existing link with the Display Link item.
- Dedicated server + client sync, two players editing at once, contraption rotation.
- Loading a world made with plain Create, then removing the addon again.
