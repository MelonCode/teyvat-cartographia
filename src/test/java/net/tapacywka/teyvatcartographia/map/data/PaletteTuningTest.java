package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mute and warmth of the mod's palette: the defaults change nothing, the ends of the sliders do what they say. */
class PaletteTuningTest {
    private static int r(int c) {
        return (c >> 16) & 0xFF;
    }

    private static int g(int c) {
        return (c >> 8) & 0xFF;
    }

    private static int b(int c) {
        return c & 0xFF;
    }

    private static void near(int expected, int actual) {
        assertTrue(Math.abs(r(expected) - r(actual)) <= 1 && Math.abs(g(expected) - g(actual)) <= 1 && Math.abs(b(expected) - b(actual)) <= 1,
                String.format("#%06X near #%06X", actual & 0xFFFFFF, expected & 0xFFFFFF));
    }

    @Test
    void theDefaultsGiveThePaletteBack() {
        for (int c : new int[] {MapPalette.GRASS, MapPalette.FOLIAGE, 0xFFDCCB98, MapPalette.WATER_SHALLOW, MapPalette.LAVA}) {
            near(c, MapPalette.tune(c, MapPalette.BASE_MUTE, MapPalette.BASE_WARMTH));
        }
    }

    @Test
    void fullMuteWithoutWarmthIsGrey() {
        int c = MapPalette.tune(MapPalette.GRASS, 100, 0);
        assertEquals(r(c), g(c), 1);
        assertEquals(g(c), b(c), 1);
    }

    @Test
    void lessMuteMeansStrongerColours() {
        int base = MapPalette.GRASS, vivid = MapPalette.tune(base, 0, MapPalette.BASE_WARMTH);
        assertTrue(g(vivid) - b(vivid) > g(base) - b(base), "green stands further from blue");
    }

    @Test
    void moreWarmthPullsTowardsTheBeige() {
        int c = MapPalette.tune(MapPalette.WATER_SHALLOW, MapPalette.BASE_MUTE, 50);
        // The beige is #B0AC94: the teal water loses blue and gains red.
        assertTrue(r(c) > r(MapPalette.WATER_SHALLOW) && b(c) < b(MapPalette.WATER_SHALLOW));
    }

    @Test
    void theAlphaStays() {
        assertEquals(0xFF000000, MapPalette.tune(MapPalette.GRASS, 60, 30) & 0xFF000000);
    }
}
