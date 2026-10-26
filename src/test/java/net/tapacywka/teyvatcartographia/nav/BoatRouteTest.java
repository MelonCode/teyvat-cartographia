package net.tapacywka.teyvatcartographia.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The boat path beside the main one: shown only when clearly shorter and over water the main path would not swim. */
class BoatRouteTest {
    @Test
    void shownOnlyWhenClearlyShorter() {
        assertTrue(BoatRoute.worth(300, 150, 80), "half the way round the lake");
        assertTrue(BoatRoute.worth(300, 210, 80), "exactly 70%");
        assertFalse(BoatRoute.worth(300, 220, 80), "only a little shorter");
        assertFalse(BoatRoute.worth(80, 50, 40), "70% but only 30 blocks saved");
        assertTrue(BoatRoute.worth(120, 80, 40), "70% and 40 blocks saved");
    }

    @Test
    void notForWaterTheMainPathSwims() {
        assertFalse(BoatRoute.worth(300, 100, PathFinder.NARROW), "a river the main path swims itself");
        assertTrue(BoatRoute.worth(300, 100, PathFinder.NARROW + 1));
        assertFalse(BoatRoute.worth(300, 100, 0), "no water at all");
    }

    @Test
    void lengthsAlongTheLine() {
        float[] xz = {0, 0, 10, 0, 20, 0, 30, 0, 40, 0, 50, 0};
        boolean[] water = {false, true, true, true, false, false};
        assertEquals(50, BoatRoute.length(xz, 6), 1e-4);
        assertEquals(20, BoatRoute.longestWater(xz, water, 6), 1e-4, "from the first to the last point on water");
        boolean[] two = {true, true, false, true, true, true};
        assertEquals(20, BoatRoute.longestWater(xz, two, 6), 1e-4, "the longer of two crossings");
    }
}
