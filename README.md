<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/banner.png" alt="Teyvat Cartographia" width="100%">
</p>

<p align="center">
  <a href="https://modrinth.com/mod/teyvat-cartographia"><img src="https://img.shields.io/modrinth/dt/teyvat-cartographia?style=flat&logo=modrinth&label=modrinth&color=00AF5C" alt="Modrinth downloads"></a>
  <a href="https://github.com/Tapacywka/teyvat-cartographia/releases/latest"><img src="https://img.shields.io/github/v/release/Tapacywka/teyvat-cartographia?style=flat&label=release&color=E6B85C" alt="Latest release"></a>
  <a href="https://github.com/Tapacywka/teyvat-cartographia/releases"><img src="https://img.shields.io/github/downloads/Tapacywka/teyvat-cartographia/total?style=flat&logo=github&label=downloads&color=4C9F70" alt="GitHub downloads"></a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/NeoForge-21.1-D7742F?style=flat" alt="NeoForge 21.1">
  <img src="https://img.shields.io/badge/side-client%20%2B%20optional%20server-5B8DEF?style=flat" alt="Client side, optional server side">
  <a href="https://github.com/Tapacywka/teyvat-cartographia/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-8A8F98?style=flat" alt="MIT license"></a>
</p>

<p align="center">
  <b>The world you explore becomes a soft painted map.</b><br>
  Full-screen world map, HUD minimap, cave layers, pins, a Genshin Impact style waypoint in the world and a walking path to it.
</p>

<p align="center">
  <a href="https://modrinth.com/mod/teyvat-cartographia"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/modrinth_vector.svg" alt="Available on Modrinth" height="56"></a>
  <a href="https://github.com/Tapacywka/teyvat-cartographia/releases/latest"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/github_vector.svg" alt="Available on GitHub" height="56"></a>
  <a href="https://neoforged.net"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/neoforge_vector.svg" alt="Supported on NeoForge" height="56"></a>
</p>

<p align="center">
  <a href="#features"><b>✨&nbsp;Features</b></a>
  &nbsp;&nbsp;|&nbsp;&nbsp;
  <a href="#controls"><b>🎮&nbsp;Controls</b></a>
  &nbsp;&nbsp;|&nbsp;&nbsp;
  <a href="#themes"><b>🎨&nbsp;Themes</b></a>
  &nbsp;&nbsp;|&nbsp;&nbsp;
  <a href="https://github.com/Tapacywka/teyvat-cartographia/issues/new/choose"><b>🐞&nbsp;Report a bug</b></a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/hud-minecraft.jpg" alt="Minimap, tracked pin with its beam of light and the walking path stars" width="100%">
</p>

## 📚 Contents

- [About](#about)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Controls](#controls)
- [Features](#features)
  - [World map](#world-map) &middot; [Cave layers](#cave-layers) &middot; [Minimap](#minimap) &middot; [Pins and tracking](#pins-and-tracking) &middot; [Pin in the world](#pin-in-the-world) &middot; [Walking path](#walking-path) &middot; [Settings](#settings)
- [Themes](#themes)
- [Performance](#performance)
- [Where data is stored](#where-data-is-stored)
- [Building from source](#building-from-source)
- [Bug reports and ideas](#bug-reports-and-ideas)
- [License and credits](#license-and-credits)

<h2 id="about">📖 About</h2>

**Teyvat Cartographia** is a client-side Minecraft mod (with an optional server side for shared pins) that brings a
**Genshin Impact inspired world map and minimap** to Minecraft. The chunks you load are drawn as a painted map with a
soft palette, relief shading, water depth and contour lines, while fog of war covers everything you have not seen yet.
Place pins, search for them and for biomes, track them, follow the path and find your way back home.

| | |
|---|---|
| Minecraft | **1.21.1** (Java Edition) |
| Mod loader | **NeoForge 21.1** (built against 21.1.257) |
| Java | **21** |
| Side | **Client**, optional on the server. Servers don't need it, so you can join vanilla and modded servers. On a NeoForge server with the mod, operators' interactive pins are shared with every player. |
| Languages | English, Russian |
| License | [MIT](https://github.com/Tapacywka/teyvat-cartographia/blob/main/LICENSE) |

> **Note:** Teyvat Cartographia is a fan-made mod. It is not affiliated with or endorsed by HoYoverse, and it contains
> no Genshin Impact assets: every texture, icon and the fog are original or CC0 (see [credits](#license-and-credits)).

<h2 id="screenshots">📸 Screenshots</h2>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/world-map.jpg" alt="World map with pins and the walking path" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/world-map-night.jpg" alt="At night the map darkens, lit places keep their colour" width="49%">
  <br>
  <sub><b>Left:</b> World map with pins and the walking path.<br>
  <b>Right:</b> At night the map darkens, lit places keep their colour.</sub>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/cave-layers.jpg" alt="Cave layer with the layer field at the right edge" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/minimap-cave.jpg" alt="The minimap switches to your cave layer" width="49%">
  <br>
  <sub><b>Left:</b> Cave layer with the layer field at the right edge.<br>
  <b>Right:</b> The minimap switches to your cave layer.</sub>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/pin-list.jpg" alt="Pin list sorted by distance" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/search.jpg" alt="Map search: pins and explored biomes by name" width="49%">
  <br>
  <sub><b>Left:</b> Pin list sorted by distance.<br>
  <b>Right:</b> Map search (Ctrl+F): pins and explored biomes by name.</sub>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/world-map-path.jpg" alt="The walking path goes round the water" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/settings.jpg" alt="Settings with foldable sections" width="49%">
  <br>
  <sub><b>Left:</b> The walking path goes round the water.<br>
  <b>Right:</b> Settings with foldable sections.</sub>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/world-map-genshin.jpg" alt="Rounded interface" width="49%">
  <img src="https://raw.githubusercontent.com/Tapacywka/teyvat-cartographia/main/.github/images/hud-genshin.jpg" alt="Round minimap" width="49%">
  <br>
  <sub><b>Left:</b> Rounded interface (Settings > Appearance > Interface style).<br>
  <b>Right:</b> Round minimap (Settings > Appearance > Minimap shape).</sub>
</p>

<h2 id="installation">📥 Installation</h2>

1. Install [NeoForge](https://neoforged.net) 21.1 for Minecraft 1.21.1.
2. Download `teyvatcartographia-v0.1.2+mc1.21.1-neoforge.jar` from
   [Modrinth](https://modrinth.com/mod/teyvat-cartographia) or the
   [latest GitHub release](https://github.com/Tapacywka/teyvat-cartographia/releases/latest) and put it into your
   `mods/` folder.
3. Launch the game and press **M** in a world.
4. Optional: make the minimap round or the interface rounded in **Settings > Appearance**.

<h2 id="controls">🎮 Controls</h2>

| Where | Action | Control |
|---|---|---|
| In game | Open world map | **M** (rebindable) |
| In game | Toggle minimap / minimap zoom in / zoom out | **N** / **=** / **-** (rebindable) |
| In game | Pin your position, stop tracking | unbound by default: Options > Controls > Key Binds > *Teyvat Cartographia* |
| In game | Pulse the tracked pin (shows the pin and the path stars again) | **V** (rebindable) |
| World map | Pan | drag with the left mouse button, or WASD / arrow keys |
| World map | Zoom (keeps the point under the cursor) | mouse wheel, `+`/`-`, or the slider on the left |
| World map | Create a pin | left-click empty ground, choose icon and name, **Confirm** |
| World map | Overlapping pins | left-click the spot, pick one from the list in the lower right corner |
| World map | Edit or track a pin | left-click it: rename, type, delete, **Track**; **Teleport** for Interactive pins (operators only) |
| World map | Close the pin panel | left-click empty ground; a left click on another pin opens that one |
| World map | Search pins and biomes | **Ctrl+F**, or the field at the top of the pin list; **Enter** opens the first result |
| World map | Centre on the player | **Space**, the locate button, or the player's edge indicator |
| World map | Cave layers | the layer button at the right edge (see [Cave layers](#cave-layers)) |
| World map | Settings / pin list / dimensions | gear and list buttons (bottom left), compass (bottom right) |
| World map | Close | **Esc** or **M** (Esc closes an open side panel first) |

<h2 id="features">✨ Features</h2>

<h3 id="world-map">World map</h3>

* **Cartography from real world data.** Loaded client chunks are sampled (surface height, block map colour, water
  depth, biome tint, block light) and shaded on background threads into 256 x 256 block regions. Nothing is loaded or
  generated just for the map. Regions are cached in memory and on disk, so the map grows as you explore and survives
  restarts.
* Fog of war over unexplored land, painted regions with relief shading and contour lines. The map darkens at night,
  while torches, lava and other lights keep their colour.
* Biome name labels (zoom 1/4 to 4), biome under the cursor, player and cursor coordinates, explored chunk counter,
  zoom slider (1/16 to 16), optional chunk grid.
* **Search** in the pin list (or **Ctrl+F**): pins of every dimension by name, and the explored biomes by name. A
  click flies the map to the result, switching the dimension if needed. A biome goes to its nearest explored place,
  and each further click to the next one ("Place 2 of 5"); the spot is marked with a few rings.
* Off-screen indicators for the player (green) and the tracked pin (in its kind's colour).
* Separate maps per world or server and per dimension. The compass opens the list of mapped dimensions: Overworld,
  The Nether, The End, then modded dimensions in alphabetical order. The Nether is mapped below its roof.

<h3 id="cave-layers">Cave layers</h3>

* In the Overworld, once you are underground (at least 4 solid blocks above your head), the caves within 32 blocks of
  you are mapped in layers 16 blocks high: floor in block colours, water, lava and ores marked, shafts darker.
* The minimap and the world map switch to the layer you are in.
* The layer field at the right edge of the world map works like the Genshin Impact map: the large button is the
  surface, a click opens the cave layers in height order around it (at most 5, scrolled with the wheel or the small
  arrows), a click on the large button goes back to the surface, and another click closes the field.
* Pins made on a cave layer belong to it; pins of other layers are fainter with a small arrow up or down. Cave layers
  can be switched off in *Cartography*.

<h3 id="minimap">Minimap</h3>

* Square or round (**Settings > Appearance > Minimap shape**), with "N" on the rim, the player arrow and view
  cone, pins, the walking path and the tracked pin held on the rim when it is far.
* North up or rotating with the view; size, corner, offsets, opacity, zoom and update rate (30 redraws a second while
  you move by default). An inner padding between the frame and the map, and how wide and how strong its edge fades.
* Coordinates, biome (long names wrap onto two lines), a clock (game time, your computer's time, or both side by
  side) and a day counter under it, with their own spacing and text size (Auto: a step smaller than the interface, always sharp).

<h3 id="pins-and-tracking">Pins and tracking</h3>

* Three kinds of pins:
  * **Custom**, for every player: boss, quest, star, food, plants, ores and fishing;
  * **Interactive**, created by operators only (permission level 2, or cheats in singleplayer): waypoint, statue and
    domain. Only these offer **Teleport**; other players can see and track them;
  * **System**: automatic points from real data, the world spawn and your last death location.
* Rename, retype, delete (an interactive pin asks first); up to 100 pins per world, a pin list sorted by distance,
  saved per world and dimension.
* Track a pin or an automatic point. The tracked pin's colour depends on its kind: custom orange, interactive mint,
  system violet. Tracking an automatic point ends on arrival.
* Distances are in blocks everywhere and in 3D: from your feet to the floor of the point.
* **Shared interactive pins**: with the mod also on a NeoForge server (and in singleplayer and LAN), the interactive
  pins operators place are shared with every player who has the mod. Operators track, teleport to, rename, retype and
  remove them; other players can only track them. The server keeps them in the world and checks the rights of every
  change; up to 200 per world, apart from each player's own 100. An operator with interactive pins of their own on such
  a server is asked once whether to move them to the shared ones; the ones kept say *local* in the pin list. Players
  without the mod can still join.
* **Export and import** from the pin list: save every pin of the world to a `.json` file, add the pins of such a file
  (or of another world's `markers.json`); repeats are skipped.
* **Import from Xaero's Minimap** (its waypoints also show on Xaero's World Map): the waypoints of the current world
  are found on their own, or pick any waypoint file of a Xaero world. Death points are left out.

<h3 id="pin-in-the-world">Pin in the world</h3>

* The tracked pin is shown in the 3D world like a Genshin Impact waypoint for a minute after you start tracking or
  press the pulse key: constant size, visible through terrain, the distance in blocks under it.
* Off screen it slides along an ellipse around the centre, above the hotbar and around the minimap.
* Within 225 blocks a beam of light in the pin's colour rises from the target (hidden behind terrain, drawn after the
  clouds); closer than 10 blocks the pin fades out. A white arrow shows a target well above or below you.
* The pulse key sends a thin diamond closing in on the pin in one second, and the glyph flashes when it arrives.

<h3 id="walking-path">Walking path</h3>

* Searched once when you start tracking (and by the pulse key when you are more than 8 blocks off the line), never
  while you walk.
* A precise path over the loaded terrain (128 blocks around you) follows a coarse path over the explored map (up to
  1024 blocks). It goes round deep water, lava, tree trunks, walls and mountains, takes steps of up to 2 blocks
  straight on, and leads out of the water when you are swimming.
* The world map and the minimap draw it as a dash-dot line in the pin's colour (pale white through unexplored land),
  without the part you have already walked. In the world, small twinkling stars follow the path like particles, only
  within 32 blocks of the camera.

<h3 id="settings">Settings</h3>

* The gear on the map holds quick switches: the three pin kinds, biome names, chunk grid.
* The full settings screen (also from the NeoForge mod list) has *Appearance*, *World map*, *Minimap* (display, size
  and position, extra information), *Pins* (display and path to the target for each kind) and *Cartography*.
  Sub-sections fold and unfold with a click.
* A setting with parts is one slider in steps: each pin kind and its path go Off, Map, Both maps, Everywhere (the
  beam, or the stars, in the world too), and the clock Off, Game, Real, Both.
* Reset to defaults and a shortcut to key binds. Stored in `config/teyvatcartographia-client.toml`.

<h2 id="themes">🎨 Themes</h2>

**Settings > Appearance** has three choices, each on its own:

* **Theme**: colours, icons and the fog of war. **Default** (`default`) is built in and always available; a resource
  pack may add more, and the list shows only themes whose pack is enabled. Other themes fall back to its assets.
* **Minimap shape**: **Square** (the default) or **Round**.
* **Interface style**: **Rectangular** (the default), with a soft vanilla-style bevel on panels, buttons and tiles, or
  **Rounded**, with circles, capsules and rounded corners.

So the minimap can be round while the menus stay rectangular, or the other way round, with any theme.

A custom theme comes as a resource pack: put it into `resourcepacks/`, enable it in the game, and its theme appears in
the setting.

<details>
<summary><b>Making your own theme</b></summary>

<br>

Every theme follows the same layout, `assets/teyvatcartographia/themes/<id>/`:

| File | Required | Content |
|---|---|---|
| `theme.json` | yes | `{ "name": "..." }`: a translation key (`teyvatcartographia.theme.<id>`) or plain text. Makes the theme selectable. |
| `colors.json` | no | Colours as `"key": "#AARRGGBB"` or `"#RRGGBB"`. Keys are the colour fields of `render/Theme.java`; `themes/default/colors.json` lists them all. Missing keys keep the built-in defaults. |
| `icons/64/<name>.png` | no (64 x 64) | Icons. Missing ones (or the whole folder) fall back to the `default` icons. |
| `fog.png` | no | Seamless background for unexplored space (512 x 512 in `default`), shown at 440 GUI units per repeat whatever its resolution. Falls back to the `default` fog. |

A theme does not set any shapes: the minimap shape and the interface style are the player's settings. Colour keys
are named by their role, not their shape: `icon_button_*` is a round or a square button, `info_box_*` a capsule or a
rectangle. Resource packs are merged, so a pack can change a few keys of any theme or add a new theme. Changes apply
when the theme is picked, or on F3+T.

</details>

<h2 id="performance">⚡ Performance</h2>

The mod is meant to cost at most 2 to 3% FPS:

* Every HUD element is drawn into a cached texture and redrawn only when what it shows changes; a still minimap costs
  about one textured quad per frame, and a moving one is redrawn at most at its update rate.
* Per-frame paths do not allocate memory. Chunk reading and shading run on worker threads; the path searches run on
  the main thread in slices of at most 1 ms per tick, and only while the path is shown.
* A chunk whose data did not change triggers no texture upload, no disk write and no minimap refresh. Biome tints are
  sampled on a 4 x 4 block grid and interpolated.
* The first search in a world reads the explored map once on a background thread (about 1 second per 1000 map
  regions) and keeps a small biome index; later searches only read what was explored since.
* On the world map, the fog is one quad, and the grid, the path and the labels are each drawn in a single call. Far
  out the map is drawn from small copies of the regions, so even zoom 1/16 costs only a few textures.

<h2 id="where-data-is-stored">💾 Where data is stored</h2>

| What | Where |
|---|---|
| Settings | `config/teyvatcartographia-client.toml` |
| Map regions | `teyvatcartographia/<world>/<dimension>/r.<x>.<z>.png` + `.dat` (heights and metadata) + `.lod.png` (64 x 64 thumbnail for far zoom) + `index.txt` |
| Cave layers | `teyvatcartographia/<world>/<dimension>/caves/b<layer>/` |
| Biome index for the search | `teyvatcartographia/<world>/<dimension>/biomes.idx` |
| Pins and the tracked target | `teyvatcartographia/<world>/markers.json` |
| Exported pins (default folder) | `teyvatcartographia/exports/` |
| Shared interactive pins (on the server) | `<world save>/data/teyvatcartographia_pins.dat` |

`<world>` is `sp_<save folder>` in singleplayer and `mp_<server address>` on servers.

<h2 id="building-from-source">🛠️ Building from source</h2>

Requirements: JDK 21 (Gradle's toolchain support will use or download one), internet on the first build.

```bash
./gradlew build
```

On Windows: `gradlew.bat build`. The mod JAR is written to `build/libs/teyvatcartographia-v<version>.jar`.
`./gradlew runClient` starts a development client with the mod.

<h2 id="bug-reports-and-ideas">🐞 Bug reports and ideas</h2>

Found a bug, a crash or have an idea? [Open an issue](https://github.com/Tapacywka/teyvat-cartographia/issues/new/choose)
on GitHub and pick the matching form. A few tips that help a lot:

1. Make sure you are on the latest version and look through the
   [existing issues](https://github.com/Tapacywka/teyvat-cartographia/issues?q=is%3Aissue) first.
2. If you play with other mods, try to reproduce the problem with Teyvat Cartographia alone.
3. Attach `logs/latest.log` (and the crash report from `crash-reports/` after a crash) as a link to a
   [GitHub Gist](https://gist.github.com) or a paste site, plus a screenshot if the problem is visual.

Release notes for every version are in
[CHANGELOG.md](https://github.com/Tapacywka/teyvat-cartographia/blob/main/CHANGELOG.md).

<h2 id="license-and-credits">📜 License and credits</h2>

Teyvat Cartographia is released under the [MIT License](https://github.com/Tapacywka/teyvat-cartographia/blob/main/LICENSE).
Source code: [github.com/Tapacywka/teyvat-cartographia](https://github.com/Tapacywka/teyvat-cartographia).

* All textures and UI icons are original. The UI icons are drawn from exact vector geometry with 8x supersampling,
  and every pin gets a thin dark outline.
* The ten pin glyphs (boss, quest, star, food, plants, ores, fishing, waypoint, statue, domain) come from
  [Game Icon Pack](https://nieobie.itch.io/free-icons) by Nieobie, released under
  [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). Thank you!
* All text uses the vanilla Minecraft font. No Genshin Impact assets are included.
