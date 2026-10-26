package net.tapacywka.teyvatcartographia.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.tapacywka.teyvatcartographia.compat.ModCompat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A scrolling list that dissolves at its top and bottom edges (user request 25.10.2026): the screen is copied once
 * after the panel under the list is drawn and before its rows ({@link #capture}); after the rows, {@link #draw} lays
 * that copy back over the edges, more opaque towards the edge, so the rows fade into exactly the panel they sit on
 * (its blurred backdrop and its translucent body together). The game leaves the screen's alpha anywhere between 0 and
 * 1, so the copy's alpha is set to 1 and the opacity comes from the vertex colours: both edges in one draw, a few
 * bands each following a smoothstep.
 */
public final class EdgeFade {
    private EdgeFade() {}

    /** Bands per edge: the smoothstep drawn as straight pieces (8 is closer than a screen's 1/255 steps can show). */
    private static final int BANDS = 8;

    private static TextureTarget under;
    private static boolean ready;

    /** Copies the screen as it is now (the panel without its rows). */
    public static void capture(GuiGraphics g) {
        if (ModCompat.vulkan()) return; // no frame copy there: the rows are cut at the edges
        g.flush();
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (under == null) {
            under = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            under.setFilterMode(GL11.GL_NEAREST);
        } else if (under.width != main.width || under.height != main.height) {
            under.resize(main.width, main.height, Minecraft.ON_OSX);
            under.setFilterMode(GL11.GL_NEAREST);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, under.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, under.frameBufferId);
        RenderSystem.colorMask(false, false, false, true); // the copy opaque, its colours kept
        RenderSystem.clearColor(0f, 0f, 0f, 1f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        main.bindWrite(true);
        ready = true;
    }

    /**
     * The edges of the list between {@code top} and {@code bottom} (x from {@code x0} to {@code x1}) faded over
     * {@code height} GUI units: the top one at strength {@code topAmount}, the bottom one at {@code bottomAmount}
     * (0 to 1: 0 while nothing lies beyond that edge, growing as the list scrolls away from it).
     */
    public static void draw(GuiGraphics g, float x0, float x1, float top, float bottom, float height, float topAmount, float bottomAmount) {
        if (!ready || under == null || topAmount <= 0 && bottomAmount <= 0) return;
        g.flush();
        var w = Minecraft.getInstance().getWindow();
        float gw = (float) (w.getWidth() / w.getGuiScale()), gh = (float) (w.getHeight() / w.getGuiScale());
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Draw.beginTex();
        for (int i = 0; i < BANDS; i++) {
            float f0 = (float) i / BANDS, f1 = (float) (i + 1) / BANDS; // 0 at the edge, 1 where the fade ends
            if (topAmount > 0) band(b, m, x0, x1, top + f0 * height, top + f1 * height, gw, gh,
                    alpha(f0, topAmount), alpha(f1, topAmount));
            if (bottomAmount > 0) band(b, m, x0, x1, bottom - f0 * height, bottom - f1 * height, gw, gh,
                    alpha(f0, bottomAmount), alpha(f1, bottomAmount));
        }
        Draw.endTex(b, under.getColorTextureId());
    }

    /** The copy's opacity {@code f} of the way from the edge: a smoothstep, full at the edge. */
    private static int alpha(float f, float amount) {
        float t = 1 - f;
        return Math.round(t * t * (3 - 2 * t) * amount * 255) << 24 | 0xFFFFFF;
    }

    /** One band from {@code ya} (opacity {@code ca}) to {@code yb} ({@code cb}). */
    private static void band(BufferBuilder b, Matrix4f m, float x0, float x1, float ya, float yb, float gw, float gh, int ca, int cb) {
        float u0 = x0 / gw, u1 = x1 / gw, va = 1 - ya / gh, vb = 1 - yb / gh;
        Draw.vtx(b, m, x0, ya, u0, va, ca); Draw.vtx(b, m, x0, yb, u0, vb, cb); Draw.vtx(b, m, x1, yb, u1, vb, cb);
        Draw.vtx(b, m, x0, ya, u0, va, ca); Draw.vtx(b, m, x1, yb, u1, vb, cb); Draw.vtx(b, m, x1, ya, u1, va, ca);
    }

    /** Forgets the copy for this frame (the screen is done drawing its list). */
    public static void release() {
        ready = false;
    }

    /** Frees the copy (the screen is closed): a whole screen's worth of video memory. */
    public static void dispose() {
        ready = false;
        if (under != null) {
            under.destroyBuffers();
            under = null;
        }
    }
}
