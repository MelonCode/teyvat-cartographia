package net.tapacywka.teyvatcartographia.nav;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Arrays;

/**
 * Walking path from the player towards the navigation target over the loaded terrain, for the stars in the world and
 * the line on the world map. A* on a grid of {@code STEP} x {@code STEP} block cells within {@code RADIUS} blocks of
 * the player: deep water (two blocks or more; a swimming player swims out through the surrounding water) and tree trunks cannot be crossed,
 * shallow water can; climbs cost more
 * the steeper they are and anything much higher than the player costs extra, so the path goes round hills and
 * mountains and stays reasonably level. A target farther away is approached along the {@link FarPath} when there is one,
 * else straight towards it. The result is smoothed into soft arcs ({@link PathSmoother}) and resampled every
 * {@code SPACING} blocks, each point on the ground.
 * <p>
 * Runs on the main thread from the client tick, and only while the path is shown ({@link Navigation#pathWanted}). The
 * path is searched once per {@link Navigation#pathRequest} (a new target, the pulse key, or the player reaching the
 * end of a path that stops short of the target), after the far path, and not again while the player walks. A search runs in slices of at most
 * {@code SLICE_NANOS} per tick, so it never stalls a frame; the previous path stays until it is done. The grid arrays
 * are allocated once.
 */
public final class PathFinder {
    private PathFinder() {}

    private static final int STEP = 2, RADIUS = 128, SIDE = 2 * RADIUS / STEP + 1, CELLS = SIDE * SIDE;
    /** Highest step up or down between neighbouring cells (two blocks apart) that is still walkable. */
    private static final int MAX_CLIMB = 3;
    /** Steps up or down of up to this many blocks cost nothing extra (a player jumps or builds over them), so the path keeps straight. */
    private static final int FREE_CLIMB = 2;
    /** Climbing is fine up to this many blocks above the player; above it the cells get more expensive. */
    private static final int HILL = 10;
    /** Distance between the points of the path (blocks). */
    public static final float SPACING = 1.5f;
    /** A line ({@link Route}) that stops short of the target is searched again once the player is this close to its end (blocks). */
    private static final float END_REACH = 8;
    /** ... and the end is this far from the target, at least (blocks). */
    private static final float SHORT_OF_TARGET = 4;
    /** Time per slice: 0.8 ms, so a slice with its last few cells stays within 1 ms. */
    private static final long SLICE_NANOS = 800_000L;
    private static final int UNKNOWN = Integer.MIN_VALUE, BLOCKED = Terrain.BLOCKED;

    // Per-cell ground height (or UNKNOWN / BLOCKED), water flag, and the A* state.
    private static final int[] height = new int[CELLS];
    private static final boolean[] shallow = new boolean[CELLS];
    /** Deep water: entered only from the start or other deep water, so a swimming player swims out and nobody swims in. */
    private static final boolean[] deep = new boolean[CELLS];
    /** The player was in the water when the search began: only then may the path swim (out of it to the shore). */
    private static boolean swimming;
    private static final float[] cost = new float[CELLS];
    private static final int[] from = new int[CELLS];
    private static final boolean[] closed = new boolean[CELLS];
    private static final CellHeap open = new CellHeap(CELLS * 8);
    private static int originX, originZ;
    /** The ground of the search: the loaded level in the game ({@link #LEVEL}), a drawn grid in the unit tests. */
    private static Terrain terrain;
    private static final LevelTerrain LEVEL = new LevelTerrain();

    /** The found path: {@code pointCount} points (x, y, z), from the player towards the target. */
    private static float[] points = new float[3 * 512];
    private static int pointCount;
    /** The end of the path was reached and a new search was asked for (once per path). */
    private static boolean continued;
    /** Changes whenever the path changes (found, cleared), for the cached minimap. */
    private static int version;

    private static Navigation.Target lastTarget;
    /** The request the path was searched for; -1 to search again (the target came into the player's dimension). */
    private static int handled = -1;
    /** {@link FarPath#generation()} the path was searched along. */
    private static int farGeneration;

    public static int pointCount() {
        return pointCount;
    }

    public static float x(int i) {
        return points[3 * i];
    }

    public static float y(int i) {
        return points[3 * i + 1];
    }

    public static float z(int i) {
        return points[3 * i + 2];
    }

    public static int version() {
        return version;
    }

    // How long the path searches took (for the performance tests): count, total, longest slice and longest smoothing, in
    // nanoseconds.
    private static long computeCount, computeTotalNanos, computeMaxNanos, buildMaxNanos;


    // A search in progress: its start, goal, best cell so far and whether it is running.
    private static boolean searching, building;
    private static int start, goal, goalX, goalZ, best, startY;
    private static float bestH, startPX, startPY, startPZ;
    private static long searchNanos;

    /** Client tick: searches the path once per request while there is a target in the player's dimension and it is shown. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Navigation.Target t = Navigation.target();
        if (p == null || mc.level == null || !Navigation.inPlayerDimension(t, p)) {
            if (pointCount > 0) version++;
            pointCount = 0;
            searching = false;
            building = false;
            lastTarget = null;
            handled = -1;
            return;
        }
        if (building) {
            // The found path is smoothed and laid on the ground in a tick of its own.
            long t0 = System.nanoTime();
            build(best);
            long spent = System.nanoTime() - t0;
            building = false;
            computeMaxNanos = Math.max(computeMaxNanos, spent);
            buildMaxNanos = Math.max(buildMaxNanos, spent);
            computeCount++;
            computeTotalNanos += searchNanos + spent;
            return;
        }
        if (!searching) {
            continueAtEnd(p, t);
            // Once per request, and again when the far path it heads along was searched anew; after the far path,
            // so both go the same way.
            boolean due = Navigation.pathRequest() != handled || FarPath.generation() != farGeneration;
            if (!due || !Navigation.pathWanted() || !FarPath.settled()) return;
            if (!t.equals(lastTarget)) {
                pointCount = 0; // the old path led somewhere else
                version++;
            }
            lastTarget = t;
            handled = Navigation.pathRequest();
            farGeneration = FarPath.generation();
            begin(mc, p, t);
        }
        long t0 = System.nanoTime();
        boolean done = step(t0 + SLICE_NANOS);
        long spent = System.nanoTime() - t0;
        searchNanos += spent;
        computeMaxNanos = Math.max(computeMaxNanos, spent);
        if (done) {
            searching = false;
            building = true;
        }
    }

    /**
     * A whole line (near and far path) that stops short of the target: underground, where there is no far path, or
     * beyond the far path's reach. Once the player is near its end, the paths are asked for again from there, so the
     * stars go on. While the far path leads on to the target, the near path's end needs nothing (the stars follow the
     * far path).
     */
    private static void continueAtEnd(LocalPlayer p, Navigation.Target t) {
        int n = Route.collect();
        if (continued || pointCount < 2 || n < 2) return;
        if (!continuesAtEnd(p.getX(), p.getZ(), Route.x(n - 1), Route.z(n - 1), t.x(), t.z())) return;
        continued = true;
        Navigation.requestPath();
    }

    /**
     * Whether a line ending at (ex, ez) is searched again for a player at (px, pz): the line stops short of the target
     * at (tx, tz) and the player has come near its end.
     */
    static boolean continuesAtEnd(double px, double pz, double ex, double ez, double tx, double tz) {
        return Math.hypot(tx - ex, tz - ez) >= SHORT_OF_TARGET && Math.hypot(px - ex, pz - ez) <= END_REACH;
    }
    /** Sets up a search from the player towards the target (the target's cell, or the cell towards it at the edge). */
    private static void begin(Minecraft mc, LocalPlayer p, Navigation.Target t) {
        double dx = t.x() - p.getX(), dz = t.z() - p.getZ(), d = Math.hypot(dx, dz);
        double reach = Math.min(d, RADIUS - STEP);
        double gx = d < 1e-6 ? p.getX() : p.getX() + dx / d * reach, gz = d < 1e-6 ? p.getZ() : p.getZ() + dz / d * reach;
        // A far target: towards the far path's last point in reach, so both paths (and the stars) go the same way.
        int guide = d > reach ? FarPath.lastWithin(p.getX(), p.getZ(), reach) : -1;
        if (guide >= 0) {
            gx = FarPath.x(guide);
            gz = FarPath.z(guide);
        }
        boolean underground = net.tapacywka.teyvatcartographia.map.data.MapDataManager.get().undergroundBand() != null;
        start(LEVEL.set(mc.level, (int) Math.floor(p.getY()), underground), p.getX(), p.getY(), p.getZ(), p.isInWater(), gx, gz);
    }

    /**
     * Sets up a search over {@code ground} from a player at (px, py, pz), in the water or not, to the point (gx, gz)
     * within {@code RADIUS}.
     */
    private static void start(Terrain ground, double px, double py, double pz, boolean inWater, double gx, double gz) {
        terrain = ground;
        originX = (int) Math.floor(px) - RADIUS;
        originZ = (int) Math.floor(pz) - RADIUS;
        Arrays.fill(height, UNKNOWN);
        Arrays.fill(cost, Float.MAX_VALUE);
        Arrays.fill(closed, false);
        startPX = (float) px;
        startPY = (float) py;
        startPZ = (float) pz;
        start = cellOf(px, pz);
        startY = (int) Math.floor(py);
        swimming = inWater;
        goal = cellOf(gx, gz);
        goalX = goal % SIDE;
        goalZ = goal / SIDE;
        open.clear();
        cost[start] = 0;
        from[start] = -1;
        open.push(start, 0);
        best = start;
        bestH = heuristic(start, goalX, goalZ);
        searchNanos = 0;
        searching = true;
    }

    /**
     * The whole search at once (the unit tests): over {@code ground} from (px, py, pz) to (gx, gz); returns the number of
     * points of the path ({@link #x}, {@link #y}, {@link #z}).
     */
    static int search(Terrain ground, double px, double py, double pz, boolean inWater, double gx, double gz) {
        start(ground, px, py, pz, inWater, gx, gz);
        while (!step(Long.MAX_VALUE)) {
            // runs to the end
        }
        searching = false;
        build(best);
        return pointCount;
    }

    /** Whether the search reached the goal's cell (the unit tests). */
    static boolean reachedGoal() {
        return best == goal;
    }

    /** Runs the search until {@code deadline} (checked every few cells); true when it is done (the path is built next tick). */
    private static boolean step(long deadline) {
        int expanded = 0;
        while (!open.isEmpty()) {
            // Checked often: a new cell reads up to 24 blocks of each of its neighbours' columns.
            if ((++expanded & 7) == 0 && System.nanoTime() > deadline) return false;
            int c = open.pop();
            if (closed[c]) continue;
            closed[c] = true;
            if (c == goal) {
                best = c;
                break;
            }
            float h = heuristic(c, goalX, goalZ);
            if (h < bestH) {
                bestH = h;
                best = c;
            }
            int cx = c % SIDE, cz = c / SIDE, ch = ground(c);
            for (int k = 0; k < 8; k++) {
                int nx = cx + NX[k], nz = cz + NZ[k];
                if (nx < 0 || nz < 0 || nx >= SIDE || nz >= SIDE) continue;
                int n = nz * SIDE + nx;
                if (closed[n]) continue;
                int nh = ground(n);
                if (nh == BLOCKED || nh == UNKNOWN) continue;
                if (deep[n] && (!swimming || c != start && !deep[c])) continue; // swum only out of the water the player is in
                int climb = ch == BLOCKED || ch == UNKNOWN ? 0 : Math.abs(nh - ch);
                if (climb > MAX_CLIMB) continue;
                float step = (k < 4 ? STEP : STEP * 1.4142f) + Math.max(0, climb - FREE_CLIMB) * Math.max(0, climb - FREE_CLIMB) * 1.5f
                        + Math.max(0, nh - startY - HILL) * 0.6f + (shallow[n] ? 2f : deep[n] ? 4f : 0f);
                float g = cost[c] + step;
                if (g < cost[n]) {
                    cost[n] = g;
                    from[n] = c;
                    open.push(n, g + heuristic(n, goalX, goalZ));
                }
            }
        }
        return true;
    }
    private static final int[] NX = {1, -1, 0, 0, 1, 1, -1, -1}, NZ = {0, 0, 1, -1, 1, -1, 1, -1};

    private static float heuristic(int c, int gx, int gz) {
        int ax = Math.abs(c % SIDE - gx), az = Math.abs(c / SIDE - gz);
        return STEP * (Math.max(ax, az) + 0.4142f * Math.min(ax, az));
    }

    private static int cellOf(double x, double z) {
        int cx = Math.max(0, Math.min(SIDE - 1, (int) Math.floor((x - originX) / STEP)));
        int cz = Math.max(0, Math.min(SIDE - 1, (int) Math.floor((z - originZ) / STEP)));
        return cz * SIDE + cx;
    }

    /** Walkable ground height of a cell (see {@link Terrain#ground}), read once: BLOCKED for a trunk, nothing found or outside the loaded chunks. */
    private static int ground(int c) {
        int h = height[c];
        if (h != UNKNOWN) return h;
        int x = originX + (c % SIDE) * STEP + STEP / 2, z = originZ + (c / SIDE) * STEP + STEP / 2;
        h = terrain.ground(x, z);
        int water = h == BLOCKED ? Terrain.DRY : terrain.water();
        shallow[c] = water == Terrain.SHALLOW;
        deep[c] = water == Terrain.DEEP;
        return height[c] = h;
    }

    /** Smoothed path from the start along the found cells, resampled every SPACING blocks on the ground. */
    private static void build(int end) {
        // Cells from the end back to the start, then reversed into raw (x, z) points.
        int n = 0;
        for (int c = end; c != -1 && n < RAW.length / 2; c = from[c]) n++;
        int i = n;
        for (int c = end; c != -1 && i > 0; c = from[c]) {
            i--;
            RAW[2 * i] = originX + (c % SIDE) * STEP + STEP / 2f;
            RAW[2 * i + 1] = originZ + (c / SIDE) * STEP + STEP / 2f;
        }
        if (n > 0) {
            RAW[0] = startPX;
            RAW[1] = startPZ;
        }
        // Soft arcs through a control point every ARC cells, kept clear of blocked ground.
        n = PathSmoother.smooth(RAW, n, ARC, PathFinder::blockedAt, SMOOTH);
        System.arraycopy(SMOOTH, 0, RAW, 0, 2 * n);

        // Resample at even spacing; each point on the walkable ground of its column.
        pointCount = 0;
        continued = false;
        version++;
        float along = 0, next = 0;
        for (int k = 0; k + 1 < n; k++) {
            float ax = RAW[2 * k], az = RAW[2 * k + 1], bx = RAW[2 * k + 2], bz = RAW[2 * k + 3];
            float len = (float) Math.hypot(bx - ax, bz - az);
            if (len < 1e-4f) continue;
            while (next <= along + len) {
                if (3 * pointCount + 3 > points.length) points = Arrays.copyOf(points, points.length * 2);
                float f = (next - along) / len, x = ax + (bx - ax) * f, z = az + (bz - az) * f;
                int g = ground(cellOf(x, z));
                points[3 * pointCount] = x;
                points[3 * pointCount + 1] = g == BLOCKED || g == UNKNOWN ? startPY : g;
                points[3 * pointCount + 2] = z;
                pointCount++;
                next += SPACING;
            }
            along += len;
        }
    }

    private static final float[] RAW = new float[2 * 4096], SMOOTH = new float[2 * 4096];
    /** Grid cells between the control points of the smoothed path (8 blocks). */
    private static final int ARC = 4;

    /**
     * Whether the curve at (x, z) crosses a blocked cell. Only cells the search already looked at count (all neighbours of
     * the path are among them): reading new columns here would make this tick too long.
     */
    private static boolean blockedAt(float x, float z) {
        return height[cellOf(x, z)] == BLOCKED;
    }
}
