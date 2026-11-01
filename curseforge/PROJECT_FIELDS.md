# CurseForge project fields

Fill in at https://authors.curseforge.com → Create Project → Minecraft → Mods.

| Field | Value |
|---|---|
| Project name | `Create: Display Link Plus` |
| Summary | `One Create Display Link, many displays: multiple targets, label templates, list formats and clipboard support` |
| Summary (short alternative) | `Multiple displays per Create Display Link, and more` |
| Main category | **Create** (under *Addons*) |
| Extra categories | **Technology**, **Utility & QoL**, **Redstone** (pick up to 4 that fit) |
| License | **GNU Lesser General Public License version 3 (LGPLv3)** |
| Logo / avatar | `curseforge/logo.png` (512×512) |
| Description | paste `curseforge/DESCRIPTION.md` (choose the Markdown editor) |
| Source / Issues links | Source: https://github.com/Rick007110/create-display-link-plus · Issues: https://github.com/Rick007110/create-display-link-plus/issues |

## First file upload

| Field | Value |
|---|---|
| File | `build/libs/create_displaylinkplus-1.0.0.jar` |
| Display name | `Display Link Plus 1.0.0` |
| Release type | **Release** (or Beta if you want early feedback first) |
| Game version | **1.21.1** |
| Mod loader | **NeoForge** |
| Java version | **Java 21** |
| Environment | **Client and Server** |
| Relations | **Required dependency: Create** |
| Changelog | see below |

## Changelog 1.0.0

```
First release.
- One Display Link can write to up to 8 displays/lines (numbered tabs, + and remove buttons)
- Add a display to an existing link by right-clicking the link with a Display Link that has a target selected
- Look at a link with a Wrench, Display Link, Clipboard or Goggles to see all its targets
- Entries respect each other's lines; warning when two entries share a line
- Label templates: {} places the value, with {k}, {,} and {.N} number formats
- Row formats for list sources: {name}, {count}, {count:k}
- Copy and paste link settings with Create's Clipboard
- New Ponder scene for the Display Link
```

## Screenshots worth adding to the gallery

Take these in-game (F2) at your normal resolution:
1. The Display Link screen with several numbered tabs.
2. The target overlay: looking at a link with a wrench, outlines and "1 · Line 1" labels on two displays.
3. A sign or Display Board using a template, e.g. "We have 64 bread in stock".
4. A list with a row format, e.g. "Bread: 1.2k".
5. The Ponder scene.
