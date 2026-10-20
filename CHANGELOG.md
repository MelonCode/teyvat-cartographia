# Changelog

All notable changes to Teyvat Cartographia. Versions follow `<mod>+mc<minecraft>-<loader>`.

## 0.1.2-dev+mc1.21.1-neoforge (in development)

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
  GUI 6 to 8).

### Fixes
* Flying or in a boat over deep water, the path to the target was missing; now it leads out across the water to the
  shore, as it does for a swimming player.

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
