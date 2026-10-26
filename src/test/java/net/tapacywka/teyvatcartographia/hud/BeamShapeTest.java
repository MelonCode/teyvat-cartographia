package net.tapacywka.teyvatcartographia.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The beam over the tracked target rises, stays bright, then fades out smoothly to its top at any distance, never cut off. */
class BeamShapeTest {
    private final float[] heights = new float[BeamShape.ROWS], alphas = new float[BeamShape.ROWS];

    @Test
    void itRisesAndFadesAtAnyDistance() {
        for (float dist = 0; dist < 2_000_000; dist = dist * 1.3f + 1) {
            BeamShape.rows(dist, heights, alphas);
            for (int i = 1; i < BeamShape.ROWS; i++) assertTrue(heights[i] > heights[i - 1], dist + " blocks, row " + i + " is not above the last");
            assertEquals(0f, alphas[0], "faint at the marker, " + dist + " blocks");
            assertEquals(1f, alphas[1], "full after the rise, " + dist + " blocks");
            assertEquals(1f, alphas[2], "full to the start of the fade, " + dist + " blocks");
            for (int i = 3; i < BeamShape.ROWS; i++) assertTrue(alphas[i] < alphas[i - 1], dist + " blocks, row " + i + " does not fade");
            assertEquals(0f, alphas[BeamShape.ROWS - 1], 1e-6, "nothing left at the top, " + dist + " blocks");
        }
    }

    /** The reported cut: 29822 blocks away the fade began above the top and folded back down over the bright part. */
    @Test
    void farAwayTheFadeStaysAboveTheBrightPart() {
        BeamShape.rows(29_822, heights, alphas);
        float rise = heights[1], full = heights[2], top = heights[BeamShape.ROWS - 1];
        assertTrue(rise < full && full < top, "rise " + rise + ", full " + full + ", top " + top);
        assertTrue(top - full > full - rise, "the fade is longer than the bright part");
    }

    /** Far away the beam keeps its look: every height in proportion to the distance. */
    @Test
    void farAwayItScalesWithTheDistance() {
        float[] h2 = new float[BeamShape.ROWS], a2 = new float[BeamShape.ROWS];
        BeamShape.rows(1_000, heights, alphas);
        BeamShape.rows(10_000, h2, a2);
        for (int i = 1; i < BeamShape.ROWS; i++) assertEquals(heights[i] * 10, h2[i], h2[i] * 1e-4, "row " + i);
    }

    /** Nearby it is as before: 320 blocks tall, full from a few blocks up. */
    @Test
    void nearbyItKeepsItsHeight() {
        BeamShape.rows(63, heights, alphas);
        assertEquals(BeamShape.HEIGHT, heights[BeamShape.ROWS - 1], 1e-3);
        assertEquals(6.3f, heights[1], 1e-3);
    }
}
