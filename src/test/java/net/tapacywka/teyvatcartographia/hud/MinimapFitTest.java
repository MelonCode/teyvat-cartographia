package net.tapacywka.teyvatcartographia.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The minimap stays on screen: a large one is drawn smaller (B23), a wide pill at the edge is shifted inward (B22). */
class MinimapFitTest {
    @Test
    void aMinimapThatFitsKeepsItsSize() {
        // 1920x1080 at GUI 3: 640x360 units.
        assertEquals(110, MinimapFit.radius(110, 640, 360, 60, 24));
        assertEquals(48, MinimapFit.radius(48, 640, 360, 60, 24));
    }

    @Test
    void aTooLargeMinimapShrinksToTheScreen() {
        // 1920x1080 at GUI 4 (270 units high), size 110 with three pills (about 64 units): B23.
        int r = MinimapFit.radius(110, 480, 270, 64, 24);
        assertEquals((270 - 6 - 64) / 2, r);
        assertTrue(2 * r + 64 + 2 * MinimapFit.EDGE <= 270, "map and pills fit");
        // Without pills the same map fits whole.
        assertEquals(110, MinimapFit.radius(110, 480, 270, 0, 24));
    }

    @Test
    void itNeverGoesBelowTheMinimum() {
        assertEquals(24, MinimapFit.radius(110, 100, 60, 64, 24));
    }

    @Test
    void autoTextIsAStepBelowTheGuiScale() {
        // Pixels per font pixel = scale x GUI scale.
        assertEquals(2, MinimapFit.textScale(0, 3) * 3, 1e-6);
        assertEquals(3, MinimapFit.textScale(0, 4) * 4, 1e-6);
        assertEquals(5, MinimapFit.textScale(0, 6) * 6, 1e-6);
        assertEquals(2, MinimapFit.textScale(0, 2) * 2, 1e-6, "GUI 2 keeps 2");
        assertEquals(1, MinimapFit.textScale(0, 1), 1e-6);
    }

    @Test
    void aChosenTextSizeIsWholePixelsAtMostTheGuiScale() {
        assertEquals(1, MinimapFit.textScale(1, 3) * 3, 1e-6);
        assertEquals(3, MinimapFit.textScale(3, 3) * 3, 1e-6);
        assertEquals(3, MinimapFit.textScale(6, 3) * 3, 1e-6, "never larger than the menus");
    }

    @Test
    void aPillThatFitsStaysCentred() {
        assertEquals(100 - 20, MinimapFit.pillLeft(40, 100, 640));
    }

    @Test
    void aWidePillAtTheRightEdgeIsShiftedInward() {
        // Size 24 in the right corner: the map's centre 27 from the edge, the coordinates pill 90 wide (B22).
        float left = MinimapFit.pillLeft(90, 640 - 27, 640);
        assertEquals(640 - MinimapFit.EDGE - 90, left);
    }

    @Test
    void aWidePillAtTheLeftEdgeIsShiftedInward() {
        assertEquals(MinimapFit.EDGE, MinimapFit.pillLeft(90, 27, 640));
    }
}
