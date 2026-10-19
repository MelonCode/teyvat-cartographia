# Teyvat Cartographia

A client-side Minecraft mod that brings a **Genshin Impact inspired world map and minimap** to Minecraft:
the world you explore is drawn as a painted map (soft palette, relief shading, water depth, contour lines),
with a full-screen interactive map, a HUD minimap, pins, navigation with a walking path, a waypoint in the 3D
world, cave layers and a styled settings UI.

| | |
|---|---|
| Minecraft | **1.21.1** (Java Edition) |
| Mod loader | **NeoForge 21.1** (built against 21.1.257) |
| Java | **21** |
| Side | Client only (servers don't need it; you can join vanilla and other servers) |
| Licence | MIT (see `LICENSE`) |

Teyvat Cartographia is a fan-made mod. It is not affiliated with or endorsed by HoYoverse, and it contains no
Genshin Impact assets: every texture, icon and the fog are original or CC0 (see *Credits*).

## Installation

1. Install NeoForge 21.1 for Minecraft 1.21.1.
2. Put `teyvatcartographia-0.1.0+mc1.21.1-neoforge.jar` into your `mods/` folder.
3. Launch the game and press **M** in a world.
4. Optional: enable the built-in **Genshin Impact** resource pack in **Options > Resource Packs**, then pick the
   Genshin Impact theme in the mod settings.

## Controls

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
| World map | Centre on the player | **Space**, the locate button, or the player's edge indicator |
| World map | Cave layers | the layer button at the right edge (see *Cave layers*) |
| World map | Settings / pin list / dimensions | gear and list buttons (bottom left), compass (bottom right) |
| World map | Close | **Esc** or **M** (Esc closes an open side panel first) |

## Features

* **Cartography from real world data.** Loaded client chunks are sampled (surface height, block map colour,
  water depth, biome tint, block light) and shaded on background threads into 256 x 256 block regions. Nothing is
  loaded or generated just for the map. Regions are cached in memory and on disk, so the map grows as you explore and
  survives restarts. Separate maps per world or server and per dimension; the Nether is mapped below the roof.
  The map darkens at night, while torches, lava and other lights keep their colour.
* **World map**: fog of war over unexplored land, painted regions with relief shading and contour lines, biome name
  labels (zoom 1/4 to 4), biome under the cursor, player and cursor coordinates, explored chunk counter, zoom slider
  (1/16 to 16), optional chunk grid, off-screen indicators for the player (green) and the tracked pin (in its kind's
  colour). Far out the map is drawn from small copies of the regions, so even 1/16 costs a few textures.
* **Dimensions**: the compass opens the list of mapped dimensions: Overworld, The Nether, The End, then modded
  dimensions in alphabetical order.
* **Cave layers** (Overworld): underground (at least 4 solid blocks above your head) the caves within 32 blocks of
  you are mapped in layers 16 blocks high: floor in block colours, water, lava and ores marked, shafts darker. The
  minimap and the world map switch to the layer you are in. The layer field at the right edge of the world map works
  like the Genshin Impact map: the large button is the surface, a click opens the cave layers in height order around
  it (at most 5, scrolled with the wheel or the small arrows), a click on the large button goes back to the surface,
  and another click closes the field. Pins made on a cave layer belong to it; pins of other layers are fainter with a
  small arrow up or down. Can be switched off in *Cartography*.
* **Minimap**: square in the Minecraft theme, round in the Genshin Impact theme, with "N" on the rim, player arrow
  and view cone, pins, the walking path and the tracked pin held on the rim when it is far. North up or rotate with
  view; size, corner, offsets, opacity, zoom and update rate (30 redraws a second while you move by default).
  Coordinates, biome (long names wrap onto two lines) and a clock (game time or your computer's time) under it.
* **Pins** in three kinds:
  * **Custom**, for every player: boss, quest, star, food, plants, ores and fishing;
  * **Interactive**, created by operators only (permission level 2, or cheats in singleplayer): waypoint, statue and
    domain; only these offer **Teleport**. Other players can see and track them;
  * **System**: automatic points from real data, the world spawn and your last death location.

  Rename, retype, delete, 100 pins per world, a pin list sorted by distance, saved per world and dimension.
* **Tracking**: track a pin or an automatic point. The tracked pin's colour depends on its kind: custom orange,
  interactive mint, system violet. Tracking an automatic point ends on arrival.
* **Pin in the world**: the tracked pin is shown in the 3D world like a Genshin Impact waypoint for a minute after
  you start tracking or press the pulse key: constant size, visible through terrain, the distance in blocks under it.
  Off screen it slides along an ellipse around the centre, above the hotbar and around the minimap. Within 225
  blocks a beam of light in the pin's colour rises from the target (hidden behind terrain, drawn after the clouds);
  closer than 10 blocks the pin fades out. A white arrow shows a target well above or below you. The pulse key sends
  a thin diamond closing in on the pin in one second, and the glyph flashes when it arrives.
* **Walking path**: searched once when you start tracking (and by the pulse key when you are more than 8 blocks off
  the line), never while you walk. A precise path over the loaded terrain (128 blocks around you) follows a coarse
  path over the explored map (up to 1024 blocks). It goes round deep water, lava, tree trunks, walls and mountains,
  takes steps of up to 2 blocks straight on, and leads out of the water when you are swimming. The world map and the
  minimap draw it as a dash-dot line in the pin's colour (pale white through unexplored land), without the part you
  have already walked. In the world, small twinkling stars follow the path like particles, only within 32 blocks of
  the camera.
* **Distances** are in blocks everywhere and in 3D: from your feet to the floor of the point.
* **Settings**: the gear on the map holds quick switches (the three pin kinds, biome names, chunk grid). The full
  settings screen (also from the NeoForge mod list) has *Appearance*, *World map*, *Minimap* (display, size and
  position, extra information), *Pins* (display and path to the target for each kind, with the beam, the lines and
  the stars) and *Cartography*. Sub-sections fold and unfold with a click. Reset to defaults and a shortcut to key
  binds. Stored in `config/teyvatcartographia-client.toml`.

## Where data is stored

* Settings: `config/teyvatcartographia-client.toml`
* Map regions: `teyvatcartographia/<world>/<dimension>/r.<x>.<z>.png` + `.dat` (heights and metadata) +
  `.lod.png` (64 x 64 thumbnail for far zoom) + `index.txt`; cave layers in `<dimension>/caves/b<layer>/`
* Pins and the tracked target: `teyvatcartographia/<world>/markers.json`

`<world>` is `sp_<save folder>` in singleplayer and `mp_<server address>` on servers.

## Building from source

Requirements: JDK 21 (Gradle's toolchain support will use or download one), internet on the first build.

```bash
./gradlew build
```

On Windows: `gradlew.bat build`. The mod JAR is written to `build/libs/teyvatcartographia-0.1.0+mc1.21.1-neoforge.jar`.
`gradlew runClient` starts a development client with the mod.

## Themes (resource packs)

The theme is picked in **Settings > Appearance > Theme**. The list shows only themes whose resource pack is
enabled:

* **Minecraft** (`minecraft`), the default and always available: square widgets with a soft vanilla-style bevel, a
  square framed minimap, smooth icons and a night-sky fog. Other themes fall back to its assets.
* **Genshin Impact** (`genshin`) ships inside the mod JAR as a built-in resource pack, off by default: soft round
  shapes with the same colours, icons and fog. Enable the **Genshin Impact** pack in **Options > Resource Packs**,
  then pick the theme in the settings. If the pack is disabled again, the map switches back to Minecraft and returns
  to Genshin Impact once the pack is back.

A custom theme works the same way: put the resource pack into `resourcepacks/`, enable it in the game, and its
theme appears in the setting.

Every theme follows the same layout, `assets/teyvatcartographia/themes/<id>/`:

| File | Required | Content |
|---|---|---|
| `theme.json` | yes | Name (translation key `teyvatcartographia.theme.<id>` or plain text) and `style`, see below. Makes the theme selectable. |
| `colors.json` | no | Colours as `"key": "#AARRGGBB"` or `"#RRGGBB"`. Keys are the colour fields of `render/Theme.java`; `themes/minecraft/colors.json` lists them all. Missing keys keep the built-in defaults. |
| `icons/64/<name>.png` | no (64 x 64) | Icons. Missing ones (or the whole folder) fall back to the `minecraft` icons. |
| `fog.png` | no | Seamless background for unexplored space (512 x 512 in `minecraft`). Falls back to the `minecraft` fog. |

The `style` keys in `theme.json`:

* `"shape"`: `"round"` or `"square"`;
* `"fog_tile"`: GUI units per repeat of the fog.

Colour keys are named by their role, not their shape. For example, `icon_button_*` is a round button in
`genshin` and a square one in `minecraft`, and `info_box_*` is a capsule or a rectangle. Resource packs are
merged, so a pack can change a few keys of any theme or add a new theme. Changes apply when the theme is
picked, or on F3+T.

The Genshin Impact pack lives in `src/main/resources/resourcepacks/genshin` and is registered as a built-in pack in
the main class.

## Performance

The mod is meant to cost at most 2 to 3% FPS:

* Every HUD element is drawn into a cached texture and redrawn only when what it shows changes; a still minimap costs
  about one textured quad per frame, and a moving one is redrawn at most at its update rate.
* Per-frame paths do not allocate memory. Chunk reading and shading run on worker threads; the path searches run on
  the main thread in slices of at most 1 ms per tick, and only while the path is shown.
* A chunk whose data did not change triggers no texture upload, no disk write and no minimap refresh. Biome tints
  are sampled on a 4 x 4 block grid and interpolated.
* On the world map, the fog is one quad, and the grid, the path and the labels are each drawn in a single call.

## Credits and assets

All textures and UI icons are original. The ten pin glyphs (boss, quest, star, food, plants, ores, fishing, waypoint,
statue, domain) come from [Game Icon Pack](https://nieobie.itch.io/free-icons) by Nieobie, released under CC0; thank
you! The UI icons are drawn from exact vector geometry with 8x supersampling, and every pin gets a thin dark outline.
All text uses the vanilla Minecraft font. No Genshin Impact assets are included. Release notes are in `CHANGELOG.md`.
