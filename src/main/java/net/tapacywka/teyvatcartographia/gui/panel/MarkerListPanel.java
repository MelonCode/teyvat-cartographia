package net.tapacywka.teyvatcartographia.gui.panel;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.gui.FileDialogs;
import net.tapacywka.teyvatcartographia.gui.PinTransfer;
import net.tapacywka.teyvatcartographia.gui.Ui;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.gui.WorldMapScreen;
import net.tapacywka.teyvatcartographia.marker.AutoMarkers;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.marker.MarkerVisibility;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.render.DistanceText;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.Icon;
import net.tapacywka.teyvatcartographia.render.MarkerRenderer;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dark list of saved markers in the viewed dimension; click to fly the map to a marker. Shows up to
 * {@link #MAX_ROWS} rows in a block centred vertically in the panel (shorter lists start at the block's top);
 * longer lists scroll by rows with the mouse wheel and show a thin scroll bar, like the pick list on the map.
 * At the bottom: Export and Import ({@link PinTransfer}); Import opens a choice between our file and Xaero's Minimap
 * above the buttons (it stays open after a pick), and the outcome is shown for a while in a popup left of the panel,
 * pointing at the button that was used.
 */
public final class MarkerListPanel extends SidePanel {
    /** Row: 2 text lines (9 + 2 + 9) with 4 units padding top and bottom = 28 = one tile. */
    private static final float ROW_H = Grid.TILE_H;
    private static final float ROW_STEP = ROW_H + Grid.GAP;
    private static final float SYMBOL = 16;
    private static final int MAX_ROWS = 10;
    private static final float BAR_W = 2;
    /** First visible row. */
    private int scroll;
    private List<Marker> items = List.of();

    public MarkerListPanel(WorldMapScreen screen) {
        super(screen);
    }

    @Override
    protected boolean dark() {
        return true;
    }

    @Override
    protected Component title() {
        return Component.translatable("teyvatcartographia.list.title", MarkerStore.localCount(), MarkerStore.MAX_MARKERS);
    }

    @Override
    protected Icon titleIcon() {
        return Icon.LIST;
    }

    // The sorted list is rebuilt when pins change, when another dimension is viewed, and twice a second for the
    // distance order, not every frame.
    private int listMarkers = Integer.MIN_VALUE, listAuto;
    private ResourceKey<Level> listDim;
    private long listNanos;

    /** Returns true if the list was rebuilt. */
    private boolean refresh() {
        long now = System.nanoTime();
        ResourceKey<Level> dim = screen.viewedDimension();
        if (MarkerStore.version() == listMarkers && AutoMarkers.version() == listAuto && dim == listDim
                && now - listNanos < 500_000_000L) return false;
        listMarkers = MarkerStore.version();
        listAuto = AutoMarkers.version();
        listDim = dim;
        listNanos = now;
        LocalPlayer p = Minecraft.getInstance().player;
        List<Marker> list = new ArrayList<>(screen.visibleMarkers(true));
        if (p != null) list.sort(Comparator.comparingDouble(m -> Navigation.distance(p, m.centerX(), m.y, m.centerZ())));
        items = list;
        return true;
    }

    /** Texts of one row, fitted to the row width: built with the list, not every frame. */
    private record RowText(String name, String coords, Component distance) {}

    private RowText[] rowTexts = new RowText[0];
    private float rowTextsWidth = -1;

    private void buildRowTexts(float rw) {
        rowTextsWidth = rw;
        LocalPlayer p = Minecraft.getInstance().player;
        ResourceKey<Level> playerDim = p == null ? null : p.level().dimension();
        rowTexts = new RowText[items.size()];
        for (int i = 0; i < rowTexts.length; i++) {
            Marker m = items.get(i);
            String dist = m.dimension == playerDim
                    ? DistanceText.blocks(Math.round(Navigation.distance(p, m.centerX(), m.y, m.centerZ()))).getString() : null;
            int distW = dist == null ? 0 : DrawText.width(dist) + Grid.GAP;
            int maxW = (int) (rw - ROW_H - Grid.PAD - distW);
            rowTexts[i] = new RowText(DrawText.fit(m.name, maxW), DrawText.fit(m.x + ", " + m.z, maxW), dist == null ? null : Component.literal(dist));
        }
    }


    private float listBottom() {
        return actionsTop() - Grid.PAD;
    }

    // ------------------------------------------------------------------ export and import

    /** True while the import choice (our file or Xaero's Minimap) is open above the button row. */
    private boolean importMenu;

    /** The row of the Export and Import buttons at the bottom. */
    private float transferRowY() {
        return bottomActionY();
    }

    private float halfWidth() {
        return (contentW() - Grid.GAP) / 2f;
    }

    /** Top of the n-th import choice above the button row (0 = the lowest: Xaero's Minimap, 1: our own file). */
    private float choiceY(int n) {
        return transferRowY() - (n + 1) * (Grid.ACTION_H + Grid.GAP);
    }

    private float actionsTop() {
        return importMenu ? choiceY(1) : transferRowY();
    }


    private void renderTransfer(GuiGraphics g, int mx, int my, float bx, float bw) {
        boolean free = !FileDialogs.isOpen();
        float row = transferRowY(), half = halfWidth();
        Ui.actionButton(g, bx, row, half, Component.translatable("teyvatcartographia.transfer.export"), Icon.EXPORT, free, mx, my);
        Ui.actionButton(g, bx + half + Grid.GAP, row, half, Component.translatable("teyvatcartographia.transfer.import"), Icon.IMPORT, free, mx, my);
        if (importMenu) {
            Ui.actionButton(g, bx, choiceY(1), bw, Component.translatable("teyvatcartographia.transfer.import.file"), null, free, mx, my);
            Ui.actionButton(g, bx, choiceY(0), bw, Component.translatable("teyvatcartographia.transfer.import.xaero"), null, free, mx, my);
        }
        // The outcome of the last export or import: a popup over the map left of the panel, pointing at its button
        // (at Import when the choice was closed since).
        PinTransfer.Outcome o = PinTransfer.message();
        if (o != null) Ui.popupLeftOf(g, o.title(), o.lines(), x - 1, sourceY(o.source()) + Grid.ACTION_H / 2f, y + h - Grid.MARGIN);
    }

    private float sourceY(PinTransfer.Source source) {
        return switch (source) {
            case EXPORT -> transferRowY();
            case FILE -> importMenu ? choiceY(1) : transferRowY();
            case XAERO -> importMenu ? choiceY(0) : transferRowY();
        };
    }

    /** Returns true if a transfer button took the click. */
    private boolean transferClicked(double mx, double my) {
        if (FileDialogs.isOpen()) return false;
        float bx = contentX(), bw = contentW(), row = transferRowY(), half = halfWidth();
        if (Ui.actionHit(bx, row, half, mx, my)) {
            importMenu = false;
            PinTransfer.export();
            return true;
        }
        if (Ui.actionHit(bx + half + Grid.GAP, row, half, mx, my)) {
            importMenu = !importMenu;
            return true;
        }
        // The choice stays open after a pick, so the outcome's popup can point at the button that was used.
        if (importMenu && Ui.actionHit(bx, choiceY(1), bw, mx, my)) {
            PinTransfer.importFile();
            return true;
        }
        if (importMenu && Ui.actionHit(bx, choiceY(0), bw, mx, my)) {
            PinTransfer.importXaero();
            return true;
        }
        return false;
    }

    /** Rows that fit at once (at most {@link #MAX_ROWS}). */
    private int capacity() {
        return Math.max(1, Math.min(MAX_ROWS, (int) ((listBottom() - contentY() + Grid.GAP) / ROW_STEP)));
    }

    /** Top of the row block: a block of {@link #capacity()} rows centred in the content area. */
    private float listTop() {
        float block = capacity() * ROW_STEP - Grid.GAP;
        return Draw.snap(contentY() + (listBottom() - contentY() - block) / 2f);
    }

    private int visibleRows() {
        return Math.min(capacity(), items.size());
    }

    private boolean scrollable() {
        return items.size() > capacity();
    }

    /** Row width: narrower by the scroll bar when the list scrolls. */
    private float rowWidth(float bw) {
        return scrollable() ? bw - BAR_W - Grid.GAP : bw;
    }

    @Override
    protected void renderBody(GuiGraphics g, int mx, int my, float bx, float by, float bw) {
        boolean rebuilt = refresh();
        scroll = Math.max(0, Math.min(items.size() - capacity(), scroll));
        renderTransfer(g, mx, my, bx, bw);
        if (items.isEmpty()) {
            DrawText.text(g, Component.translatable("teyvatcartographia.list.empty"), bx, DrawText.textY(by, Grid.ROW), Theme.TEXT_MUTED, false);
            return;
        }
        float top = listTop();
        float rw = rowWidth(bw);
        if (rebuilt || rw != rowTextsWidth || rowTexts.length != items.size()) buildRowTexts(rw);
        for (int k = 0, n = visibleRows(); k < n; k++) {
            float ry = top + k * ROW_STEP;
            Marker m = items.get(scroll + k);
            RowText t = rowTexts[scroll + k];
            boolean hover = Draw.inRect(mx, my, bx, ry, rw, ROW_H);
            Draw.rect(g, bx, ry, bx + rw, ry + ROW_H, hover ? Theme.PANEL_DARK_TILE_HOVER : Theme.PANEL_DARK_TILE);
            if (Navigation.isTracking(m)) Draw.rect(g, bx, ry, bx + 3, ry + ROW_H, Theme.worldMarkerPulse(MarkerVisibility.kind(m)));
            // Symbol centred in the row's leading 28 x 28 square; text starts right after it.
            MarkerRenderer.drawCentered(g, m.type, bx + ROW_H / 2f, ry + ROW_H / 2f, SYMBOL, 1f);
            float tx = bx + ROW_H;
            DrawText.text(g, t.name, tx, ry + 4, Theme.CREAM, false);
            DrawText.text(g, t.coords, tx, ry + 4 + DrawText.LINE + 2, Theme.TEXT_MUTED, false);
            if (t.distance != null) DrawText.textRight(g, t.distance, bx + rw - Grid.PAD, DrawText.textY(ry, ROW_H), Theme.GOLD, false);
        }
        if (scrollable()) {
            // Track over the block of rows, thumb over the visible part (as in the map's pick list).
            float barX = bx + bw - BAR_W, h = capacity() * ROW_STEP - Grid.GAP;
            Draw.rect(g, barX, top, barX + BAR_W, top + h, Theme.SCROLL_TRACK);
            float th = Math.max(8, h * capacity() / items.size());
            float ty = top + (h - th) * scroll / (items.size() - capacity());
            Draw.rect(g, barX, ty, barX + BAR_W, ty + th, Theme.SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closeClicked(mx, my)) return true;
        if (transferClicked(mx, my)) return true;
        float top = listTop();
        if (my >= top) {
            int k = (int) ((my - top) / ROW_STEP);
            if (k < visibleRows() && Draw.inRect(mx, my, contentX(), top + k * ROW_STEP, rowWidth(contentW()), ROW_H)) {
                screen.focusMarker(items.get(scroll + k));
                return true;
            }
        }
        return contains(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        scroll = Math.max(0, Math.min(items.size() - capacity(), scroll - (int) Math.signum(amount)));
        return true;
    }
}
