package net.tapacywka.teyvatcartographia.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tracked pin's marker in the world: shown for a minute, fading in and out; a pulse ignores new presses while it runs (B8). */
class MarkerTimingTest {
    @Test
    void itFadesInThenStaysForAMinute() {
        assertEquals(0f, MarkerTiming.visibility(0));
        assertTrue(MarkerTiming.visibility(200) > 0.2f && MarkerTiming.visibility(200) < 0.8f, "half way in");
        assertEquals(1f, MarkerTiming.visibility(MarkerTiming.FADE_IN_MS));
        assertEquals(1f, MarkerTiming.visibility(30_000));
        assertEquals(1f, MarkerTiming.visibility(59_999));
    }

    @Test
    void thenItFadesOut() {
        float mid = MarkerTiming.visibility(MarkerTiming.SHOW_MS + MarkerTiming.FADE_OUT_MS / 2);
        assertTrue(mid > 0.2f && mid < 0.8f, "half way out: " + mid);
        assertEquals(0f, MarkerTiming.visibility(MarkerTiming.SHOW_MS + MarkerTiming.FADE_OUT_MS));
        assertEquals(0f, MarkerTiming.visibility(10 * 60_000));
    }

    @Test
    void itNeverGoesOutOfRange() {
        for (long t = -1000; t < 70_000; t += 37) {
            float v = MarkerTiming.visibility(t);
            assertTrue(v >= 0f && v <= 1f, t + " ms: " + v);
        }
    }

    @Test
    void theBeamFadesOutNearTheLimiterEllipse() {
        assertEquals(1f, MarkerTiming.edgeFade(0f), 1e-6, "at the screen centre");
        assertEquals(1f, MarkerTiming.edgeFade(0.85f * 0.85f), 1e-6, "where the fade begins");
        float mid = MarkerTiming.edgeFade(0.925f * 0.925f);
        assertTrue(mid > 0.3f && mid < 0.7f, "halfway through the fade: " + mid);
        assertEquals(0f, MarkerTiming.edgeFade(1f), 1e-6, "gone on the ellipse");
        assertEquals(0f, MarkerTiming.edgeFade(1.5f), 1e-6, "and beyond it");
        float last = 1f;
        for (float r = 0.85f; r <= 1f; r += 0.01f) {
            float f = MarkerTiming.edgeFade(r * r);
            assertTrue(f <= last + 1e-6f, "never brighter further out (" + r + ")");
            last = f;
        }
    }

    @Test
    void aPulseRunsOneSecondAndPressesMeanwhileAreIgnored() {
        assertFalse(MarkerTiming.pulseRunning(Long.MIN_VALUE, 5_000), "no pulse yet");
        assertTrue(MarkerTiming.pulseRunning(1_000, 1_500));
        assertTrue(MarkerTiming.pulseRunning(1_000, 1_999));
        assertFalse(MarkerTiming.pulseRunning(1_000, 2_000), "after a second a new pulse may start");
    }
}
