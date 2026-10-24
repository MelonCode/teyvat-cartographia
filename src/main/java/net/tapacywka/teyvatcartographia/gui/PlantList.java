package net.tapacywka.teyvatcartographia.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.tapacywka.teyvatcartographia.gui.Ui.Grid;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.Plants;
import net.tapacywka.teyvatcartographia.map.data.TextureColors;
import net.tapacywka.teyvatcartographia.render.Draw;
import net.tapacywka.teyvatcartographia.render.DrawText;
import net.tapacywka.teyvatcartographia.render.Icon;
import net.tapacywka.teyvatcartographia.render.Theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The list of plants under the Plants row of the settings (opened by its arrow): a field at the top that filters it by
 * name as you type, with how many are switched on, then every plant of the game and its mods with its item, its name and
 * a switch. Drawn over the rows under the Plants row, inside the settings list; scrolled by the wheel.
 */
final class PlantList {
    private static final int ITEM_H = 20, MAX_ROWS = 9;

    private record Entry(Block block, ItemStack item, Component name, String lower) {}

    private final ChipField field;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private String filter = "";
    private int scroll;
    /** Whether a switch changed since opening: the chunks around are mapped again once the list closes. */
    private boolean changed;
    // Where the list was drawn last (for the mouse).
    private float x, y, w, h;

    PlantList() {
        Component hint = Component.translatable("teyvatcartographia.config.plants.find");
        field = new ChipField(ChipField.Style.DARK, Icon.SEARCH, hint, hint, 32);
        field.setResponder(s -> {
            filter = s.toLowerCase(Locale.ROOT).trim();
            scroll = 0;
            refilter();
        });
        for (Block b : Plants.all()) {
            ItemStack item = new ItemStack(b.asItem());
            Component name = b.getName();
            all.add(new Entry(b, item, name, name.getString().toLowerCase(Locale.ROOT)));
        }
        all.sort(Comparator.comparing(Entry::lower));
        refilter();
    }

    private void refilter() {
        shown.clear();
        for (Entry e : all) if (filter.isEmpty() || e.lower.contains(filter)) shown.add(e);
    }

    /** Height the list takes with its rows (at most {@code room}). */
    float height(float room) {
        return Math.min(room, 2 * Grid.GAP + Grid.CHIP_H + Grid.GAP + Math.min(MAX_ROWS, Math.max(1, shown.size())) * ITEM_H + Grid.GAP);
    }

    private int rowsFit() {
        return Math.max(1, (int) ((h - 3 * Grid.GAP - Grid.CHIP_H - Grid.GAP) / ITEM_H));
    }

    void render(GuiGraphics g, float x, float y, float w, float h, int mx, int my) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        // Opaque: the rows under it must not show through.
        Draw.rect(g, x, y, x + w, y + h, Theme.PANEL_DARK_HEADER | 0xFF000000);
        Draw.frame(g, x, y, w, h, 1, Theme.PANEL_LINE);
        float fy = y + Grid.GAP;
        field.render(g, x + Grid.GAP, fy, w - 2 * Grid.GAP, mx, my);
        int on = 0;
        for (Entry e : all) if (Plants.listed(e.block)) on++;
        Component count = Component.translatable("teyvatcartographia.config.plants.count", on, all.size());
        if (!field.focused()) {
            DrawText.textRight(g, count, x + w - 2 * Grid.GAP, DrawText.textY(fy, Grid.CHIP_H), Theme.TEXT_MUTED, false);
        }
        int rows = rowsFit();
        scroll = Math.max(0, Math.min(Math.max(0, shown.size() - rows), scroll));
        float ry = fy + Grid.CHIP_H + Grid.GAP;
        if (shown.isEmpty()) {
            DrawText.text(g, Component.translatable("teyvatcartographia.config.plants.none"), x + 2 * Grid.GAP, DrawText.textY(ry, ITEM_H), Theme.TEXT_MUTED, false);
        }
        for (int i = scroll; i < shown.size() && i < scroll + rows; i++, ry += ITEM_H) {
            Entry e = shown.get(i);
            boolean listed = Plants.listed(e.block);
            float ix = x + 2 * Grid.GAP;
            if (!e.item.isEmpty()) {
                g.renderItem(e.item, Math.round(ix), Math.round(ry + (ITEM_H - 16) / 2f));
            } else {
                // A crop without an item of its own: its colour on the map.
                TextureColors.Entry c = TextureColors.get(e.block);
                Draw.rect(g, ix + 2, ry + 4, ix + 14, ry + 16, c != null ? c.argb() : Theme.PANEL_LINE);
            }
            float nameW = w - 2 * Grid.GAP - 16 - Grid.GAP - Grid.TOGGLE_W - 3 * Grid.GAP;
            DrawText.text(g, DrawText.fit(e.name.getString(), (int) nameW), ix + 16 + Grid.GAP, DrawText.textY(ry, ITEM_H), listed ? Theme.CREAM : Theme.TEXT_MUTED, false);
            Ui.toggle(g, toggleX(), ry + (ITEM_H - Grid.TOGGLE_H) / 2f, listed, mx, my);
        }
        // A thin bar where the list goes on.
        if (shown.size() > rows) {
            float top = fy + Grid.CHIP_H + Grid.GAP, track = rows * ITEM_H, th = Math.max(8, track * rows / shown.size());
            float ty = top + (track - th) * scroll / (shown.size() - rows);
            Draw.rect(g, x + w - 3, top, x + w - 1, top + track, Theme.SCROLL_TRACK);
            Draw.rect(g, x + w - 3, ty, x + w - 1, ty + th, Theme.GOLD);
        }
    }

    private float toggleX() {
        return x + w - 2 * Grid.GAP - Grid.TOGGLE_W - 3;
    }

    boolean over(double mx, double my) {
        return Draw.inRect(mx, my, x, y, w, h);
    }

    /** A click on the list: the field, or a plant's switch. True when it was over the list. */
    boolean click(double mx, double my, int button) {
        if (!over(mx, my)) {
            field.blur();
            return false;
        }
        if (field.click(mx, my, button)) return true;
        float ry = y + Grid.GAP + Grid.CHIP_H + Grid.GAP;
        int rows = rowsFit();
        for (int i = scroll; i < shown.size() && i < scroll + rows; i++, ry += ITEM_H) {
            if (Ui.toggleHit(toggleX(), ry + (ITEM_H - Grid.TOGGLE_H) / 2f, mx, my)) {
                Block b = shown.get(i).block;
                Plants.set(b, !Plants.listed(b));
                changed = true;
                return true;
            }
        }
        return true;
    }

    void scrollBy(double amount) {
        scroll -= (int) Math.signum(amount) * Math.max(1, (int) Math.abs(amount));
    }

    boolean keyPressed(int key, int scan, int mods) {
        return field.keyPressed(key, scan, mods, null);
    }

    boolean charTyped(char c, int mods) {
        return field.charTyped(c, mods);
    }

    /** The list is closed: the chunks around are mapped again if a plant was switched. */
    void closed() {
        field.blur();
        if (changed) MapDataManager.get().remapLoaded();
        changed = false;
    }
}
