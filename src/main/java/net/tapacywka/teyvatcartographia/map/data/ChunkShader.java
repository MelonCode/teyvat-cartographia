package net.tapacywka.teyvatcartographia.map.data;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.config.MapConfig;

/**
 * Stage 3 of the pipeline (worker threads): pure function turning a chunk {@link ChunkScanner.Snapshot} plus a
 * 2-block border of neighbouring heights/flags into final pixels. Produces the painted look of the references:
 * relief shading, thin contour lines on height steps, a subtle shoreline on the land side and a depth gradient for water.
 */
public final class ChunkShader {
    private ChunkShader() {}

    public static final int PAD = 2;
    public static final int W = 16 + 2 * PAD;
    private static final int CONTOUR_STEP = 6;

    /** Neighbourhood input: heights and flags in a 20x20 grid, chunk at [PAD, PAD+16). */
    public record Neighbourhood(short[] heights, byte[] flags, int neighbourMask) {}

    /**
     * A shaded chunk. {@code merge}: only its known columns are written (a cave layer reveals a chunk bit by bit), the
     * others keep what the map had.
     */
    public record Result(int cx, int cz, int[] abgr, short[] heights, byte[] flags, byte[] light, int neighbourMask, ResourceKey<Biome> biome, boolean merge) {}

    public static int pidx(int x, int z) {
        return (z + PAD) * W + (x + PAD);
    }

    public static Result shade(ChunkScanner.Snapshot s, Neighbourhood n, MapConfig.Detail detail) {
        short[] H = n.heights();
        byte[] F = n.flags();
        int[] out = new int[256];
        byte[] flags = new byte[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = z * 16 + x;
                int p = pidx(x, z);
                flags[i] = F[p];
                if (s.isVoid()[i]) {
                    out[i] = 0;
                    continue;
                }
                int h = H[p];
                int color;
                if ((F[p] & MapTile.FLAG_WATER) != 0) {
                    float t = Math.min(1f, (s.waterDepth()[i] & 0xFF) / 12f);
                    t = (float) Math.pow(t, 0.7);
                    // Shallow water near the shore is already lighter through the depth gradient; no extra rim
                    // highlight (Genshin's coasts have none, and it made small swamp pools glow).
                    color = MapPalette.lerp(s.color()[i], MapPalette.deepWater(s.color()[i]), t);
                } else {
                    color = s.color()[i];
                    if (detail != MapConfig.Detail.LOW) {
                        int hN = known(F, pidx(x, z - 1)) ? H[pidx(x, z - 1)] : h;
                        int hW = known(F, pidx(x - 1, z)) ? H[pidx(x - 1, z)] : h;
                        float slope = Math.max(-6, Math.min(6, (h - hN) + (h - hW)));
                        float elev = Math.max(-0.3f, Math.min(1f, (h - 62) / 120f));
                        color = MapPalette.scale(color, 1f + slope * 0.035f + elev * 0.07f);
                    }
                    if (detail == MapConfig.Detail.HIGH && isStepTop(H, F, x, z, h)) {
                        color = MapPalette.scale(color, 0.82f);
                    }
                    if (touchesWater(F, x, z)) {
                        color = MapPalette.scale(color, 0.86f); // subtle shoreline, not a dark outline
                    }
                }
                // No soft fade at the explored edge here: it would be measured in blocks and change width with zoom.
                // The explored area simply ends at the chunk border.
                out[i] = MapPalette.toAbgr(color);
            }
        }
        short[] heights = new short[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) heights[z * 16 + x] = H[pidx(x, z)];
        return new Result(s.cx(), s.cz(), out, heights, flags, s.blockLight(), n.neighbourMask(), s.biome(), false);
    }

    private static boolean known(byte[] F, int p) {
        return (F[p] & MapTile.FLAG_KNOWN) != 0;
    }

    private static boolean isLand(byte[] F, int p) {
        return (F[p] & MapTile.FLAG_KNOWN) != 0 && (F[p] & MapTile.FLAG_WATER) == 0;
    }

    private static boolean isWater(byte[] F, int p) {
        return (F[p] & MapTile.FLAG_WATER) != 0;
    }

    private static boolean touchesWater(byte[] F, int x, int z) {
        return isWater(F, pidx(x + 1, z)) || isWater(F, pidx(x - 1, z)) || isWater(F, pidx(x, z + 1)) || isWater(F, pidx(x, z - 1));
    }

    /** True on the upper edge of a height step crossing a contour level (drawn as a thin dark line). */
    private static boolean isStepTop(short[] H, byte[] F, int x, int z, int h) {
        int level = Math.floorDiv(h, CONTOUR_STEP);
        return lower(H, F, pidx(x + 1, z), level) || lower(H, F, pidx(x - 1, z), level)
                || lower(H, F, pidx(x, z + 1), level) || lower(H, F, pidx(x, z - 1), level);
    }

    private static boolean lower(short[] H, byte[] F, int p, int level) {
        return isLand(F, p) && Math.floorDiv(H[p], CONTOUR_STEP) < level;
    }
}
