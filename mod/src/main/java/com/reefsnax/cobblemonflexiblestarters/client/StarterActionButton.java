package com.reefsnax.cobblemonflexiblestarters.client;

import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.CobblemonRenderable;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * A button drawn with Cobblemon's own starter-selection plate.
 *
 * <p>Cobblemon's {@code SelectionButton} is {@code final} and hard-codes both its 106px width and
 * its "I choose you!" label, so it cannot be subclassed or relabelled for the Choose/Undo/Confirm
 * row this add-on needs. This class is the minimum workaround: it reuses the native
 * {@code choose_button.png} texture (including its hover row), the native
 * {@code CobblemonResources.DEFAULT_LARGE} font and the native
 * {@code drawScaledText}/{@code blitk} draw calls, and only adds a variable width (via
 * {@link NativeGui#blitTwoSlice}) and a disabled state.</p>
 */
public class StarterActionButton extends Button implements CobblemonRenderable {

    /** Native plate: 106x28, two 14px-tall rows - idle at v=0, hovered at v=14. */
    private static final ResourceLocation TEXTURE =
            NativeGui.cobblemon("textures/gui/starterselection/choose_button.png");
    private static final int TEX_W = 106;
    private static final int TEX_H = 28;

    /** Native {@code SelectionButton.BUTTON_HEIGHT}. */
    public static final int HEIGHT = 14;

    private static final int DISABLED_COLOUR = 0xFF8A8A8A;
    private static final float DISABLED_ALPHA = 0.45F;

    private final MutableComponent label;

    public StarterActionButton(int x, int y, int width, MutableComponent label, OnPress onPress) {
        super(x, y, Math.min(width, TEX_W), HEIGHT, (Component) label, onPress, DEFAULT_NARRATION);
        this.label = label;
    }

    /** Cobblemon's buttons are silent; their actions play {@code GUI_CLICK} themselves. */
    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        PoseStack pose = context.pose();
        boolean highlighted = this.active && this.isHovered();
        float alpha = this.active ? 1.0F : DISABLED_ALPHA;

        NativeGui.blitTwoSlice(pose, TEXTURE, this.getX(), this.getY(), HEIGHT, this.width,
                highlighted ? HEIGHT : 0, TEX_W, TEX_H, alpha);

        // Same baseline as the native SelectionButton (y + 7 - 5), centred on the plate.
        // maxCharacterWidth keeps any translation inside the plate instead of bleeding past it.
        NativeGui.text(context, CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                TextKt.bold(this.label.copy()),
                this.getX() + this.width / 2.0, this.getY() + 2.0,
                1.0F, this.width - 8,
                this.active ? NativeGui.WHITE : DISABLED_COLOUR, true, true);
    }
}
