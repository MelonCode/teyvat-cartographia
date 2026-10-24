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
    static void pins(GuiGraphics g, float x, float y, float w, float h, MapArea map, String focusKey) {
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
        // Pins: the world map, the minimap, the marker over a tracked pin in the world and its beam; the path: the
        // two maps and the stars in the world.
        int n = path ? 3 : 4;
        float[][] at = new float[n][];
        float fw = (w - Grid.GAP) / 2f, fh = (h - Grid.GAP - 2 * label) / 2f;
        at[0] = new float[] {x, y, fw, fh};
        at[1] = new float[] {x + fw + Grid.GAP, y, fw, fh};
        if (path) {
            // The world under the maps across the whole width.
            at[2] = new float[] {x, y + fh + label + Grid.GAP, w, fh};
        } else {
            at[2] = new float[] {x, y + fh + label + Grid.GAP, fw, fh};
            at[3] = new float[] {x + fw + Grid.GAP, y + fh + label + Grid.GAP, fw, fh};
        }
        for (float[] a : at) {
            float x1 = Draw.snap(a[0] + a[2]), y1 = Draw.snap(a[1] + a[3]);
            a[0] = Draw.snap(a[0]);
            a[1] = Draw.snap(a[1]);
            a[2] = x1 - a[0];
            a[3] = y1 - a[1];
        }
        boolean[] on = path ? new boolean[] {o.pathOnMap(), o.pathOnMinimap(), o.pathInWorld()} : new boolean[] {o.onMap(), o.onMinimap(), o.marker(), o.beam()};
        String[] keys = path ? new String[] {"teyvatcartographia.config.preview.frame.map", "teyvatcartographia.config.preview.frame.minimap",
                "teyvatcartographia.config.preview.frame.stars"}
                : new String[] {"teyvatcartographia.config.preview.frame.map", "teyvatcartographia.config.preview.frame.minimap",
                "teyvatcartographia.config.preview.frame.marker", "teyvatcartographia.config.preview.frame.beam"};
        double zoom = MapView.snapZoom(MapConfig.get(MapConfig.MAP_DEFAULT_ZOOM));
        for (int i = 0; i < n; i++) {
            float fx = at[i][0], fy = at[i][1], fw2 = at[i][2], fh2 = at[i][3];
            g.enableScissor((int) fx, (int) fy, (int) (fx + fw2), (int) (fy + fh2));
            if (i < 2) {
                // Both maps drawn alike, as the world map (user decision 24.10.2026).
                mapArea(g, fx, fy, fw2, fh2, map, zoom);
                samplePins(g, fx + fw2 / 2f, fy + fh2 / 2f, Math.min(fw2, fh2), focus, path);
            } else {
                world(g, fx, fy, fw2, fh2, focus, path ? WORLD_STARS : i == 2 ? WORLD_MARKER : WORLD_BEAM);
            }
            if (!on[i]) Draw.rect(g, fx, fy, fx + fw2, fy + fh2, OFF_DIM);
            g.disableScissor();
            Draw.frame(g, fx, fy, fw2, fh2, 1, Theme.PANEL_LINE);
            Component caption = Component.translatable(keys[i]);
            DrawText.room(caption, fw2);
            DrawText.textCentered(g, Component.literal(DrawText.fit(caption.getString(), (int) fw2)), Draw.snap(fx + fw2 / 2f), fy + fh2 + Grid.GAP,
                    on[i] ? Theme.CREAM : Theme.TEXT_MUTED, false);
        }
    }

    /**
     * The real minimap as the HUD draws it now (its shape, frame, zoom, "N" and pins; prepared by the screen before any
     * clipping), its map scaled to fill the largest square of the area, in the middle.
     */
    static void minimap(GuiGraphics g, float x, float y, float w, float h) {
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

    /** What the world frame shows: the marker over the tracked pin alone, with its beam, or the stars of the path. */
    private static final int WORLD_MARKER = 0, WORLD_BEAM = 1, WORLD_STARS = 2;

    /**
     * The world as the player sees it: sky and ground, and the marker of a tracked pin of the kind (its glyph and the
     * distance), with its beam of light ({@link #WORLD_BEAM}: a soft glow in the kind's colour around a light core), or
     * with the stars of the path leading up to it from the near ground ({@link #WORLD_STARS}).
     */
    private static void world(GuiGraphics g, float x, float y, float w, float h, MapConfig.PinKind kind, int mode) {
        float horizon = Draw.snap(y + h * 0.55f);
        Draw.gradientRectV(g, x, y, x + w, horizon, SKY_TOP, SKY_HORIZON);
        Draw.gradientRectV(g, x, horizon, x + w, y + h, GROUND_FAR, GROUND_NEAR);
        MarkerType type = PATH_TO[kind.ordinal()];
        boolean stars = mode == WORLD_STARS;
        float cx = Draw.snap(x + w * (stars ? 0.6f : 0.5f)), foot = Draw.snap(y + h * 0.62f);
        if (stars) {
            // Stars from the near ground to the pin, smaller and closer together with the distance.
            float sx = x + w * 0.3f, sy = y + h * 0.96f;
            int c = Theme.worldMarkerPulse(kind);
            for (int i = 0; i < 9; i++) {
                float t = i / 8f, px = sx + (cx - sx) * t, py = sy + (foot + 6 - sy) * t * (2 - t);
                star(g, px, py, Math.max(1.5f, 4.5f - 3f * t), c);
            }
        } else if (mode == WORLD_BEAM) {
            int beam = Theme.worldMarkerBeam(kind), core = Theme.worldMarkerBeamCore(kind);
            for (int r = 12; r >= 2; r -= 2) Draw.rect(g, cx - r, y, cx + r, foot, Theme.withAlpha(beam, 0.18f));
            Draw.rect(g, cx - 1, y, cx + 1, foot, core);
        }
        Draw.icon(g, type.icon, cx, foot + 4, 10, Theme.WHITE);
        // The distance under the glyph; over it where the stars come up from below.
        DrawText.textCentered(g, net.tapacywka.teyvatcartographia.render.DistanceText.blocks(61), cx, stars ? foot - 12 : foot + 11, Theme.WHITE, true);
    }

    /** A four-pointed star of light {@code s} units from its middle to a point. */
    private static void star(GuiGraphics g, float cx, float cy, float s, int argb) {
        float t = s * 0.3f;
        Draw.triangle(g, cx, cy - s, cx + t, cy, cx - t, cy, argb);
        Draw.triangle(g, cx, cy + s, cx - t, cy, cx + t, cy, argb);
        Draw.triangle(g, cx - s, cy, cx, cy - t, cx, cy + t, argb);
        Draw.triangle(g, cx + s, cy, cx, cy + t, cx, cy - t, argb);
    }

    // ------------------------------------------------------------------ Cave map

    /** Draws a layer of the map around the player into an area: the surface (null) or a cave band. */
    interface LayerArea {
        void draw(GuiGraphics g, float x, float y, float w, float h, Integer band);
    }

    private static final int CAVE_BIG = 28, CAVE_SMALL_MAX = 5, CAVE_STEP = 28;
    /** The layer the cave preview shows (null: the surface) and whether its field is open. */
    private static Integer caveBand;
    private static boolean caveOpen;
    /** Where the field was drawn last, for clicks: its items (null for the surface) and their centres. */
    private static final List<Integer> caveItems = new java.util.ArrayList<>();
    private static float[] caveY = new float[0];
    private static float caveX;

    /**
     * Cave map: the map around the player over the whole area with the layer field inside it at the right edge, as on
     * the world map: the large button
     * is the surface; a click opens the cave layers mapped in this dimension (at most five, in height order around the
     * surface), a click on one shows it, a click on the large button goes back to the surface and then closes the
     * field. Under the map the layer shown.
     */
    static void caves(GuiGraphics g, float x, float y, float w, float h, LayerArea map, int mx, int my) {
        java.util.NavigableSet<Integer> bands = MapDataManager.get().caveBands();
        if (caveBand != null && !bands.contains(caveBand)) caveBand = null;
        float label = Grid.GAP + DrawText.LINE;
        float mw = w, mh = h - label;
        map.draw(g, x, y, mw, mh, caveBand);
        // The field's items from top to bottom: the bands above the sea level, the surface, the bands under it.
        caveItems.clear();
        var level = Minecraft.getInstance().level;
        caveItems.addAll(net.tapacywka.teyvatcartographia.map.data.CaveScanner.layerOrder(bands, level == null ? 63 : level.getSeaLevel()));
        // Closed: the large button alone; open: up to five layers around the surface.
        int surface = caveItems.indexOf(null), from = surface, to = surface + 1;
        if (caveOpen) {
            from = Math.max(0, surface - CAVE_SMALL_MAX / 2);
            to = Math.min(caveItems.size(), from + CAVE_SMALL_MAX + 1);
            from = Math.max(0, to - CAVE_SMALL_MAX - 1);
        }
        caveItems.subList(to, caveItems.size()).clear();
        caveItems.subList(0, from).clear();
        caveX = Draw.snap(x + mw - CAVE_BIG / 2f - Grid.PAD); // inside the map, at its right edge like on the world map
        caveY = new float[caveItems.size()];
        float total = 0;
        for (Integer it : caveItems) total += it == null ? CAVE_BIG + 2 * Grid.GAP : CAVE_STEP;
        float cy = Draw.snap(y + mh / 2f - total / 2f);
        for (int i = 0; i < caveItems.size(); i++) {
            float slot = caveItems.get(i) == null ? CAVE_BIG + 2 * Grid.GAP : CAVE_STEP;
            caveY[i] = cy + slot / 2f;
            cy += slot;
        }
        if (caveItems.size() > 1) Draw.rect(g, caveX - 0.75f, caveY[0], caveX + 0.75f, caveY[caveY.length - 1], Theme.PANEL_LINE);
        for (int i = 0; i < caveItems.size(); i++) {
            Integer it = caveItems.get(i);
            if (it == null) Ui.iconButton(g, caveX, caveY[i], CAVE_BIG, Grid.ICON_L, net.tapacywka.teyvatcartographia.render.Icon.LAYERS, caveBand == null, mx, my);
            else Ui.iconButton(g, caveX, caveY[i], net.tapacywka.teyvatcartographia.render.Icon.LAYERS, it.equals(caveBand), mx, my);
        }
        Component caption = bands.isEmpty() ? Component.translatable("teyvatcartographia.config.preview.caves_none")
                : caveBand == null ? Component.translatable("teyvatcartographia.map.layer.surface")
                : Component.translatable("teyvatcartographia.map.caves", caveBand * net.tapacywka.teyvatcartographia.map.data.CaveScanner.BAND,
                caveBand * net.tapacywka.teyvatcartographia.map.data.CaveScanner.BAND + net.tapacywka.teyvatcartographia.map.data.CaveScanner.BAND - 1);
        DrawText.room(caption, mw);
        DrawText.textCentered(g, Component.literal(DrawText.fit(caption.getString(), (int) mw)), Draw.snap(x + mw / 2f), y + mh + Grid.GAP, Theme.CREAM, false);
    }

    /** A click on the cave preview's layer field (as laid out when last drawn); true when it took it. */
    static boolean cavesClick(double mx, double my) {
        for (int i = 0; i < caveItems.size(); i++) {
            Integer it = caveItems.get(i);
            if (it == null && Ui.buttonHit(caveX, caveY[i], CAVE_BIG, mx, my)) {
                // Open the field; on a cave layer go back to the surface; on the surface close it.
                if (!caveOpen) caveOpen = !MapDataManager.get().caveBands().isEmpty();
                else if (caveBand != null) caveBand = null;
                else caveOpen = false;
                return true;
            }
            if (it != null && Ui.iconButtonHit(caveX, caveY[i], mx, my)) {
                caveBand = it;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Terrain detail

    /** Chunks east and west, and north and south, of the player's in the detail samples (11 x 3 chunks, 176 x 48 blocks). */
    private static final int DETAIL_RX = 5, DETAIL_RZ = 1, DETAIL_W = 16 * (2 * DETAIL_RX + 1), DETAIL_H = 16 * (2 * DETAIL_RZ + 1);
    private static final net.minecraft.client.renderer.texture.DynamicTexture[] DETAIL_TEX =
            new net.minecraft.client.renderer.texture.DynamicTexture[MapConfig.Detail.values().length];
    private static volatile int[][] detailPixels;
    private static int[][] detailShown;
    private static long detailAsked;
    private static final String[] DETAIL_KEYS = {"teyvatcartographia.config.value.low", "teyvatcartographia.config.value.medium",
            "teyvatcartographia.config.value.high"};

    /**
     * Terrain detail: a wide strip of the ground around the player in each mode, one over the other across the whole
     * area (shaded anew, every few seconds, on a
     * worker), each captioned with its mode; the chosen one in cream, the others muted. The fog shows where nothing is
     * loaded.
     */
    static void detail(GuiGraphics g, float x, float y, float w, float h) {
        long now = System.currentTimeMillis();
        if (now - detailAsked > 3000) {
            detailAsked = now;
            MapDataManager.get().sampleDetails(DETAIL_RX, DETAIL_RZ, p -> detailPixels = p);
        }
        int[][] p = detailPixels;
        if (p != null && p != detailShown) {
            detailShown = p;
            for (int i = 0; i < DETAIL_TEX.length; i++) {
                if (DETAIL_TEX[i] == null) DETAIL_TEX[i] = new net.minecraft.client.renderer.texture.DynamicTexture(DETAIL_W, DETAIL_H, true);
                com.mojang.blaze3d.platform.NativeImage img = DETAIL_TEX[i].getPixels();
                for (int k = 0; k < p[i].length; k++) img.setPixelRGBA(k % DETAIL_W, k / DETAIL_W, p[i][k]);
                DETAIL_TEX[i].upload();
            }
        }
        int n = DETAIL_TEX.length, chosen = MapConfig.get(MapConfig.DETAIL).ordinal();
        float label = Grid.GAP + DrawText.LINE;
        float sw = Draw.snap(w), sh = Draw.snap((h - n * label - (n - 1) * Grid.GAP) / n);
        // The part of the sample with the strip's proportions, from its middle (blocks stay square).
        float aspect = sw / Math.max(1, sh), sample = DETAIL_W / (float) DETAIL_H;
        float u0 = 0, u1 = 1, v0 = 0, v1 = 1;
        if (aspect > sample) {
            float part = sample / aspect;
            v0 = (1 - part) / 2f;
            v1 = v0 + part;
        } else {
            float part = aspect / sample;
            u0 = (1 - part) / 2f;
            u1 = u0 + part;
        }
        float fx = Draw.snap(x);
        for (int i = 0; i < n; i++) {
            float fy = Draw.snap(y + i * (sh + label + Grid.GAP));
            FogTexture.draw(g, fx, fy, fx + sw, fy + sh, 0, 0, Theme.WHITE);
            if (DETAIL_TEX[i] != null && detailShown != null) Draw.texturedQuad(g, DETAIL_TEX[i].getId(), fx, fy, fx + sw, fy + sh, u0, v0, u1, v1, 0xFFFFFFFF);
            Draw.frame(g, fx, fy, sw, sh, 1, i == chosen ? Theme.GOLD : Theme.PANEL_LINE);
            Component c = Component.translatable(DETAIL_KEYS[i]);
            DrawText.room(c, sw);
            DrawText.textCentered(g, Component.literal(DrawText.fit(c.getString(), (int) sw)), Draw.snap(fx + sw / 2f), fy + sh + Grid.GAP,
                    i == chosen ? Theme.CREAM : Theme.TEXT_MUTED, false);
        }
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
        float top = Draw.snap(y + h - STAT_KEYS.length * STAT_H); // at the bottom of the card, under the description
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

    private static final Block ICHOR_SNOW = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("the_sift", "ichor_snow_block")).orElse(null);
    private static XaeroWaypoints.Options xaeroFor;
    private static Path xaeroFolder;
    private static int xaeroCount = -1;
    private static long xaeroAt;

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

    /** Blocks across the large sample of ichor or snow, and its pictures (ichor, snow), drawn again when a switch changes. */
    private static final int SIFT_BLOCKS = 48;
    private static final net.minecraft.client.renderer.texture.DynamicTexture[] SIFT_TEX = new net.minecraft.client.renderer.texture.DynamicTexture[2];
    private static final Boolean[] SIFT_SHOWN = new Boolean[2];

    /**
     * Compatibility: the sample of the setting {@code key} alone, over the whole area: a piece of the dimension's fog of war
     * (the theme's fog while it is off), a field of ichor or of ichor snow as the map colours them (shimmering or
     * plain), or for Xaero's Minimap how many waypoints the import takes, with their pin.
     */
    static void compatSample(GuiGraphics g, float x, float y, float w, float h, String key) {
        x = Draw.snap(x);
        y = Draw.snap(y);
        w = Draw.snap(w);
        h = Draw.snap(h);
        if (key.endsWith("_fog")) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = key.endsWith("sift_fog") ? ModCompat.SIFT_DIMENSION
                    : key.endsWith("aether_fog") ? ModCompat.AETHER_DIMENSION : key.endsWith("nether_fog") ? net.minecraft.world.level.Level.NETHER
                    : net.minecraft.world.level.Level.END;
            FogTexture.draw(g, dim, x, y, x + w, y + h, 0, 0, Theme.WHITE);
            Draw.frame(g, x, y, w, h, 1, Theme.PANEL_LINE);
        } else if (key.endsWith("compat.sift_ichor") || key.endsWith("compat.sift_snow")) {
            boolean snow = key.endsWith("sift_snow");
            int i = snow ? 1 : 0;
            boolean shimmer = MapConfig.get(snow ? MapConfig.SIFT_SNOW_SHIMMER : MapConfig.SIFT_ICHOR_SHIMMER);
            if (SIFT_TEX[i] == null) SIFT_TEX[i] = new net.minecraft.client.renderer.texture.DynamicTexture(SIFT_BLOCKS, SIFT_BLOCKS, true);
            if (!Boolean.valueOf(shimmer).equals(SIFT_SHOWN[i])) {
                SIFT_SHOWN[i] = shimmer;
                int plainIchor = TextureColors.ichor();
                TextureColors.Entry plainSnow = ICHOR_SNOW == null ? null : TextureColors.get(ICHOR_SNOW);
                com.mojang.blaze3d.platform.NativeImage img = SIFT_TEX[i].getPixels();
                for (int bz = 0; bz < SIFT_BLOCKS; bz++) {
                    for (int bx = 0; bx < SIFT_BLOCKS; bx++) {
                        int c = snow ? (shimmer || plainSnow == null ? 0xFF000000 | Shimmer.ichorSnow(bx, bz) : plainSnow.argb())
                                : (shimmer || plainIchor == 0 ? 0xFF000000 | Shimmer.ichor(bx, bz) : plainIchor);
                        // ARGB to the image's ABGR.
                        img.setPixelRGBA(bx, bz, (c & 0xFF00FF00) | (c & 0xFF) << 16 | (c >> 16) & 0xFF);
                    }
                }
                SIFT_TEX[i].upload();
            }
            // Square blocks: as many as the longer side shows of the field.
            float side = Math.max(w, h);
            Draw.texturedQuad(g, SIFT_TEX[i].getId(), x, y, x + w, y + h, 0, 0, w / side, h / side, 0xFFFFFFFF);
            Draw.frame(g, x, y, w, h, 1, Theme.PANEL_LINE);
        } else if (key.contains("compat.xaero")) {
            Component line = xaeroLine();
            float lw = Math.min(w, Grid.ROW + Grid.GAP + DrawText.width(line)), lx = Draw.snap(x + (w - lw) / 2f), ly = Draw.snap(y + (h - Grid.ROW) / 2f);
            MarkerRenderer.drawCentered(g, MapConfig.get(MapConfig.XAERO_PIN_TYPE), lx + Grid.ROW / 2f, ly + Grid.ROW / 2f, Grid.ROW, 1f);
            label(g, line, lx + Grid.ROW + Grid.GAP, ly, lw - Grid.ROW - Grid.GAP, Grid.ROW);
        }
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
