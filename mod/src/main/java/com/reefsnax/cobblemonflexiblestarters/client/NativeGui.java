package com.reefsnax.cobblemonflexiblestarters.client;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Thin, typed wrappers around the Cobblemon GUI primitives this add-on draws with.
 *
 * <p>Both {@code blitk} and {@code drawScaledText} are Kotlin functions with long
 * {@code @JvmOverloads}/default-argument tails; from Java the only safe way to call them is to pass
 * every parameter explicitly. These helpers do that once, so the screen code reads like the
 * decompiled Cobblemon sources it mirrors. Nothing here reimplements Cobblemon rendering - it is
 * all delegation.</p>
 */
public final class NativeGui {

    /** Fully opaque white, matching {@code drawScaledText}'s default {@code colour}. */
    public static final int WHITE = 0xFFFFFFFF;

    private NativeGui() {}

    /** Cobblemon asset path helper, equivalent to Cobblemon's {@code cobblemonResource(..)}. */
    public static ResourceLocation cobblemon(String path) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", path);
    }

    /**
     * {@code blitk} with an explicit source rectangle.
     *
     * <p>Note Cobblemon's argument order is (x, y, <b>height</b>, <b>width</b>, u, v, texW, texH) -
     * height before width - and that it maps the source rectangle 1:1 onto the destination (there
     * is no independent source/destination size), which is why
     * {@link #blitTwoSlice} exists.</p>
     */
    public static void blit(PoseStack pose, ResourceLocation texture,
                            double x, double y, double height, double width,
                            double u, double v, double textureWidth, double textureHeight) {
        blit(pose, texture, x, y, height, width, u, v, textureWidth, textureHeight, 1.0F);
    }

    /** As {@link #blit}, with an alpha multiplier (used for disabled button states). */
    public static void blit(PoseStack pose, ResourceLocation texture,
                            double x, double y, double height, double width,
                            double u, double v, double textureWidth, double textureHeight,
                            float alpha) {
        GuiUtilsKt.blitk(pose, texture, x, y, height, width, u, v, textureWidth, textureHeight,
                0, 1, 1, 1, alpha, true, 1.0F);
    }

    /** {@code blitk} at an explicit render scale, mirroring Cobblemon's small-icon blits. */
    public static void blitScaled(PoseStack pose, ResourceLocation texture,
                                  double x, double y, double height, double width, float scale) {
        GuiUtilsKt.blitk(pose, texture, x / scale, y / scale, height, width, 0, 0, width, height,
                0, 1, 1, 1, 1, true, scale);
    }

    /**
     * Draws a horizontally resizable native plate by blitting its left and right halves at 1:1
     * and letting them meet in the middle.
     *
     * <p>Cobblemon's own chrome textures ({@code choose_button}, {@code selection_container*}) are
     * uniform between their end caps, so this keeps the borders pixel-exact at any width instead of
     * stretching them - {@code blitk} cannot scale a source rect independently of the destination.
     * Only valid for {@code width <= textureWidth}.</p>
     */
    public static void blitTwoSlice(PoseStack pose, ResourceLocation texture,
                                    double x, double y, double height, int width,
                                    double v, double textureWidth, double textureHeight,
                                    float alpha) {
        int left = Math.max(1, width / 2);
        int right = Math.max(1, width - left);
        blit(pose, texture, x, y, height, left, 0, v, textureWidth, textureHeight, alpha);
        blit(pose, texture, x + left, y, height, right, textureWidth - right, v,
                textureWidth, textureHeight, alpha);
    }

    /**
     * {@code drawScaledText}. {@code maxCharacterWidth} is Cobblemon's own fit-to-box mechanism:
     * text wider than it is shrunk rather than allowed to overflow, which is how every label in
     * this add-on stays inside its bounds.
     */
    public static void text(GuiGraphics context, ResourceLocation font, MutableComponent text,
                            double x, double y, float scale, int maxCharacterWidth,
                            int colour, boolean centered, boolean shadow) {
        RenderHelperKt.drawScaledText(context, font, text, x, y, scale, 1.0F,
                maxCharacterWidth, colour, centered, shadow, null, null);
    }
}
