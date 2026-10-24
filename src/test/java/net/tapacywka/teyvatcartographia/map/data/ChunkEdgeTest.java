package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which sides of a chunk a changed column lies on ({@link MapTile#edgeBits}): the neighbours there are mapped again, as
 * they were shaded with the old edge. Land levelled across chunk borders left a dark line along half of them
 * (24.10.2026). The bits follow {@code MapDataManager.DIRS}: north, south, west, east.
 */
class ChunkEdgeTest {
    @Test
    void aColumnInsideTouchesNoSide() {
        assertEquals(0, MapTile.edgeBits(7, 7));
        assertEquals(0, MapTile.edgeBits(1, 14));
    }

    @Test
    void edgeColumnsTouchTheirSide() {
        assertEquals(1, MapTile.edgeBits(7, 0), "north");
        assertEquals(2, MapTile.edgeBits(7, 15), "south");
        assertEquals(4, MapTile.edgeBits(0, 7), "west");
        assertEquals(8, MapTile.edgeBits(15, 7), "east");
    }

    @Test
    void cornersTouchTwoSides() {
        assertEquals(1 | 4, MapTile.edgeBits(0, 0));
        assertEquals(1 | 8, MapTile.edgeBits(15, 0));
        assertEquals(2 | 4, MapTile.edgeBits(0, 15));
        assertEquals(2 | 8, MapTile.edgeBits(15, 15));
    }
}
