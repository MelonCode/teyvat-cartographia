package net.tapacywka.teyvatcartographia.map.data;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;

import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The colour of each block as its texture shows it, like Xaero's Minimap in its default mode: the average of the
 * non-transparent pixels of the texture on the block's top face. Read again after every resource reload: the models
 * are looked at on the main thread (a few milliseconds), the textures are read and averaged on a background thread.
 * A block whose top face is tinted (grass, leaves) keeps the tint to apply ({@link Tint}); a fixed tint (redstone,
 * stems, lily pads) is already in its colour. Blocks without a model (air, chests) have none: the usual map colour.
 */
public final class TextureColors {
    private TextureColors() {}

    /** Which biome tint a block's texture still needs. */
    public enum Tint { NONE, GRASS, FOLIAGE }

    /** One block: its texture colour (ARGB) and the biome tint to multiply it by. */
    public record Entry(int argb, Tint tint) {}

    private static volatile Map<Block, Entry> blocks = Map.of();
    private static volatile int water, lava, snow;
    /** A reload happened: read again once the game is out of its loading screen (the models are baked by then). */
    private static volatile boolean pending = true;
    /** New colours arrived: the map draws the loaded chunks again. */
    private static volatile boolean fresh;

    /** The texture colour of a block, or null without one. */
    public static Entry get(Block block) {
        return blocks.get(block);
    }

    /** The water, lava and snow textures (ARGB), 0 until read. */
    public static int water() {
        return water;
    }

    public static int lava() {
        return lava;
    }

    public static int snow() {
        return snow;
    }

    /** Called after a resource reload (packs, F3+T). */
    public static void reloaded() {
        pending = true;
    }

    /**
     * Once per client tick: starts the reading after a reload, and returns true once when new colours arrived (the
     * caller maps the loaded chunks again).
     */
    public static boolean tick() {
        Minecraft mc = Minecraft.getInstance();
        if (pending && mc.getOverlay() == null) {
            pending = false;
            start(mc);
        }
        if (fresh) {
            fresh = false;
            return true;
        }
        return false;
    }

    private record Found(ResourceLocation texture, Tint tint, int fixedTint) {}

    private static void start(Minecraft mc) {
        RandomSource random = RandomSource.create(42L);
        Map<Block, Found> found = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            try {
                Found f = top(mc, block.defaultBlockState(), random);
                if (f != null) found.put(block, f);
            } catch (RuntimeException e) {
                // A model that cannot answer outside a world: the block keeps its map colour.
            }
        }
        ResourceManager rm = mc.getResourceManager();
        Util.backgroundExecutor().execute(() -> {
            Map<ResourceLocation, Integer> averages = new HashMap<>();
            Map<Block, Entry> out = new IdentityHashMap<>();
            for (Map.Entry<Block, Found> e : found.entrySet()) {
                Found f = e.getValue();
                int argb = averages.computeIfAbsent(f.texture(), t -> read(rm, t));
                if (argb == 0) continue;
                if (f.fixedTint() != -1) argb = multiply(argb, f.fixedTint());
                out.put(e.getKey(), new Entry(argb, f.tint()));
            }
            water = averages.computeIfAbsent(ResourceLocation.withDefaultNamespace("block/water_still"), t -> read(rm, t));
            lava = averages.computeIfAbsent(ResourceLocation.withDefaultNamespace("block/lava_still"), t -> read(rm, t));
            snow = averages.computeIfAbsent(ResourceLocation.withDefaultNamespace("block/snow"), t -> read(rm, t));
            blocks = out;
            fresh = true;
            TeyvatCartographia.LOGGER.info("Teyvat Cartographia: texture colours of {} blocks ({} textures)", out.size(), averages.size());
        });
    }

    /** The texture on a block's top face and its tint, or null for a block drawn without a model. */
    private static Found top(Minecraft mc, BlockState state, RandomSource random) {
        if (state.getRenderShape() != RenderShape.MODEL) return null;
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        BakedQuad quad = first(model.getQuads(state, Direction.UP, random), true);
        if (quad == null) quad = first(model.getQuads(state, null, random), true);
        if (quad == null) quad = first(model.getQuads(state, null, random), false); // plants: their crossed planes
        TextureAtlasSprite sprite = quad != null ? quad.getSprite() : model.getParticleIcon();
        if (sprite == null) return null;
        int index = quad != null ? quad.getTintIndex() : -1;
        Tint tint = Tint.NONE;
        int fixed = -1;
        if (index >= 0) {
            MapColor mc0 = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (state.is(Blocks.SPRUCE_LEAVES)) fixed = 0x619961;
            else if (state.is(Blocks.BIRCH_LEAVES)) fixed = 0x80A755;
            else if (state.is(BlockTags.LEAVES) || mc0 == MapColor.PLANT && !GRASSY.contains(state.getBlock())) tint = Tint.FOLIAGE;
            else if (mc0 == MapColor.GRASS || GRASSY.contains(state.getBlock())) tint = Tint.GRASS;
            else fixed = fixedTint(mc, state, index);
        }
        ResourceLocation name = sprite.contents().name();
        return name.equals(MissingTextureAtlasSprite.getLocation()) ? null : new Found(name, tint, fixed);
    }

    /** The tint a block gives itself without a world (-1: none, or it needs a world). */
    private static int fixedTint(Minecraft mc, BlockState state, int index) {
        try {
            return mc.getBlockColors().getColor(state, null, null, index);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Plants that take the grass tint (their map colour says plant). */
    private static final Set<Block> GRASSY = Set.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.SUGAR_CANE);

    private static BakedQuad first(List<BakedQuad> quads, boolean upOnly) {
        for (BakedQuad q : quads) if (!upOnly || q.getDirection() == Direction.UP) return q;
        return null;
    }

    /** The average colour of a texture ({@code block/stone}: {@code textures/block/stone.png}), 0 if unreadable. */
    private static int read(ResourceManager rm, ResourceLocation texture) {
        ResourceLocation file = texture.withPath(p -> "textures/" + p + ".png");
        Optional<Resource> res = rm.getResource(file);
        if (res.isEmpty()) return 0;
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth(), h = img.getHeight();
            int frame = h > w && h % w == 0 ? w : h; // an animated texture: its first frame
            int[] abgr = new int[w * frame];
            for (int y = 0; y < frame; y++) for (int x = 0; x < w; x++) abgr[y * w + x] = img.getPixelRGBA(x, y);
            return average(abgr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * The average of the non-transparent pixels (NativeImage ABGR) as an opaque ARGB colour, or 0 when all are
     * transparent.
     */
    static int average(int[] abgr) {
        long r = 0, g = 0, b = 0;
        int n = 0;
        for (int p : abgr) {
            if ((p >>> 24) == 0) continue;
            r += p & 0xFF;
            g += (p >> 8) & 0xFF;
            b += (p >> 16) & 0xFF;
            n++;
        }
        if (n == 0) return 0;
        return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** A colour multiplied by a tint channel by channel, as the game tints a texture (ARGB, the tint's alpha ignored). */
    public static int multiply(int argb, int tint) {
        int r = ((argb >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
        int g = ((argb >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
        int b = (argb & 0xFF) * (tint & 0xFF) / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
