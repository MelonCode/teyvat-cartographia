package net.tapacywka.teyvatcartographia.nav;

/**
 * The path to the target as one line for the world map, the minimap and the stars in the world: the precise near path
 * over the loaded terrain ({@link PathFinder}), continued by the far path over the explored map ({@link FarPath}) from
 * its point closest to the near path's end. Points in world blocks, with which of them lie in the fog, their height
 * (only on the near path, NaN on the far one) and their distance along the line; rebuilt only when either path changed.
 */
public final class Route {
    private Route() {}

    /** The far path joins the near one at its point closest to the near path's end, among its first ones. */
    private static final int JOIN_SEARCH = 40;

    private static float[] xz = new float[1024];
    private static float[] ys = new float[512];
    private static float[] along = new float[512];
    private static boolean[] fog = new boolean[512];
    private static int count, kNear = -1, kFar = -1;

    /** Number of points of the line, collected again when a path changed. */
    public static int collect() {
        if (PathFinder.version() == kNear && FarPath.version() == kFar) return count;
        kNear = PathFinder.version();
        kFar = FarPath.version();
        int near = PathFinder.pointCount(), far = FarPath.pointCount();
        int join = 0;
        if (near > 0 && far > 0) {
            float ex = PathFinder.x(near - 1), ez = PathFinder.z(near - 1), bestD = Float.MAX_VALUE;
            for (int i = 0; i < Math.min(far, JOIN_SEARCH); i++) {
                float d = (FarPath.x(i) - ex) * (FarPath.x(i) - ex) + (FarPath.z(i) - ez) * (FarPath.z(i) - ez);
                if (d < bestD) {
                    bestD = d;
                    join = i + 1;
                }
            }
        }
        int n = near + Math.max(0, far - join);
        if (fog.length < n) {
            xz = new float[2 * n];
            ys = new float[n];
            along = new float[n];
            fog = new boolean[n];
        }
        for (int i = 0; i < near; i++) {
            xz[2 * i] = PathFinder.x(i);
            xz[2 * i + 1] = PathFinder.z(i);
            ys[i] = PathFinder.y(i);
            fog[i] = false;
        }
        for (int i = join, k = near; i < far; i++, k++) {
            xz[2 * k] = FarPath.x(i);
            xz[2 * k + 1] = FarPath.z(i);
            ys[k] = Float.NaN;
            fog[k] = FarPath.fog(i);
        }
        for (int i = 0; i < n; i++) {
            along[i] = i == 0 ? 0 : along[i - 1] + (float) Math.hypot(xz[2 * i] - xz[2 * i - 2], xz[2 * i + 1] - xz[2 * i - 1]);
        }
        return count = n;
    }

    /**
     * A line given point by point, as if collected from the paths (the unit tests): {@code n} points (x, z), none in the
     * fog; the walked part starts again from its beginning.
     */
    static void useForTest(float[] points, int n) {
        kNear = PathFinder.version();
        kFar = FarPath.version();
        xz = java.util.Arrays.copyOf(points, Math.max(2 * n, 2));
        ys = new float[Math.max(n, 1)];
        along = new float[Math.max(n, 1)];
        fog = new boolean[Math.max(n, 1)];
        java.util.Arrays.fill(ys, Float.NaN);
        for (int i = 1; i < n; i++) along[i] = along[i - 1] + (float) Math.hypot(xz[2 * i] - xz[2 * i - 2], xz[2 * i + 1] - xz[2 * i - 1]);
        count = n;
        progressVersion = Integer.MIN_VALUE;
    }

    /** Changes whenever the line changes (for cached layers). */
    public static int version() {
        return 31 * PathFinder.version() + FarPath.version();
    }

    public static float x(int i) {
        return xz[2 * i];
    }

    public static float z(int i) {
        return xz[2 * i + 1];
    }

    /** Ground height at point {@code i}: known on the near path, NaN on the far one (its heights are the coarse map's). */
    public static float y(int i) {
        return ys[i];
    }

    /** Distance along the line from its start to point {@code i} (blocks). */
    public static float along(int i) {
        return along[i];
    }

    public static boolean fog(int i) {
        return fog[i];
    }

    // The point where the walked part ends, from the last cut().
    private static float cutX, cutZ, cutAlong;
    /** Squared distance from the asked point to the cut point (last search). */
    private static double cutDist2;
    /** How far along the line the walked part reaches, for the line it was found on: it only grows. */
    private static float progress;
    private static int progressVersion = Integer.MIN_VALUE;
    /**
     * The cut is searched from a little behind the walked part ({@code BACK}) to {@code AHEAD} blocks in front of it,
     * so on a looping line it does not jump to a part that passes close by; only a player more than {@code LOST}
     * blocks off that stretch (flown away, back from elsewhere) gets the nearest point of the whole line.
     */
    private static final float BACK = 4, AHEAD = 48, LOST = 16;

    /**
     * Where the walked part of the line ends: the point of the line nearest to (x, z, the player) on the stretch ahead
     * of the part already walked, so it moves forward only. Returns the segment holding it (0..n-2); {@link #cutX},
     * {@link #cutZ} and {@link #cutAlong} give the point. The world map, the minimap and the stars start there (the
     * path itself is kept whole).
     */
    public static int cut(double x, double z) {
        int n = collect();
        if (version() != progressVersion) {
            progressVersion = version();
            progress = 0;
        }
        int at = nearest(x, z, n, progress - BACK, progress + AHEAD);
        if (cutDist2 > LOST * LOST) {
            at = nearest(x, z, n, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            progress = cutAlong;
        } else {
            progress = Math.max(progress, cutAlong);
        }
        return at;
    }

    /** The point of the line nearest to (x, z) on the segments between {@code lo} and {@code hi} blocks along it. */
    private static int nearest(double x, double z, int n, float lo, float hi) {
        int at = 0;
        cutDist2 = Double.POSITIVE_INFINITY;
        cutX = n > 0 ? x(0) : 0;
        cutZ = n > 0 ? z(0) : 0;
        cutAlong = 0;
        for (int i = 0; i + 1 < n; i++) {
            if (along[i + 1] < lo || along[i] > hi) continue;
            float ax = x(i), az = z(i), dx = x(i + 1) - ax, dz = z(i + 1) - az, len2 = dx * dx + dz * dz;
            float t = len2 < 1e-6f ? 0 : (float) Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / len2));
            double ex = ax + dx * t - x, ez = az + dz * t - z, d = ex * ex + ez * ez;
            if (d < cutDist2) {
                cutDist2 = d;
                at = i;
                cutX = ax + dx * t;
                cutZ = az + dz * t;
                cutAlong = along[i] + (along[i + 1] - along[i]) * t;
            }
        }
        return at;
    }

    public static float cutX() {
        return cutX;
    }

    public static float cutZ() {
        return cutZ;
    }

    public static float cutAlong() {
        return cutAlong;
    }

    /** Horizontal distance from (x, z) to the line; infinite without a line. */
    /** The pulse key keeps the path while the player is this close to its line (blocks); farther off it searches again. */
    public static final double NEAR_LINE = 8;

    /** Whether (x, z) is near the line: the pulse key then shows the stars again without a new search (B10). */
    public static boolean nearLine(double x, double z) {
        return distanceTo(x, z) <= NEAR_LINE;
    }

    public static double distanceTo(double x, double z) {
        int n = collect();
        double best = n == 1 ? Math.hypot(x(0) - x, z(0) - z) : Double.POSITIVE_INFINITY;
        for (int i = 0; i + 1 < n; i++) {
            double ax = x(i), az = z(i), dx = x(i + 1) - ax, dz = z(i + 1) - az, len2 = dx * dx + dz * dz;
            double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / len2));
            best = Math.min(best, Math.hypot(ax + dx * t - x, az + dz * t - z));
        }
        return best;
    }
}
