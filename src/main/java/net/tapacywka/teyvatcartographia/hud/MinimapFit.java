package net.tapacywka.teyvatcartographia.hud;

/**
 * Keeping the minimap and its pills on screen ({@link MinimapLayer}): a minimap too large for the screen is drawn
 * smaller (B23), and a pill wider than the map near the screen edge is shifted inward (B22).
 */
final class MinimapFit {
    private MinimapFit() {}

    /** Space kept between the minimap (or a pill) and the screen edge. */
    static final int EDGE = 3;

    /**
     * The radius to draw a minimap of radius {@code r} with: no larger than a {@code guiW} x {@code guiH} screen allows
     * with {@code textBlock} units of pills over or under it, and never below {@code min}.
     */
    static int radius(int r, int guiW, int guiH, int textBlock, int min) {
        int fit = Math.min((guiW - 2 * EDGE) / 2, (guiH - 2 * EDGE - textBlock) / 2);
        return Math.max(min, Math.min(r, fit));
    }

    /**
     * Scale of the pill text in GUI units: whole screen pixels per font pixel, so the pixel font stays sharp.
     * {@code size} 0 is "Auto": a step below the GUI scale (GUI 3 gives 2, GUI 4 gives 3), but never below 2 unless
     * the GUI scale is 1; any other size is that many pixels, at most the GUI scale (the menus' own size).
     */
    static float textScale(int size, double guiScale) {
        int gui = Math.max(1, (int) Math.round(guiScale));
        int px = size <= 0 ? (gui <= 2 ? gui : gui - 1) : Math.min(size, gui);
        return (float) (px / guiScale);
    }

    /** Left edge of a pill {@code w} wide: centred on {@code cx}, shifted inward to keep {@code EDGE} from the screen sides. */
    static float pillLeft(float w, float cx, int guiW) {
        return Math.max(EDGE, Math.min(guiW - EDGE - w, cx - w / 2f));
    }
}
