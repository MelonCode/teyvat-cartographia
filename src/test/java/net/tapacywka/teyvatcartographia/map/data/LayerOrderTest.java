package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The layer field of the world map and of the cave map's preview: the cave bands from the highest down, the surface
 * among them under the bands that start above the sea level (63).
 */
class LayerOrderTest {
    @Test
    void theSurfaceSitsAtTheSeaLevel() {
        // Bands 5 (80 to 95) and 4 (64 to 79) lie above the sea, 2 and -3 under it.
        assertEquals(Arrays.asList(5, 4, null, 2, -3), CaveScanner.layerOrder(List.of(5, 4, 2, -3), 63));
    }

    @Test
    void allUnderTheSeaPutTheSurfaceOnTop() {
        assertEquals(Arrays.asList(null, 1, -1), CaveScanner.layerOrder(List.of(1, -1), 63));
    }

    @Test
    void allAboveTheSeaPutTheSurfaceLast() {
        assertEquals(Arrays.asList(7, 6, null), CaveScanner.layerOrder(List.of(7, 6), 63));
    }

    @Test
    void noBandsLeaveTheSurfaceAlone() {
        assertEquals(Arrays.asList((Integer) null), CaveScanner.layerOrder(List.of(), 63));
    }
}
