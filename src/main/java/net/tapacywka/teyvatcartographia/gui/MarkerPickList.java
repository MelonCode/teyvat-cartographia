package net.tapacywka.teyvatcartographia.gui;

import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Pick list for overlapping pins (like Genshin Impact): a click on a spot where several pins overlap opens a column
 * of rows (icon + name) at the right edge of the screen; clicking a row selects that pin.
 * <ul>
 *   <li>At most {@link #MAX_ROWS} rows at a time; longer lists scroll (mouse wheel) and show a thin scroll bar.</li>
 *   <li>Rows are dark bands with a rounded (rounded style) or straight left end that fade out to the right.</li>
 *   <li>The list sits in the lower right corner, with the off-screen indicators' gap to the right and bottom edges.</li>
 * </ul>
 */
final class MarkerPickList {
    private static final int MAX_ROWS = 5;
    private static final int ROW_H = Grid.ACTION_H, STEP = ROW_H + Grid.GAP;
    private static final int W = 190;
    private static final int BAR_W = 2;
    /** Pointer of the hovered row: sway distance (GUI units). */
    private static final float POINTER_SWAY = 3f;
    /**
     * One shared cycle (radians per second) for the hovered row: the pointer is farthest out exactly when the
     * outline is white, and back at the row when it is grey.
     */
    private static final float HOVER_SPEED = 3f;

    private final List<Marker> items;
    private final float x, y, w;
    private final int visible;
    private int scroll;

    /**
     * Anchored to the right edge of the screen at distance {@code edge} (the gap between an off-screen indicator
     * and the screen edge), vertically centred on the screen whatever the number of rows.
     */
    MarkerPickList(List<Marker> items, int screenW, int screenH, float edge) {
        this.items = items;
        w = Math.min(W, screenW - 2 * edge);
        visible = Math.max(1, Math.min(MAX_ROWS, Math.min(items.size(), (int) ((screenH - 2 * edge + Grid.GAP) / STEP))));
        x = Draw.snap(screenW - edge - w);
        y = Draw.snap(screenH - edge - height()); // same gap to the bottom edge as to the right one
        int maxText = (int) (w * 0.75f - ROW_H - Grid.GAP); // the right quarter fades out
        names = new String[items.size()];
        for (int i = 0; i < names.length; i++) names[i] = DrawText.fit(items.get(i).name, maxText);
    }

    /** Pin names fitted to the rows once, not every frame. */
    private final String[] names;

    private float height() {
        return visible * STEP - Grid.GAP;
    }

    boolean contains(double mx, double my) {
        return Draw.inRect(mx, my, x, y, w, height());
    }

    /** Pin of the row under the point, or null. */
    Marker rowAt(double mx, double my) {
        if (!contains(mx, my)) return null;
        int i = (int) ((my - y) / STEP);
        if (my - y - i * STEP >= ROW_H) return null; // in the gap between rows
        int k = scroll + i;
        return k >= 0 && k < items.size() ? items.get(k) : null;
    }

    boolean scroll(double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        scroll = Math.max(0, Math.min(items.size() - visible, scroll - (int) Math.signum(amount)));
        return true;
    }

    /** {@code time}: seconds, drives the swaying pointer and the pulsing outline of the hovered row. */
    void render(GuiGraphics g, int mx, int my, float time) {
        Marker hover = rowAt(mx, my);
        if (hover != null) Cursors.want(Cursors.Shape.HAND);
        float phase = 0.5f + 0.5f * (float) Math.sin(time * HOVER_SPEED); // 0 = grey / pointer in, 1 = white / out
        for (int i = 0; i < visible; i++) {
            Marker m = items.get(scroll + i);
            float ry = y + i * STEP;
            boolean hot = m == hover;
            row(g, ry, hot, phase);
            float cap = ROW_H / 2f;
            if (hot) { // white pointer left of the row, slowly swaying left and right, like Genshin
                float cy = ry + cap, px = x - 3 - POINTER_SWAY * phase;
                Draw.triangle(g, px, cy, px - 5, cy - 4, px - 5, cy + 4, Theme.PICK_POINTER);
            }
            Draw.icon(g, m.type.icon, x + cap, ry + cap, Grid.ICON_L, Theme.WHITE);
            DrawText.text(g, names[scroll + i], x + ROW_H + Grid.GAP, DrawText.textY(ry, ROW_H), hot ? Theme.WHITE : Theme.CREAM, false);
        }
        if (items.size() > visible) scrollBar(g);
    }

    /**
     * One row: a dark band fading out to the right, with a thin outline (the rounded or straight left end, then the
     * top and bottom edges fading out with the band). The hovered row's outline pulses from grey to white and back.
     */
    private void row(GuiGraphics g, float ry, boolean hot, float phase) {
        int fill = hot ? Theme.PICK_ROW_HOVER : Theme.PICK_ROW;
        float h = ROW_H, r = h / 2f, x0 = x;
        if (!Theme.Style.squareUi) { // rounded left end
            Draw.sector(g, x + r, ry + r, r, (float) (-Math.PI / 2), (float) (Math.PI / 2), fill, fill);
            x0 = x + r;
        }
        // Solid for the first part, then fading out towards the right end.
        float fadeFrom = x + w * 0.45f;
        Draw.rect(g, x0, ry, fadeFrom, ry + h, fill);
        Draw.gradientRectH(g, fadeFrom, ry, x + w, ry + h, fill, fill & 0x00FFFFFF);

        int line = hot ? Theme.lerp(Theme.PICK_OUTLINE, Theme.PICK_OUTLINE_HOVER, phase) : Theme.PICK_OUTLINE;
        if (Theme.Style.squareUi) Draw.rect(g, x, ry, x + 1, ry + h, line);
        else Draw.arc(g, x + r, ry + r, r - 0.5f, 1f, (float) Math.PI, (float) (2 * Math.PI), line);
        edgeLine(g, x0, ry, line);
        edgeLine(g, x0, ry + h - 1, line);
    }

    /** Top or bottom outline: solid, then fading out with the band. */
    private void edgeLine(GuiGraphics g, float x0, float ly, int line) {
        float fade = x + w * 0.4f;
        Draw.rect(g, x0, ly, fade, ly + 1, line);
        Draw.gradientRectH(g, fade, ly, x + w * 0.95f, ly + 1, line, line & 0x00FFFFFF);
    }

    /** Thin bar at the list's right end: track over all items, thumb over the visible ones. */
    private void scrollBar(GuiGraphics g) {
        float bx = x + w - BAR_W, h = height();
        Draw.rect(g, bx, y, bx + BAR_W, y + h, Theme.SCROLL_TRACK);
        float th = Math.max(8, h * visible / items.size());
        float ty = y + (h - th) * scroll / (items.size() - visible);
        Draw.rect(g, bx, ty, bx + BAR_W, ty + th, Theme.SCROLL_THUMB);
    }
}
