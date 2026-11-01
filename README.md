# Create: Display Link Plus

An addon for [Create](https://modrinth.com/mod/create) that makes the Display Link far more flexible.
One Display Link can feed several displays and lines, labels can hold the value anywhere you like,
lists get custom row formats, and link settings can be copied with Create's Clipboard.

Minecraft 1.21.1 · NeoForge 21.1 · Create 6.0.10+. Install on both the client and the server.

## Features

### Multiple displays per link
- Open a Display Link: each **numbered tab** on the left is one *entry* with its own information type,
  settings, display and line.
- **+** adds an entry on the next free line of the same display. The **trash** button removes the current entry.
- **Add another display:** right-click the display with a Display Link item (as usual), then right-click the
  *existing* Display Link. The display is added as a new entry instead of placing a second link.
- Up to 8 entries per link (server config `maxEntriesPerLink`).
- Entries of one link respect each other's lines like separate links do: a list stops where another entry writes.
  Tabs show a red **!** when two entries start on the same line.

### See where a link writes
Look at a Display Link while holding a **Wrench**, **Display Link** or **Clipboard**, or while wearing **Goggles**:
every target is outlined, connected with a line, and labelled with its entry number and line.

### Label templates
Put `{}` in a label to place the value where you want it.

| Label | Shows |
|---|---|
| `We have {} bread in stock` | We have 64 bread in stock |
| `{} items/s` | 12 items/s |
| `Bread` (no `{}`) | Bread 64 (Create's normal format) |

Number formats: `{k}` → 1.2k / 3.4M, `{,}` → 1,234,567, `{.1}` → one decimal (`{.0}`–`{.3}`).
Units such as mB or RPM are kept. Works for every information type that has a label box.

### List row formats
List sources (List matching Items / Fluids, Factory Gauge status, Scoreboard) get a **Row format** box.

| Row format | Shows |
|---|---|
| `{name}: {count}` | Bread: 1k |
| `{count:,}x {name}` | 1,234x Bread |
| *(empty)* | Create's own layout |

`{count}` follows the list's Shortened / Full Number setting; `{count:k}`, `{count:,}` and `{count:.N}` format the full amount.

### Clipboard support
Right-click a Display Link with Create's **Clipboard** to copy all its entries, left-click another link to paste.
Each existing entry keeps its own display and gets the copied information, settings and line. Extra copied entries
are added with the same relative target, so an identical setup next to the original is reproduced completely.

### Ponder
Hold a Display Link and press **W** for a new Ponder scene that walks through all of the above.

## Compatibility
- Existing Display Links keep working: Create's own data is entry 1, extra entries are stored separately.
- Removing the addon falls back to entry 1 of each link. Labels containing `{}` then show the raw text.

## Building
Requires a Java 21 JDK (set `JAVA_HOME`). Create is downloaded from the Create team's Maven automatically.

| Command | What it does |
|---|---|
| `./gradlew build` | builds the mod jar into `build/libs/` |
| `./gradlew runClient` (or `runClient.bat`) | starts a dev client with Create installed |
| `./gradlew runGameTestServer` | runs the automated in-game tests |

Implementation notes are in [NOTES.md](NOTES.md). Bug reports and suggestions: [issues](https://github.com/Rick007110/create-display-link-plus/issues).

## License
LGPL-3.0. Create itself is © the Create Team. The mod icon uses the Create-style logo background and a render of
Create's Display Link; all other assets are original.
