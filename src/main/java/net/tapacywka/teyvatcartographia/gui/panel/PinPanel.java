package net.tapacywka.teyvatcartographia.gui.panel;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.gui.Ui;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.gui.WorldMapScreen;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.TileStore;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.Icon;
import net.tapacywka.teyvatcartographia.render.MarkerRenderer;
import net.tapacywka.teyvatcartographia.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The pin panel (reference: fullscreen_custom_markers.png). One panel for everything a pin needs:
 * <ul>
 *   <li>new pin: name, coordinates, type grid, total pins, "Confirm";</li>
 *   <li>existing custom pin (opened by clicking it): the same editor plus "Track", delete in the total-pins bar;
 *       "Teleport" for interaction pins (operators only);</li>
 *   <li>read-only view (automatic points, and interaction pins for players without operator rights): coordinates
 *       and "Track" only.</li>
 * </ul>
 * The type grid has two sections: "Custom" for everyone and "System" for operators only.
 */
public final class PinPanel extends SidePanel {
    private static final List<MarkerType.Category> ALL_SECTIONS = List.of(MarkerType.Category.EXPLORATION, MarkerType.Category.INTERACTION);
    private static final List<MarkerType.Category> PLAYER_SECTIONS = List.of(MarkerType.Category.EXPLORATION);
    private static final List<List<MarkerType>> SECTION_TYPES = ALL_SECTIONS.stream()
            .map(c -> Arrays.stream(MarkerType.values()).filter(t -> t.category == c).toList()).toList();
    private static final int CELL = Grid.SLOT + Grid.GAP;
    /** Section header: one text line above the section's tiles. */
    private static final int SECTION_HEAD = DrawText.LINE + Grid.GAP;

    private enum Action { TELEPORT, TRACK, CONFIRM }

    private final Marker editing;
    private final ResourceKey<Level> dimension;
    private final int px, py, pz;
    private MarkerType type;
    private final EditBox name;

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
        this.type = editing != null && editing.type.custom ? editing.type : MarkerType.BOSS; // Genshin pre-selects the first type
        name = new EditBox(Minecraft.getInstance().font, 0, 0, 100, DrawText.LINE, Component.translatable("teyvatcartographia.pin.name"));
        name.setBordered(false);
        name.setTextShadow(false); // dark text on a light chip: the vanilla drop shadow smears it
        name.setMaxLength(48);
        name.setTextColor(Theme.TEXT_DARK);
        name.setHint(Component.translatable("teyvatcartographia.pin.name_hint"));
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
        return Component.translatable("teyvatcartographia.marker.default_name", MarkerStore.count() + 1).getString();
    }

    @Override
    protected boolean dark() {
        return false;
    }

    @Override
    protected Component title() {
        // Like the automatic points: the header shows the pin's (current) name and its selected type icon.
        if (readOnly()) return Component.literal(editing.name);
        String n = name.getValue().trim();
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
        return gridBottom() + Grid.PAD > totalWithThree;
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

    /** Type sections the player may pick from: interaction only for operators. */
    private static List<MarkerType.Category> sections() {
        return MarkerType.isOperator() ? ALL_SECTIONS : PLAYER_SECTIONS;
    }

    private int columns() {
        return Math.max(3, (int) ((contentW() + Grid.GAP) / CELL));
    }

    /** Grid is centred horizontally in the content area. */
    private float gridX() {
        int cols = columns();
        return contentX() + (contentW() - (cols * CELL - Grid.GAP)) / 2f;
    }

    /** Top of section {@code k} (its header line); sections are stacked with PAD between them. */
    private float sectionY(int k) {
        float y = gridY();
        int cols = columns();
        for (int j = 0; j < k; j++) {
            int rows = (SECTION_TYPES.get(j).size() + cols - 1) / cols;
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

    /** Bottom of the type grid (all visible sections). */
    private float gridBottom() {
        int last = sections().size() - 1, cols = columns();
        int rows = (SECTION_TYPES.get(last).size() + cols - 1) / cols;
        return sectionY(last) + SECTION_HEAD + rows * CELL - Grid.GAP;
    }

    @Override
    protected void renderBody(GuiGraphics g, int mx, int my, float bx, float by, float bw) {
        int maxText = (int) (bw - Grid.CHIP_H - Grid.GAP);
        boolean readOnly = readOnly();
        // Operator rights revoked while a new pin had an interaction type selected: back to the first type.
        if (!readOnly && type.operatorOnly() && !MarkerType.isOperator()) type = MarkerType.BOSS;
        if (!readOnly) {
            // Name chip
            float ny = nameY();
            Ui.chip(g, bx, ny, bw, Grid.CHIP_H, Theme.PANEL_LIGHT_CHIP);
            Draw.icon(g, Icon.PENCIL, bx + Grid.CHIP_H / 2f, ny + Grid.CHIP_H / 2f, Grid.ICON, name.isFocused() ? Theme.CHIP_ICON : Theme.CHIP_ICON_SOFT);
            name.setTextColor(Theme.TEXT_DARK); // follows theme changes
            name.setX(Math.round(bx + Grid.CHIP_H));
            name.setY(Math.round(DrawText.textY(ny, Grid.CHIP_H)));
            name.setWidth(maxText);
            if (name.isFocused()) {
                name.render(g, mx, my, 0); // vanilla editor (cursor, selection) only while editing
            } else {
                // Idle: the plain panel text, shortened with "..." to the field width.
                String v = name.getValue();
                boolean hint = v.isEmpty();
                String shown = hint ? Component.translatable("teyvatcartographia.pin.name_hint").getString() : v;
                DrawText.text(g, DrawText.fit(shown, maxText), bx + Grid.CHIP_H, DrawText.textY(ny, Grid.CHIP_H), hint ? Theme.TEXT_DARK_SOFT : Theme.TEXT_DARK, false);
            }
        }

        // Location chip
        float cy = coordsY();
        Ui.chip(g, bx, cy, bw, Grid.CHIP_H, readOnly ? Theme.PANEL_LIGHT_CHIP : Theme.PANEL_LIGHT_CHIP_EDIT);
        Draw.icon(g, Icon.LOCATE, bx + Grid.CHIP_H / 2f, cy + Grid.CHIP_H / 2f, Grid.ICON, Theme.CHIP_ICON);
        String coords = Component.translatable("teyvatcartographia.pin.coords", px, pz, WorldMapScreen.dimensionName(dimension)).getString();
        DrawText.text(g, DrawText.fit(coords, maxText), bx + Grid.CHIP_H, DrawText.textY(cy, Grid.CHIP_H), Theme.TEXT_DARK, false);

        if (!readOnly) {
            // Type grid by section (header line, then tiles): the same tiles as in the pin list.
            List<MarkerType.Category> sections = sections();
            for (int k = 0; k < sections.size(); k++) {
                DrawText.text(g, Component.translatable(sections.get(k).langKey()), gridX(), sectionY(k), Theme.TEXT_DARK_SOFT, false);
                List<MarkerType> types = SECTION_TYPES.get(k);
                for (int i = 0; i < types.size(); i++) {
                    MarkerType t = types.get(i);
                    float sx = slotX(i), sy = slotY(k, i), s = Grid.SLOT;
                    MarkerRenderer.badge(g, t.icon, sx + s / 2f, sy + s / 2f, s, t == type, Draw.inRect(mx, my, sx, sy, s, s), 1f);
                }
            }
            // Total pins bar (delete button when editing)
            float ty = totalY();
            Ui.chip(g, bx, ty, bw, Grid.CHIP_H, Theme.PANEL_LIGHT_FIELD);
            float inset = Theme.Style.square ? Grid.PAD : Grid.CHIP_H / 2f; // text clear of the rounded cap
            DrawText.text(g, Component.translatable("teyvatcartographia.pin.total", MarkerStore.count(), MarkerStore.MAX_MARKERS),
                    bx + inset, DrawText.textY(ty, Grid.CHIP_H), Theme.PANEL_LIGHT_BAR, false);
            if (editing != null) Ui.toolButton(g, trashX(), ty, Icon.TRASH, mx, my);
            if (compactTeleport()) Ui.toolButton(g, teleportToolX(), ty, Icon.TARGET, mx, my);
        }

        // Action buttons, bottom-up
        List<Action> actions = actions();
        for (int i = 0; i < actions.size(); i++) {
            float ay = actionY(i);
            switch (actions.get(i)) {
                case CONFIRM -> Ui.actionButton(g, bx, ay, bw, Component.translatable("teyvatcartographia.pin.confirm"), Icon.CHECK,
                        editing != null || MarkerStore.count() < MarkerStore.MAX_MARKERS, mx, my);
                case TRACK -> Ui.actionButton(g, bx, ay, bw,
                        Component.translatable(Navigation.isTracking(editing) ? "teyvatcartographia.pin.untrack" : "teyvatcartographia.pin.track"), Icon.LOCATE, true, mx, my);
                case TELEPORT -> Ui.actionButton(g, bx, ay, bw, Component.translatable("teyvatcartographia.pin.teleport"), Icon.TARGET, true, mx, my);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closeClicked(mx, my)) return true;
        if (!contains(mx, my)) {
            name.setFocused(false);
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
        name.setFocused(Draw.inRect(mx, my, bx, nameY(), bw, Grid.CHIP_H));
        if (name.isFocused()) {
            name.mouseClicked(mx, my, button);
            return true;
        }
        for (int k = 0; k < sections().size(); k++) {
            List<MarkerType> types = SECTION_TYPES.get(k);
            for (int i = 0; i < types.size(); i++) {
                if (Draw.inRect(mx, my, slotX(i), slotY(k, i), Grid.SLOT, Grid.SLOT)) {
                    type = types.get(i);
                    return true;
                }
            }
        }
        if (editing != null && Draw.inRect(mx, my, trashX(), totalY(), Grid.TOOL, Grid.TOOL)) {
            MarkerStore.remove(editing);
            screen.closePanel();
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
        String n = name.getValue().trim();
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
    public boolean keyPressed(int key, int scan, int mods) {
        if (name.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                name.setFocused(false);
                return true;
            }
            name.keyPressed(key, scan, mods);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return name.isFocused() && name.charTyped(c, mods);
    }

    @Override
    public boolean capturesKeyboard() {
        return name.isFocused();
    }
}
