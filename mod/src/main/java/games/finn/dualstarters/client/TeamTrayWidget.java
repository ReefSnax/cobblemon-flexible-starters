package games.finn.dualstarters.client;

import java.util.List;
import java.util.function.Supplier;

import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.CobblemonRenderable;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The "your team" tray: one native starter slot per allowed pick, docked to the left of
 * Cobblemon's starter panel.
 *
 * <p>Every pixel of chrome here is a Cobblemon asset used the way Cobblemon uses it. The tray body
 * is the {@code selection_container} header/edge/body/footer stack that {@code CategoryList} draws
 * for each category, and each slot is the same 25x25 {@code starter_selection(_active)} plate the
 * category grid uses, with the portrait drawn by the native
 * {@code PokemonGuiUtilsKt.drawProfilePokemon} at the same transform the grid uses. The container
 * art is 89px wide, so it is two-sliced down to the tray width (see
 * {@link NativeGui#blitTwoSlice}).</p>
 *
 * <p>State readout: a slot is empty (idle plate), filled (active plate + portrait + 1-based order
 * number) or filled-and-most-recent (active plate's bright row), which is the entry Undo removes.
 * A pick made from a category's "random" slot has no species to render, so it shows Cobblemon's
 * own {@code platform_unknown} glyph exactly as the category grid does.</p>
 */
public class TeamTrayWidget extends AbstractWidget implements CobblemonRenderable {

    private static final ResourceLocation HEADER =
            NativeGui.cobblemon("textures/gui/starterselection/selection_container_header.png");
    private static final ResourceLocation EDGE =
            NativeGui.cobblemon("textures/gui/starterselection/selection_container_edge.png");
    private static final ResourceLocation CONTAINER =
            NativeGui.cobblemon("textures/gui/starterselection/selection_container.png");
    private static final ResourceLocation FOOTER =
            NativeGui.cobblemon("textures/gui/starterselection/selection_container_footer.png");
    private static final ResourceLocation SLOT =
            NativeGui.cobblemon("textures/gui/starterselection/starter_selection.png");
    private static final ResourceLocation SLOT_ACTIVE =
            NativeGui.cobblemon("textures/gui/starterselection/starter_selection_active.png");
    private static final ResourceLocation UNKNOWN =
            NativeGui.cobblemon("textures/gui/pokedex/platform_unknown.png");

    /** Native container art width; everything is two-sliced from it. */
    private static final int CONTAINER_TEX_W = 89;
    /** Native CategoryList constants. */
    private static final int HEADER_H = 7;
    private static final int SLOT_SIZE = 25;
    private static final int SLOT_GAP = 3;
    /** 5px side margin, matching the category grid's inset inside its 89px container. */
    private static final int SIDE_MARGIN = 5;

    public static final int WIDTH = SLOT_SIZE + 2 * SIDE_MARGIN;

    private final int max;
    private final Supplier<List<RenderablePokemon>> picks;
    /** One reusable animation state per slot; Cobblemon models are stateful across frames. */
    private final FloatingState[] states;

    public TeamTrayWidget(int x, int y, int max, Supplier<List<RenderablePokemon>> picks) {
        super(x, y, WIDTH, heightFor(max), CommonComponents.EMPTY);
        this.max = max;
        this.picks = picks;
        this.states = new FloatingState[max];
        for (int i = 0; i < max; i++) {
            this.states[i] = new FloatingState();
        }
    }

    /** Body height uses CategoryList's own row formula so the tray stacks like a category does. */
    private static int bodyHeight(int max) {
        return SLOT_SIZE * max + SLOT_GAP * (max + 1);
    }

    public static int heightFor(int max) {
        // header + edge + body + edge + footer, the exact stack CategoryList.Category renders.
        return HEADER_H + 1 + bodyHeight(max) + 1 + 1;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        PoseStack pose = context.pose();
        List<RenderablePokemon> chosen = this.picks.get();
        int x = this.getX();
        int y = this.getY();
        int body = bodyHeight(this.max);

        NativeGui.blitTwoSlice(pose, HEADER, x, y, HEADER_H, WIDTH, 0, CONTAINER_TEX_W, HEADER_H, 1.0F);
        NativeGui.text(context, null,
                TextKt.bold(Component.translatable("dualstarters.screen.team",
                        chosen.size(), this.max)),
                x + WIDTH / 2.0, y + 2.0, 0.5F, (WIDTH - 4) * 2, NativeGui.WHITE, true, true);

        NativeGui.blitTwoSlice(pose, EDGE, x, y + HEADER_H, 1, WIDTH, 0, CONTAINER_TEX_W, 1, 1.0F);
        NativeGui.blitTwoSlice(pose, CONTAINER, x, y + HEADER_H + 1, body, WIDTH, 0,
                CONTAINER_TEX_W, body, 1.0F);
        NativeGui.blitTwoSlice(pose, EDGE, x, y + HEADER_H + 1 + body, 1, WIDTH, 0,
                CONTAINER_TEX_W, 1, 1.0F);
        NativeGui.blitTwoSlice(pose, FOOTER, x, y + HEADER_H + 2 + body, 1, WIDTH, 0,
                CONTAINER_TEX_W, 1, 1.0F);

        for (int slot = 0; slot < this.max; slot++) {
            int slotX = x + SIDE_MARGIN;
            int slotY = y + HEADER_H + 1 + SLOT_GAP + slot * (SLOT_SIZE + SLOT_GAP);
            boolean filled = slot < chosen.size();
            boolean newest = filled && slot == chosen.size() - 1;

            // starter_selection*.png is 25x50: idle row at v=0, bright row at v=25. The bright row
            // of the active plate marks the most recent pick - the one Undo takes back.
            NativeGui.blit(pose, filled ? SLOT_ACTIVE : SLOT, slotX, slotY, SLOT_SIZE, SLOT_SIZE,
                    0, newest ? SLOT_SIZE : 0, SLOT_SIZE, SLOT_SIZE * 2);

            if (!filled) {
                continue;
            }

            RenderablePokemon pokemon = chosen.get(slot);
            if (pokemon == null) {
                // A "random" pick: no species is known client-side, so show the same unknown glyph
                // the native category grid shows for its random slot.
                // platform_unknown.png is 390x444; the native grid draws it at 0.04 scale.
                NativeGui.blitScaled(pose, UNKNOWN, slotX + 4.5, slotY + 3.5, 444, 390, 0.04F);
            } else {
                drawPortrait(pose, pokemon, this.states[slot], slotX, slotY);
            }

            NativeGui.text(context, CobblemonResources.INSTANCE.getDEFAULT_LARGE(),
                    TextKt.bold(Component.literal(String.valueOf(slot + 1))),
                    slotX + 21.0, slotY + 17.0, 0.5F, 12, NativeGui.WHITE, true, true);
        }
    }

    /** The exact transform CategoryList.Category uses for its 25x25 grid portraits. */
    private static void drawPortrait(PoseStack pose, RenderablePokemon pokemon,
                                     FloatingState state, int slotX, int slotY) {
        pose.pushPose();
        pose.translate(slotX + 12.5, slotY - 6.0, 0.0);
        pose.scale(3.5F, 3.5F, 1.0F);
        // drawProfilePokemon conjugates the rotation it is handed, so it needs a fresh one.
        Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(
                new Quaternionf(), new Vector3f(13.0F, 35.0F, 0.0F));
        PokemonGuiUtilsKt.drawProfilePokemon(pokemon, pose, rotation, PoseType.PROFILE, state,
                0.0F, 4.5F, ProfileTransformType.PROFILE, false,
                1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 13);
        pose.popPose();
    }

    /** Purely a readout - it must never swallow clicks aimed at the screen behind it. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
