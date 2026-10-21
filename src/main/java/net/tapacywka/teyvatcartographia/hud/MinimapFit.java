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

    /** Left edge of a pill {@code w} wide: centred on {@code cx}, shifted inward to keep {@code EDGE} from the screen sides. */
    static float pillLeft(float w, float cx, int guiW) {
        return Math.max(EDGE, Math.min(guiW - EDGE - w, cx - w / 2f));
    }
}
