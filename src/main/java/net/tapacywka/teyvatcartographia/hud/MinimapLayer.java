package net.tapacywka.teyvatcartographia.hud;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.Ui;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.MapTile;
import net.tapacywka.teyvatcartographia.map.data.TileStore;
import net.tapacywka.teyvatcartographia.marker.AutoMarkers;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.marker.MarkerVisibility;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.nav.Route;
import net.tapacywka.teyvatcartographia.render.DayLight;
import net.tapacywka.teyvatcartographia.render.CachedLayer;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.MapLayerRenderer;
import net.tapacywka.teyvatcartographia.render.MarkerRenderer;
import net.tapacywka.teyvatcartographia.render.Theme;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The HUD minimap (reference: minimap.png): the map fading out towards a thin frame (a circle, or a square in square
 * themes), "N" inside the frame, player arrow with a view cone, pins inside the frame and the navigation target held
 * at the edge when outside. Uses the same {@link MapView} transform and {@link TileStore} as the world map.
 */
public final class MinimapLayer implements LayeredDraw.Layer {
    /** Info pills under/over the minimap: one text line with 4 units of padding = 16. */
    private static final int PILL_H = 16;
    private static final float ARROW = 16;
    /** Smallest radius a minimap shrunk to fit the screen gets (the setting's own minimum). */
    private static final int MIN_RADIUS = 24;

    private final MapView view = new MapView();

    /**
     * Whether the minimap is shown right now: enabled, HUD not hidden (F1), no menu open and no debug screen (F3).
     * The chat is not a menu.
     */
    private static boolean visible(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.options.hideGui || !MapConfig.get(MapConfig.MINIMAP_ENABLED)) return false;
        boolean menu = mc.screen != null && !(mc.screen instanceof ChatScreen);
        return !menu && !mc.getDebugOverlay().showDebugScreen();
    }
    private static final Component NORTH = Component.literal("N");

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!visible(mc)) {
            shown = false;
            Navigation.setPathWanted(Navigation.PATH_MINIMAP, false);
            return;
        }

        float pt = delta.getGameTimeDeltaPartialTick(true);
        Vec3 pos = player.getPosition(pt);
        float yaw = player.getViewYRot(pt);
        int r = MapConfig.get(MapConfig.MINIMAP_RADIUS);
        float alpha = MapConfig.get(MapConfig.MINIMAP_OPACITY).floatValue();
        double zoom = MapView.snapZoom(MapConfig.get(MapConfig.MINIMAP_ZOOM));
        boolean rotate = MapConfig.get(MapConfig.MINIMAP_ORIENTATION) == MapConfig.Orientation.PLAYER_UP;
        // Info pills: coordinates, biome and clock, each only when enabled. The texts are rebuilt only when what they
        // show changes, so their identity tells when the layer must be redrawn.
        Component coordsLine = MapConfig.get(MapConfig.MINIMAP_SHOW_COORDS) ? coordsText(player) : null;
        Component biomeLine = MapConfig.get(MapConfig.MINIMAP_SHOW_BIOME) ? biomeText(player) : null;
        Component timeLine = MapConfig.get(MapConfig.MINIMAP_SHOW_TIME) ? timeText(mc) : null;
        int textBlock = layoutPills(coordsLine, biomeLine, timeLine, r);
        // A minimap larger than the screen allows (with its pills, at a large GUI scale) is drawn smaller to fit; the
        // setting keeps its value. A smaller map may wrap the biome to more lines, so the fit is checked once more.
        for (int i = 0; i < 2; i++) {
            int fit = MinimapFit.radius(r, g.guiWidth(), g.guiHeight(), textBlock, MIN_RADIUS);
            if (fit == r) break;
            r = fit;
            textBlock = layoutPills(coordsLine, biomeLine, timeLine, r);
        }
        screenW = g.guiWidth();
        boolean textBelow;

        int ox = MapConfig.get(MapConfig.MINIMAP_OFFSET_X), oy = MapConfig.get(MapConfig.MINIMAP_OFFSET_Y);
        float cx, cy;
        switch (MapConfig.get(MapConfig.MINIMAP_CORNER)) {
            case TOP_RIGHT -> {
                // Keep clear of the vanilla status-effect icons in the top right corner.
                int effectRows = player.getActiveEffects().isEmpty() ? 0 : 2;
                cx = g.guiWidth() - ox - r;
                cy = oy + r + effectRows * 25;
                textBelow = true;
            }
            case BOTTOM_LEFT -> {
                cx = ox + r;
                cy = g.guiHeight() - oy - r;
                textBelow = false;
            }
            case BOTTOM_RIGHT -> {
                cx = g.guiWidth() - ox - r;
                cy = g.guiHeight() - oy - r;
                textBelow = false;
            }
            default -> {
                cx = ox + r;
                cy = oy + r;
                textBelow = true;
            }
        }
        // Keep fully on screen whatever the GUI scale; centre on the physical pixel grid.
        cx = Draw.snap(Math.max(r + 3, Math.min(g.guiWidth() - r - 3, cx)));
        cy = Draw.snap(Math.max(r + 3 + (textBelow ? 0 : textBlock), Math.min(g.guiHeight() - r - 3 - (textBelow ? textBlock : 0), cy)));

        view.set(pos.x, pos.z, zoom, rotate ? MapView.rotationForYaw(yaw) : 0, cx, cy);

        // Map content (backdrop and regions) lives in a small north-up cached texture around the player that is
        // re-rendered only when the map, zoom, size or lighting changes or the player leaves its margin.
        // Underground: the cave layer the player is in, not lit by the sky.
        TileStore store = MapDataManager.get().shownStore();
        boolean cave = store != null && store.cave;
        refreshCache(g, mc, store, pos.x, pos.z, zoom, r, cave ? 0xFFFFFFFF : DayLight.tint(pt), cave ? 0f : DayLight.night(pt));

        // Everything else (faded disc, frame, markers, arrow, "N", pills) is composed into a second small layer
        // that is redrawn only when something visible changed; a still player costs one textured quad per frame.
        ResourceKey<Level> dim = player.level().dimension();
        Navigation.Target target = Navigation.target();
        int pinState = MarkerVisibility.state(zoom, true);
        // The path to the target, if the settings of its kind show it here: searched while shown, redrawn when it changes.
        MapConfig.PinOptions targetOptions = MarkerVisibility.targetOptions();
        boolean path = targetOptions != null && targetOptions.pathOnMinimap() && Navigation.inPlayerDimension(target, player);
        Navigation.setPathWanted(Navigation.PATH_MINIMAP, path);
        int route = path ? Route.version() : Integer.MIN_VALUE;
        // The layer spans the map and the pills (shifted inward at the screen edge).
        Component[] cl = coordsLine == null ? null : coordsLines, bl = biomeLine == null ? null : biomeLines, tl = timeLine == null ? null : timeLines;
        float left = Math.min(cx - r - 4, Math.min(pillLeft(cl, cx), Math.min(pillLeft(bl, cx), pillLeft(tl, cx))));
        float right = Math.max(cx + r + 4, Math.max(pillRight(cl, cx), Math.max(pillRight(bl, cx), pillRight(tl, cx))));
        int bx0 = (int) Math.floor(left), bx1 = (int) Math.ceil(right);
        int by0 = (int) Math.floor(textBelow ? cy - r - 4 : cy - r - 4 - textBlock);
        int by1 = (int) Math.ceil(textBelow ? cy + r + 4 + textBlock : cy + r + 4);
        float heading = (float) view.headingOnScreen(yaw);
        shown = true;
        shownX0 = bx0;
        shownY0 = by0;
        shownX1 = bx1;
        shownY1 = by1;

        // Movement and turning (position, heading, the numbers in the pills and the map cache following the player)
        // redraw the layer at most at the chosen update rate; the old layer stays on screen in between, at its old
        // bounds. Any other change redraws at once.
        boolean changed = !layer.exists() || !layerValid
                || cx != kCx || cy != kCy || r != kR || alpha != kAlpha
                || MarkerStore.version() != kMarkers || AutoMarkers.version() != kAuto
                || target != kTarget || pinState != kPinState
                || dim != kDim || textBelow != kTextBelow || Theme.generation() != kTheme
                || (coordsLine == null) != (kCoords == null) || (biomeLine == null) != (kBiome == null) || (timeLine == null) != (kTime == null);
        boolean moved = pos.x != kX || pos.z != kZ || heading != kHeading || view.rotation != kRotation
                || cacheGeneration != kGeneration || coordsLine != kCoords || biomeLine != kBiome || timeLine != kTime
                || bx0 != kBx0 || by0 != kBy0 || bx1 != kBx1 || by1 != kBy1 || route != kRoute;
        long now = Util.getNanos();
        if (changed || moved && now - kDrawnNs >= MapConfig.get(MapConfig.MINIMAP_UPDATE_RATE).intervalNs) {
            kDrawnNs = now;
            Window w = mc.getWindow();
            int bw = bx1 - bx0, bh = by1 - by0;
            int pw = Math.max(1, (int) Math.ceil(bw * w.getGuiScale())), ph = Math.max(1, (int) Math.ceil(bh * w.getGuiScale()));
            layer.bind(g, pw, ph, false);
            float guiW = (float) (w.getWidth() / w.getGuiScale()), guiH = (float) (w.getHeight() / w.getGuiScale());
            g.pose().pushPose();
            g.pose().scale(guiW / bw, guiH / bh, 1f);
            g.pose().translate(-bx0, -by0, 0);
            Draw.beginLayer();
            try {
                drawContents(g, player, cx, cy, r, alpha, heading, dim, target, zoom, path, coordsLine, biomeLine, timeLine, textBelow, textBlock);
                g.flush();
            } finally {
                Draw.snapIcons(true);
                Draw.endLayer();
                g.pose().popPose();
                CachedLayer.unbind();
            }
            layerValid = true;
            kX = pos.x; kZ = pos.z; kHeading = heading; kRotation = view.rotation;
            kCx = cx; kCy = cy; kR = r; kAlpha = alpha; kBx0 = bx0; kBy0 = by0; kBx1 = bx1; kBy1 = by1;
            kGeneration = cacheGeneration; kMarkers = MarkerStore.version(); kAuto = AutoMarkers.version();
            kTarget = target; kPinState = pinState; kRoute = route; kCoords = coordsLine; kBiome = biomeLine; kTime = timeLine; kDim = dim; kTextBelow = textBelow; kTheme = Theme.generation();
        }
        Draw.texturedQuadPremultiplied(g, layer.textureId(), kBx0, kBy0, kBx1, kBy1, 0f, 1f, 1f, 0f);
    }

    // Composite layer and the inputs it was drawn from.
    private final CachedLayer layer = new CachedLayer();
    private boolean layerValid, kTextBelow;
    private double kX, kZ, kRotation;
    private float kHeading, kCx, kCy, kAlpha;
    private int kR, kBx0, kBy0, kBx1, kBy1, kGeneration, kMarkers, kAuto, kTheme, kPinState, kRoute;
    private long kDrawnNs;
    // The pill texts and minimap radius they were laid out for, their lines; the GUI width the pills keep inside.
    private Component pillCoords, pillBiome, pillTime;
    private Component[] coordsLines, biomeLines, timeLines;
    private int pillR, screenW;

    /**
     * Lays out the pills for a minimap of radius {@code r} (the biome name wrapped to its width), only when a text or
     * the radius changed; returns the height of the text block under or over the map.
     */
    private int layoutPills(Component coordsLine, Component biomeLine, Component timeLine, int r) {
        if (coordsLine != pillCoords || biomeLine != pillBiome || timeLine != pillTime || r != pillR) {
            pillCoords = coordsLine;
            pillBiome = biomeLine;
            pillTime = timeLine;
            pillR = r;
            coordsLines = pillLines(coordsLine, 2 * r, false);
            biomeLines = pillLines(biomeLine, 2 * r, true);
            timeLines = pillLines(timeLine, 2 * r, false);
        }
        int pills = (coordsLine != null ? 1 : 0) + (biomeLine != null ? 1 : 0) + (timeLine != null ? 1 : 0);
        return pills == 0 ? 0 : Grid.GAP + pillHeight(coordsLines) + pillHeight(biomeLines) + pillHeight(timeLines) + (pills - 1) * Grid.GAP;
    }

    /** Left edge of a pill: centred under the map, shifted inward at the screen edge; on the pixel grid. */
    private float pillX(Component[] lines, float cx) {
        return Draw.snap(MinimapFit.pillLeft(pillWidth(lines), cx, screenW));
    }

    private float pillLeft(Component[] lines, float cx) {
        return lines == null ? Float.MAX_VALUE : pillX(lines, cx) - 2;
    }

    private float pillRight(Component[] lines, float cx) {
        return lines == null ? -Float.MAX_VALUE : pillX(lines, cx) + pillWidth(lines) + 2;
    }

    /**
     * The area the minimap (with its pills) covered when last drawn, for the marker in the world to go around it.
     * {@link WorldMarkerLayer} is drawn just before the minimap, so it sees the previous frame's area.
     */
    static boolean shown;
    static int shownX0, shownY0, shownX1, shownY1;
    private Navigation.Target kTarget;
    private Component kCoords, kBiome, kTime;
    private ResourceKey<Level> kDim;

    private void drawContents(GuiGraphics g, LocalPlayer player, float cx, float cy, int r, float alpha, float heading, ResourceKey<Level> dim,
                              Navigation.Target target, double zoom, boolean path, Component coordsLine, Component biomeLine, Component timeLine,
                              boolean textBelow, int textBlock) {
        if (Theme.Style.square) {
            // Square map with the same frame as the round one: a thin, soft line on the edge and a faint glow inside.
            drawSquareMap(g, cx, cy, r, alpha);
            Draw.frame(g, cx - r - 0.5f, cy - r - 0.5f, 2 * r + 1, 2 * r + 1, 1, Theme.withAlpha(Theme.MINIMAP_FRAME, alpha));
            Draw.squareRing(g, cx, cy, r - 5, r, Theme.withAlpha(Theme.MINIMAP_FRAME_GLOW, 0f), Theme.withAlpha(Theme.MINIMAP_FRAME_GLOW, alpha));
        } else {
            drawFadedDisc(g, cx, cy, r, alpha);
            // Thin, soft, semi-transparent frame
            Draw.circleOutline(g, cx, cy, r, 1.5f, Theme.withAlpha(Theme.MINIMAP_FRAME, alpha));
            Draw.ring(g, cx, cy, r - 5, r, Theme.withAlpha(Theme.MINIMAP_FRAME_GLOW, 0f), Theme.withAlpha(Theme.MINIMAP_FRAME_GLOW, alpha));
        }

        // Markers: kept inside the circle on which the "N" sits (never on or beyond the frame).
        float inner = innerRadius(r);
        // The path to the target under them, inside the same circle (or square).
        if (path) drawRoute(g, cx, cy, inner, alpha);
        // Pins by MarkerVisibility (none but the target below scale 1); the target itself is drawn just after. The map
        // under them moves smoothly between pixels, so they are not snapped to the pixel grid (snapped, they shook).
        Draw.snapIcons(false);
        if (!MarkerVisibility.pinsHidden(zoom)) {
            for (int i = 0, n = MarkerStore.count(); i < n; i++) {
                Marker m = MarkerStore.get(i);
                if (m.dimension == dim && MarkerVisibility.shown(m, zoom, true)) drawMarker(g, m, cx, cy, inner, alpha);
            }
            List<Marker> auto = AutoMarkers.forDimension(dim);
            for (int i = 0; i < auto.size(); i++) {
                if (MarkerVisibility.shown(auto.get(i), zoom, true)) drawMarker(g, auto.get(i), cx, cy, inner, alpha);
            }
        }
        if (target != null && target.dimension() == dim) drawTarget(g, target, cx, cy, inner, alpha);
        Draw.snapIcons(true);

        // View cone and player arrow
        Draw.sector(g, cx, cy, r * 0.8f, heading, 0.72f, Theme.withAlpha(Theme.MINIMAP_VIEW_CONE, alpha), Theme.withAlpha(Theme.MINIMAP_VIEW_CONE, 0f));
        MarkerRenderer.player(g, cx, cy, ARROW, heading, alpha);

        // "N": inside the circle, no background, pointing to world north.
        double northAngle = view.rotation; // screen angle of world north, clockwise from up
        float ns = (float) Math.sin(northAngle), nc = (float) Math.cos(northAngle);
        // On the inner circle, or (square map) on the inner square's edge.
        float nk = Theme.Style.square ? inner / Math.max(Math.abs(ns), Math.abs(nc)) : inner;
        float nx = cx + ns * nk, ny = cy - nc * nk;
        // Vanilla-font letter in the frame's colour, with a faint dark outline so it stays readable on light ground
        // and stands apart from the (dark) pin tiles. Drawn last, above the markers. Left batched: the layer is flushed
        // once at the end (the pills below never overlap it).
        DrawText.textOutlined(g, NORTH, nx, ny, Theme.withAlpha(Theme.MINIMAP_NORTH, alpha), Theme.withAlpha(Theme.MINIMAP_NORTH_OUTLINE, alpha), false);

        // Pills: coordinates, biome, clock
        float ty = textBelow ? cy + r + 3 + Grid.GAP : cy - r - 3 - textBlock + Grid.GAP;
        ty = pill(g, cx, ty, coordsLine == null ? null : coordsLines, Theme.WHITE, alpha);
        ty = pill(g, cx, ty, biomeLine == null ? null : biomeLines, Theme.WHITE, alpha);
        pill(g, cx, ty, timeLine == null ? null : timeLines, Theme.WHITE, alpha);
    }
    // Screen points of the path inside the map, one piece at a time, reused.
    private float[] routeXY = new float[512];
    private boolean[] routeFog = new boolean[256];

    /** The path as a dash-dot line, gold over explored land and white through the fog, cut where it leaves the map. */
    private void drawRoute(GuiGraphics g, float cx, float cy, float inner, float alpha) {
        int n = Route.collect(), k = 0;
        if (n < 2) return;
        int line = Theme.withAlpha(Theme.worldMarkerPulse(MarkerVisibility.kind(Navigation.target())), alpha), fog = Theme.withAlpha(Theme.ROUTE_FOG, alpha), shadow = Theme.withAlpha(Theme.ROUTE_SHADOW, alpha);
        // The walked part is gone: from the line's point nearest the player (the map's centre) on. Each piece's dashes
        // start at their place along the whole line, so they stay put on the map as the player walks.
        int first = Route.cut(view.centerX, view.centerZ);
        float phase = 0;
        for (int i = first; i <= n; i++) {
            boolean in = false;
            float sx = 0, sy = 0;
            if (i < n) {
                float wx = i == first ? Route.cutX() : Route.x(i), wz = i == first ? Route.cutZ() : Route.z(i);
                sx = view.screenX(wx, wz);
                sy = view.screenY(wx, wz);
                in = Theme.Style.square ? Math.abs(sx - cx) <= inner && Math.abs(sy - cy) <= inner : Draw.inCircle(sx, sy, cx, cy, inner);
                if (in && k == 0) phase = (float) ((i == first ? Route.cutAlong() : Route.along(i)) * view.scale);
            }
            if (in) {
                if (k == routeFog.length) {
                    routeXY = java.util.Arrays.copyOf(routeXY, 4 * k);
                    routeFog = java.util.Arrays.copyOf(routeFog, 2 * k);
                }
                routeXY[2 * k] = sx;
                routeXY[2 * k + 1] = sy;
                routeFog[k++] = Route.fog(i);
                continue;
            }
            if (k >= 2) {
                Draw.route(g, routeXY, routeFog, k, 2f, shadow, shadow, phase);
                Draw.route(g, routeXY, routeFog, k, 1f, line, fog, phase);
            }
            k = 0;
        }
    }

    private void drawMarker(GuiGraphics g, Marker m, float cx, float cy, float inner, float alpha) {
        if (Navigation.isTracking(m)) return;
        float sx = view.screenX(m.centerX(), m.centerZ()), sy = view.screenY(m.centerX(), m.centerZ());
        if (Theme.Style.square ? Math.abs(sx - cx) <= inner && Math.abs(sy - cy) <= inner : Draw.inCircle(sx, sy, cx, cy, inner)) {
            // Pins on another layer than the one shown (the cave band the player is in, or the surface) get an arrow.
            MarkerRenderer.draw(g, m.type, sx, sy, MarkerRenderer.SIZE_SMALL, false, false, alpha,
                    MarkerVisibility.layerRelation(m, MapDataManager.get().undergroundBand()));
        }
    }

    // Pill texts are rebuilt only when what they show changes.
    private int coordsX = Integer.MIN_VALUE, coordsY, coordsZ;
    private Component coords;

    private Component coordsText(LocalPlayer p) {
        int x = p.getBlockX(), y = p.getBlockY(), z = p.getBlockZ();
        if (coords == null || x != coordsX || y != coordsY || z != coordsZ) {
            coordsX = x;
            coordsY = y;
            coordsZ = z;
            coords = Component.literal(x + "  " + y + "  " + z);
        }
        return coords;
    }

    // The biome is looked up again only when the player enters another block, or while the chunk under the player was
    // not loaded yet at the last look-up (after a teleport an empty chunk answers plains until the real one arrives).
    private final BlockPos.MutableBlockPos biomePos = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, 0, 0);
    private Holder<Biome> biomeHolder;
    private Component biome;
    private boolean biomeUnloaded;

    private Component biomeText(LocalPlayer p) {
        int x = p.getBlockX(), y = p.getBlockY(), z = p.getBlockZ();
        if (biome == null || biomeUnloaded || x != biomePos.getX() || y != biomePos.getY() || z != biomePos.getZ()) {
            biomePos.set(x, y, z);
            biomeUnloaded = !p.level().hasChunkAt(biomePos);
            Holder<Biome> h = p.level().getBiome(biomePos);
            if (h != biomeHolder || biome == null) {
                biomeHolder = h;
                biome = h.unwrapKey().map(MapLayerRenderer::biomeName).orElse(Component.empty());
            }
        }
        return biome;
    }

    // The clock text changes once a minute (of game or real time).
    private int clockMinute = -1;
    private MapConfig.TimeSource clockSource;
    private Component clock;
    private static final TimeZone ZONE = TimeZone.getDefault();

    /** "HH:MM": the time of day in the world (tick 0 is 06:00) or the computer's local time. */
    private Component timeText(Minecraft mc) {
        MapConfig.TimeSource source = MapConfig.get(MapConfig.MINIMAP_TIME_SOURCE);
        int minute;
        if (source == MapConfig.TimeSource.REAL_TIME) {
            long now = System.currentTimeMillis();
            minute = (int) (Math.floorDiv(now + ZONE.getOffset(now), 60_000L) % 1440);
        } else {
            long ticks = Math.floorMod(mc.level.getDayTime() + 6000, 24000L);
            minute = (int) (ticks * 1440 / 24000);
        }
        if (clock == null || minute != clockMinute || source != clockSource) {
            clockMinute = minute;
            clockSource = source;
            clock = Component.literal(String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60));
        }
        return clock;
    }

    private static float pillWidth(Component[] lines) {
        if (lines == null) return 0;
        int w = 0;
        for (Component line : lines) w = Math.max(w, DrawText.width(line));
        return PILL_H + w;
    }

    /** Height of a pill with {@code lines} text lines (0 for none). */
    private static int pillHeight(Component[] lines) {
        return lines == null ? 0 : PILL_H + (lines.length - 1) * DrawText.LINE;
    }

    /**
     * The text as the lines of its pill: the biome name wrapped at words so that its pill is no wider than the minimap
     * ({@code maxWidth}, never narrower than {@code MIN_WRAP}), the others as one line. Built only when the text or
     * the minimap size changes.
     */
    private static Component[] pillLines(Component text, int maxWidth, boolean wrap) {
        if (text == null) return null;
        int room = Math.max(MIN_WRAP, maxWidth - PILL_H);
        if (!wrap || DrawText.width(text) <= room) return new Component[]{text};
        java.util.List<net.minecraft.network.chat.FormattedText> parts =
                DrawText.font().getSplitter().splitLines(text, room, net.minecraft.network.chat.Style.EMPTY);
        Component[] lines = new Component[parts.size()];
        for (int i = 0; i < lines.length; i++) lines[i] = Component.literal(parts.get(i).getString());
        return lines;
    }

    /** A wrapped biome pill keeps at least this much room for its text (a tiny minimap would split every word). */
    private static final int MIN_WRAP = 60;

    /** Pill centred on cx at y, if there are lines (each centred in it); returns the y of the next pill. */
    private float pill(GuiGraphics g, float cx, float y, Component[] lines, int color, float alpha) {
        if (lines == null) return y;
        float w = pillWidth(lines);
        int h = pillHeight(lines);
        float x = pillX(lines, cx);
        int fill = Theme.withAlpha(Theme.INFO_BOX, alpha);
        // Several lines: rounded like the one-line capsule, not a taller capsule.
        if (lines.length > 1 && !Theme.Style.square) Draw.roundRect(g, x, y, w, h, PILL_H / 2f, fill);
        else Ui.pillShape(g, x, y, w, h, fill, 0);
        float ty = DrawText.textY(y, PILL_H);
        for (Component line : lines) {
            DrawText.text(g, line, x + (w - DrawText.width(line)) / 2f, ty, Theme.withAlpha(color, alpha), false);
            ty += DrawText.LINE;
        }
        return y + h + Grid.GAP;
    }
    private void drawTarget(GuiGraphics g, Navigation.Target t, float cx, float cy, float r, float alpha) {
        float sx = view.screenX(t.x(), t.z()), sy = view.screenY(t.x(), t.z());
        MarkerType type = t.auto();
        if (t.markerId() != null) {
            Marker m = MarkerStore.find(t.markerId());
            type = m != null ? m.type : MarkerType.STAR;
        }
        float size = MarkerRenderer.SIZE_SMALL;
        float dx = sx - cx, dy = sy - cy;
        // Distance in the shape's own metric: Euclidean for the round map, Chebyshev for the square one.
        float d = Theme.Style.square ? Math.max(Math.abs(dx), Math.abs(dy)) : (float) Math.sqrt(dx * dx + dy * dy);
        if (d > r) {
            // Out of range: held on the inner circle (same radius as the "N") in the target's direction.
            sx = cx + dx / d * r;
            sy = cy + dy / d * r;
        }
        MarkerRenderer.draw(g, type, sx, sy, size, false, false, alpha);
    }

    /** Radius of the inner circle carrying the "N" and bounding the markers: a marker's half-size inside the frame. */
    private static float innerRadius(float r) {
        return r - MarkerRenderer.SIZE_SMALL / 2f - 3;
    }

    // ------------------------------------------------------------------ cached map picture and faded disc

    /** GUI units of map kept around the visible disc, so walking does not re-render the cache every frame. */
    private static final int MARGIN = 24;
    /** The cache is rendered at twice the screen density, so sampling it rotated stays sharp. */
    private static final int SUPERSAMPLE = 2;
    /** Map content changes (newly scanned chunks) refresh the cache at most this often. */
    private static final long CONTENT_REFRESH_MS = 250;
    /** Disc tessellation: 48 segments keep the edge within a pixel of a true circle even at the largest size. */
    private static final int SEGMENTS = 48, BANDS = 6;
    private static final float[] COS = new float[SEGMENTS + 1], SIN = new float[SEGMENTS + 1];

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = 2 * Math.PI * i / SEGMENTS;
            COS[i] = (float) Math.cos(a);
            SIN[i] = (float) Math.sin(a);
        }
    }

    private final MapView cacheView = new MapView();
    private final CachedLayer cache = new CachedLayer();
    private TileStore cacheStore;
    private int cacheVersion, cacheR, cacheSize, cacheTint, cacheNight, cacheTheme;
    private double cacheX, cacheZ, cacheZoom;
    private long cacheTime;
    private boolean cacheValid;
    /** Incremented whenever the cached map picture is re-rendered (part of the composite layer's key). */
    private int cacheGeneration;

    /** Re-renders the cached square (radius {@code r + MARGIN}, north up, centred near the player) when needed. */
    private void refreshCache(GuiGraphics g, Minecraft mc, TileStore store, double px, double pz, double zoom, int r, int tint, float night) {
        Window w = mc.getWindow();
        // The rotated view must stay inside the cache: a square map reaches r * sqrt(2) at its corners.
        int cR = (Theme.Style.square ? (int) Math.ceil(r * 1.4143f) : r) + MARGIN;
        int size = Math.min(4096, (int) Math.ceil(2 * cR * w.getGuiScale() * SUPERSAMPLE));
        int nightQ = Math.round(night * 32);
        long now = Util.getMillis();
        boolean stale = !cacheValid || !cache.exists()
                || store != cacheStore || zoom != cacheZoom || cR != cacheR || size != cacheSize
                || tint != cacheTint || nightQ != cacheNight || Theme.generation() != cacheTheme
                || Math.hypot(px - cacheX, pz - cacheZ) * zoom > MARGIN - 1
                || (store != null && store.version() != cacheVersion && now - cacheTime >= CONTENT_REFRESH_MS);
        if (!stale) {
            touchTiles(store);
            return;
        }

        cache.bind(g, size, size, true);

        // The GUI projection spans the whole (now cache-sized) viewport; scale cache units (0..2cR) onto it.
        float guiW = (float) (w.getWidth() / w.getGuiScale()), guiH = (float) (w.getHeight() / w.getGuiScale());
        g.pose().pushPose();
        g.pose().scale(guiW / (2f * cR), guiH / (2f * cR), 1f);
        Draw.rect(g, 0, 0, 2 * cR, 2 * cR, store != null && store.cave ? Theme.MINIMAP_CAVE_BACKDROP : DayLight.apply(Theme.MINIMAP_BACKDROP, tint));
        boolean complete = true;
        if (store != null) {
            cacheView.set(px, pz, zoom, 0, cR, cR);
            // The backdrop made the square opaque; keep its alpha so translucent layers only change colour.
            RenderSystem.colorMask(true, true, true, false);
            complete = MapLayerRenderer.drawRect(g, store, cacheView, 0, 0, 2 * cR, 2 * cR, tint, night);
            RenderSystem.colorMask(true, true, true, true);
        }
        g.pose().popPose();
        CachedLayer.unbind();

        cacheValid = complete;
        cacheGeneration++;
        cacheStore = store;
        cacheVersion = store == null ? 0 : store.version();
        cacheZoom = zoom;
        cacheR = cR;
        cacheSize = size;
        cacheTint = tint;
        cacheNight = nightQ;
        cacheTheme = Theme.generation();
        cacheX = px;
        cacheZ = pz;
        cacheTime = now;
    }

    /** Keeps the cached regions marked as used so the store's LRU does not evict what the minimap shows. */
    private void touchTiles(TileStore store) {
        if (store == null) return;
        double half = cacheR / cacheZoom;
        int tx0 = MapView.blockToTile((int) Math.floor(cacheX - half)), tx1 = MapView.blockToTile((int) Math.floor(cacheX + half));
        int tz0 = MapView.blockToTile((int) Math.floor(cacheZ - half)), tz1 = MapView.blockToTile((int) Math.floor(cacheZ + half));
        for (int tz = tz0; tz <= tz1; tz++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                MapTile t = store.peek(tx, tz);
                if (t != null) t.touch();
            }
        }
    }

    /** Fraction of the radius that stays fully opaque; beyond it the map fades to transparent at the frame. */
    private static final float FADE_START = 0.80f;

    /** Draws the cache as a disc: opaque centre, then an annulus whose alpha falls smoothly to 0 at radius r. */
    private void drawFadedDisc(GuiGraphics g, float cx, float cy, float r, float alpha) {
        if (!cache.exists()) return;
        Matrix4f m = g.pose().last().pose();
        int[] colors = bandColors;
        for (int k = 0; k <= BANDS; k++) colors[k] = Theme.withAlpha(Theme.WHITE, alpha * fade((float) k / BANDS));
        float r0 = r * FADE_START;
        // Every grid point (ring k at segment i) is projected once; the triangles below only pick them up.
        for (int k = 0; k <= BANDS; k++) {
            float rk = r0 + (r - r0) * k / BANDS;
            for (int i = 0; i <= SEGMENTS; i++) gridPoint(k * (SEGMENTS + 1) + i, cx + COS[i] * rk, cy + SIN[i] * rk);
        }
        gridPoint(GRID - 1, cx, cy);
        BufferBuilder b = Draw.beginTex();
        for (int i = 0; i < SEGMENTS; i++) {
            // opaque centre
            gridVertex(b, m, GRID - 1, colors[0]);
            gridVertex(b, m, i, colors[0]);
            gridVertex(b, m, i + 1, colors[0]);
            // fade bands (smoothstep falloff)
            for (int k = 0; k < BANDS; k++) {
                int a0 = k * (SEGMENTS + 1) + i, b0 = a0 + SEGMENTS + 1;
                int ca = colors[k], cb = colors[k + 1];
                gridVertex(b, m, a0, ca);
                gridVertex(b, m, b0, cb);
                gridVertex(b, m, b0 + 1, cb);
                gridVertex(b, m, a0, ca);
                gridVertex(b, m, b0 + 1, cb);
                gridVertex(b, m, a0 + 1, ca);
            }
        }
        Draw.endTex(b, cache.textureId());
    }

    /** Disc grid: (BANDS + 1) rings of (SEGMENTS + 1) points, then the centre. Screen position and cache UV. */
    private static final int GRID = (BANDS + 1) * (SEGMENTS + 1) + 1;
    private final float[] gridX = new float[GRID], gridY = new float[GRID], gridU = new float[GRID], gridV = new float[GRID];

    private void gridPoint(int i, float x, float y) {
        double side = 2.0 * cacheR;
        gridX[i] = x;
        gridY[i] = y;
        gridU[i] = (float) ((cacheR + (view.worldX(x, y) - cacheX) * cacheZoom) / side);
        gridV[i] = (float) (1 - (cacheR + (view.worldZ(x, y) - cacheZ) * cacheZoom) / side);
    }

    private void gridVertex(BufferBuilder b, Matrix4f m, int i, int color) {
        Draw.vtx(b, m, gridX[i], gridY[i], gridU[i], gridV[i], color);
    }

    private final int[] bandColors = new int[BANDS + 1];

    /**
     * Square themes: the cache as a square whose alpha fades out towards the frame, like the round minimap: an opaque
     * centre square, then {@link #BANDS} square rings (4 trapezoids each) with the same smoothstep falloff.
     */
    private void drawSquareMap(GuiGraphics g, float cx, float cy, float r, float alpha) {
        if (!cache.exists()) return;
        Matrix4f m = g.pose().last().pose();
        int[] colors = bandColors;
        for (int k = 0; k <= BANDS; k++) colors[k] = Theme.withAlpha(Theme.WHITE, alpha * fade((float) k / BANDS));
        float r0 = r * FADE_START;
        BufferBuilder b = Draw.beginTex();
        int c = colors[0];
        discVertex(b, m, cx - r0, cy - r0, c); discVertex(b, m, cx - r0, cy + r0, c); discVertex(b, m, cx + r0, cy + r0, c);
        discVertex(b, m, cx - r0, cy - r0, c); discVertex(b, m, cx + r0, cy + r0, c); discVertex(b, m, cx + r0, cy - r0, c);
        for (int k = 0; k < BANDS; k++) {
            float a = r0 + (r - r0) * k / BANDS, o = r0 + (r - r0) * (k + 1) / BANDS;
            int ci = colors[k], co = colors[k + 1];
            // corners of the inner (a) and outer (o) squares, clockwise from top-left
            for (int side = 0; side < 4; side++) {
                float ix0 = cx + Draw.SQUARE_X[side] * a, iy0 = cy + Draw.SQUARE_Y[side] * a;
                float ix1 = cx + Draw.SQUARE_X[side + 1] * a, iy1 = cy + Draw.SQUARE_Y[side + 1] * a;
                float ox0 = cx + Draw.SQUARE_X[side] * o, oy0 = cy + Draw.SQUARE_Y[side] * o;
                float ox1 = cx + Draw.SQUARE_X[side + 1] * o, oy1 = cy + Draw.SQUARE_Y[side + 1] * o;
                discVertex(b, m, ix0, iy0, ci); discVertex(b, m, ox0, oy0, co); discVertex(b, m, ox1, oy1, co);
                discVertex(b, m, ix0, iy0, ci); discVertex(b, m, ox1, oy1, co); discVertex(b, m, ix1, iy1, ci);
            }
        }
        Draw.endTex(b, cache.textureId());
    }

    /** 1 at the start of the fade band, 0 at the frame, smooth at both ends. */
    private static float fade(float t) {
        float s = 1 - t;
        return s * s * (3 - 2 * s);
    }

    /** Screen point -> world (through the rotated view) -> position in the north-up cache texture. */
    private void discVertex(BufferBuilder b, Matrix4f m, float x, float y, int color) {
        double side = 2.0 * cacheR;
        float u = (float) ((cacheR + (view.worldX(x, y) - cacheX) * cacheZoom) / side);
        float v = (float) (1 - (cacheR + (view.worldZ(x, y) - cacheZ) * cacheZoom) / side);
        Draw.vtx(b, m, x, y, u, v, color);
    }
}
