# Changelog

All notable changes to Teyvat Cartographia. Versions follow `<mod>+mc<minecraft>-<loader>`.

## 0.1.3+mc1.21.1-neoforge (24.10.2026)

### Shapes as settings, one built-in theme
* Two new choices: **Settings > Minimap > Minimap shape** (Square or Round) and **Settings > Map > Interface style**
  (Rectangular or Rounded). They are independent: a round minimap with rectangular menus works, and so does the other
  way round. Both start square, as before.
* The built-in theme is now called **Default** (it was Minecraft). A theme sets only colours, icons and the fog of
  war; `theme.json` has just a name (no `style` block, no `fog_tile`).
* The built-in **Genshin Impact** resource pack is removed: its theme was the same as the default one but for the
  round shapes, which are now the settings above. Players who had it selected get the Default theme with the square
  shapes; pick Round and Rounded to get the old look back.
* The Rectangular style is flat now: the vanilla-style frame and bevel are gone from panels, buttons, fields,
  switches, sliders and tiles; it is the Rounded style with square corners.

### Minimap
* New sliders in **Settings > Minimap > Look**: **Inner padding** (a gap between the frame and the map),
  **Edge fade width** (how much of the radius fades out, 0% for a sharp edge) and **Edge fade strength** (how
  transparent the very edge gets); and in **Extra information**: **Line spacing**, the gap from the map to the lines
  under it and between them.
* **Text size** of the lines under the minimap (Extra information): Auto (one step smaller than the interface, so 2
  screen pixels per font pixel at GUI scale 3, about the size of other minimaps) or a whole number of pixels, so the
  pixel font stays sharp. The lines shrink with their text, and so do the N, E, S, W on the minimap.
* The clock can show **Both** game and real time: two pills side by side, game time on the left. The real time is
  marked "IRL" so it is not taken for the game time.
* **Time format** (Extra information): 24-hour (14:35), 12-hour (2:35) or 12-hour with AM/PM (2:35 PM), for both
  clocks (available while the clock is on).
* In a bottom corner the lines over the minimap are mirrored: the coordinates nearest to the map, the clock on top.
* The **Size** slider goes only as far as the minimap fits the screen with its lines at your GUI scale; the offsets go
  from 0 to 64, the line spacing from 0 to 8, the text size is Auto, 1, 2, 3 or 4.
* When the minimap turns with the camera, it shows **E**, **S** and **W** too, fainter than **N** so north still stands
  out; with north up it shows only **N**, as before.
* **Background opacity** of the lines under the minimap (Extra information): 0% leaves the text only; the default
  70% looks as before.
* **Day counter** (Extra information, on by default): the day of the world, in one pill
  with the game time ("Day 12 · 14:35") or in a pill of its own when the clock shows no game time. When the row is
  wider than the minimap, the real time moves to a line of its own.
* Settings that were a switch with sub-settings are one slider in steps now: the **Clock** (Off, Game, Real, Both),
  each kind of **pins** (Off, Map, Both maps, Everywhere with the beam) and the **path** to each kind (Off, Map, Both
  maps, Everywhere with the stars). Older settings files keep working.
* Right after joining a world the biome line could stay on Plains while the player stood still; it is checked again
  every second and on a change of world or dimension.

### World map
* A pin opened from the pin list goes back to that list when you close it (the cross, Esc, a click on the map, or
  saving or deleting the pin), with your search and scroll as they were. Esc or a click on the map then closes the
  list as before.
* The quick settings of the map (the gear) have a **Path to the target** switch: the path on the world map for every
  kind of pin at once (All settings still set it per kind).
* The zoom slider eases the map to the new scale like the mouse wheel, the + and - keys and the buttons do; its thumb
  follows the cursor at once.

### Pins
* Each kind of pins has five steps (**Settings > Pins**): Off, Map, Both maps, **In the world** (the marker over a
  tracked pin with its distance, which can now be switched off) and Everywhere (its beam of light too). The path to a
  tracked pin keeps four: Off, Map, Both maps, Everywhere (the stars).
* The interactive pins are now **Sign**, **Party** and **Structure** (they were Waypoint, Statue and Domain), with new
  icons; the boss pin has a horned demon. Pins saved, exported or shared by a server of 0.1.2 and earlier keep working:
  the old types are read as the new ones. A server and its players should update together: a 0.1.2 server does not
  know the new types.
* The custom pins are named by what they show: **Demon**, **Quest** (now a shield with an exclamation mark, it was a
  flag), **Star**, **Meat**, **Flower**, **Pickaxe** and **Hook**; the name shows when you point at a tile in the pin
  editor. Old pins keep their kind.
* All pins look the same size: each is scaled by its visual weight instead of its outer box, so solid ones (star,
  flower, grave) are a little smaller and thin ones (pickaxe, hook, party) a little larger.
* The dimensions panel of the world map has its own globe icon (it used the cave layers icon).
* Every icon but the player arrow now comes from Nieobie's Game Icon Pack, like the pins before: the interface icons
  (close, zoom, settings, list, layers, edit, delete, export and import, search and the others), the world spawn (a
  house) and the last death (a skull). The Key binds button of the settings has a keyboard, Reset a circular arrow.
* For theme makers: some icon files have new names, after the pins: `boss`, `food`, `clover`, `statue`, `domain`,
  `waypoint` and `grave` are now `demon`, `meat`, `flower`, `party`, `structure`, `sign` and `death`; new ones are
  `earth`, `keyboard` and `reset`, and the mouse pointers are in `cursors/`.
* Fixed: the path to a pin you cannot walk to (on an island) within about 128 blocks went round the lake to the
  shore nearest to it, then jumped in a straight line back across the water and went round the lake once more. It
  now stops on that shore.

### New defaults
* For new players and after **Reset**: the minimap 4 units from the screen corner (was 8), opacity 0.85 (was 0.95),
  edge fade width 15% (was 20%), line spacing 2 (was 4) and the day counter on. **Pin your position** is on **B** and
  **Stop tracking** on **Z** (they had no key). Settings and keys you already changed stay as they are.

### Mouse pointer
* The world map with its panels and the settings have their own mouse pointer in the theme's colours (from Nieobie's
  Game Icon Pack): the arrow, a hand over buttons, tiles, list rows and pins, a text cursor over text fields, a move
  cross while dragging the map, up and down arrows on the zoom slider, left and right arrows on the settings sliders,
  a no sign over a button that is off, and an arrow with an hourglass while the search reads the biomes. It is a
  system cursor, so it never lags, sized to the interface scale; the usual pointer comes back when the menu closes.

### In-world marker
* The beam of light fades out as the marker nears the edge of its limiter ellipse and is gone when the marker starts
  to ride the ellipse (it used to vanish at once); turning back, it fades in the same way.

### Other mods
* The settings have a **Compatibility** tab with a part for each supported mod that is in the game (no tab
  without them).
* **The Aether**: no cave layers in the Aether (its islands float over the void, so the space under them read as
  caves). Its map has a fog of its own: a sea of white and blue clouds seen
  from above, on the world map and the minimap, with the islands casting a soft shadow on it.
* **Xaero's Minimap**: the import settings moved to its part: the type the waypoints get (Star by default), whether
  hidden waypoints come too (yes by default) and death points (no by default). Without Xaero's Minimap and its
  waypoints, Import in the pin list opens the file dialog at once instead of asking where from.
* The names of other mods' dimensions read as titles: `aether:the_aether` is "The Aether" (it was "The aether").
* For theme makers: a theme may paint a dimension's blocks in colours of its own, in
  `dimensions/<namespace>/<path>.json` (`{ "blocks": { "<block id>": "#AARRGGBB" } }`); other blocks keep their
  texture colours.

### Map colours
* New map colours: every block is drawn in the colour of its texture, like Xaero's Minimap (the average of the
  texture on its top, with the biome tint of grass, leaves and water), in every dimension and for the blocks of any
  mod. Basalt no longer looks like blackstone, and modded worlds look like themselves. Read again when resource
  packs change. Places mapped with an earlier version keep their old colours until you pass by again.
* The settings are rearranged into tabs, each about one thing: **Map** (appearance, what the map shows, the world
  map), **Minimap**, **Pins**, **System** (performance) and, with a supported mod in the game, **Compatibility**,
  where everything made for one mod is (its fog of war too). A thin scrollbar at the right of the list shows where you are; drag it, or click its track to
  jump.
* Leaves and plants of other mods that the game does not tint with the biome keep their own colour (the cyan
  willows of The Sift).
* Fixed: after the land changed across chunk borders (a forest cut down, ground levelled), a thin dark line could stay
  along half of those borders on the map: the chunk mapped first kept the shading of its neighbour's old edge. A
  chunk whose edge changes now has its neighbours on that side mapped again.
* Fixed: where two biomes met on a chunk border, their water, grass or leaf colours changed in a sharp straight
  line; the blend now goes on across chunk borders.
* Plants show on the map, like on Xaero's Minimap: **Settings > Map > Plants** has an arrow that opens the list of
  every plant of the game and your mods, with a search field and a switch for each. At first only the cactus, the
  dead bush and chorus show; switch on flowers, crops, lily pads and the rest as you like. A solid plant switched off
  (a cactus) shows the ground under it. The relief under plants is unchanged.
* Blocks that their own colour handler tints in the world (lily pads, stems, redstone, the leaf litter and bushes of
  Vanilla Backport, leaves and grass of other mods with a palette of their own) take the colour it gives at their
  place, like on Xaero's Minimap: leaf litter is red brown, not dark. Mangrove leaves have their own green, not the
  biome's.
* The Sift's rainbow ichor and its snow shimmer on the map as they do in the world (a still pattern of their own
  colours) instead of plain water blue and white.
* The fog of war is redrawn in every dimension, softer and finer, after the fog of Genshin Impact's map: slow wisps
  and softly glowing motes. It repeats every 512 GUI units (was 440); a theme may set its own `"repeat"` in the fog's
  `.json`.
* A live **Preview** beside the settings shows the setting you point at: its picture, then its name and what it
  does (the settings have no hover tooltips any more); the map around you on the Map tab, the minimap (where it sits on the
  screen, and at its real size) on the Minimap tab, the row you point at on the Pins tab (on the world map, the
  minimap and in the world: the beam or the stars, darkened where the setting leaves them off), the mapping speed,
  regions and memory on the System tab, and samples of the mods' fogs, The Sift's ichor and the Xaero waypoints to
  import on the Compatibility tab, all changing as you change a setting. Without room beside the panel (a small
  screen or a large interface) a button in the header says to lower the GUI scale.
* A **Colours** section in **Settings > Map**: **Terrain detail** (a slider now; the preview shows the three modes
  side by side), **Block colours from the world** (off: one colour per block, the same everywhere), **Deep
  water shading** (how much deep water darkens) and **Clear shallow water** (how much of the ground shows through
  shallow water; off by default).
* New switches in **Compatibility**: **Fog of war: The Aether**, and for Mielon's The Sift **Fog of war: The Sift**,
  **Rainbow ichor** and **Ichor snow** (off: the average colour of their textures, like Xaero's Minimap). The Nether
  and the End always have their own fog. A dimension of another mod has no fog of its own unless made for it.
* Fog of war of its own in the Nether (smoke and glowing embers), the End (a void with a violet nebula and stars;
  its islands cast a shadow on it, like the Aether's) and The Sift (a pearly haze in the colours of its ichor), on the
  world map and the minimap. The overworld keeps the night clouds.

## 0.1.2+mc1.21.1-neoforge

### Map search
* The pin list has a search field (click it, or press Ctrl+F on the world map). It finds pins of every dimension by
  name and the explored biomes by name. A click on a pin flies the map to it and opens it; a click on a biome flies to
  its nearest explored place, and each further click to the next one ("Place 2 of 5"). The found spot sends out a few
  rings when the map arrives. Enter opens the first result, Esc leaves the field.
* The first search in a world reads the explored map in the background once (about 1 second per 1000 map regions)
  and keeps a small index next to the map, so later searches are instant.

### Pin list and panels
* Rows of the pin list: the name uses the whole first line; the coordinates and the distance share the second.
* An interactive pin only you have (on a server without the mod, or one not moved to the shared pins) says
  "local" in the list.
* Deleting an interactive pin asks first, in a popup beside the pin panel ("Delete" or "Cancel"; Esc or a click
  elsewhere cancels). For a shared pin it says the pin disappears for every player. Custom pins are deleted at once,
  as before.
* The pin panel leaves the dimension out of the coordinates line when it does not fit.
* Side panels are never narrower than 176 units, so the buttons and Russian labels fit on small GUI scales (2K at
  GUI 6).

### Fixes
* Flying or in a boat over deep water, the path to the target was missing; now it leads out across the water to the
  shore, as it does for a swimming player.
* The pin editor's type grid scrolls with the mouse wheel when it does not fit above the pin count (small windows,
  large GUI scales), and opens showing the selected type.
* A small minimap in a right corner no longer pushes its coordinates off the screen: a pill wider than the map is
  shifted inward at the screen edge.
* A minimap too large for the screen with its pills (a large size at a large GUI scale) is drawn smaller to fit;
  the size setting keeps its value.
* After a teleport the minimap's biome line could show Plains until the player moved; it now shows the biome as
  soon as the chunk arrives.

### Other
* The JAR file name has a "v" before the version: `teyvatcartographia-v0.1.2+mc1.21.1-neoforge.jar`.

## 0.1.1+mc1.21.1-neoforge

### Shared interactive pins (optional server side)
* Install the mod on a NeoForge server (or play singleplayer or LAN) and the interactive pins (waypoint, statue,
  domain) operators place are shared with every player who has the mod. Operators can track, teleport to, rename,
  retype and remove them; other players can only track them. The server keeps them in the world and checks the
  rights of every change. Up to 200 shared pins per world, apart from each player's own 100.
* On a server with the mod, an operator with interactive pins of their own there is asked once whether to move them
  to the shared ones.
* Both sides stay optional: players without the mod can join a server with it, and on a server without it
  interactive pins stay on your computer, as before (the pin panel says so).

### Pins
* Export and import: buttons at the bottom of the pin list. Export saves every pin of the world (all dimensions) to a
  `.json` file you choose; Import adds the pins of such a file (or of another world's `markers.json`). Pins that are
  already there are skipped, the 100 pin limit holds, interactive pins come in only for operators.
* Import from Xaero's Minimap (its waypoints also show on Xaero's World Map): the waypoints of the current world are
  found on their own, otherwise you pick any waypoint file of the Xaero world (a game without Xaero data just says
  so). Death points are left out; waypoints become star pins.

## 0.1.0+mc1.21.1-neoforge (first release)

The first public release for Minecraft 1.21.1 on NeoForge 21.1. Client only.

### World map
* Full-screen map of the world you have explored, drawn from loaded chunks: soft painted palette, relief shading,
  contour lines, water depth, fog of war over unexplored land, darker at night with lit places keeping their colour.
* Zoom from 1/16 to 16 towards the cursor, panning with the mouse or keys, chunk grid, biome labels, biome under the
  cursor, player and cursor coordinates, explored chunk counter.
* Separate maps per world or server and per dimension, saved on disk; dimension list with vanilla dimensions first
  and modded ones in alphabetical order; the Nether mapped below its roof.
* Off-screen indicators for the player and the tracked pin.

### Cave layers
* Caves around you are mapped in layers 16 blocks high while you are underground (Overworld).
* The minimap and the world map switch to your layer; a layer field at the right edge of the world map shows the
  others, like the Genshin Impact map. Pins belong to their layer.

### Minimap
* Square (Minecraft theme) or round (Genshin Impact theme) minimap with the player arrow, view cone, pins, the path
  and the tracked pin on the rim.
* North up or rotating with the view; size, corner, offsets, opacity, zoom and update rate.
* Coordinates, biome (long names wrap) and a clock under it.

### Pins and tracking
* Custom pins for everyone, Interactive pins (waypoint, statue, domain) for operators with teleport, and System
  points: world spawn and last death.
* Up to 100 pins per world, pin list sorted by distance, rename, retype, delete.
* Track a pin: its colour follows its kind (custom orange, interactive mint, system violet).

### Pin in the world and path
* The tracked pin appears in the 3D world like a Genshin Impact waypoint: constant size, visible through terrain,
  distance in blocks, a beam of light within 225 blocks, an arrow when the target is above or below you.
* Pulse key (V): a diamond closes in on the pin and the pin and the path stars show again.
* Walking path to the target: a precise path over loaded terrain following a coarse path over the explored map (up
  to 1024 blocks), round water, lava, trees, walls and mountains. Drawn as a dash-dot line on the map and the minimap
  (without the walked part) and as twinkling stars in the world near the camera.

### Settings and themes
* Quick switches on the map and a full settings screen (also from the mod list) with foldable sub-sections, settings
  per pin kind for the map, the minimap, the beam and the path.
* Minecraft theme by default; Genshin Impact theme as a built-in resource pack. Custom themes through resource packs.
* English and Russian translations.

### Performance
* Cached HUD layers, no per-frame allocations, chunk work on worker threads, path searches in short slices: the mod
  is meant to cost at most 2 to 3% FPS.
