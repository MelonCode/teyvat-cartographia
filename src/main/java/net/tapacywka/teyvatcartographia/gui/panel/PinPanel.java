package net.tapacywka.teyvatcartographia.gui.panel;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.gui.ChipField;
import net.tapacywka.teyvatcartographia.gui.Cursors;
import net.tapacywka.teyvatcartographia.gui.Ui;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.gui.WorldMapScreen;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.TileStore;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import net.tapacywka.teyvatcartographia.marker.SharedPins;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.Icon;
import net.tapacywka.teyvatcartographia.render.MarkerRenderer;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.tapacywka.teyvatcartographia.shared.SharedPinsServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The pin panel (reference: fullscreen_custom_markers.png). One panel for everything a pin needs:
 * <ul>
 *   <li>new pin: name, coordinates, type grid, total pins, "Confirm";</li>
 *   <li>existing custom pin (opened by clicking it): the same editor plus "Track", delete in the total-pins bar (an interactive
 *       pin asks first, in a popup left of the panel);
 *       "Teleport" for interaction pins (operators only);</li>
 *   <li>read-only view (automatic points, and interaction pins for players without operator rights): coordinates
 *       and "Track" only.</li>
 * </ul>
 * The type grid has two sections: "Custom" for everyone and "System" for operators only. On a server with the mod,
 * interactive pins are shared with every player ({@code SharedPins}): a shared pin keeps an interactive type, and the
 * total bar counts the shared pins against the server's limit.
 */
public final class PinPanel extends SidePanel {
    private static final List<MarkerType.Category> ALL_SECTIONS = List.of(MarkerType.Category.EXPLORATION, MarkerType.Category.INTERACTION);
    private static final List<MarkerType.Category> PLAYER_SECTIONS = List.of(MarkerType.Category.EXPLORATION);
    private static final List<MarkerType.Category> SHARED_SECTIONS = List.of(MarkerType.Category.INTERACTION);
    private static final java.util.Map<MarkerType.Category, List<MarkerType>> SECTION_TYPES = new java.util.EnumMap<>(MarkerType.Category.class);
    static {
        for (MarkerType.Category c : ALL_SECTIONS) SECTION_TYPES.put(c, Arrays.stream(MarkerType.values()).filter(t -> t.category == c).toList());
    }
    private static final int CELL = Grid.SLOT + Grid.GAP;
    /** Section header: one text line above the section's tiles. */
    private static final int SECTION_HEAD = DrawText.LINE + Grid.GAP;

    private enum Action { TELEPORT, TRACK, CONFIRM }

    private final Marker editing;
    private final ResourceKey<Level> dimension;
    private final int px, py, pz;
    private MarkerType type;
    private final ChipField name;

    /** Opens an existing marker (custom pins are editable, automatic points are read-only). */
    public PinPanel(WorldMapScreen screen, Marker marker) {
        this(screen, marker, marker.dimension, marker.x, marker.y, marker.z);
    }

    /** Creates a new pin at the given block. */
    public PinPanel(WorldMapScreen screen, ResourceKey<Level> dimension, int x, int y, int z) {
        this(screen, null, dimension, x, y, z);
    }

    private PinPanel(WorldMapScreen screen, Marker editing, ResourceKey<Level> dimension, int x, int y, int z) {
        super(screen);
        this.editing = editing;
        this.dimension = dimension;
        this.px = x;
        this.py = y;
        this.pz = z;
        this.type = editing != null && editing.type.custom ? editing.type : MarkerType.DEMON; // Genshin pre-selects the first type
        name = new ChipField(ChipField.Style.LIGHT, Icon.PENCIL, Component.translatable("teyvatcartographia.pin.name"),
                Component.translatable("teyvatcartographia.pin.name_hint"), 48);
        name.setValue(editing != null ? editing.name : defaultName());
    }

    /**
     * Read-only view: automatic points, and interaction pins for a player without operator rights. Checked every
     * frame, since /op and /deop can change it while the panel is open.
     */
    private boolean readOnly() {
        return editing != null && (editing.automatic || editing.type.operatorOnly() && !MarkerType.isOperator());
    }

    private static String defaultName() {
        return Component.translatable("teyvatcartographia.marker.default_name", MarkerStore.localCount() + 1).getString();
    }

    @Override
    protected boolean dark() {
        return false;
    }

    @Override
    protected Component title() {
        // Like the automatic points: the header shows the pin's (current) name and its selected type icon.
        if (readOnly()) return Component.literal(editing.name);
        String n = name.value().trim();
        return Component.literal(n.isEmpty() ? (editing != null ? editing.name : defaultName()) : n);
    }

    @Override
    protected Icon titleIcon() {
        return readOnly() ? editing.type.icon : type.icon;
    }

    // ------------------------------------------------------------------ actions available

    private int teleportHeight() {
        if (editing != null && editing.hasY()) return editing.y;
        if (py != Integer.MIN_VALUE) return py;
        TileStore s = MapDataManager.get().viewStore(dimension);
        int h = s == null ? Integer.MIN_VALUE : s.heightAt(px, pz);
        return h == Integer.MIN_VALUE ? Integer.MIN_VALUE : h + 1;
    }

    /** Teleport: to saved interaction pins only, and for operators only. */
    private boolean canTeleport() {
        return editing != null && editing.type.operatorOnly() && MarkerType.isOperator() && teleportHeight() != Integer.MIN_VALUE;
    }

    /** Bottom-up stack of action buttons. */
    private List<Action> actions() {
        List<Action> a = new ArrayList<>(3);
        if (!readOnly()) a.add(Action.CONFIRM);
        if (editing != null) { // a new pin can only be tracked once it exists
            a.add(Action.TRACK);
            if (canTeleport() && !compactTeleport()) a.add(Action.TELEPORT);
        }
        return a;
    }

    /**
     * On short screens three full-width buttons would push the "total pins" bar into the type grid: then Teleport
     * becomes a round tool button in that bar, next to the delete button.
     */
    private boolean compactTeleport() {
        if (!canTeleport() || readOnly()) return false;
        float totalWithThree = actionY(2) - Grid.GAP - Grid.CHIP_H;
        return gridY() + gridHeight() + Grid.PAD > totalWithThree;
    }

    private float teleportToolX() {
        return trashX() - Grid.TOOL - Grid.GAP;
    }

    private float actionY(int indexFromBottom) {
        return bottomActionY() - indexFromBottom * (Grid.ACTION_H + Grid.GAP);
    }

    // ------------------------------------------------------------------ layout (all on the grid)

    private float nameY() {
        return contentY();
    }

    /** The name chip is not shown for automatic points (their name is the title). */
    private float coordsY() {
        return readOnly() ? contentY() : nameY() + Grid.CHIP_H + Grid.GAP;
    }

    private float gridY() {
        return coordsY() + Grid.CHIP_H + Grid.PAD;
    }

    /** Type sections the player may pick from: interaction only for operators; a shared pin keeps an interactive type. */
    private List<MarkerType.Category> sections() {
        if (editing != null && editing.shared) return SHARED_SECTIONS;
        return MarkerType.isOperator() ? ALL_SECTIONS : PLAYER_SECTIONS;
    }

    private List<MarkerType> types(int section) {
        return SECTION_TYPES.get(sections().get(section));
    }

    /** True if the pin is (or, when confirmed, becomes) one of the server's shared pins. */
    private boolean shared() {
        return editing != null ? editing.shared || MarkerStore.sharedType(type) : MarkerStore.sharedType(type);
    }

    /** An interactive pin in multiplayer on a server without the mod: only this player sees it (the bar says so). */
    private boolean localOnly() {
        return MarkerStore.localOnly(type, shared());
    }

    /** The total bar: shared pins against the server's limit, or the player's own against theirs. */
    private Component totalText() {
        if (shared()) return Component.translatable("teyvatcartographia.pin.total_shared", SharedPins.count(), SharedPinsServer.MAX_SHARED);
        if (localOnly()) return Component.translatable("teyvatcartographia.pin.local_only");
        return Component.translatable("teyvatcartographia.pin.total", MarkerStore.localCount(), MarkerStore.MAX_MARKERS);
    }

    /** Room for a new pin of the selected type. */
    private boolean room() {
        return shared() ? SharedPins.count() < SharedPinsServer.MAX_SHARED : MarkerStore.localCount() < MarkerStore.MAX_MARKERS;
    }

    private int columns() {
        return Math.max(3, (int) ((contentW() + Grid.GAP) / CELL));
    }

    /** Grid is centred horizontally in the content area. */
    private float gridX() {
        int cols = columns();
        return contentX() + (contentW() - (cols * CELL - Grid.GAP)) / 2f;
    }

    /** Top of section {@code k} (its header line), scrolled; sections are stacked with PAD between them. */
    private float sectionY(int k) {
        float y = gridY() - gridScroll;
        int cols = columns();
        for (int j = 0; j < k; j++) {
            int rows = (types(j).size() + cols - 1) / cols;
            y += SECTION_HEAD + rows * CELL - Grid.GAP + Grid.PAD;
        }
        return y;
    }

    private float slotX(int i) {
        return gridX() + (i % columns()) * CELL;
    }

    private float slotY(int k, int i) {
        return sectionY(k) + SECTION_HEAD + (i / columns()) * CELL;
    }

    private float totalY() {
        return actionY(actions().size() - 1) - Grid.GAP - Grid.CHIP_H; // same gap as between the action buttons
    }

    private float trashX() {
        return contentX() + contentW() - Grid.TOOL;
    }

    /** Height of the type grid (all visible sections, headers included). */
    private float gridHeight() {
        int last = sections().size() - 1, cols = columns();
        int rows = (types(last).size() + cols - 1) / cols;
        return sectionY(last) + gridScroll - gridY() + SECTION_HEAD + rows * CELL - Grid.GAP;
    }

    // ------------------------------------------------------------------ grid scrolling (short screens, B20)

    /** How far the type grid is scrolled down; whether it was drawn yet (its first scroll shows the selected type). */
    private float gridScroll;
    private boolean gridShown;

    /** Bottom of the room for the type grid: above the total bar. */
    private float gridViewBottom() {
        return totalY() - Grid.PAD;
    }

    /** On short screens (large GUI scale, small window) the grid does not fit above the total bar and scrolls. */
    private float maxGridScroll() {
        return Math.max(0, gridHeight() - (gridViewBottom() - gridY()));
    }

    private boolean inGridView(double my) {
        return my >= gridY() && my < gridViewBottom();
    }

    private static final float BAR_W = 3;

    @Override
    protected void renderBody(GuiGraphics g, int mx, int my, float bx, float by, float bw) {
        localOnlyHint = false;
        hoveredType = null;
        int maxText = (int) (bw - Grid.CHIP_H - Grid.GAP);
        boolean readOnly = readOnly();
        // Operator rights revoked while a new pin had an interaction type selected: back to the first type.
        if (!readOnly && type.operatorOnly() && !MarkerType.isOperator()) type = MarkerType.DEMON;
        if (!readOnly) {
            name.render(g, bx, nameY(), bw, mx, my);
        }

        // Location chip
        float cy = coordsY();
        Ui.chip(g, bx, cy, bw, Grid.CHIP_H, readOnly ? Theme.PANEL_LIGHT_CHIP : Theme.PANEL_LIGHT_CHIP_EDIT);
        Draw.icon(g, Icon.LOCATE, bx + Grid.CHIP_H / 2f, cy + Grid.CHIP_H / 2f, Grid.ICON, Theme.CHIP_ICON);
        // Too long with the dimension (always the one on the map): the coordinates alone.
        String coords = DrawText.fitFirst(maxText,
                Component.translatable("teyvatcartographia.pin.coords", px, pz, WorldMapScreen.dimensionName(dimension)).getString(),
                Component.translatable("teyvatcartographia.pin.coords_short", px, pz).getString());
        DrawText.text(g, coords,bx + Grid.CHIP_H, DrawText.textY(cy, Grid.CHIP_H), Theme.TEXT_DARK, false);

        if (!readOnly) {
            // Type grid by section (header line, then tiles): the same tiles as in the pin list. Where it does not fit
            // above the total bar it scrolls, clipped to its room, with a thin bar in the panel's right padding.
            float max = maxGridScroll();
            if (!gridShown) {
                // Opened scrolled just enough to show the selected type (an interactive one is in the last section).
                gridShown = true;
                for (int k = 0; k < sections().size(); k++) {
                    int i = types(k).indexOf(type);
                    if (i >= 0) gridScroll = slotY(k, i) + Grid.SLOT - gridViewBottom();
                }
            }
            gridScroll = Math.max(0, Math.min(max, gridScroll));
            float viewTop = gridY(), viewBottom = gridViewBottom();
            if (max > 0) g.enableScissor((int) Math.floor(x), (int) viewTop, (int) Math.ceil(x + w), (int) viewBottom);
            boolean hoverGrid = max == 0 || inGridView(my);
            List<MarkerType.Category> sections = sections();
            for (int k = 0; k < sections.size(); k++) {
                Component head = Component.translatable(sections.get(k).langKey());
                DrawText.room(head, bx + bw - gridX());
                DrawText.text(g, head, gridX(), sectionY(k), Theme.TEXT_DARK_SOFT, false);
                List<MarkerType> types = types(k);
                for (int i = 0; i < types.size(); i++) {
                    MarkerType t = types.get(i);
                    float sx = slotX(i), sy = slotY(k, i), s = Grid.SLOT;
                    boolean hover = hoverGrid && Draw.inRect(mx, my, sx, sy, s, s);
                    if (hover) {
                        hoveredType = t;
                        Cursors.want(Cursors.Shape.HAND);
                    }
                    MarkerRenderer.badge(g, t.icon, sx + s / 2f, sy + s / 2f, s, t == type, hover, 1f);
                }
            }
            if (max > 0) {
                g.disableScissor();
                float barX = x + w - Grid.PAD + 2, h = viewBottom - viewTop; // 2 units clear of the tiles
                float th = Math.max(8, h * h / gridHeight());
                Draw.rect(g, barX, viewTop, barX + BAR_W, viewBottom, Theme.PANEL_LIGHT_FIELD);
                float thumbY = viewTop + (h - th) * gridScroll / max;
                Draw.rect(g, barX, thumbY, barX + BAR_W, thumbY + th, Theme.TEXT_DARK);
            }
            // Total pins bar (delete button when editing)
            float ty = totalY();
            Ui.chip(g, bx, ty, bw, Grid.CHIP_H, Theme.PANEL_LIGHT_FIELD);
            float inset = Theme.Style.squareUi ? Grid.PAD : Grid.CHIP_H / 2f; // text clear of the rounded cap
            int textW = (int) (bw - inset - (editing != null ? Grid.TOOL + Grid.GAP : 0) - (compactTeleport() ? Grid.TOOL + Grid.GAP : 0));
            DrawText.text(g, DrawText.fit(totalText().getString(), textW), bx + inset, DrawText.textY(ty, Grid.CHIP_H), Theme.PANEL_LIGHT_BAR, false);
            if (localOnly() && Draw.inRect(mx, my, bx, ty, bw, Grid.CHIP_H)) localOnlyHint = true;
            if (editing != null) Ui.toolButton(g, trashX(), ty, Icon.TRASH, mx, my);
            if (compactTeleport()) Ui.toolButton(g, teleportToolX(), ty, Icon.TARGET, mx, my);
        }

        // Action buttons, bottom-up
        List<Action> actions = actions();
        for (int i = 0; i < actions.size(); i++) {
            float ay = actionY(i);
            switch (actions.get(i)) {
                case CONFIRM -> Ui.actionButton(g, bx, ay, bw, Component.translatable("teyvatcartographia.pin.confirm"), Icon.CHECK,
                        editing != null || room(), mx, my);
                case TRACK -> Ui.actionButton(g, bx, ay, bw,
                        Component.translatable(Navigation.isTracking(editing) ? "teyvatcartographia.pin.untrack" : "teyvatcartographia.pin.track"), Icon.LOCATE, true, mx, my);
                case TELEPORT -> Ui.actionButton(g, bx, ay, bw, Component.translatable("teyvatcartographia.pin.teleport"), Icon.TARGET, true, mx, my);
            }
        }
        if (localOnlyHint) Ui.wrappedTooltip(g, Component.translatable("teyvatcartographia.pin.local_only"),
                Component.translatable("teyvatcartographia.pin.local_only.hint"), mx, my, screen.width, screen.height);
        // The name of the type under the mouse in the grid.
        if (hoveredType != null && deleteLines == null)
            Ui.tooltip(g, Component.translatable(hoveredType.langKey()).getString(), null, mx, my, screen.width, screen.height);
        if (deleteLines != null) renderDeleteConfirm(g, mx, my);
    }

    // ------------------------------------------------------------------ delete confirmation (interactive pins)

    private static final Component DELETE_TITLE = Component.translatable("teyvatcartographia.pin.delete.title");
    private static final Component DELETE_YES = Component.translatable("teyvatcartographia.pin.delete.yes");
    private static final Component DELETE_NO = Component.translatable("teyvatcartographia.pin.delete.no");

    /** The lines of the open delete confirmation, or null while it is closed. */
    private List<Component> deleteLines;
    /** Where the confirmation and its buttons were drawn last (for the clicks). */
    private Ui.Box deleteBox;
    private float deleteButtonW;

    /** Deleting an interactive pin asks first: a shared one disappears for every player. */
    private void askDelete() {
        deleteLines = List.of(Component.translatable(editing.shared ? "teyvatcartographia.pin.delete.shared" : "teyvatcartographia.pin.delete.local"));
    }

    /** A popup left of the panel pointing at the delete button, with "Delete" and "Cancel" under its text. */
    private void renderDeleteConfirm(GuiGraphics g, int mx, int my) {
        int bw = Math.max(Ui.actionButtonWidth(DELETE_YES), Ui.actionButtonWidth(DELETE_NO));
        deleteBox = Ui.popupLeftOf(g, DELETE_TITLE, deleteLines, 2 * bw + Grid.GAP, Grid.PAD + Grid.ACTION_H,
                x - 1, totalY() + Grid.CHIP_H / 2f, y + h - Grid.MARGIN);
        // The buttons share the width inside the padding, which is wider than they need when the text is long.
        deleteButtonW = (deleteBox.w() - 2 * Grid.PAD - Grid.GAP) / 2f;
        float by = deleteButtonY();
        Ui.actionButton(g, deleteBox.x() + Grid.PAD, by, deleteButtonW, DELETE_YES, Icon.TRASH, true, mx, my);
        Ui.actionButton(g, deleteNoX(), by, deleteButtonW, DELETE_NO, Icon.CLOSE, true, mx, my);
    }

    private float deleteButtonY() {
        return deleteBox.y() + deleteBox.h() - Grid.PAD - Grid.ACTION_H;
    }

    private float deleteNoX() {
        return deleteBox.x() + Grid.PAD + deleteButtonW + Grid.GAP;
    }

    /** A click while the confirmation is open: its buttons act, any other click only closes it. */
    private void deleteConfirmClicked(double mx, double my) {
        boolean yes = deleteBox != null && Ui.actionHit(deleteBox.x() + Grid.PAD, deleteButtonY(), deleteButtonW, mx, my);
        deleteLines = null;
        deleteBox = null;
        if (yes) delete();
    }

    private void delete() {
        MarkerStore.remove(editing);
        screen.closePanel();
    }

    /** The mouse is over the total bar while it says the pin is the player's own only (its tooltip explains why). */
    private boolean localOnlyHint;
    /** The type tile under the mouse on this frame, for its name's tooltip; null for none. */
    private MarkerType hoveredType;

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (deleteLines != null) {
            // Like the map's pick list: any click closes the confirmation, only "Delete" deletes.
            deleteConfirmClicked(mx, my);
            return true;
        }
        if (closeClicked(mx, my)) return true;
        if (!contains(mx, my)) {
            name.blur();
            return false;
        }
        float bx = contentX(), bw = contentW();
        List<Action> actions = actions();
        for (int i = 0; i < actions.size(); i++) {
            if (Ui.actionHit(bx, actionY(i), bw, mx, my)) {
                run(actions.get(i));
                return true;
            }
        }
        if (readOnly()) return true;
        if (compactTeleport() && Draw.inRect(mx, my, teleportToolX(), totalY(), Grid.TOOL, Grid.TOOL)) {
            teleport();
            return true;
        }
        if (name.click(mx, my, button)) return true;
        for (int k = 0; k < sections().size() && inGridView(my); k++) {
            List<MarkerType> types = types(k);
            for (int i = 0; i < types.size(); i++) {
                if (Draw.inRect(mx, my, slotX(i), slotY(k, i), Grid.SLOT, Grid.SLOT)) {
                    type = types.get(i);
                    return true;
                }
            }
        }
        if (editing != null && Draw.inRect(mx, my, trashX(), totalY(), Grid.TOOL, Grid.TOOL)) {
            if (editing.type.operatorOnly()) askDelete();
            else delete();
            return true;
        }
        return true;
    }

    private void run(Action a) {
        switch (a) {
            case CONFIRM -> confirm();
            case TRACK -> {
                if (Navigation.isTracking(editing)) Navigation.clear();
                else Navigation.track(editing);
            }
            case TELEPORT -> teleport();
        }
    }

    private void confirm() {
        if (type.operatorOnly() && !MarkerType.isOperator()) return; // re-checked: op status may have been revoked meanwhile
        String n = name.value().trim();
        if (n.isEmpty()) n = editing != null ? editing.name : defaultName();
        if (editing != null) {
            MarkerStore.update(editing, n, type);
            screen.closePanel();
        } else {
            Marker m = Marker.custom(n, type, dimension, px, py, pz);
            m.band = screen.viewedBand(); // a pin made on a cave layer belongs to it
            if (MarkerStore.add(m)) screen.closePanel();
        }
    }

    /** Uses the vanilla /tp command; only offered for interaction pins to operators (see {@link #canTeleport}). */
    private void teleport() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (!canTeleport()) return; // re-checked: op status may have been revoked meanwhile
        int ty = teleportHeight();
        p.connection.sendCommand("execute in " + dimension.location() + " run tp @s " + px + ".5 " + ty + " " + pz + ".5");
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        // Half a tile per wheel step; only the type grid scrolls, and only where it does not fit.
        if (!readOnly() && deleteLines == null) gridScroll = Math.max(0, Math.min(maxGridScroll(), gridScroll - (float) Math.signum(amount) * CELL / 2f));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (deleteLines != null && key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            deleteLines = null; // Esc closes the confirmation first, then the panel
            deleteBox = null;
            return true;
        }
        return name.keyPressed(key, scan, mods, null);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return name.charTyped(c, mods);
    }

    @Override
    public boolean capturesKeyboard() {
        return name.focused();
    }
}
