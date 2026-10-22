package net.tapacywka.teyvatcartographia.map.data;

import com.mojang.blaze3d.platform.NativeImage;
import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.render.Theme;

/**
 * The Teyvat Cartographia palette, applied when the map is shown: the regions keep the standard colours (those of the
 * block textures, like Xaero's Minimap), and their textures are drawn through the player's mute and warm haze. So a
 * change of the palette or its sliders repaints the whole explored map at once, the regions on disk stay as they are.
 * Chunks mapped before 0.1.3 (no {@link MapTile#STANDARD} mark) were saved in the colours of that time and are shown
 * as they are until mapped again.
 */
public final class PaletteTuning {
    private PaletteTuning() {}

    /** The warm beige the haze leans to. */
    static final int HAZE_R = 0xB0, HAZE_G = 0xAC, HAZE_B = 0x94;

    private static int mute = -1, warmth = -1;
    private static MapConfig.Palette palette;
    private static boolean active;
    private static int generation;

    /**
     * Reads the settings and, when they changed, starts a new generation: every region texture is drawn again (a few
     * per frame) and the cached map layers redraw. Render thread, before the map is drawn.
     */
    public static void refresh() {
        MapConfig.Palette p = MapConfig.get(MapConfig.PALETTE);
        int m = MapConfig.get(MapConfig.PALETTE_MUTE), w = MapConfig.get(MapConfig.PALETTE_WARMTH);
        if (p == palette && m == mute && w == warmth) return;
        palette = p;
        mute = m;
        warmth = w;
        active = p == MapConfig.Palette.TEYVAT && (m != 0 || w != 0);
        generation++;
        Theme.invalidate();
    }

    /** Whether the regions are drawn through the tuning (the Teyvat Cartographia palette, not both sliders at 0). */
    public static boolean active() {
        return active;
    }

    public static int generation() {
        return generation;
    }

    /**
     * One colour through the tuning (ARGB, alpha kept): the saturation taken away by {@code mute} percent (towards the
     * grey of the same luminance), then {@code warmth} percent of the warm beige mixed in.
     */
    public static int tune(int argb, int mute, int warmth) {
        float keep = 1 - mute / 100f, w = warmth / 100f;
        float r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float lum = 0.3f * r + 0.59f * g + 0.11f * b;
        r = lum + (r - lum) * keep;
        g = lum + (g - lum) * keep;
        b = lum + (b - lum) * keep;
        r += (HAZE_R - r) * w;
        g += (HAZE_G - g) * w;
        b += (HAZE_B - b) * w;
        return (argb & 0xFF000000) | MapPalette.clamp(Math.round(r)) << 16 | MapPalette.clamp(Math.round(g)) << 8 | MapPalette.clamp(Math.round(b));
    }

    /** {@link #tune} with the settings read last by {@link #refresh}. */
    public static int tune(int argb) {
        return active ? tune(argb, mute, warmth) : argb;
    }

    /** {@link #tune} on a NativeImage pixel (ABGR). */
    private static int tuneAbgr(int abgr) {
        int argb = MapPalette.toAbgr(abgr); // the swap is its own inverse
        return MapPalette.toAbgr(tune(argb, mute, warmth));
    }

    /**
     * Copies a region image into {@code dst} through the tuning. {@code chunkState} (one byte per chunk, or null for
     * every pixel) leaves out the chunks without the {@link MapTile#STANDARD} mark (mapped before 0.1.3).
     */
    static void apply(NativeImage src, NativeImage dst, byte[] chunkState) {
        int w = src.getWidth(), h = src.getHeight();
        int lastIn = 0, lastOut = 0; // neighbouring pixels are often the same colour: tuned once
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = src.getPixelRGBA(x, y);
                boolean legacy = chunkState != null && (chunkState[(y >> 4) * MapTile.CHUNKS + (x >> 4)] & MapTile.STANDARD) == 0;
                if ((c >>> 24) != 0 && !legacy) {
                    if (c != lastIn) {
                        lastIn = c;
                        lastOut = tuneAbgr(c);
                    }
                    c = lastOut;
                }
                dst.setPixelRGBA(x, y, c);
            }
        }
    }
}
