package net.tapacywka.teyvatcartographia.map.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.tapacywka.teyvatcartographia.config.MapConfig;

import java.util.Map;

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

    /**
     * The theme's own colours per dimension, block by block (ARGB), from
     * {@code themes/<theme>/dimensions/<namespace>/<path>.json} ({@code ThemeLoader}); read by the chunk scans on worker
     * threads, so replaced whole.
     */
    private static volatile Map<ResourceKey<Level>, Map<Block, Integer>> dimensions = Map.of();

    public static void setDimensions(Map<ResourceKey<Level>, Map<Block, Integer>> palettes) {
        dimensions = Map.copyOf(palettes);
    }

    /** The theme's palette of this dimension, or null when it has none. */
    public static Map<Block, Integer> dimension(ResourceKey<Level> dimension) {
        return dimensions.get(dimension);
    }

    /** Whether the map shows the colours of the block textures ({@link MapConfig.Palette#ORIGINAL}) instead of this palette. */
    public static boolean original() {
        return MapConfig.get(MapConfig.PALETTE) == MapConfig.Palette.ORIGINAL;
    }

    /** The mute and the warmth the palette was made with ({@link #soften}), in the settings 25% and 15%. */
    static final int BASE_MUTE = 25, BASE_WARMTH = 15;
    /** The warm beige the palette leans to. */
    private static final int HAZE_R = 0xB0, HAZE_G = 0xAC, HAZE_B = 0x94;

    /**
     * The player's mute and warmth (Settings > Cartography > Palette), percent, or null when they are the ones the
     * palette was made with (then nothing is changed). Read once per mapped chunk.
     */
    public static int[] tuning() {
        int mute = MapConfig.get(MapConfig.PALETTE_MUTE), warmth = MapConfig.get(MapConfig.PALETTE_WARMTH);
        return mute == BASE_MUTE && warmth == BASE_WARMTH ? null : new int[] {mute, warmth};
    }

    /**
     * A colour of this palette as if it had been made with another mute and warmth: the palette's warmth is taken back
     * out, the saturation scaled from the palette's mute to the new one (around the colour's luminance, which the mute
     * keeps), and the new warmth put in. The hand-picked colours change the same way as the computed ones.
     */
    public static int tune(int argb, int mute, int warmth) {
        float w0 = BASE_WARMTH / 100f, w = warmth / 100f;
        float s = (100 - mute) / (float) (100 - BASE_MUTE);
        float r = (((argb >> 16) & 0xFF) - HAZE_R * w0) / (1 - w0);
        float g = (((argb >> 8) & 0xFF) - HAZE_G * w0) / (1 - w0);
        float b = ((argb & 0xFF) - HAZE_B * w0) / (1 - w0);
        float lum = 0.3f * r + 0.59f * g + 0.11f * b;
        r = lum + (r - lum) * s;
        g = lum + (g - lum) * s;
        b = lum + (b - lum) * s;
        r = r * (1 - w) + HAZE_R * w;
        g = g * (1 - w) + HAZE_G * w;
        b = b * (1 - w) + HAZE_B * w;
        return (argb & 0xFF000000) | clamp(Math.round(r)) << 16 | clamp(Math.round(g)) << 8 | clamp(Math.round(b));
    }

    /** Plains grass and leaves, ocean water: the tints of the samples. */
    private static final int PLAINS_GRASS = 0x91BD59, PLAINS_FOLIAGE = 0x77AB2F, OCEAN_WATER = 0x3F76E4;

    /**
     * Samples of the map colours for the settings, as the map draws them now: plains grass, plains leaves, sand,
     * stone, ocean water and snow, in the palette and tuning chosen (a texture not read yet: its Teyvat colour).
     */
    public static int[] samples() {
        int[] teyvat = {tinted(GRASS, PLAINS_GRASS, 0.85f), scale(tinted(FOLIAGE, PLAINS_FOLIAGE, 0.8f), 0.92f), of(MapColor.SAND),
                of(MapColor.STONE), water(OCEAN_WATER), of(MapColor.SNOW)};
        if (original()) {
            int[] tex = {texture(Blocks.GRASS_BLOCK, PLAINS_GRASS), texture(Blocks.OAK_LEAVES, PLAINS_FOLIAGE), texture(Blocks.SAND, 0),
                    texture(Blocks.STONE, 0), TextureColors.water() == 0 ? 0 : TextureColors.multiply(TextureColors.water(), OCEAN_WATER),
                    TextureColors.snow()};
            for (int i = 0; i < tex.length; i++) if (tex[i] != 0) teyvat[i] = tex[i];
            return teyvat;
        }
        int[] t = tuning();
        if (t != null) for (int i = 0; i < teyvat.length; i++) teyvat[i] = tune(teyvat[i], t[0], t[1]);
        return teyvat;
    }

    /** A block's texture colour with its biome tint ({@code tint}, used only if the block takes one), or 0. */
    private static int texture(Block block, int tint) {
        TextureColors.Entry e = TextureColors.get(block);
        if (e == null) return 0;
        return e.tint() == TextureColors.Tint.NONE ? e.argb() : TextureColors.multiply(e.argb(), tint);
    }

    public static int of(MapColor mc) {
        return mc.id >= 0 && mc.id < TABLE.length ? TABLE[mc.id] : soften(0xFF000000 | mc.col);
    }

    /** Pulls a raw vanilla colour toward the soft painted palette: less saturation, slightly warm. */
    public static int soften(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float lum = 0.3f * r + 0.59f * g + 0.11f * b;
        r = (int) (r + (lum - r) * 0.25f);
        g = (int) (g + (lum - g) * 0.25f);
        b = (int) (b + (lum - b) * 0.25f);
        r = (int) (r * 0.85f + HAZE_R * 0.15f);
        g = (int) (g * 0.85f + HAZE_G * 0.15f);
        b = (int) (b * 0.85f + HAZE_B * 0.15f);
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
