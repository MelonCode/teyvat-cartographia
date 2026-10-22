package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Original palette: a texture's colour is the average of its visible pixels, tinted channel by channel. */
class TextureColorsTest {
    /** NativeImage pixels are ABGR. */
    private static int abgr(int a, int r, int g, int b) {
        return a << 24 | b << 16 | g << 8 | r;
    }

    @Test
    void theAverageLeavesOutTransparentPixels() {
        int[] px = {abgr(255, 100, 200, 0), abgr(255, 200, 100, 50), abgr(0, 255, 255, 255), abgr(0, 0, 0, 0)};
        assertEquals(0xFF969619, TextureColors.average(px), "only the two visible pixels count, as ARGB");
    }

    @Test
    void aFullyTransparentTextureHasNoColour() {
        assertEquals(0, TextureColors.average(new int[] {abgr(0, 10, 20, 30)}));
        assertEquals(0, TextureColors.average(new int[0]));
    }

    @Test
    void theColourDistanceTellsCloseColoursFromOtherOnes() {
        assertEquals(0f, TextureColors.distance(0xFF808080, 0xFF808080), 0.01f);
        assertEquals(100f, TextureColors.distance(0xFF000000, 0xFFFFFFFF), 0.5f, "black to white");
        // Basalt's grey texture against the black map colour the game gives it: drawn from the texture.
        assertTrue(TextureColors.distance(0xFF505055, 0xFF191919) > TextureColors.OFF_MAP);
        // Stone against its own map colour: the map colour stays.
        assertTrue(TextureColors.distance(0xFF7D7D7D, 0xFF707070) < TextureColors.OFF_MAP);
    }

    @Test
    void aTintMultipliesEachChannel() {
        // The grey grass texture of the game (about #939393) under a plains grass tint (#91BD59).
        assertEquals(0xFF536C33, TextureColors.multiply(0xFF939393, 0x91BD59));
        assertEquals(0xFF123456, TextureColors.multiply(0xFF123456, 0xFFFFFF), "white leaves the colour");
        assertEquals(0xFF000000, TextureColors.multiply(0xFF123456, 0x000000));
    }
}
