package net.tapacywka.teyvatcartographia.hud;

import net.tapacywka.teyvatcartographia.render.MarkerRenderer;

/**
 * How long the tracked pin's marker shows in the world ({@link WorldMarkerLayer}): {@code SHOW_MS} after tracking
 * starts or the pulse key is pressed, fading in over {@code FADE_IN_MS} and out over {@code FADE_OUT_MS}; and while a
 * pulse (the diamond) runs, a new press of the key is ignored.
 */
final class MarkerTiming {
    private MarkerTiming() {}

    static final long SHOW_MS = 60_000, FADE_IN_MS = 400, FADE_OUT_MS = 1_500;

    /** Visibility 0..1 of the marker {@code elapsedMs} after it was shown. */
    static float visibility(long elapsedMs) {
        if (elapsedMs < FADE_IN_MS) return smooth(elapsedMs / (double) FADE_IN_MS);
        if (elapsedMs < SHOW_MS) return 1f;
        return smooth(1 - (elapsedMs - SHOW_MS) / (double) FADE_OUT_MS);
    }

    /** Whether the pulse started at {@code pulseStart} ({@code Long.MIN_VALUE}: none yet) still runs at {@code now}. */
    static boolean pulseRunning(long pulseStart, long now) {
        return pulseStart != Long.MIN_VALUE && now - pulseStart < MarkerRenderer.WORLD_PULSE_LIFE * 1000;
    }

    /** Share of the way from the screen centre to the limiter ellipse over which the beam fades out at the edge. */
    static final float EDGE_FADE = 0.15f;

    /**
     * Visibility 0..1 of the beam for a marker at {@code ellipse} = ((x / rx)^2 + (y / ry)^2) of the limiter (1 on the
     * ellipse): full inside, fading out over the last {@code EDGE_FADE} of the way, gone on the ellipse where the
     * marker starts to ride it.
     */
    static float edgeFade(float ellipse) {
        return smooth((1 - Math.sqrt(Math.max(0, ellipse))) / EDGE_FADE);
    }

    static float smooth(double t) {
        float x = (float) Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }
}
