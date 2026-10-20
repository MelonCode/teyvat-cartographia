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
    void aPulseRunsOneSecondAndPressesMeanwhileAreIgnored() {
        assertFalse(MarkerTiming.pulseRunning(Long.MIN_VALUE, 5_000), "no pulse yet");
        assertTrue(MarkerTiming.pulseRunning(1_000, 1_500));
        assertTrue(MarkerTiming.pulseRunning(1_000, 1_999));
        assertFalse(MarkerTiming.pulseRunning(1_000, 2_000), "after a second a new pulse may start");
    }
}
