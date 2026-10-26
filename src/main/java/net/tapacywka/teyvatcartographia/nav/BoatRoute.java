package net.tapacywka.teyvatcartographia.nav;

/**
 * The path by boat beside the main path on foot (idea of a tester, user decision 25.10.2026): the near and the far
 * boat paths ({@link PathFinder#BOAT}, {@link FarPath#BOAT}) joined like {@link Route}, drawn grey on the world map
 * and the minimap only (no stars), dotted over deep water, with a boat icon where each crossing begins. It is shown
 * only when it is clearly shorter than the main line ({@link #worth}) and crosses water the main path would not swim.
 * Points in world blocks, with which of them lie on deep water; rebuilt only when either line changed.
 */
public final class BoatRoute {
    private BoatRoute() {}

    /** Shown only when at most this share of the main line's length ... */
    static final float SHORTER = 0.7f;
    /** ... and at least this many blocks shorter. */
    static final float SAVES = 32;

    private static float[] farXZ = new float[1024];
    private static float[] xz = new float[1024];
    private static boolean[] water = new boolean[512];
    private static float[] along = new float[512];
    private static int count, kNear = -1, kFar = -1, kMain = Integer.MIN_VALUE;
    private static boolean shown;

    /** Number of points of the boat line when it is worth showing, else 0; collected again when a line changed. */
    public static int collect() {
        int main = Route.collect();
        PathFinder near = PathFinder.BOAT;
        FarPath far = FarPath.BOAT;
        if (near.version == kNear && far.version == kFar && Route.version() == kMain) return revealed();
        kNear = near.version;
        kFar = far.version;
        kMain = Route.version();
        int nn = near.pointCount, fn = far.pointCount, join = 0;
        if (nn > 0 && fn > 0) {
            if (farXZ.length < 2 * fn) farXZ = new float[2 * fn];
            for (int i = 0; i < fn; i++) {
                farXZ[2 * i] = far.px(i);
                farXZ[2 * i + 1] = far.pz(i);
            }
            join = Route.join(farXZ, fn, near.points[3 * (nn - 1)], near.points[3 * (nn - 1) + 2]);
        }
        int n = nn + Math.max(0, fn - join);
        if (water.length < n) {
            xz = new float[2 * n];
            water = new boolean[n];
            along = new float[n];
        }
        for (int i = 0; i < nn; i++) {
            xz[2 * i] = near.points[3 * i];
            xz[2 * i + 1] = near.points[3 * i + 2];
            water[i] = near.water[i];
        }
        for (int i = join, k = nn; i < fn; i++, k++) {
            xz[2 * k] = far.px(i);
            xz[2 * k + 1] = far.pz(i);
            water[k] = far.water[i];
        }
        for (int i = 0; i < n; i++) along[i] = i == 0 ? 0 : along[i - 1] + (float) Math.hypot(xz[2 * i] - xz[2 * i - 2], xz[2 * i + 1] - xz[2 * i - 1]);
        count = n;
        Navigation.Target t = Navigation.target();
        shown = false;
        if (t != null && n >= 2) {
            float boatLen = length(xz, n), mainLen = main >= 2 ? Route.along(main - 1) : 0;
            float boatGap = gap(xz, n, t), mainGap = main >= 1 ? (float) Math.hypot(t.x() - Route.x(main - 1), t.z() - Route.z(main - 1)) : Float.POSITIVE_INFINITY;
            shown = worth(mainLen + mainGap, boatLen + boatGap, longestWater(xz, water, n));
        }
        return revealed();
    }

    /** Feeds the line as it is shown (none when not worth it) to its drawing-in; returns its point count. */
    private static int revealed() {
        int n = shown ? count : 0;
        REVEAL.update(Navigation.pathRequest(), rawVersion(), n, xz, along, water, net.minecraft.Util.getMillis());
        return n;
    }

    /**
     * Whether a boat line of {@code boat} blocks (its length plus what is left of the straight way to the target from
     * its end) is shown beside a main line of {@code main} blocks (the same): at most {@link #SHORTER} of it and
     * {@link #SAVES} blocks shorter at least, and only when it crosses more than {@link PathFinder#NARROW} blocks of
     * deep water in one go ({@code longestWater}; narrower water the main path swims itself).
     */
    static boolean worth(float main, float boat, float longestWater) {
        return longestWater > PathFinder.NARROW && boat <= main * SHORTER && main - boat >= SAVES;
    }

    /** Length of the line through the first {@code n} points (x, z) of {@code xz}. */
    static float length(float[] xz, int n) {
        float len = 0;
        for (int i = 1; i < n; i++) len += (float) Math.hypot(xz[2 * i] - xz[2 * i - 2], xz[2 * i + 1] - xz[2 * i - 1]);
        return len;
    }

    /** The longest run of points on deep water, in blocks along the line. */
    static float longestWater(float[] xz, boolean[] water, int n) {
        float best = 0, run = 0;
        for (int i = 1; i < n; i++) {
            if (water[i] && water[i - 1]) {
                run += (float) Math.hypot(xz[2 * i] - xz[2 * i - 2], xz[2 * i + 1] - xz[2 * i - 1]);
                best = Math.max(best, run);
            } else {
                run = 0;
            }
        }
        return best;
    }

    private static float gap(float[] xz, int n, Navigation.Target t) {
        return (float) Math.hypot(t.x() - xz[2 * n - 2], t.z() - xz[2 * n - 1]);
    }

    /** Changes whenever the boat line or whether it is shown changes (for cached layers). */
    public static int version() {
        collect();
        return rawVersion();
    }

    private static int rawVersion() {
        return 31 * (31 * kNear + kFar) + (shown ? 1 : 0);
    }

    public static float x(int i) {
        return xz[2 * i];
    }

    public static float z(int i) {
        return xz[2 * i + 1];
    }

    /** Distance along the line from its start to point {@code i} (blocks): the dashes stay put as its start moves. */
    public static float along(int i) {
        return along[i];
    }

    /** The drawing-in of a new boat line and the wiping of the one before it, as for the main line ({@link Route#shownTo}). */
    private static final LineReveal REVEAL = new LineReveal();

    public static float shownTo() {
        return REVEAL.shownTo(net.minecraft.Util.getMillis(), count > 0 ? along[count - 1] : 0);
    }

    public static boolean revealing() {
        return REVEAL.running(net.minecraft.Util.getMillis());
    }

    public static int oldCount() {
        return REVEAL.oldCount(net.minecraft.Util.getMillis());
    }

    public static float oldFrom() {
        return REVEAL.oldFrom(net.minecraft.Util.getMillis());
    }

    public static float oldX(int i) {
        return REVEAL.oldX(i);
    }

    public static float oldZ(int i) {
        return REVEAL.oldZ(i);
    }

    public static float oldAlong(int i) {
        return REVEAL.oldAlong(i);
    }

    public static boolean oldWater(int i) {
        return REVEAL.oldFlag(i);
    }

    /** Whether point {@code i} lies on deep water (drawn dotted). */
    public static boolean water(int i) {
        return water[i];
    }

    /**
     * Whether a crossing begins at point {@code i}: on deep water after land (or at the start), and the water goes on
     * for more than {@link PathFinder#NARROW} blocks (the boat icon goes there).
     */
    public static boolean crossingAt(int i) {
        if (!water[i] || i > 0 && water[i - 1]) return false;
        float run = 0;
        for (int k = i + 1; k < count && water[k]; k++) {
            run += (float) Math.hypot(xz[2 * k] - xz[2 * k - 2], xz[2 * k + 1] - xz[2 * k - 1]);
            if (run > PathFinder.NARROW) return true;
        }
        return false;
    }

    /** Point index the walked part reaches, for the line it was found on: it only grows. */
    private static int walked;
    private static int walkedVersion = Integer.MIN_VALUE;
    /** The walked part is looked for this many points ahead of it (about 48 blocks), within this many blocks of the player. */
    private static final int AHEAD = 32;
    private static final float REACH = 16;

    /**
     * The first point drawn: the line stays where it was found, only the part the player walked along it is left out.
     * It moves forward only, and only while the player is within {@link #REACH} blocks of the stretch ahead of it; a
     * player walking elsewhere (along the main line) leaves the boat line as it is (it followed the player around
     * while it was drawn from its point nearest to the player).
     */
    public static int start(double x, double z) {
        int v = version();
        if (v != walkedVersion) {
            walkedVersion = v;
            walked = 0;
        }
        int at = walked;
        double best = REACH * REACH;
        for (int i = walked; i < Math.min(count, walked + AHEAD); i++) {
            double dx = xz[2 * i] - x, dz = xz[2 * i + 1] - z, d = dx * dx + dz * dz;
            if (d <= best) {
                best = d;
                at = i;
            }
        }
        return walked = at;
    }
}
