# Changelog

All notable changes to Teyvat Cartographia are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each release is built for one game and loader, named in
the build metadata after the version: `0.2.0+mc1.21.1-neoforge` is version 0.2.0 for Minecraft 1.21.1 on NeoForge.

## [Unreleased]

## [0.3.0] - 2026-10-25

### Added
- The world map shows its zoom ("1/4", "1", "8") beside the thumb of its zoom slider.
- Lists glide when they scroll, like the map's zoom, with no steps: the settings, the plant list, the pin list, the
  list of overlapping pins on the map and the pin editor's types. The wheel and the scrollbar move them freely (they
  stopped at the top of a row before), and their rows dissolve softly at the top and bottom edges while more of the
  list lies beyond them.

### Changed
- Zoom goes from 1/32 to 32, each step twice the one before, on the world map, the minimap (also with its keys) and
  in the settings; 1/3 and 3 are gone (a saved 3 becomes 4, 1/3 becomes 1/4).
- Pins, biome names and the region grid show at every zoom down to 1/8; only at 1/16 and 1/32 the map shows the
  land alone (the tracked pin stays). Pins showed from 1 before, biome names from 1/4 to 4.
- The beam of light and the above / below arrow of the tracked pin show at any distance (they stopped at 225
  blocks). The beam fades in over a second when the pin comes onto the screen and out over a second when it goes to
  the edge (it faded by its way to the edge before).
- The settings scrollbar runs along the list exactly, and the list fills its room down to the buttons: scrolled to
  the end, the last setting sits on the bottom edge (up to a row was left empty below it).

### Fixed
- Dark lines along chunk borders on water (and in grass and leaf colours) behind a player who was moving, flying or
  sailing: a chunk mapped while its neighbour was being unloaded took that neighbour's colour from the default biome.

## [0.2.0] - 2026-10-24

### Added
- **Right-click menu on the world map**: on empty ground **New pin** and, for operators (permission level 2, or
  cheats in singleplayer), **Teleport**, in any dimension the map shows and onto unexplored ground too (the server
  finds the top block there); on a pin **Edit** (**Open** for the spawn and death points) and **Track** or **Stop
  tracking**.
- **Settings > Minimap > Minimap shape** (Square or Round) and **Settings > Map > Interface style** (Rectangular or
  Rounded), independent of each other. Both start square, as before.
- **Plants on the map**, like on Xaero's Minimap: **Settings > Map > Plants** opens the list of every plant of the
  game and your mods, with a search field and a switch for each. All are on at first, also those of mods added later.
  A solid plant switched off (a cactus) shows the ground under it. **Plants** is also among the quick settings of the
  world map (the gear).
- **Settings > Map > View cone and glow**: off, the player arrow is drawn alone on the world map and the minimap.
- A live **Preview** beside the settings shows the setting you point at: its name and what it does, then pictures that
  change as you change it (the map around you, the minimap, a pin on both maps and in the world, the mapping speed,
  samples of each fog). On a small screen or a large interface a button in the header says to lower the GUI scale.
- A **Colours** section in **Settings > Map**: **Terrain detail** (flat colours, shaded slopes, or slopes with contour
  lines), **Deep water shading** (how much deep water darkens) and **Clear shallow water** (how much of the ground
  shows through shallow water; off at first).
- Minimap **Look**: **Inner padding** (a gap between the frame and the map), **Edge fade width** and **Edge fade
  strength**. **Extra information**: **Line spacing**, **Text size** (Auto, or a whole number of pixels so the pixel
  font stays sharp), **Background opacity** of the lines, **Time format** (24-hour, 12-hour, 12-hour with AM/PM) and a
  **Day counter** ("Day 12 · 14:35").
- The clock can show **Both** game and real time; the real time is marked "IRL".
- When the minimap turns with the camera, it shows **E**, **S** and **W** too, fainter than **N**.
- Pins have a fifth step, **In the world**: the marker over a tracked pin with its distance, now with a way to switch
  it off (**Settings > Pins**: Off, Map, Both maps, In the world, Everywhere).
- **Path to the target** in the quick settings of the world map: the path on the world map for every kind of pin at
  once.
- A **Compatibility** tab: first **Minecraft** with **Fog of War: The Nether** and **Fog of War: The End**, then a
  part for each supported mod in the game: **The Aether** (its Fog of War), **The Sift** (its Fog of War, **Rainbow
  ichor**, **Ichor snow**) and **Xaero's Minimap** (the import options).
- A Fog of War of its own in The Nether (smoke and glowing embers), The End (a void with a violet nebula and stars,
  the islands casting a shadow on it), The Aether (a sea of clouds under the islands) and The Sift (a pearly haze in
  the colours of its ichor), on the world map and the minimap.
- The Sift's rainbow ichor and its snow shimmer on the map as they do in the world.
- The world map and the settings have a mouse pointer of their own in the theme's colours: a hand over buttons and
  pins, a text cursor in text fields, a move cross while dragging the map, arrows on sliders. It is a system cursor,
  so it never lags.
- For theme makers: `dimensions/<namespace>/<path>.json` paints one dimension's blocks in colours of its own, and a
  fog's `.json` may set its `"repeat"`.

### Changed
- **Map colours**: every block is drawn in the colour of its texture, like Xaero's Minimap (the average of its top
  face, with the biome tint of grass, leaves and water), in every dimension and for the blocks of any mod. Blocks that
  their own colour handler tints (lily pads, stems, leaf litter, mod leaves and grass) take the colour it gives at
  their place. Read again when resource packs change.
- **The map of earlier versions is reset once**: drawn in the old colours, it left chunk-shaped patches beside the
  newly mapped land, so the world is mapped again as you explore. Pins are kept.
- The settings are in five tabs, each about one thing: **Map**, **Minimap**, **Pins**, **System** and
  **Compatibility**, with a scrollbar you can drag. The settings have no hover tooltips any more: the preview shows
  what a setting does.
- The **Clock**, each kind of **pins** and the **path** to each kind are one slider in steps instead of a switch with
  sub-settings. Older settings files keep working.
- **Interactive pins are for every player**, like the custom ones: anyone creates, edits and deletes them.
- Up to **300 pins** per world (was 100).
- A left click on empty ground of the world map no longer creates a pin (use the right-click menu); a left click on a
  pin still opens it. The quick pin key (**B**) opens the world map with the new pin editor where you stand.
- A pin opened from the pin list goes back to the list when you close it, with your search and scroll as they were.
- The interactive pins are **Sign**, **Party** and **Structure** (they were Waypoint, Statue and Domain), the custom
  pins are named by what they show (**Demon**, **Quest**, **Star**, **Meat**, **Flower**, **Pickaxe**, **Hook**).
  Pins saved or exported by earlier versions keep working.
- Every icon but the player arrow now comes from Nieobie's Game Icon Pack, and all pins look the same size (scaled
  by their visual weight). The dimensions panel has a globe icon.
- For theme makers: the built-in theme is called **Default** (it was Minecraft); `theme.json` has just a name. Some
  icon files are renamed after the pins: `boss`, `food`, `clover`, `statue`, `domain`, `waypoint` and `grave` are
  `demon`, `meat`, `flower`, `party`, `structure`, `sign` and `death`; new are `earth`, `keyboard`, `reset`, `view`,
  `new_pin`, and the mouse pointers in `cursors/`.
- The Rectangular style is flat: no vanilla-style frame and bevel on panels, buttons, fields and sliders.
- The Fog of War is redrawn after the map fog of Genshin Impact: soft slow wisps and glowing motes, repeating every
  512 GUI units (was 440).
- In a bottom corner the lines over the minimap are mirrored: the coordinates nearest to the map.
- The minimap **Size** slider goes only as far as the minimap fits the screen at your GUI scale.
- The zoom slider of the world map eases to the new scale like the mouse wheel does.
- The beam of light fades out as the marker nears the edge of its limiter ellipse instead of vanishing at once.
- The Aether has no cave layers (the space under its floating islands read as caves).
- Without Xaero's Minimap and its waypoints, **Import** in the pin list opens the file dialog at once.
- The names of other mods' dimensions read as titles: "The Aether", not "The aether".
- New defaults for new players and after **Reset**: the minimap 4 units from the screen corner (was 8), opacity 0.85
  (was 0.95), edge fade width 15% (was 20%), line spacing 0 (was 4), no background behind the lines (was 70%), the
  day counter on, every plant on; **New pin where you stand** on **B** and **Stop tracking** on **Z** (they had no
  key). Settings and keys you already changed stay as they are.
- The JAR is 15% smaller (the fog images compressed better, the same pixels).

### Removed
- **The server side: the mod is client side only again.** The interactive pins that operators shared through a server
  since 0.1.1 are no longer shared. In singleplayer they become your own pins the first time you open the world; on a
  dedicated server they stay in its save (`data/teyvatcartographia_pins.dat`).
- The built-in **Genshin Impact** resource pack: its theme differed from the default one only by its round shapes,
  which are now settings. Players who had it get the Default theme with square shapes; pick Round and Rounded to get
  the old look back.
- The **Teleport** button on pins and the confirmation before deleting an interactive pin.

### Fixed
- The path to a pin on an island within about 128 blocks went round the lake to the nearest shore, then jumped back
  across the water and went round once more. It now stops on that shore.
- Thin dark lines along chunk borders after the land changed there (a forest cut down, ground levelled).
- Dark lines in steps along chunk borders, mostly on water, where the edge of the loaded area had passed.
- Water, grass and leaf colours changed in a sharp straight line where two biomes met on a chunk border.
- Plants on the map: double plants (rose bush, peony, lilac, sunflower, tall grass, pitcher plant, small dripleaf)
  showed their stem instead of their top; crops showed their first stage whatever their age (ripe wheat was green);
  every kind of leaves was 14% too dark; mangrove leaves did not take the biome's colour; many plants of other mods
  (flowers, shrubs, branches and leaf litter of Regions Unexplored, The Sift's sculkflower) were not drawn at all.
- Cherry grove crowns were left off the map at first (the game counts cherry and flowering azalea leaves among its
  flowers) and were beige when shown (the game leaves them pink).
- Right after joining a world the minimap's biome line could stay on Plains while you stood still.

## [0.1.3] - 2026-10-24 [YANKED]

Withdrawn the day it came out: its changes, with the fixes found right after, are in 0.2.0.

## [0.1.2] - 2026-10-21

### Added
- **Map search**: the pin list has a search field (click it, or press Ctrl+F on the world map). It finds pins of every
  dimension and the explored biomes by name. A click on a pin flies the map to it and opens it; a click on a biome
  flies to its nearest explored place, and each further click to the next one ("Place 2 of 5"). Enter opens the first
  result, Esc leaves the field.
- The first search in a world reads the explored map in the background once (about 1 second per 1000 map regions)
  and keeps a small index next to the map, so later searches are instant.
- An interactive pin only you have (on a server without the mod) says "local" in the pin list.
- Deleting an interactive pin asks first, in a popup beside the pin panel.

### Changed
- Rows of the pin list: the name uses the whole first line, the coordinates and the distance share the second.
- The pin panel leaves the dimension out of the coordinates line when it does not fit.
- Side panels are never narrower than 176 units, so the buttons and Russian labels fit at small GUI scales.
- The JAR file name has a "v" before the version: `teyvatcartographia-v0.1.2+mc1.21.1-neoforge.jar`.

### Fixed
- Flying or in a boat over deep water, the path to the target was missing; it now leads out across the water to the
  shore.
- The pin editor's type grid scrolls with the mouse wheel when it does not fit, and opens at the selected type.
- A small minimap in a right corner pushed its coordinates off the screen.
- A minimap too large for the screen with its lines is drawn smaller to fit; the size setting keeps its value.
- After a teleport the minimap's biome line could show Plains until you moved.

## [0.1.1] - 2026-10-20

### Added
- **Shared interactive pins** (optional server side): with the mod on a NeoForge server (or in singleplayer or LAN),
  the interactive pins operators place are shared with every player who has the mod. Up to 200 shared pins per world,
  apart from each player's own 100. Both sides stay optional.
- **Export and import** at the bottom of the pin list: save every pin of the world to a `.json` file, add the pins of
  such a file (or of another world's `markers.json`); repeats are skipped.
- **Import from Xaero's Minimap**: the waypoints of the current world are found on their own, otherwise you pick a
  waypoint file of the Xaero world. Death points are left out; waypoints become star pins.

## [0.1.0] - 2026-10-19

The first public release, for Minecraft 1.21.1 on NeoForge 21.1. Client only.

### Added
- **World map** of the world you have explored, drawn from loaded chunks: relief shading, contour lines, water depth,
  a Fog of War over unexplored land, darker at night with lit places keeping their colour. Zoom from 1/16 to 16
  towards the cursor, chunk grid, biome labels, coordinates, explored chunk counter, separate maps per world or
  server and per dimension, The Nether mapped below its roof.
- **Cave layers**: caves around you are mapped in layers 16 blocks high while you are underground; the minimap and
  the world map switch to your layer, and a layer field on the world map shows the others.
- **Minimap**, square or round, north up or turning with the view, with coordinates, biome and a clock under it.
- **Pins**: custom pins for everyone, interactive pins for operators, the world spawn and the last death; up to 100
  per world, a list sorted by distance.
- **Tracking** like a Genshin Impact waypoint: the pin in the 3D world with its distance and a beam of light, a pulse
  key (V), and a walking path round water, lava, trees and mountains, drawn on the maps and as stars in the world.
- Quick switches on the map and a full settings screen; the Minecraft theme by default, a Genshin Impact theme as a
  built-in resource pack, more themes through resource packs.
- English and Russian translations.
- Cached HUD layers, chunk work on worker threads and path searches in short slices: the mod is meant to cost at most
  2 to 3% FPS.

[Unreleased]: https://github.com/Tapacywka/teyvat-cartographia/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/Tapacywka/teyvat-cartographia/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Tapacywka/teyvat-cartographia/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/Tapacywka/teyvat-cartographia/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/Tapacywka/teyvat-cartographia/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/Tapacywka/teyvat-cartographia/releases/tag/v0.1.0
