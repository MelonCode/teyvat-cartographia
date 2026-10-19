package net.tapacywka.teyvatcartographia.map.data;

import net.minecraft.world.level.material.MapColor;

/**
 * Converts Minecraft {@link MapColor}s into a soft, slightly desaturated "painted map" palette inspired by
 * the reference screenshots (olive greens, beige sand, warm greys, teal water). All colours ARGB.
 */
public final class MapPalette {
    private MapPalette() {}

    public static final int WATER_SHALLOW = 0xFF6CC2C6;
    public static final int WATER_DEEP = 0xFF1D5C74;
    public static final int GRASS = 0xFF93AC5C;
    public static final int FOLIAGE = 0xFF5F8A3C;
    public static final int LAVA = 0xFFF08A3A;

    private static final int[] TABLE = new int[64];

    static {
        for (int i = 0; i < TABLE.length; i++) {
            MapColor mc = MapColor.byId(i);
            TABLE[i] = soften(0xFF000000 | mc.col);
        }
        put(MapColor.GRASS, GRASS);
        put(MapColor.PLANT, FOLIAGE);
        put(MapColor.SAND, 0xFFDCCB98);
        put(MapColor.WOOL, 0xFFDAD6CC);
        put(MapColor.ICE, 0xFFB9DDE8);
        put(MapColor.SNOW, 0xFFF0F4F6);
        put(MapColor.CLAY, 0xFFA9AEBB);
        put(MapColor.DIRT, 0xFFA3845C);
        put(MapColor.STONE, 0xFFA09E96);
        put(MapColor.WATER, WATER_SHALLOW);
        put(MapColor.WOOD, 0xFFA8845A);
        put(MapColor.QUARTZ, 0xFFEDE7DA);
        put(MapColor.PODZOL, 0xFF84693F);
        put(MapColor.NETHER, 0xFF8E3F3A);
        put(MapColor.DEEPSLATE, 0xFF6F6F72);
        put(MapColor.COLOR_ORANGE, 0xFFD38C4A);
        put(MapColor.TERRACOTTA_ORANGE, 0xFFB0703F);
        put(MapColor.TERRACOTTA_WHITE, 0xFFD3BFA6);
        put(MapColor.TERRACOTTA_YELLOW, 0xFFC09A4E);
        put(MapColor.TERRACOTTA_BROWN, 0xFF6E5038);
        put(MapColor.TERRACOTTA_RED, 0xFF9A4E3E);
        put(MapColor.TERRACOTTA_LIGHT_GRAY, 0xFF9C8478);
        put(MapColor.CRIMSON_NYLIUM, 0xFFAE4844);
        put(MapColor.WARPED_NYLIUM, 0xFF3C9184);
        put(MapColor.WARPED_WART_BLOCK, 0xFF2E7F78);
        put(MapColor.COLOR_BROWN, 0xFF7B6048);
        put(MapColor.COLOR_BLACK, 0xFF3A3A42);
        put(MapColor.COLOR_GRAY, 0xFF66666C);
        put(MapColor.COLOR_LIGHT_GRAY, 0xFFA2A2A4);
        put(MapColor.FIRE, LAVA);
    }

    private static void put(MapColor mc, int argb) {
        TABLE[mc.id] = argb;
    }

    public static int of(MapColor mc) {
        return mc.id >= 0 && mc.id < TABLE.length ? TABLE[mc.id] : soften(0xFF000000 | mc.col);
    }

    /** Pulls a raw vanilla colour toward the soft painted palette: less saturation, slightly warm. */
    private static int soften(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float lum = 0.3f * r + 0.59f * g + 0.11f * b;
        r = (int) (r + (lum - r) * 0.25f);
        g = (int) (g + (lum - g) * 0.25f);
        b = (int) (b + (lum - b) * 0.25f);
        r = (int) (r * 0.85f + 0xB0 * 0.15f);
        g = (int) (g * 0.85f + 0xAC * 0.15f);
        b = (int) (b * 0.85f + 0x94 * 0.15f);
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /**
     * Shallow water in the biome's own water colour (swamp = murky green, warm ocean = turquoise, cold = deep
     * blue...), lightened and slightly softened to fit the painted palette.
     */
    public static int water(int biomeWater) {
        int base = lerp(0xFF000000 | (biomeWater & 0xFFFFFF), 0xFFFFFFFF, 0.22f);
        return lerp(WATER_SHALLOW, soften(base), 0.7f);
    }

    /** Deep variant of a (biome-tinted) water colour: darker, pulled a little towards the shared deep tone. */
    public static int deepWater(int shallow) {
        return lerp(scale(shallow, 0.45f), WATER_DEEP, 0.35f);
    }

    /** Biome-tinted vegetation: blends the biome tint with the painted base, then softens. */
    public static int tinted(int base, int biomeTint, float amount) {
        int t = 0xFF000000 | (biomeTint & 0xFFFFFF);
        return soften(lerp(base, t, amount));
    }

    public static int lerp(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int scale(int argb, float f) {
        int r = (int) (((argb >> 16) & 0xFF) * f), g = (int) (((argb >> 8) & 0xFF) * f), b = (int) ((argb & 0xFF) * f);
        return (argb & 0xFF000000) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** ARGB to NativeImage ABGR. */
    public static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }
}
