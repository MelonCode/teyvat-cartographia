package net.tapacywka.teyvatcartographia.nav;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.map.data.MapTile;
import net.tapacywka.teyvatcartographia.map.data.TileStore;

import java.util.Arrays;

/**
 * The long walking path to the tracked target over the explored map: drawn on the world map, and the precise near
 * {@link PathFinder} (the stars in the world) heads along it. A* on a grid of {@code STEP} x {@code STEP} block cells within
 * {@code RADIUS} blocks of the player, read from the stored map regions: each cell samples four columns; deep water and
 * lava ({@link MapTile#FLAG_IMPASSABLE}) cannot be crossed (a swimming player swims out through the surrounding water),
 * shallow water can, climbs and anything much higher than the
 * player cost more, like the near path. Unexplored cells (the fog) can be crossed at a small extra cost ({@link #fog}
 * marks the points there), and the path is smoothed into soft arcs ({@link PathSmoother}). The forest canopy stands in for the ground there, so the path is
 * coarse; near the player {@link PathFinder} is the precise one.
 * <p>
 * Runs on the main thread from the client tick, only while the path is shown ({@link Navigation#pathWanted}) and the
 * target is farther than the near path reaches. The path is searched once per {@link Navigation#pathRequest} (a new
 * target, the pulse key, the end of the near path), not again while the player walks. Only when
 * some regions in range were still on disk, it is searched once more {@code RETRY_TICKS} later, with them loaded. A
 * search runs in slices of at most {@code SLICE_NANOS} per tick. The grid arrays are allocated on the first search.
 */
public final class FarPath {
    private FarPath() {}

    private static final int STEP = 8, RADIUS = 1024, SIDE = 2 * RADIUS / STEP + 1, CELLS = SIDE * SIDE;
    /** Highest step up or down between neighbouring cells (eight blocks apart) that is still walkable. */
    private static final int MAX_CLIMB = 12;
    /** Steps up or down of up to this many blocks between cells cost nothing extra, as two blocks on the near path. */
    private static final int FREE_CLIMB = 4;
    /** Climbing is fine up to this many blocks above the player; above it the cells get more expensive. */
    private static final int HILL = 10;
    /** The fog costs this much more than explored flat ground, so the path keeps to the known land when it can. */
    private static final float FOG_COST = 1.3f;
    /** Distance between the points of the path (blocks). */
    private static final float SPACING = 4f;
    /** Closer targets are left to the near path alone. */
    private static final double NEAR_ENOUGH = 48;
    /** A search that found regions still on disk is repeated once, this many ticks later. */
    private static final int RETRY_TICKS = 20;

    /** Time per slice: 0.8 ms, so a slice with its last few cells stays within 1 ms. */
    private static final long SLICE_NANOS = 800_000L;
    private static final int UNKNOWN = Integer.MIN_VALUE, BLOCKED = Integer.MIN_VALUE + 1, FOG = Integer.MIN_VALUE + 2;

    // Per-cell ground height (or UNKNOWN / BLOCKED / FOG), shallow water flag, and the A* state; allocated once.
    private static int[] height;
    private static boolean[] shallow;
    /** Deep water: entered only from the start or other deep water, so a swimming player swims out and nobody swims in. */
    private static boolean[] deep;
    /** The player was in the water when the search began: only then may the path swim (out of it to the shore). */
    private static boolean swimming;
    private static float[] cost;
    private static int[] from;
    private static boolean[] closed;
    private static CellHeap open;
    private static int originX, originZ;

    /** The found path: {@code pointCount} points (x, z) from the player towards the target, and which lie in the fog. */
    private static float[] points = new float[2 * 512];
    private static boolean[] fogPoint = new boolean[512];
    private static int pointCount;
    /** Changes whenever the path changes (found, cleared), for the cached minimap. */
    private static int version;
    /** Counts the searches done ({@link PathFinder} searches again after each). */
    private static int generation;

    private static Navigation.Target lastTarget;
    private static TileStore lastStore;
    /** The request the path was searched for; -1 to search again (the target came into the player's dimension). */
    private static int handled = -1;
    /** Regions in range were still on disk at the last search: it is repeated once after {@code retryIn} ticks. */
    private static boolean retry;
    private static int retryIn;

    public static int pointCount() {
        return pointCount;
    }

    public static float x(int i) {
        return points[2 * i];
    }

    public static float z(int i) {
        return points[2 * i + 1];
    }

    /** Whether point {@code i} lies in unexplored land (the part of the path that is only a guess). */
    public static boolean fog(int i) {
        return fogPoint[i];
    }

    public static int version() {
        return version;
    }

    public static int generation() {
        return generation;
    }

    /** Whether the far path is done with the current request (searched, or nothing to search), so the near path can follow it. */
    public static boolean settled() {
        return handled == Navigation.pathRequest() && !searching && !building;
    }

    // How long the searches took (for the tests): count, total and longest slice, in nanoseconds.
    private static long computeCount, computeTotalNanos, computeMaxNanos;


    /**
     * The last of the first points of the path that are all within {@code r} blocks of (x, z), or -1 when there is no
     * path or it does not start near (x, z) (it was found from somewhere else). The near path heads there.
     */
    public static int lastWithin(double x, double z, double r) {
        if (pointCount() == 0 || Math.hypot(x(0) - x, z(0) - z) > START_SLACK) return -1;
        int last = -1;
        for (int i = 0; i < pointCount() && Math.hypot(x(i) - x, z(i) - z) <= r; i++) last = i;
        return last;
    }
    private static final double START_SLACK = 24;

    // A search in progress.
    private static boolean searching, building;
    private static int start, goal, goalX, goalZ, best, startY;
    private static float bestH, startPX, startPZ;
    private static long searchNanos;
    private static TileStore store;

    /** Client tick: searches the path once per request while it is shown and the target is far. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Navigation.Target t = Navigation.target();
        TileStore active = MapDataManager.get().active();
        if (p == null || active == null || !Navigation.inPlayerDimension(t, p)) {
            clear();
            handled = -1;
            return;
        }
        if ((searching || building) && active != store) searching = building = false; // the dimension changed under it
        if (building) {
            // The found path is smoothed in a tick of its own.
            long t0 = System.nanoTime();
            build(best);
            long spent = System.nanoTime() - t0;
            building = false;
            computeMaxNanos = Math.max(computeMaxNanos, spent);
            computeCount++;
            computeTotalNanos += searchNanos + spent;
            return;
        }
        if (!searching) {
            boolean request = Navigation.pathRequest() != handled;
            if (retry && !request && --retryIn > 0) return;
            if (!request && !retry || !Navigation.pathWanted()) return;
            retry = false;
            handled = Navigation.pathRequest();
            // Underground it would follow the surface map; a close target is left to the near path.
            if (MapDataManager.get().undergroundBand() != null || Math.hypot(t.x() - p.getX(), t.z() - p.getZ()) < NEAR_ENOUGH) {
                clear();
                return;
            }
            if (!t.equals(lastTarget) || active != lastStore) {
                pointCount = 0;
                version++;
            }
            lastTarget = t;
            lastStore = active;
            begin(p, t, active);
            if (request && retry) retryIn = RETRY_TICKS;
            else retry = false; // a repeated search is not repeated again
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

    private static void clear() {
        if (pointCount > 0) version++;
        pointCount = 0;
        searching = building = retry = false;
        lastTarget = null;
    }

    private static void begin(LocalPlayer p, Navigation.Target t, TileStore s) {
        if (height == null) {
            height = new int[CELLS];
            shallow = new boolean[CELLS];
            deep = new boolean[CELLS];
            cost = new float[CELLS];
            from = new int[CELLS];
            closed = new boolean[CELLS];
            open = new CellHeap(CELLS * 4);
        }
        store = s;
        // Cells on the world grid of STEP blocks, so a cell never straddles two regions.
        originX = Math.floorDiv((int) Math.floor(p.getX()) - RADIUS, STEP) * STEP;
        originZ = Math.floorDiv((int) Math.floor(p.getZ()) - RADIUS, STEP) * STEP;
        Arrays.fill(height, UNKNOWN);
        Arrays.fill(cost, Float.MAX_VALUE);
        Arrays.fill(closed, false);
        // Ask for the regions in range, so those still on disk come in for the next search.
        int t0x = MapView.blockToTile(originX), t0z = MapView.blockToTile(originZ);
        int t1x = MapView.blockToTile(originX + SIDE * STEP), t1z = MapView.blockToTile(originZ + SIDE * STEP);
        for (int tz = t0z; tz <= t1z; tz++) {
            for (int tx = t0x; tx <= t1x; tx++) {
                if (s.exists(tx, tz) && s.peek(tx, tz) == null) {
                    s.get(tx, tz);
                    retry = true;
                }
            }
        }
        startPX = (float) p.getX();
        startPZ = (float) p.getZ();
        start = cellOf(p.getX(), p.getZ());
        startY = (int) Math.floor(p.getY());
        swimming = p.isInWater();
        double dx = t.x() - p.getX(), dz = t.z() - p.getZ(), d = Math.hypot(dx, dz);
        double reach = Math.min(d, RADIUS - STEP);
        goal = cellOf(p.getX() + dx / d * reach, p.getZ() + dz / d * reach);
        goalX = goal % SIDE;
        goalZ = goal / SIDE;
        open.clear();
        cost[start] = 0;
        from[start] = -1;
        open.push(start, 0);
        best = start;
        bestH = heuristic(start);
        searchNanos = 0;
        searching = true;
    }

    private static boolean step(long deadline) {
        int expanded = 0;
        while (!open.isEmpty()) {
            // Checked often: a cell here reads up to 32 map columns for its neighbours.
            if ((++expanded & 7) == 0 && System.nanoTime() > deadline) return false;
            int c = open.pop();
            if (closed[c]) continue;
            closed[c] = true;
            if (c == goal) {
                best = c;
                break;
            }
            float h = heuristic(c);
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
                if (nh == BLOCKED) continue;
                if (deep[n] && (!swimming || c != start && !deep[c])) continue; // swum only out of the water the player is in
                float length = k < 4 ? STEP : STEP * 1.4142f;
                float step;
                if (nh == FOG) {
                    step = length * FOG_COST;
                } else {
                    int climb = ch == FOG || ch == BLOCKED ? 0 : Math.abs(nh - ch);
                    if (climb > MAX_CLIMB) continue;
                    // The near path's costs, scaled to cells four times as long.
                    step = length + Math.max(0, climb - FREE_CLIMB) * Math.max(0, climb - FREE_CLIMB) * 0.375f + Math.max(0, nh - startY - HILL) * 2.4f + (shallow[n] ? 8f : deep[n] ? 16f : 0f);
                }
                float g = cost[c] + step;
                if (g < cost[n]) {
                    cost[n] = g;
                    from[n] = c;
                    open.push(n, g + heuristic(n));
                }
            }
        }
        return true;
    }
    private static final int[] NX = {1, -1, 0, 0, 1, 1, -1, -1}, NZ = {0, 0, 1, -1, 1, -1, 1, -1};

    private static float heuristic(int c) {
        int ax = Math.abs(c % SIDE - goalX), az = Math.abs(c / SIDE - goalZ);
        return STEP * (Math.max(ax, az) + 0.4142f * Math.min(ax, az));
    }

    private static int cellOf(double x, double z) {
        int cx = Math.max(0, Math.min(SIDE - 1, (int) Math.floor((x - originX) / STEP)));
        int cz = Math.max(0, Math.min(SIDE - 1, (int) Math.floor((z - originZ) / STEP)));
        return cz * SIDE + cx;
    }

    /**
     * Height of a cell from four of its columns on the map, looked up once: their average over the walkable ones;
     * BLOCKED when at least half of the known ones are deep water or lava; FOG when none is known.
     */
    private static int ground(int c) {
        int h = height[c];
        if (h != UNKNOWN) return h;
        int x0 = originX + (c % SIDE) * STEP, z0 = originZ + (c / SIDE) * STEP;
        shallow[c] = false;
        deep[c] = false;
        MapTile tile = store.peek(MapView.blockToTile(x0), MapView.blockToTile(z0));
        if (tile == null) return height[c] = FOG;
        int lx0 = x0 & (MapTile.SIZE - 1), lz0 = z0 & (MapTile.SIZE - 1);
        int known = 0, blocked = 0, walkable = 0, sum = 0, water = 0, lava = 0, deepSum = 0;
        for (int s = 0; s < 4; s++) {
            int lx = lx0 + (s & 1) * 4 + 2, lz = lz0 + (s >> 1) * 4 + 2;
            byte f = tile.flags(lx, lz);
            if ((f & MapTile.FLAG_KNOWN) == 0) continue;
            known++;
            if ((f & MapTile.FLAG_IMPASSABLE) != 0) {
                blocked++;
                // Deep water is impassable water; lava is impassable without the water flag.
                if ((f & MapTile.FLAG_WATER) == 0) lava++;
                else deepSum += tile.height(lx, lz);
                continue;
            }
            if ((f & MapTile.FLAG_WATER) != 0) water++;
            walkable++;
            sum += tile.height(lx, lz);
        }
        if (known == 0) return height[c] = FOG;
        if (2 * blocked >= known || walkable == 0) {
            if (lava > 0) return height[c] = BLOCKED;
            // Deep water: swum only from the start or other deep water (see deep).
            deep[c] = true;
            return height[c] = Math.round(deepSum / (float) blocked) + 1;
        }
        shallow[c] = water > 0;
        return height[c] = Math.round(sum / (float) walkable) + 1;
    }

    /**
     * The path from the start along the found cells, smoothed into soft arcs ({@link PathSmoother}), with points every
     * SPACING blocks, each marked whether it lies in the fog.
     */
    private static void build(int end) {
        int n = 0;
        for (int c = end; c != -1 && n < CELL_PATH.length; c = from[c]) n++;
        int i = n;
        for (int c = end; c != -1 && i > 0; c = from[c]) CELL_PATH[--i] = c;
        // Raw points: the cell centres, from the player's own position.
        for (int k = 0; k < n; k++) {
            int c = CELL_PATH[k];
            RAW[2 * k] = originX + (c % SIDE) * STEP + STEP / 2f;
            RAW[2 * k + 1] = originZ + (c / SIDE) * STEP + STEP / 2f;
        }
        if (n > 0) {
            RAW[0] = startPX;
            RAW[1] = startPZ;
        }
        // Soft arcs through a control point every ARC cells, kept clear of blocked ground (the fog is not blocked).
        int m = PathSmoother.smooth(RAW, n, ARC, FarPath::blockedAt, SMOOTH);
        System.arraycopy(SMOOTH, 0, RAW, 0, 2 * m);
        pointCount = 0;
        version++;
        generation++;
        float along = 0, next = 0;
        for (int k = 0; k + 1 < m; k++) {
            float ax = RAW[2 * k], az = RAW[2 * k + 1], bx = RAW[2 * k + 2], bz = RAW[2 * k + 3];
            float len = (float) Math.hypot(bx - ax, bz - az);
            if (len < 1e-4f) continue;
            while (next <= along + len) {
                if (pointCount == fogPoint.length) {
                    points = Arrays.copyOf(points, points.length * 2);
                    fogPoint = Arrays.copyOf(fogPoint, fogPoint.length * 2);
                }
                float f = (next - along) / len, x = ax + (bx - ax) * f, z = az + (bz - az) * f;
                points[2 * pointCount] = x;
                points[2 * pointCount + 1] = z;
                fogPoint[pointCount] = ground(cellOf(x, z)) == FOG;
                pointCount++;
                next += SPACING;
            }
            along += len;
        }
    }

    private static final int[] CELL_PATH = new int[4096];
    private static final float[] RAW = new float[2 * 8192], SMOOTH = new float[2 * 8192];
    /** Grid cells between the control points of the smoothed path (24 blocks). */
    private static final int ARC = 3;

    private static boolean blockedAt(float x, float z) {
        return ground(cellOf(x, z)) == BLOCKED;
    }
}
