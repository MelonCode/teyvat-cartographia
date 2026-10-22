package net.tapacywka.teyvatcartographia.map.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.tapacywka.teyvatcartographia.compat.ModCompat;

import java.util.Map;

/**
 * Stage 1-2 of the pipeline: reads one loaded client chunk on a worker thread and produces a compact,
 * immutable {@link Snapshot} (surface height, painted base colour, water depth, biome). Only chunks already
 * present in the client chunk cache are read; nothing is loaded or generated. Like the vanilla section compile,
 * it reads blocks, biome tints and block light off the main thread; a read that fails because the chunk changed
 * meanwhile throws, and the chunk is mapped again later.
 */
public final class ChunkScanner {
    private ChunkScanner() {}

    /** Immutable sampled data of one chunk, safe to hand to worker threads. */
    public record Snapshot(int cx, int cz, int[] color, short[] height, byte[] waterDepth, boolean[] isVoid, byte[] blockLight, ResourceKey<Biome> biome) {}

    private static final int MAX_WATER_DEPTH = 24;
    /** {@link Snapshot#waterDepth} of a lava column (no water there, but not walkable either). */
    public static final byte LAVA = -1;

    public static Snapshot scan(ClientLevel level, LevelChunk chunk, boolean ceiling) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int[] color = new int[256];
        short[] height = new short[256];
        byte[] depth = new byte[256];
        boolean[] isVoid = new boolean[256];
        int minY = level.getMinBuildHeight();
        int ceilingStart = Math.min(level.getMaxBuildHeight() - 1, minY + level.dimensionType().logicalHeight() - 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        TintGrid water = new TintGrid(BiomeColors.WATER_COLOR_RESOLVER), grass = new TintGrid(BiomeColors.GRASS_COLOR_RESOLVER),
                foliageTint = new TintGrid(BiomeColors.FOLIAGE_COLOR_RESOLVER);
        // The colours of the block textures: the Original palette everywhere, the mod's own one (softened) in worlds whose
        // map colours look nothing like them (the Aether). The theme's own colours for this dimension's blocks come first.
        boolean original = MapPalette.original();
        boolean textured = original || ModCompat.texturedWorld(level.dimension());
        Map<Block, Integer> own = original ? null : MapPalette.dimension(level.dimension());

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = z * 16 + x;
                int wx = (cx << 4) + x, wz = (cz << 4) + z;
                int y = ceiling ? findCeilingSurface(chunk, pos, wx, wz, ceilingStart, minY)
                        : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z); // already the Y of the top block
                BlockState state = Blocks.AIR.defaultBlockState();
                MapColor mc = MapColor.NONE;
                // Skip invisible blocks (glass, barriers, ...) down to something with a map colour.
                for (int guard = 0; y >= minY && guard < 32; guard++, y--) {
                    pos.set(wx, y, wz);
                    state = chunk.getBlockState(pos);
                    mc = state.getMapColor(level, pos);
                    if (mc != MapColor.NONE || !state.getFluidState().isEmpty()) break;
                }
                if (y < minY) {
                    isVoid[i] = true;
                    height[i] = (short) minY;
                    continue;
                }
                height[i] = (short) y;
                FluidState fluid = state.getFluidState();
                if (fluid.is(FluidTags.WATER)) {
                    int d = 1;
                    while (d < MAX_WATER_DEPTH && y - d >= minY) {
                        pos.set(wx, y - d, wz);
                        if (!chunk.getBlockState(pos).getFluidState().is(FluidTags.WATER)) break;
                        d++;
                    }
                    depth[i] = (byte) d;
                    pos.set(wx, y, wz);
                    int tint = water.at(level, cx, cz, x, y, z);
                    color[i] = original && TextureColors.water() != 0 ? TextureColors.multiply(TextureColors.water(), tint) : MapPalette.water(tint);
                    continue;
                }
                if (fluid.is(FluidTags.LAVA)) {
                    depth[i] = LAVA;
                    color[i] = original && TextureColors.lava() != 0 ? TextureColors.lava() : MapPalette.LAVA;
                    continue;
                }
                pos.set(wx, y + 1, wz);
                BlockState above = chunk.getBlockState(pos);
                pos.set(wx, y, wz);
                Integer mine = own == null ? null : own.get(state.getBlock());
                // The texture: always in a textured palette, else only where the block's map colour is far from it.
                TextureColors.Entry tex = TextureColors.get(state.getBlock());
                if (tex != null && !textured && !tex.offMap()) tex = null;
                if (above.getBlock() instanceof SnowLayerBlock) {
                    color[i] = original && TextureColors.snow() != 0 ? TextureColors.snow() : MapPalette.of(MapColor.SNOW);
                } else if (mine != null) {
                    color[i] = mine; // before the biome tints: the palette gives the block's whole colour
                } else if (tex != null) {
                    int c = tex.argb();
                    if (tex.tint() == TextureColors.Tint.GRASS) c = TextureColors.multiply(c, grass.at(level, cx, cz, x, y, z));
                    else if (tex.tint() == TextureColors.Tint.FOLIAGE) c = TextureColors.multiply(c, foliageTint.at(level, cx, cz, x, y, z));
                    color[i] = original ? c : MapPalette.soften(c);
                } else if (state.is(Blocks.GRASS_BLOCK) || mc == MapColor.GRASS) {
                    color[i] = MapPalette.tinted(MapPalette.GRASS, grass.at(level, cx, cz, x, y, z), 0.85f);
                } else if (state.is(BlockTags.LEAVES) && !isBiomeTinted(state)) {
                    // Cherry and azalea leaves keep their own colour (pink / green), no biome tint.
                    color[i] = MapPalette.of(mc);
                } else if (state.is(BlockTags.LEAVES) || mc == MapColor.PLANT) {
                    int foliage = state.is(Blocks.SPRUCE_LEAVES) ? 0x619961 : state.is(Blocks.BIRCH_LEAVES) ? 0x80A755
                            : foliageTint.at(level, cx, cz, x, y, z);
                    color[i] = MapPalette.scale(MapPalette.tinted(MapPalette.FOLIAGE, foliage, 0.8f), 0.92f);
                } else {
                    color[i] = MapPalette.of(mc);
                }
            }
        }
        // The player's mute and warmth of the mod's palette (none for the Original one).
        int[] tuning = original ? null : MapPalette.tuning();
        if (tuning != null) {
            for (int i = 0; i < 256; i++) if (!isVoid[i]) color[i] = MapPalette.tune(color[i], tuning[0], tuning[1]);
        }
        // Real block light (torches, lava, glowstone, lanterns...) just above the visible surface, 0..15,
        // as computed by the vanilla light engine. Sky light is ignored: the map shows artificial light only.
        byte[] blockLight = new byte[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = z * 16 + x;
                if (isVoid[i]) continue;
                pos.set((cx << 4) + x, height[i] + 1, (cz << 4) + z);
                int surface = level.getBrightness(LightLayer.BLOCK, pos);
                pos.set((cx << 4) + x, height[i], (cz << 4) + z);
                int self = chunk.getBlockState(pos).getLightEmission(); // emissive surface blocks (lava, glowstone)
                blockLight[i] = (byte) Math.max(surface, self);
            }
        }
        pos.set((cx << 4) + 8, height[8 * 16 + 8], (cz << 4) + 8);
        Holder<Biome> biome = level.getBiome(pos);
        return new Snapshot(cx, cz, color, height, depth, isVoid, blockLight, biome.unwrapKey().orElse(null));
    }

    /**
     * One biome tint (grass, foliage or water) of a chunk, sampled lazily at the centres of its 4 x 4 cells and
     * interpolated per column. The vanilla tint is already a blend over neighbouring biomes, so it changes smoothly;
     * asking it for every column took most of the time of mapping a chunk.
     */
    private static final class TintGrid {
        private final ColorResolver resolver;
        private final int[] cells = new int[16];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        /** Bit per cell already sampled. */
        private int sampled;

        TintGrid(ColorResolver resolver) {
            this.resolver = resolver;
        }

        int at(ClientLevel level, int cx, int cz, int x, int y, int z) {
            float fx = Math.max(0f, Math.min(3f, (x - 1.5f) / 4f)), fz = Math.max(0f, Math.min(3f, (z - 1.5f) / 4f));
            int x0 = Math.min(2, (int) fx), z0 = Math.min(2, (int) fz);
            float tx = fx - x0, tz = fz - z0;
            int c00 = cell(level, cx, cz, x0, z0, y), c10 = cell(level, cx, cz, x0 + 1, z0, y);
            int c01 = cell(level, cx, cz, x0, z0 + 1, y), c11 = cell(level, cx, cz, x0 + 1, z0 + 1, y);
            int r = mix(c00 >> 16, c10 >> 16, c01 >> 16, c11 >> 16, tx, tz);
            int g = mix(c00 >> 8, c10 >> 8, c01 >> 8, c11 >> 8, tx, tz);
            int b = mix(c00, c10, c01, c11, tx, tz);
            return (r << 16) | (g << 8) | b;
        }

        private int cell(ClientLevel level, int cx, int cz, int gx, int gz, int y) {
            int k = gz * 4 + gx;
            if ((sampled & (1 << k)) == 0) {
                pos.set((cx << 4) + gx * 4 + 2, y, (cz << 4) + gz * 4 + 2);
                cells[k] = level.getBlockTint(pos, resolver);
                sampled |= 1 << k;
            }
            return cells[k];
        }

        private static int mix(int c00, int c10, int c01, int c11, float tx, float tz) {
            float top = (c00 & 0xFF) + ((c10 & 0xFF) - (c00 & 0xFF)) * tx;
            float bottom = (c01 & 0xFF) + ((c11 & 0xFF) - (c01 & 0xFF)) * tx;
            return Math.round(top + (bottom - top) * tz);
        }
    }

    /** Leaves whose in-game colour comes from the biome foliage tint (others have a fixed texture colour). */
    private static boolean isBiomeTinted(BlockState s) {
        return !(s.is(Blocks.CHERRY_LEAVES) || s.is(Blocks.AZALEA_LEAVES) || s.is(Blocks.FLOWERING_AZALEA_LEAVES));
    }

    /** For dimensions with a ceiling (Nether): first floor below the roof. */
    private static int findCeilingSurface(LevelChunk chunk, BlockPos.MutableBlockPos pos, int wx, int wz, int start, int minY) {
        int y = start;
        while (y > minY && isSolid(chunk, pos.set(wx, y, wz))) y--;
        while (y > minY && !isSolid(chunk, pos.set(wx, y, wz))) y--;
        return y;
    }

    private static boolean isSolid(LevelChunk chunk, BlockPos pos) {
        BlockState s = chunk.getBlockState(pos);
        return !s.isAir() && (s.blocksMotion() || !s.getFluidState().isEmpty());
    }
}
