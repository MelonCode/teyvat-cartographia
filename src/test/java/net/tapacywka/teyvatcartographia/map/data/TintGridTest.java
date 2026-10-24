package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The biome tint of the map's columns between the sampled quarts, also across chunk borders (no straight seams). */
class TintGridTest {
    private static final int A = 0x2040A0, B = 0x40D0E0;

    /** A biome edge on the border between chunks 0 and 1 (world x 16); {@code loaded} tells which chunks x are loaded. */
    private static ChunkScanner.TintSource edgeAt16(IntPredicate loaded) {
        return new ChunkScanner.TintSource() {
            @Override
            public int tint(int wx, int y, int wz) {
                return wx < 16 ? A : B;
            }

            @Override
            public boolean loaded(int cx, int cz) {
                return loaded.test(cx);
            }
        };
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    @Test
    void anEdgeOnTheChunkBorderIsAGradientNotAStep() {
        ChunkScanner.TintSource source = edgeAt16(cx -> true);
        ChunkScanner.TintGrid left = new ChunkScanner.TintGrid(source, 0, 0), right = new ChunkScanner.TintGrid(source, 1, 0);
        int[] g = new int[32];
        for (int x = 0; x < 16; x++) {
            g[x] = green(left.at(x, 64, 8));
            g[16 + x] = green(right.at(x, 64, 8));
        }
        int quarter = (green(B) - green(A)) / 4;
        for (int wx = 1; wx < 32; wx++) {
            assertTrue(g[wx] >= g[wx - 1], "the tint only grows towards the second biome at x " + wx);
            assertTrue(g[wx] - g[wx - 1] <= quarter + 1, "no step larger than between two quarts at x " + wx
                    + " (" + g[wx - 1] + " to " + g[wx] + ")");
        }
        assertEquals(green(A), g[0], "far from the edge the first biome's tint");
        assertEquals(green(B), g[31], "far from the edge the second biome's tint");
    }

    @Test
    void anUnloadedNeighbourIsNotAsked() {
        // Chunk 1 not loaded: chunk 0 must not mix in what the game would say there (its default biome), but keep
        // its own edge.
        ChunkScanner.TintSource source = edgeAt16(cx -> cx != 1);
        ChunkScanner.TintGrid left = new ChunkScanner.TintGrid(source, 0, 0);
        for (int x = 0; x < 16; x++) assertEquals(A, left.at(x, 64, 8), "column " + x);
    }
}
