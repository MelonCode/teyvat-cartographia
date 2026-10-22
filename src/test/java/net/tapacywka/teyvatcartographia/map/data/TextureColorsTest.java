package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void aTintMultipliesEachChannel() {
        // The grey grass texture of the game (about #939393) under a plains grass tint (#91BD59).
        assertEquals(0xFF536C33, TextureColors.multiply(0xFF939393, 0x91BD59));
        assertEquals(0xFF123456, TextureColors.multiply(0xFF123456, 0xFFFFFF), "white leaves the colour");
        assertEquals(0xFF000000, TextureColors.multiply(0xFF123456, 0x000000));
    }
}
