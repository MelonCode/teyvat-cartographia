package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Teyvat Cartographia palette: the standard colours through mute and warm haze. */
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

    private static final int GRASS = 0xFF5C8E3A, WATER = 0xFF3F76E4;

    @Test
    void bothSlidersAtZeroLeaveTheStandardColours() {
        for (int c : new int[] {GRASS, WATER, 0xFFDBCFA3, 0xFF7D7D7D, 0xFF000000, 0xFFFFFFFF}) {
            assertEquals(c, PaletteTuning.tune(c, 0, 0));
        }
    }

    @Test
    void fullMuteWithoutHazeIsTheGreyOfTheSameLuminance() {
        int c = PaletteTuning.tune(GRASS, 100, 0);
        assertEquals(r(c), g(c));
        assertEquals(g(c), b(c));
        assertEquals(Math.round(0.3f * r(GRASS) + 0.59f * g(GRASS) + 0.11f * b(GRASS)), r(c));
    }

    @Test
    void moreMuteMeansWeakerColours() {
        int some = PaletteTuning.tune(GRASS, 25, 0), more = PaletteTuning.tune(GRASS, 60, 0);
        assertTrue(g(GRASS) - b(GRASS) > g(some) - b(some) && g(some) - b(some) > g(more) - b(more));
    }

    @Test
    void theHazeLeansToTheWarmBeige() {
        int c = PaletteTuning.tune(WATER, 0, 50);
        assertTrue(r(c) > r(WATER) && b(c) < b(WATER), "the blue water warms up");
        assertEquals(0xFFB0AC94, PaletteTuning.tune(WATER, 0, 100), "full haze is the beige itself");
    }

    @Test
    void theAlphaStays() {
        assertEquals(0x80000000, PaletteTuning.tune(0x80123456, 60, 30) & 0xFF000000);
    }
}
