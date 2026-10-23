package net.tapacywka.teyvatcartographia.gui;

import net.tapacywka.teyvatcartographia.compat.ModCompat;
import net.tapacywka.teyvatcartographia.compat.XaeroWaypoints;
import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.MapTile;
import net.tapacywka.teyvatcartographia.map.data.Shimmer;
import net.tapacywka.teyvatcartographia.map.data.TextureColors;
import net.tapacywka.teyvatcartographia.map.data.TileStore;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.FogTexture;
import net.tapacywka.teyvatcartographia.render.MarkerRenderer;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.List;

/**
 * The live previews of the Pins, System and Compatibility tabs of {@link TeyvatConfigScreen}. Each draws into the area
 * it is given (the card beside the panel, or over the list), as one group centred on both axes, on the pixel grid.
 */
final class SettingsPreview {
    private SettingsPreview() {}

    /** Draws the map around the player into an area at a zoom (GUI units per block). */
    interface MapArea {
        void draw(GuiGraphics g, float x, float y, float w, float h, double zoom);
    }

    private static final int LINE = DrawText.LINE + 2;

    // ------------------------------------------------------------------ Pins

    /** The pin that each kind's sample path leads to. */
    private static final MarkerType[] PATH_TO = {MarkerType.DEMON, MarkerType.SIGN, MarkerType.SPAWN};
    private static final float[] ROUTE = new float[6];
    private static final boolean[] NO_FOG = new boolean[3];
    /** The illustration of the world under the beam and the stars: sky to the horizon, then the ground. */
    private static final int SKY_TOP = 0xFF5A86C8, SKY_HORIZON = 0xFFA8C8EC, GROUND_NEAR = 0xFF4E7A34, GROUND_FAR = 0xFF355624;
    /** A frame the chosen step leaves off: darkened this much. */
    private static final int OFF_DIM = 0xB0222A35;

    private static MapConfig.PinKind kind(MarkerType t) {
        return switch (t.category) {
            case EXPLORATION -> MapConfig.PinKind.CUSTOM;
            case INTERACTION -> MapConfig.PinKind.INTERACTIVE;
            case AUTO -> MapConfig.PinKind.SYSTEM;
        };
    }

    /**
     * Pins. With a pin row in focus ({@code focusKey}: the key of the row last pointed at, "...pins.custom" or
     * "...path.custom" and so on), three frames of that setting filling the area, each with its caption: the world map,
     * the minimap and the world (the beam over a pin, or the stars of the path); a frame the chosen step leaves off is
     * darkened. Without one, the map around the player (the fog without a world) with a pin of every type in a ring
     * and a path to one of each kind, faint where the world map does not show them.
     */
    static void pins(GuiGraphics g, float x, float y, float w, float h, MapArea map, String focusKey, boolean minimapReady) {
        MapConfig.PinKind focus = null;
        boolean path = false;
        for (MapConfig.PinKind k : MapConfig.PinKind.values()) {
            if (("teyvatcartographia.config.pins." + k.id).equals(focusKey)) focus = k;
            if (("teyvatcartographia.config.path." + k.id).equals(focusKey)) {
                focus = k;
                path = true;
            }
        }
        if (focus == null) {
            float s = Draw.snap(Math.min(w, h)), mx = Draw.snap(x + (w - s) / 2f), my = Draw.snap(y + (h - s) / 2f);
            mapArea(g, mx, my, s, s, map, MapView.snapZoom(MapConfig.get(MapConfig.MAP_DEFAULT_ZOOM)));
            samplePins(g, mx + s / 2f, my + s / 2f, s, null, false);
            return;
        }
        MapConfig.PinOptions o = MapConfig.pins(focus);
        float label = Grid.GAP + DrawText.LINE;
        float[][] at = new float[3][];
        if (w > h * 1.3f) {
            // Wide (over the list): the three side by side.
            float fw = (w - 2 * Grid.GAP) / 3f, fh = h - label;
            for (int i = 0; i < 3; i++) at[i] = new float[] {x + i * (fw + Grid.GAP), y, fw, fh};
        } else {
            // The maps side by side, the world under them across the whole width.
            float fw = (w - Grid.GAP) / 2f, fh = (h - Grid.GAP - 2 * label) / 2f;
            at[0] = new float[] {x, y, fw, fh};
            at[1] = new float[] {x + fw + Grid.GAP, y, fw, fh};
            at[2] = new float[] {x, y + fh + label + Grid.GAP, w, fh};
        }
        for (float[] a : at) {
            float x1 = Draw.snap(a[0] + a[2]), y1 = Draw.snap(a[1] + a[3]);
            a[0] = Draw.snap(a[0]);
            a[1] = Draw.snap(a[1]);
            a[2] = x1 - a[0];
            a[3] = y1 - a[1];
        }
        boolean[] on = path ? new boolean[] {o.pathOnMap(), o.pathOnMinimap(), o.pathInWorld()} : new boolean[] {o.onMap(), o.onMinimap(), o.beam()};
        String[] keys = {"teyvatcartographia.config.preview.frame.map", "teyvatcartographia.config.preview.frame.minimap",
                path ? "teyvatcartographia.config.preview.frame.stars" : "teyvatcartographia.config.preview.frame.beam"};
        double[] zooms = {MapView.snapZoom(MapConfig.get(MapConfig.MAP_DEFAULT_ZOOM)), MapView.snapZoom(MapConfig.get(MapConfig.MINIMAP_ZOOM))};
        for (int i = 0; i < 3; i++) {
            float fx = at[i][0], fy = at[i][1], fw = at[i][2], fh = at[i][3];
            g.enableScissor((int) fx, (int) fy, (int) (fx + fw), (int) (fy + fh));
            if (i == 1 && minimapReady) {
                minimap(g, fx, fy, fw, fh);
                samplePins(g, fx + fw / 2f, fy + fh / 2f, Math.min(fw, fh), focus, path);
            } else if (i < 2) {
                mapArea(g, fx, fy, fw, fh, map, zooms[i]);
                samplePins(g, fx + fw / 2f, fy + fh / 2f, Math.min(fw, fh), focus, path);
            } else {
                world(g, fx, fy, fw, fh, focus, path);
            }
            if (!on[i]) Draw.rect(g, fx, fy, fx + fw, fy + fh, OFF_DIM);
            g.disableScissor();
            Draw.frame(g, fx, fy, fw, fh, 1, Theme.PANEL_LINE);
            Component caption = Component.translatable(keys[i]);
            DrawText.room(caption, fw);
            DrawText.textCentered(g, Component.literal(DrawText.fit(caption.getString(), (int) fw)), Draw.snap(fx + fw / 2f), fy + fh + Grid.GAP,
                    on[i] ? Theme.CREAM : Theme.TEXT_MUTED, false);
        }
    }

    /**
     * The real minimap as the HUD draws it now (its shape, frame, zoom, "N" and pins; prepared by the screen before any
     * clipping), its map scaled to fill the largest square of the area, in the middle.
     */
    private static void minimap(GuiGraphics g, float x, float y, float w, float h) {
        float[] m = net.tapacywka.teyvatcartographia.hud.MinimapLayer.preparedMap();
        float side = Draw.snap(Math.min(w, h)), sx = Draw.snap(x + (w - side) / 2f), sy = Draw.snap(y + (h - side) / 2f), s = side / (2 * m[2]);
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK_HEADER);
        g.enableScissor((int) sx, (int) sy, (int) (sx + side), (int) (sy + side));
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-(m[0] - m[2]), -(m[1] - m[2]), 0);
        net.tapacywka.teyvatcartographia.hud.MinimapLayer.drawPrepared(g);
        g.pose().popPose();
        g.disableScissor();
    }

    /** The map around the player in an area at a zoom, or the theme's fog without a world. */
    private static void mapArea(GuiGraphics g, float x, float y, float w, float h, MapArea map, double zoom) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) map.draw(g, x, y, w, h, zoom);
        else {
            FogTexture.draw(g, x, y, x + w, y + h, 0, 0, Theme.WHITE);
            Draw.frame(g, x, y, w, h, 1, Theme.PANEL_LINE);
        }
    }

    /**
     * Sample pins in a ring around (cx, cy) in an area {@code size} across. Without a kind: a pin of every type and a
     * path to one of each kind, faint where the world map does not show them. With one: its pins ({@code path}: only
     * the one its path leads to, as a tracked pin always shows) and its path.
     */
    private static void samplePins(GuiGraphics g, float cx, float cy, float size, MapConfig.PinKind only, boolean path) {
        MarkerType[] types = MarkerType.values();
        float r = size * 0.34f, pin = Math.min(MarkerRenderer.SIZE, size / 7f);
        for (MarkerType to : PATH_TO) {
            MapConfig.PinKind k = kind(to);
            if (only != null && (k != only || !path)) continue;
            double a = angle(to.ordinal(), types.length);
            float ex = cx + (float) Math.cos(a) * (r - pin / 2f), ey = cy + (float) Math.sin(a) * (r - pin / 2f);
            // A gentle bend half way, like a walking path.
            float bx = (cx + ex) / 2f - (ey - cy) * 0.18f, by = (cy + ey) / 2f + (ex - cx) * 0.18f;
            ROUTE[0] = cx;
            ROUTE[1] = cy;
            ROUTE[2] = bx;
            ROUTE[3] = by;
            ROUTE[4] = ex;
            ROUTE[5] = ey;
            float alpha = only != null || MapConfig.pins(k).pathOnMap() ? 1f : 0.25f;
            int shadow = Theme.withAlpha(Theme.ROUTE_SHADOW, alpha), c = Theme.withAlpha(Theme.worldMarkerPulse(k), alpha);
            Draw.route(g, ROUTE, NO_FOG, 3, 2.4f, shadow, shadow);
            Draw.route(g, ROUTE, NO_FOG, 3, 1.4f, c, c);
        }
        for (MarkerType t : types) {
            if (only != null && (kind(t) != only || path && t != PATH_TO[only.ordinal()])) continue;
            double a = angle(t.ordinal(), types.length);
            float alpha = only != null || MapConfig.pins(kind(t)).onMap() ? 1f : 0.3f;
            MarkerRenderer.draw(g, t, Draw.snap(cx + (float) Math.cos(a) * r), Draw.snap(cy + (float) Math.sin(a) * r), pin, false, false, alpha);
        }
    }

    /** The place of pin {@code i} of {@code n} in the ring: the first at the top, clockwise. */
    private static double angle(int i, int n) {
        return -Math.PI / 2 + i * 2 * Math.PI / n;
    }

    /**
     * The world as the player sees it: sky and ground, and a pin of the kind with its beam of light (a soft glow in the
     * kind's colour around a light core, the glyph and the distance at its foot), or ({@code path}) the stars leading
     * up to it from the near ground.
     */
    private static void world(GuiGraphics g, float x, float y, float w, float h, MapConfig.PinKind kind, boolean path) {
        float horizon = Draw.snap(y + h * 0.55f);
        Draw.gradientRectV(g, x, y, x + w, horizon, SKY_TOP, SKY_HORIZON);
        Draw.gradientRectV(g, x, horizon, x + w, y + h, GROUND_FAR, GROUND_NEAR);
        MarkerType type = PATH_TO[kind.ordinal()];
        float cx = Draw.snap(x + w * (path ? 0.6f : 0.5f)), foot = Draw.snap(y + h * 0.62f);
        if (path) {
            // Stars from the near ground to the pin, smaller and closer together with the distance.
            float sx = x + w * 0.3f, sy = y + h * 0.96f;
            int c = Theme.worldMarkerPulse(kind);
            for (int i = 0; i < 9; i++) {
                float t = i / 8f, px = sx + (cx - sx) * t, py = sy + (foot + 6 - sy) * t * (2 - t);
                star(g, px, py, Math.max(1.5f, 4.5f - 3f * t), c);
            }
        } else {
            int beam = Theme.worldMarkerBeam(kind), core = Theme.worldMarkerBeamCore(kind);
            for (int r = 12; r >= 2; r -= 2) Draw.rect(g, cx - r, y, cx + r, foot, Theme.withAlpha(beam, 0.18f));
            Draw.rect(g, cx - 1, y, cx + 1, foot, core);
        }
        Draw.icon(g, type.icon, cx, foot + 4, 10, Theme.WHITE);
        // The distance under the glyph; over it where the stars come up from below.
        DrawText.textCentered(g, net.tapacywka.teyvatcartographia.render.DistanceText.blocks(61), cx, path ? foot - 12 : foot + 11, Theme.WHITE, true);
    }

    /** A four-pointed star of light {@code s} units from its middle to a point. */
    private static void star(GuiGraphics g, float cx, float cy, float s, int argb) {
        float t = s * 0.3f;
        Draw.triangle(g, cx, cy - s, cx + t, cy, cx - t, cy, argb);
        Draw.triangle(g, cx, cy + s, cx - t, cy, cx + t, cy, argb);
        Draw.triangle(g, cx - s, cy, cx, cy - t, cx, cy + t, argb);
        Draw.triangle(g, cx + s, cy, cx, cy + t, cx, cy - t, argb);
    }

    // ------------------------------------------------------------------ System

    private static final String[] STAT_KEYS = {"teyvatcartographia.config.preview.system.explored", "teyvatcartographia.config.preview.system.regions",
            "teyvatcartographia.config.preview.system.memory", "teyvatcartographia.config.preview.system.speed",
            "teyvatcartographia.config.preview.system.waiting"};
    private static final String[] STATS = new String[STAT_KEYS.length];
    private static long statsAt;

    /** Height of a line of the numbers. */
    private static final int STAT_H = 16;

    /** System: live numbers of the mapping (read once a second); in a world only. */
    static void system(GuiGraphics g, float x, float y, float w, float h) {
        long now = System.currentTimeMillis();
        if (now - statsAt > 1000 || STATS[0] == null) {
            statsAt = now;
            readStats();
        }
        float top = Draw.snap(y + (h - STAT_KEYS.length * STAT_H) / 2f);
        for (int i = 0; i < STAT_KEYS.length; i++) {
            float ry = top + i * STAT_H;
            String value = STATS[i];
            int vw = DrawText.width(value);
            DrawText.text(g, DrawText.fit(Component.translatable(STAT_KEYS[i]).getString(), (int) (w - vw - Grid.PAD)), x, DrawText.textY(ry, STAT_H), Theme.CREAM, false);
            DrawText.text(g, value, x + w - vw, DrawText.textY(ry, STAT_H), Theme.GOLD, false);
            Draw.rect(g, x, ry + STAT_H - 1, x + w, ry + STAT_H, Theme.PANEL_LINE);
        }
    }

    private static void readStats() {
        MapDataManager data = MapDataManager.get();
        TileStore store = data.active();
        int regions = data.regionsInMemory();
        STATS[0] = String.valueOf(store == null ? 0 : store.exploredChunks());
        STATS[1] = Component.translatable("teyvatcartographia.config.preview.system.regions_value", store == null ? 0 : store.regions(),
                MapConfig.get(MapConfig.MAX_CACHED_TILES)).getString();
        STATS[2] = Component.translatable("teyvatcartographia.config.preview.system.memory_value", Math.round(regions * (double) MapTile.BYTES / (1 << 20))).getString();
        STATS[3] = Component.translatable("teyvatcartographia.config.preview.system.speed_value", Math.round(data.chunksPerSecond())).getString();
        STATS[4] = String.valueOf(data.waiting());
    }

    // ------------------------------------------------------------------ Compatibility

    /** One mod's part: its name and what it shows. */
    record ModSample(String key, Component name) {}

    private static final Block ICHOR_SNOW = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("the_sift", "ichor_snow_block")).orElse(null);
    private static XaeroWaypoints.Options xaeroFor;
    private static Path xaeroFolder;
    private static int xaeroCount = -1;
    private static long xaeroAt;

    private static final int TILE_MAX = 48, TILE_MIN = 24;

    /**
     * Compatibility: for each supported mod (in the order of the list), its name and samples as the map draws them now:
     * The Sift's haze and ichor with its snow, the Aether's sea of clouds (a switched off fog shows the fog that takes
     * its place), and for Xaero's Minimap how many of its waypoints the import would take, with their pin. One column
     * while the samples keep {@link #TILE_MIN} units; lower (over the list) the first mod beside the others.
     */
    static void compat(GuiGraphics g, float x, float y, float w, float h, List<ModSample> mods) {
        Component xaero = mods.stream().anyMatch(SettingsPreview::xaero) ? xaeroLine() : Component.empty();
        float one = tileFor(mods, h);
        if (one >= TILE_MIN || mods.size() < 2) {
            float tile = Math.max(Grid.ROW, one), cw = Math.min(w, columnWidth(mods, tile, xaero));
            column(g, mods, x + (w - cw) / 2f, y, cw, h, tile, xaero);
            return;
        }
        List<ModSample> a = mods.subList(0, 1), b = mods.subList(1, mods.size());
        float half = (w - 2 * Grid.PAD) / 2f, tile = Math.max(Grid.ROW, Math.min(tileFor(a, h), tileFor(b, h)));
        float wa = Math.min(half, columnWidth(a, tile, xaero)), wb = Math.min(half, columnWidth(b, tile, xaero));
        float left = x + (w - (wa + 2 * Grid.PAD + wb)) / 2f;
        column(g, a, left, y, wa, h, tile, xaero);
        column(g, b, left + wa + 2 * Grid.PAD, y, wb, h, tile, xaero);
    }

    private static boolean sift(ModSample m) {
        return m.key().endsWith("compat_sift");
    }

    private static boolean aether(ModSample m) {
        return m.key().endsWith("compat_aether");
    }

    private static boolean xaero(ModSample m) {
        return m.key().endsWith("compat_xaero");
    }

    /** The largest sample (a multiple of 4, at most {@link #TILE_MAX}) with which these mods fit {@code h}. */
    private static float tileFor(List<ModSample> mods, float h) {
        int tiles = 0;
        float fixed = (mods.size() - 1) * Grid.PAD;
        for (ModSample m : mods) {
            fixed += LINE + Grid.GAP;
            if (sift(m)) {
                tiles += 3;
                fixed += 2 * Grid.GAP;
            } else if (aether(m)) tiles += 1;
            else fixed += Grid.ROW;
        }
        if (tiles == 0) return TILE_MAX;
        return Math.min(TILE_MAX, (float) Math.floor((h - fixed) / tiles / 4f) * 4f);
    }

    /** The width these mods' column takes with samples {@code tile} units high, its texts on one line. */
    private static float columnWidth(List<ModSample> mods, float tile, Component xaero) {
        float w = 0;
        for (ModSample m : mods) {
            w = Math.max(w, DrawText.width(m.name()));
            if (sift(m)) {
                w = Math.max(w, tile + Grid.PAD + DrawText.width(Component.translatable("teyvatcartographia.config.compat.sift_fog")));
                w = Math.max(w, tile + Grid.PAD + DrawText.width(Component.translatable("teyvatcartographia.config.preview.compat.ichor")));
                w = Math.max(w, tile + Grid.PAD + DrawText.width(Component.translatable("teyvatcartographia.config.preview.compat.snow")));
            } else if (aether(m)) {
                w = Math.max(w, tile + Grid.PAD + DrawText.width(Component.translatable("teyvatcartographia.config.compat.aether_fog")));
            } else w = Math.max(w, Grid.ROW + Grid.GAP + DrawText.width(xaero));
        }
        return w;
    }

    /** These mods one under another in a column {@code w} wide at x, centred on the height {@code h} from y. */
    private static void column(GuiGraphics g, List<ModSample> mods, float x, float y, float w, float h, float tile, Component xaero) {
        float textW = w - Grid.ROW - Grid.GAP;
        int xaeroLines = Math.max(1, DrawText.font().split(xaero, (int) textW).size());
        float total = (mods.size() - 1) * Grid.PAD;
        for (ModSample m : mods) {
            total += LINE + Grid.GAP;
            if (sift(m)) total += 3 * tile + 2 * Grid.GAP;
            else if (aether(m)) total += tile;
            else total += Math.max(Grid.ROW, xaeroLines * LINE);
        }
        float lx = Draw.snap(x), ly = Draw.snap(y + (h - total) / 2f), labelX = lx + tile + Grid.PAD, labelW = w - tile - Grid.PAD;
        for (int i = 0; i < mods.size(); i++) {
            ModSample m = mods.get(i);
            if (i > 0) ly += Grid.PAD;
            DrawText.text(g, DrawText.fit(m.name().getString(), (int) w), lx, ly, Theme.GOLD, false);
            ly += LINE + Grid.GAP;
            if (sift(m)) {
                fogTile(g, ModCompat.SIFT_DIMENSION, lx, ly, tile);
                label(g, Component.translatable("teyvatcartographia.config.compat.sift_fog"), labelX, ly, labelW, tile);
                ly += tile + Grid.GAP;
                siftTile(g, lx, ly, tile, false);
                label(g, Component.translatable("teyvatcartographia.config.preview.compat.ichor"), labelX, ly, labelW, tile);
                ly += tile + Grid.GAP;
                siftTile(g, lx, ly, tile, true);
                label(g, Component.translatable("teyvatcartographia.config.preview.compat.snow"), labelX, ly, labelW, tile);
                ly += tile;
            } else if (aether(m)) {
                fogTile(g, ModCompat.AETHER_DIMENSION, lx, ly, tile);
                label(g, Component.translatable("teyvatcartographia.config.compat.aether_fog"), labelX, ly, labelW, tile);
                ly += tile;
            } else {
                float rh = Math.max(Grid.ROW, xaeroLines * LINE);
                MarkerRenderer.drawCentered(g, MapConfig.get(MapConfig.XAERO_PIN_TYPE), lx + Grid.ROW / 2f, ly + rh / 2f, Grid.ROW, 1f);
                label(g, xaero, lx + Grid.ROW + Grid.GAP, ly, textW, rh);
                ly += rh;
            }
        }
    }

    /** A text {@code w} wide beside a sample {@code h} high, wrapped onto as many lines as fit and centred on it. */
    private static void label(GuiGraphics g, Component text, float x, float y, float w, float h) {
        List<FormattedCharSequence> lines = DrawText.font().split(text, (int) w);
        int n = Math.min(lines.size(), Math.max(1, (int) ((h + 2) / LINE)));
        DrawText.room(text, w * n);
        float ty = Draw.snap(y + (h - (n * LINE - 2)) / 2f);
        for (int i = 0; i < n; i++) {
            g.pose().pushPose();
            g.pose().translate(Draw.snap(x), ty + i * LINE, 0);
            g.drawString(DrawText.font(), lines.get(i), 0, 0, Theme.CREAM, false);
            g.pose().popPose();
        }
    }

    /** A piece of a dimension's fog of war, as the map shows it now (the theme's fog while the dimension's is off). */
    private static void fogTile(GuiGraphics g, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, float x, float y, float s) {
        FogTexture.draw(g, dim, x, y, x + s, y + s, 0, 0, Theme.WHITE);
        Draw.frame(g, x, y, s, s, 1, Theme.PANEL_LINE);
    }

    /** The Sift's ichor or its snow, a block every two units, coloured as the map colours them (shimmering or plain). */
    private static void siftTile(GuiGraphics g, float x, float y, float s, boolean onSnow) {
        boolean shimmer = MapConfig.get(MapConfig.SIFT_SHIMMER);
        int plainIchor = TextureColors.ichor();
        TextureColors.Entry snow = ICHOR_SNOW == null ? null : TextureColors.get(ICHOR_SNOW);
        int n = (int) (s / 2);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int c;
                if (onSnow) c = shimmer || snow == null ? 0xFF000000 | Shimmer.ichorSnow(i, j) : snow.argb();
                else c = shimmer || plainIchor == 0 ? 0xFF000000 | Shimmer.ichor(i, j) : plainIchor;
                Draw.rect(g, x + 2 * i, y + 2 * j, x + 2 * i + 2, y + 2 * j + 2, c);
            }
        }
        Draw.frame(g, x, y, s, s, 1, Theme.PANEL_LINE);
    }

    /** How many waypoints the import would take from Xaero's folder of this world (read again on a change of the options or every 5 s). */
    private static Component xaeroLine() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Component.translatable("teyvatcartographia.config.preview.no_world");
        XaeroWaypoints.Options o = PinTransfer.xaeroOptions();
        long now = System.currentTimeMillis();
        if (!o.equals(xaeroFor) || now - xaeroAt > 5000) {
            xaeroFor = o;
            xaeroAt = now;
            xaeroFolder = XaeroWaypoints.currentWorldFolder();
            try {
                xaeroCount = xaeroFolder == null ? 0 : XaeroWaypoints.read(xaeroFolder, o).size();
            } catch (java.io.IOException e) {
                xaeroCount = 0;
            }
        }
        if (xaeroCount <= 0) return Component.translatable("teyvatcartographia.config.preview.xaero.none");
        return Component.translatable("teyvatcartographia.config.preview.xaero.count", xaeroCount);
    }
}
