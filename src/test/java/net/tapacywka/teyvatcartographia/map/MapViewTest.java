package net.tapacywka.teyvatcartographia.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The zoom steps shared by the world map, the minimap and the settings sliders. */
class MapViewTest {
    @Test
    void stepsGoUpFrom1Over16To16() {
        double[] s = MapView.ZOOM_STEPS;
        assertEquals(1 / 16.0, s[0]);
        assertEquals(16.0, s[s.length - 1]);
        for (int i = 1; i < s.length; i++) assertTrue(s[i] > s[i - 1], "step " + i + " is larger than the one before");
        assertEquals(1.0, s[MapView.ZOOM_DEFAULT]);
    }

    @Test
    void oneStepUpAndDownStopsAtTheEnds() {
        assertEquals(2.0, MapView.stepZoom(1, 1));
        assertEquals(0.5, MapView.stepZoom(1, -1));
        assertEquals(16.0, MapView.stepZoom(16, 1));
        assertEquals(1 / 16.0, MapView.stepZoom(1 / 16.0, -1));
    }

    @Test
    void screenAndWorldAreTheSamePointBothWays() {
        MapView v = new MapView().set(1088.5, -811.25, 2, 0, 480, 270);
        assertEquals(480, v.screenX(1088.5, -811.25), 1e-3);
        assertEquals(270, v.screenY(1088.5, -811.25), 1e-3);
        // 10 blocks east at 2 pixels per block: 20 pixels right; north is up.
        assertEquals(500, v.screenX(1098.5, -811.25), 1e-3);
        assertEquals(250, v.screenY(1088.5, -821.25), 1e-3);
        for (double[] p : new double[][]{{0, 0}, {123.4, 567.8}, {960, 540}}) {
            assertEquals(p[0], v.screenX(v.worldX(p[0], p[1]), v.worldZ(p[0], p[1])), 1e-2);
            assertEquals(p[1], v.screenY(v.worldX(p[0], p[1]), v.worldZ(p[0], p[1])), 1e-2);
        }
    }

    @Test
    void aTurnedMapKeepsTheCentreAndTurnsAroundIt() {
        // The minimap turned with the player looking east (a quarter turn): the block ahead (east) is up.
        MapView v = new MapView().set(0, 0, 1, -Math.PI / 2, 100, 100);
        assertEquals(100, v.screenX(0, 0), 1e-3);
        assertEquals(100, v.screenY(0, 0), 1e-3);
        double sx = v.screenX(10, 0), sy = v.screenY(10, 0);
        assertEquals(100, sx, 1e-3);
        assertEquals(90, sy, 1e-3);
        assertEquals(10, v.worldX(sx, sy), 1e-3);
        assertEquals(0, v.worldZ(sx, sy), 1e-3);
    }

    @Test
    void nearestStepTellsAThirdFromAQuarter() {
        assertEquals("1/3", MapView.zoomLabel(0.32));
        assertEquals("1/4", MapView.zoomLabel(0.26));
        assertEquals("8", MapView.zoomLabel(7));
        assertEquals("1/16", MapView.zoomLabel(0.0001));
        assertEquals(1 / 3.0, MapView.snapZoom(0.34));
    }
}
