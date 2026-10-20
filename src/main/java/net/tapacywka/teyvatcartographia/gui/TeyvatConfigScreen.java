package net.tapacywka.teyvatcartographia.gui;

import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.render.Backdrop;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.FogTexture;
import net.tapacywka.teyvatcartographia.render.Icon;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.tapacywka.teyvatcartographia.render.ThemeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Full settings screen in the dark-panel style (switches, sliders, choice cyclers), reachable from the map's
 * settings panel and from the NeoForge mod list. Values are written to the client config immediately.
 * All rows share one layout: label on the left, control right-aligned in the right half, on the 4-unit grid.
 */
public final class TeyvatConfigScreen extends Screen {
    private static final int SECTION_H = 24;
    /** Width reserved for slider values (right column). */
    private static final int VALUE_W = 32;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private float scroll;
    private Row draggingRow;
    private float panelX, panelY, panelW, panelH, listTop, listBottom;
    private int blurW = -1, blurH = -1, blurTheme = -1, blurFrames;

    private abstract static class Row {
        final Component label;
        /** Longer explanation shown when hovering the label; null for section headers. */
        final Component tooltip;
        float y;
        /** A nested setting: a step to the right, inactive (darker, not clickable) while this one is off. */
        ModConfigSpec.BooleanValue parent;
        /** The sub-heading the row belongs to: hidden (no height) while it is folded. */
        SubSection owner;

        boolean shown() {
            return owner == null || owner.open();
        }

        /** Height in the list: 0 while folded away. */
        float listHeight() {
            return shown() ? height() : 0;
        }

        Row under(ModConfigSpec.BooleanValue parent) {
            this.parent = parent;
            return this;
        }

        boolean active() {
            return parent == null || MapConfig.get(parent);
        }

        float indent() {
            return parent == null ? 0 : 2 * Grid.GAP;
        }

        Row(String key, boolean hasTooltip) {
            this.label = Component.translatable(key);
            this.tooltip = hasTooltip ? Component.translatable(key + ".tooltip") : null;
        }

        Row(String key) {
            this(key, true);
        }

        float height() {
            return Grid.ROW;
        }

        void renderLabel(GuiGraphics g, float x, float w) {
            float in = indent();
            DrawText.text(g, DrawText.fit(label.getString(), (int) (w / 2 - Grid.GAP - in)), x + in, DrawText.textY(y, Grid.ROW),
                    active() ? Theme.WHITE : Ui.inactive(Theme.WHITE), false);
        }

        abstract void render(GuiGraphics g, float x, float w, int mx, int my);

        boolean click(double mx, double my, float x, float w) {
            return false;
        }

        void drag(double mx, float x, float w) {}
    }

    private static final class Section extends Row {
        Section(String key) {
            super(key, false);
        }

        @Override
        float height() {
            return SECTION_H;
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            DrawText.room(label, w);
            DrawText.text(g, label, x, y + Grid.PAD, Theme.GOLD, false);
            Draw.rect(g, x, y + SECTION_H - 3, x + w, y + SECTION_H - 2, Theme.PANEL_LINE);
        }
    }

    /** Sub-headings opened by the player, by their key (all start folded): kept while the game runs (not saved). */
    private static final java.util.Set<String> OPENED = new java.util.HashSet<>();

    /**
     * Sub-heading inside a section (e.g. the minimap's display and its position): a dark band across the list with a
     * gold mark on the left, so it stands apart from the settings rows. Folded at first; a click unfolds or folds its rows; the small
     * arrow on the right points down while open, right while folded.
     */
    private static final class SubSection extends Row {
        final String key;

        SubSection(String key) {
            super(key, false);
            this.key = key;
        }

        boolean open() {
            return OPENED.contains(key);
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            boolean hover = Draw.inRect(mx, my, x, y + 2, w, Grid.ROW - 4);
            Draw.rect(g, x, y + 2, x + w, y + Grid.ROW - 2, hover ? Theme.PANEL_DARK_TILE_HOVER : Theme.INFO_BOX);
            Draw.rect(g, x, y + 2, x + 2, y + Grid.ROW - 2, Theme.GOLD);
            DrawText.room(label, w - 2 - 3 * Grid.GAP - 6); // up to the fold arrow
            DrawText.text(g, label, x + 2 + Grid.GAP, DrawText.textY(y, Grid.ROW), Theme.CREAM, false);
            float ax = x + w - Grid.GAP - 3, ay = y + Grid.ROW / 2f;
            if (open()) Draw.triangle(g, ax - 3, ay - 1.5f, ax + 3, ay - 1.5f, ax, ay + 2f, Theme.CREAM);
            else Draw.triangle(g, ax - 1.5f, ay - 3, ax - 1.5f, ay + 3, ax + 2f, ay, Theme.CREAM);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Draw.inRect(mx, my, x, y + 2, w, Grid.ROW - 4)) return false;
            if (!OPENED.remove(key)) OPENED.add(key);
            return true;
        }
    }

    private static final class BoolRow extends Row {
        final ModConfigSpec.BooleanValue value;

        BoolRow(String key, ModConfigSpec.BooleanValue value) {
            super(key);
            this.value = value;
        }

        private float tx(float x, float w) {
            return x + w - Grid.TOGGLE_W;
        }

        private float ty() {
            return y + (Grid.ROW - Grid.TOGGLE_H) / 2f;
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            Ui.toggle(g, tx(x, w), ty(), MapConfig.get(value), mx, my, active());
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Ui.toggleHit(tx(x, w), ty(), mx, my)) return false;
            value.set(!MapConfig.get(value));
            MapConfig.save();
            return true;
        }
    }

    private static final class SliderRow extends Row {
        final double min, max;
        final boolean log, integer;
        final DoubleSupplier getter;
        final DoubleConsumer setter;
        final String format;
        /** Optional fixed values; the slider then snaps to them (evenly spaced along the track). */
        double[] steps;
        /** Optional text for a value (instead of {@link #format}). */
        java.util.function.DoubleFunction<String> labeler;

        SliderRow(String key, double min, double max, boolean log, boolean integer, String format, DoubleSupplier getter, DoubleConsumer setter) {
            super(key);
            this.min = min;
            this.max = max;
            this.log = log;
            this.integer = integer;
            this.format = format;
            this.getter = getter;
            this.setter = setter;
        }

        static SliderRow ofInt(String key, ModConfigSpec.IntValue v, int min, int max) {
            return new SliderRow(key, min, max, false, true, "%d", () -> MapConfig.get(v), d -> v.set((int) Math.round(d)));
        }

        static SliderRow ofDouble(String key, ModConfigSpec.DoubleValue v, double min, double max, boolean log, String format) {
            return new SliderRow(key, min, max, log, false, format, () -> MapConfig.get(v), v::set);
        }

        /** A zoom setting: the steps of MapView.ZOOM_STEPS, shown as "1/4", "1", "8". */
        static SliderRow ofZoom(String key, ModConfigSpec.DoubleValue v) {
            double[] steps = MapView.ZOOM_STEPS;
            SliderRow r = new SliderRow(key, steps[0], steps[steps.length - 1], false, false, "%.2f", () -> MapConfig.get(v), v::set);
            r.steps = steps;
            r.labeler = MapView::zoomLabel;
            return r;
        }

        private int nearestStep(double v) {
            return MapView.nearestZoomStep(v);
        }

        double toT(double v) {
            if (steps != null) return nearestStep(v) / (double) (steps.length - 1);
            return log ? (Math.log(v) - Math.log(min)) / (Math.log(max) - Math.log(min)) : (v - min) / (max - min);
        }

        double fromT(double t) {
            t = Math.max(0, Math.min(1, t));
            if (steps != null) return steps[(int) Math.round(t * (steps.length - 1))];
            double v = log ? Math.exp(Math.log(min) + t * (Math.log(max) - Math.log(min))) : min + t * (max - min);
            return integer ? Math.round(v) : Math.round(v * 100) / 100.0;
        }

        /** Track spans the right half minus the value column; 8 units of inset so the knob never overhangs. */
        float trackX(float x, float w) {
            return x + w / 2 + Grid.PAD;
        }

        float trackW(float w) {
            return w / 2 - VALUE_W - 2 * Grid.PAD;
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            float tx = trackX(x, w), tw = trackW(w), cy = y + Grid.ROW / 2f;
            double v = getter.getAsDouble();
            float t = (float) Math.max(0, Math.min(1, toT(v)));
            boolean hover = Draw.inRect(mx, my, tx - Grid.PAD, y, tw + 2 * Grid.PAD, Grid.ROW);
            if (Theme.Style.square) {
                Draw.bevel(g, tx, cy - 3, tw, 6, Theme.TOGGLE_TRACK_OFF, Theme.BEVEL_BORDER, true);
                Draw.rect(g, tx + 1, cy - 2, tx + 1 + Math.max(0, (tw - 2) * t), cy + 2, Theme.GOLD);
                Draw.bevel(g, Math.round(tx + tw * t) - 4, cy - 7, 8, 14, Theme.TOGGLE_KNOB, hover ? Theme.WHITE : Theme.SLIDER_TICK, false);
            } else {
                Draw.capsule(g, tx, cy - 2, tw, 4, Theme.TOGGLE_TRACK_OFF);
                Draw.capsule(g, tx, cy - 2, Math.max(4, tw * t), 4, Theme.GOLD);
                float k = hover ? 7 : 6;
                Draw.diamond(g, tx + tw * t, cy, k, Theme.TOGGLE_KNOB);
                Draw.diamondOutline(g, tx + tw * t, cy, k, 1, Theme.SLIDER_TICK);
            }
            String text = labeler != null ? labeler.apply(v)
                    : integer ? String.format(Locale.ROOT, format, (int) Math.round(v)) : String.format(Locale.ROOT, format, v);
            DrawText.room(text, VALUE_W + Grid.GAP); // right of the track and its knob
            DrawText.textRight(g, Component.literal(text), x + w, DrawText.textY(y, Grid.ROW), Theme.CREAM, false);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            if (!Draw.inRect(mx, my, trackX(x, w) - Grid.PAD, y, trackW(w) + 2 * Grid.PAD, Grid.ROW)) return false;
            drag(mx, x, w);
            return true;
        }

        @Override
        void drag(double mx, float x, float w) {
            setter.accept(fromT((mx - trackX(x, w)) / trackW(w)));
            MapConfig.save();
        }
    }

    private static final class EnumRow<E extends Enum<E>> extends Row {
        static final int H = 16;
        final ModConfigSpec.EnumValue<E> value;
        final E[] values;
        final Runnable onChange;

        EnumRow(String key, ModConfigSpec.EnumValue<E> value, E[] values, Runnable onChange) {
            super(key);
            this.value = value;
            this.values = values;
            this.onChange = onChange;
        }

        float bx(float x, float w) {
            return x + w / 2;
        }

        float by() {
            return y + (Grid.ROW - H) / 2f;
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            float bx = bx(x, w), bw = w / 2, by = by();
            boolean hover = Draw.inRect(mx, my, bx, by, bw, H);
            Component c = Component.translatable("teyvatcartographia.config.value." + MapConfig.get(value).name().toLowerCase(Locale.ROOT));
            cycler(g, bx, by, bw, H, c, hover, active());
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            float bx = bx(x, w), bw = w / 2;
            if (!Draw.inRect(mx, my, bx, by(), bw, H)) return false;
            int dir = mx < bx + bw / 2 ? -1 : 1;
            int i = (MapConfig.get(value).ordinal() + dir + values.length) % values.length;
            value.set(values[i]);
            MapConfig.save();
            if (onChange != null) onChange.run();
            return true;
        }
    }

    /** Choice button with arrows at both ends (clicking the left/right half steps back/forward). */
    static void cycler(GuiGraphics g, float bx, float by, float bw, float h, Component value, boolean hover) {
        cycler(g, bx, by, bw, h, value, hover, true);
    }

    /** Darker when there is nothing to choose ({@code active} false): no hover, dimmed text and arrows. */
    static void cycler(GuiGraphics g, float bx, float by, float bw, float h, Component value, boolean hover, boolean active) {
        hover &= active;
        int fill = active ? hover ? Theme.BUTTON_HOVER : Theme.BUTTON : Ui.inactive(Theme.BUTTON);
        if (Theme.Style.square) Draw.bevel(g, bx, by, bw, h, fill, hover ? Theme.BUTTON_HOVER_OUTLINE : Theme.BEVEL_BORDER, false);
        else Draw.capsule(g, bx, by, bw, h, fill);
        DrawText.room(value, bw - 2 * h); // between the arrows in the caps
        DrawText.textCentered(g, value, bx + bw / 2, DrawText.textY(by, h), active ? Theme.CREAM : Ui.inactive(Theme.CREAM), false);
        float cy = by + h / 2f, a = h / 2f; // arrows centred in the caps
        int arrow = active ? Theme.GOLD : Ui.inactive(Theme.GOLD);
        Draw.triangle(g, bx + a - 2, cy, bx + a + 2, cy - 4, bx + a + 2, cy + 4, arrow);
        Draw.triangle(g, bx + bw - a + 2, cy, bx + bw - a - 2, cy - 4, bx + bw - a - 2, cy + 4, arrow);
    }


    /** Theme picker: cycles through the themes found in the resources and applies the choice at once. */
    private static final class ThemeRow extends Row {
        static final int H = EnumRow.H;

        ThemeRow() {
            super("teyvatcartographia.config.theme");
        }

        @Override
        void render(GuiGraphics g, float x, float w, int mx, int my) {
            renderLabel(g, x, w);
            float bx = x + w / 2, bw = w / 2, by = y + (Grid.ROW - H) / 2f;
            // Only the built-in theme: nothing to choose until a theme pack is enabled.
            cycler(g, bx, by, bw, H, ThemeLoader.displayName(Theme.id()), Draw.inRect(mx, my, bx, by, bw, H), ThemeLoader.available().size() > 1);
        }

        @Override
        boolean click(double mx, double my, float x, float w) {
            float bx = x + w / 2, bw = w / 2, by = y + (Grid.ROW - H) / 2f;
            if (!Draw.inRect(mx, my, bx, by, bw, H)) return false;
            java.util.List<String> ids = ThemeLoader.available();
            if (ids.size() < 2) return true;
            int i = Math.max(0, ids.indexOf(Theme.id()));
            i = (i + (mx < bx + bw / 2 ? -1 : 1) + ids.size()) % ids.size();
            MapConfig.THEME.set(ids.get(i));
            MapConfig.save();
            ThemeLoader.applyNow();
            return true;
        }
    }

    public TeyvatConfigScreen(Screen parent) {
        super(Component.translatable("teyvatcartographia.config.title"));
        this.parent = parent;
        // Appearance
        rows.add(new Section("teyvatcartographia.config.section.appearance"));
        rows.add(new ThemeRow());
        // World map
        rows.add(new Section("teyvatcartographia.config.section.worldmap"));
        rows.add(SliderRow.ofZoom("teyvatcartographia.config.map_zoom", MapConfig.MAP_DEFAULT_ZOOM));
        rows.add(new BoolRow("teyvatcartographia.config.map_labels", MapConfig.MAP_SHOW_LABELS));
        rows.add(new BoolRow("teyvatcartographia.config.map_grid", MapConfig.MAP_SHOW_GRID));
        // Minimap: what it shows, its size and place on the screen, the information under it
        rows.add(new Section("teyvatcartographia.config.section.minimap"));
        rows.add(new SubSection("teyvatcartographia.config.subsection.minimap_display"));
        rows.add(new BoolRow("teyvatcartographia.config.minimap_enabled", MapConfig.MINIMAP_ENABLED));
        rows.add(SliderRow.ofZoom("teyvatcartographia.config.minimap_zoom", MapConfig.MINIMAP_ZOOM));
        rows.add(new EnumRow<>("teyvatcartographia.config.minimap_orientation", MapConfig.MINIMAP_ORIENTATION, MapConfig.Orientation.values(), null));
        rows.add(new EnumRow<>("teyvatcartographia.config.minimap_rate", MapConfig.MINIMAP_UPDATE_RATE, MapConfig.UpdateRate.values(), null));
        rows.add(new SubSection("teyvatcartographia.config.subsection.minimap_layout"));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.minimap_radius", MapConfig.MINIMAP_RADIUS, 24, 110));
        rows.add(new EnumRow<>("teyvatcartographia.config.minimap_corner", MapConfig.MINIMAP_CORNER, MapConfig.Corner.values(), null));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.minimap_offset_x", MapConfig.MINIMAP_OFFSET_X, 0, 300));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.minimap_offset_y", MapConfig.MINIMAP_OFFSET_Y, 0, 300));
        rows.add(SliderRow.ofDouble("teyvatcartographia.config.minimap_opacity", MapConfig.MINIMAP_OPACITY, 0.2, 1.0, false, "%.2f"));
        rows.add(new SubSection("teyvatcartographia.config.subsection.minimap_info"));
        rows.add(new BoolRow("teyvatcartographia.config.minimap_coords", MapConfig.MINIMAP_SHOW_COORDS));
        rows.add(new BoolRow("teyvatcartographia.config.minimap_biome", MapConfig.MINIMAP_SHOW_BIOME));
        rows.add(new BoolRow("teyvatcartographia.config.minimap_time", MapConfig.MINIMAP_SHOW_TIME));
        rows.add(new EnumRow<>("teyvatcartographia.config.minimap_time_source", MapConfig.MINIMAP_TIME_SOURCE, MapConfig.TimeSource.values(), null).under(MapConfig.MINIMAP_SHOW_TIME));
        // Pins: each kind on or off as a whole, with where it is shown; then the path to a tracked pin of each kind
        rows.add(new Section("teyvatcartographia.config.section.pins"));
        rows.add(new SubSection("teyvatcartographia.config.subsection.pins_display"));
        for (MapConfig.PinKind kind : MapConfig.PinKind.values()) {
            MapConfig.PinOptions o = MapConfig.pins(kind);
            rows.add(new BoolRow("teyvatcartographia.config.pins." + kind.id, o.enabled));
            rows.add(new BoolRow("teyvatcartographia.config.pins.map", o.map).under(o.enabled));
            rows.add(new BoolRow("teyvatcartographia.config.pins.minimap", o.minimap).under(o.enabled));
            rows.add(new BoolRow("teyvatcartographia.config.pins.beam", o.beam).under(o.enabled));
        }
        rows.add(new SubSection("teyvatcartographia.config.subsection.pins_path"));
        for (MapConfig.PinKind kind : MapConfig.PinKind.values()) {
            MapConfig.PinOptions o = MapConfig.pins(kind);
            rows.add(new BoolRow("teyvatcartographia.config.path." + kind.id, o.path));
            rows.add(new BoolRow("teyvatcartographia.config.path.map", o.pathMap).under(o.path));
            rows.add(new BoolRow("teyvatcartographia.config.path.minimap", o.pathMinimap).under(o.path));
            rows.add(new BoolRow("teyvatcartographia.config.path.world", o.pathWorld).under(o.path));
        }
        // Cartography: how the map is drawn and how much work it may take
        rows.add(new Section("teyvatcartographia.config.section.cartography"));
        rows.add(new EnumRow<>("teyvatcartographia.config.detail", MapConfig.DETAIL, MapConfig.Detail.values(), () -> MapDataManager.get().remapLoaded()));
        rows.add(new BoolRow("teyvatcartographia.config.caves", MapConfig.CAVES_ENABLED));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.scan_radius", MapConfig.SCAN_RADIUS, 2, 32));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.chunks_per_tick", MapConfig.CHUNKS_PER_TICK, 1, 32));
        rows.add(SliderRow.ofInt("teyvatcartographia.config.max_tiles", MapConfig.MAX_CACHED_TILES, 32, 1024));
        // Rows after a sub-heading (up to the next heading) fold with it.
        SubSection current = null;
        for (Row r : rows) {
            if (r instanceof Section) current = null;
            else if (r instanceof SubSection s) current = s;
            else r.owner = current;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        FogTexture.draw(g, 0, 0, width, height, 0, 0, Theme.Style.fogTile, Theme.WHITE);
    }

    private void layout() {
        panelW = Math.min(360, (width - 2 * 2 * Grid.MARGIN) / Grid.STEP * Grid.STEP);
        panelX = Math.round((width - panelW) / 2f);
        panelY = Grid.MARGIN;
        panelH = height - 2 * Grid.MARGIN;
        listTop = panelY + Grid.HEADER_H + Grid.GAP;
        listBottom = footerY() - Grid.PAD;
    }

    private float contentX() {
        return panelX + 2 * Grid.PAD;
    }

    private float contentW() {
        return panelW - 4 * Grid.PAD;
    }

    private float footerY() {
        return panelY + panelH - Grid.PAD - Grid.ACTION_H;
    }

    private float footerButtonW() {
        return (contentW() - 2 * Grid.GAP) / 3f;
    }

    private float footerButtonX(int i) {
        return contentX() + i * (footerButtonW() + Grid.GAP);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        layout();
        // Blur the fog before the dimming layer: the translucent dim writes alpha < 1 into the frame, which the vanilla
        // blur turns into black. The fog is static, so the copy is reused: refreshed for the first frames, after a
        // resize and after a theme change.
        boolean refresh = blurFrames < 3 || width != blurW || height != blurH || Theme.generation() != blurTheme;
        Backdrop.capture(g, pt, refresh, this);
        blurFrames++;
        blurW = width;
        blurH = height;
        blurTheme = Theme.generation();
        Draw.rect(g, 0, 0, width, height, Theme.SETTINGS_DIM);
        Ui.panel(g, panelX, panelY, panelW, panelH, true, title, Icon.GEAR, mx, my);
        Backdrop.release();
        float total = 0;
        for (Row r : rows) total += r.listHeight();
        scroll = Math.max(0, Math.min(Math.max(0, total - (listBottom - listTop)), scroll));

        g.enableScissor((int) panelX, (int) listTop, (int) (panelX + panelW), (int) listBottom);
        float yy = listTop - scroll;
        for (Row r : rows) {
            r.y = yy;
            if (r.shown() && yy + r.height() >= listTop && yy <= listBottom) r.render(g, contentX(), contentW(), mx, my);
            yy += r.listHeight();
        }
        g.disableScissor();

        float bw = footerButtonW(), by = footerY();
        Ui.actionButton(g, footerButtonX(0), by, bw, Component.translatable("teyvatcartographia.config.keybinds"), Icon.LIST, true, mx, my);
        Ui.actionButton(g, footerButtonX(1), by, bw, Component.translatable("teyvatcartographia.config.reset"), Icon.TRASH, true, mx, my);
        Ui.actionButton(g, footerButtonX(2), by, bw, Component.translatable("gui.done"), Icon.CHECK, true, mx, my);

        // Tooltip for the setting whose label is under the cursor.
        if (my >= listTop && my < listBottom && mx >= contentX() && mx < contentX() + contentW() / 2) {
            for (Row r : rows) {
                if (r.tooltip != null && r.shown() && my >= r.y && my < r.y + r.height()) {
                    Ui.wrappedTooltip(g, r.label, r.tooltip, mx, my, width, height);
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        if (Ui.closeHit(panelX, panelY, panelW, mx, my)) {
            onClose();
            return true;
        }
        float bw = footerButtonW(), by = footerY();
        if (Ui.actionHit(footerButtonX(0), by, bw, mx, my)) {
            Minecraft.getInstance().setScreen(new KeyBindsScreen(this, Minecraft.getInstance().options));
            return true;
        }
        if (Ui.actionHit(footerButtonX(1), by, bw, mx, my)) {
            MapConfig.resetAll();
            ThemeLoader.applyNow();
            MapDataManager.get().remapLoaded();
            return true;
        }
        if (Ui.actionHit(footerButtonX(2), by, bw, mx, my)) {
            onClose();
            return true;
        }
        if (my >= listTop && my < listBottom) {
            for (Row r : rows) {
                if (r.shown() && r.active() && r.click(mx, my, contentX(), contentW())) {
                    if (r instanceof SliderRow) draggingRow = r;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingRow != null) {
            draggingRow.drag(mx, contentX(), contentW());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingRow = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll -= (float) sy * Grid.ROW;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        MapConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
