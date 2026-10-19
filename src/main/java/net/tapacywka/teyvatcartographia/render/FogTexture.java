package net.tapacywka.teyvatcartographia.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;

import java.io.InputStream;
import java.util.Optional;

/**
 * "Fog of war" backdrop for unexplored space: the theme's seamlessly tiling {@code fog.png} (soft dark clouds with
 * sparse faint stars in the built-in theme), loaded once per theme and drawn as one repeating quad.
 */
public final class FogTexture {
    private FogTexture() {}

    private static DynamicTexture texture;

    private static int textureId() {
        if (texture == null) {
            NativeImage img = themeImage();
            texture = new DynamicTexture(img != null ? img : plain());
            texture.setFilter(true, false);
            // Repeat, so the whole screen is one quad with texture coordinates beyond 0..1.
            texture.bind();
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
        return texture.getId();
    }

    /**
     * The theme's {@code fog.png} (any size, tiles seamlessly), else the default {@code minecraft} theme's (like icons), or
     * null for a plain fill.
     */
    private static NativeImage themeImage() {
        ResourceLocation loc = Theme.asset("fog.png");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (res.isEmpty() && !Theme.id().equals(Theme.DEFAULT)) {
            loc = Theme.asset(Theme.DEFAULT, "fog.png");
            res = Minecraft.getInstance().getResourceManager().getResource(loc);
        }
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) {
            return NativeImage.read(NativeImage.Format.RGBA, in);
        } catch (Exception e) {
            TeyvatCartographia.LOGGER.warn("Could not read fog texture {}", loc, e);
            return null;
        }
    }

    /** Frees the texture; it is loaded again (for the current theme) on the next draw. */
    public static void invalidate() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }

    /** Fallback when no fog.png can be read: a plain fill in the theme's {@link Theme#FOG_BASE} colour. */
    private static NativeImage plain() {
        NativeImage img = new NativeImage(4, 4, false);
        int c = Theme.FOG_BASE;
        img.fillRect(0, 0, 4, 4, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16)); // ARGB -> ABGR
        return img;
    }

    /**
     * Fills the rectangle with the fog; {@code offsetX/Y} scroll it (parallax with the map), {@code tile} is the
     * on-screen size of one texture repeat.
     */
    public static void draw(GuiGraphics g, float x0, float y0, float x1, float y1, float offsetX, float offsetY, float tile, int tint) {
        int id = textureId();
        float u0 = mod(offsetX, tile) / tile, v0 = mod(offsetY, tile) / tile;
        Draw.texturedQuad(g, id, x0, y0, x1, y1, u0, v0, u0 + (x1 - x0) / tile, v0 + (y1 - y0) / tile, tint);
    }

    private static float mod(float a, float m) {
        float r = a % m;
        return r < 0 ? r + m : r;
    }
}
