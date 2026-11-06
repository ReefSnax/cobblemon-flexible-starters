package dev.sharkwire.cobblemonflexiblestarters.mixin.client;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.client.gui.startselection.widgets.CategoryList;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;

import dev.sharkwire.cobblemonflexiblestarters.client.StarterSelectionState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds tray-aware colour states without replacing Cobblemon's scrollable category widget. */
@Mixin(value = CategoryList.Category.class, remap = false)
public class CategoryEntryMixin {

    private static final ResourceLocation SELECTED_SLOT =
            ResourceLocation.fromNamespaceAndPath("cobblemon_flexible_starters",
                    "textures/gui/starterselection/starter_selection_selected.png");
    private static final ResourceLocation GROUP_BLOCKED_SLOT =
            ResourceLocation.fromNamespaceAndPath("cobblemon_flexible_starters",
                    "textures/gui/starterselection/starter_selection_group_blocked.png");

    @Shadow @Final private RenderableStarterCategory category;
    @Shadow @Final private CategoryList this$0;
    @Unique private int cobblemonFlexibleStarters$renderSlotIndex;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void cobblemonFlexibleStarters$resetSlotIndex(GuiGraphics graphics, int entryIndex, int rowTop, int rowLeft,
                                             int rowWidth, int rowHeight, float delta, CallbackInfo ci) {
        this.cobblemonFlexibleStarters$renderSlotIndex = 0;
    }

    /**
     * Cobblemon's sixth blit in {@code Category.render} is the per-Pokemon 25x25 slot plate. By
     * swapping only that plate, portraits, scrolling, hit testing, and the native blue current
     * selection remain exactly Cobblemon's own implementation.
     */
    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/api/gui/GuiUtilsKt;blitk$default"
                    + "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;"
                    + "Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;"
                    + "Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;"
                    + "Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;"
                    + "Ljava/lang/Number;ZFILjava/lang/Object;)V", ordinal = 5),
            index = 1,
            remap = false
    )
    private ResourceLocation cobblemonFlexibleStarters$slotPlate(ResourceLocation vanillaPlate) {
        StarterSelectionScreen screen = ((CategoryListAccessor) (Object) this.this$0)
                .cobblemonFlexibleStarters$getStarterSelectionScreen();
        int index = this.cobblemonFlexibleStarters$renderSlotIndex++;
        // The active blue selection has absolute visual priority, including when it is already in
        // the tray or would otherwise be blocked by a cap.
        if (screen.getCurrentCategory() == this.category && screen.getCurrentSelection() == index) {
            return vanillaPlate;
        }
        return switch (StarterSelectionState.slotState(screen, this.category, index)) {
            case SELECTED -> SELECTED_SLOT;
            case GROUP_BLOCKED -> GROUP_BLOCKED_SLOT;
            case NORMAL -> vanillaPlate;
        };
    }
}
