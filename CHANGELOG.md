# Changelog

All notable changes to Teyvat Cartographia. Versions follow `<mod>+mc<minecraft>-<loader>`.

## 0.1.3-dev+mc1.21.1-neoforge (in development)

### Shapes as settings, one built-in theme
* **Settings > Appearance** has two new choices: **Minimap shape** (Square or Round) and **Interface style**
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
* New sliders in **Settings > Minimap > Size and position**: **Inner padding** (a gap between the frame and the map),
  **Edge fade width** (how much of the radius fades out, 0% for a sharp edge) and **Edge fade strength** (how
  transparent the very edge gets); and in **Extra information**: **Line spacing**, the gap from the map to the lines
  under it and between them. The defaults keep the look of 0.1.2.
* **Text size** of the lines under the minimap (Extra information): Auto (one step smaller than the interface, so 2
  screen pixels per font pixel at GUI scale 3, about the size of other minimaps) or a whole number of pixels, so the
  pixel font stays sharp. The lines shrink with their text.
* The clock can show **Both** game and real time: two pills side by side, game time on the left.
* Settings that were a switch with sub-settings are one slider in steps now: the **Clock** (Off, Game, Real, Both),
  each kind of **pins** (Off, Map, Both maps, Everywhere with the beam) and the **path** to each kind (Off, Map, Both
  maps, Everywhere with the stars). Older settings files keep working.
* Right after joining a world the biome line could stay on Plains while the player stood still; it is checked again
  every second and on a change of world or dimension.

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
