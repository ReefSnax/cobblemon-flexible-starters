package dev.sharkwire.cobblemonflexiblestarters.mixin.client;

import java.util.List;

import com.cobblemon.mod.common.client.gui.VariableObjectSelectionList;
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.client.gui.startselection.widgets.CategoryList;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corrects the row budget Cobblemon reserves for a starter category.
 *
 * <p>{@code CategoryList.createEntries} sizes each entry from {@code pokemon.size()} alone, while
 * {@code Category.render}/{@code getButtonHovered} lay out {@code pokemon.size() + (randomStarter
 * ? 1 : 0)} slots. For any category whose starter count is a multiple of three and which offers a
 * random slot, the extra row is drawn outside the entry's reserved height - it overlaps the next
 * category and {@code getEntryAtPosition} never matches it, so the random slot is unclickable.</p>
 *
 * <p>This add-on makes that random slot a real, selectable pick on arbitrary datapack-provided
 * starter lists, so the budget is recomputed here with the same formula {@code render} uses, and
 * the list's stacking and total height are rebuilt through its own public API. Categories that do
 * not hit the mismatch keep exactly the height Cobblemon gave them.</p>
 */
@Mixin(value = CategoryList.class, remap = false)
public class CategoryListMixin {

    private static final int SLOT_SIZE = 25;
    private static final int SLOT_GAP = 3;
    private static final int COLUMNS = 3;
    /** Header (7) plus the trailing padding Cobblemon adds to every entry. */
    private static final int ENTRY_CHROME = 7 + 3;

    @Inject(
            method = "<init>(IILjava/util/List;IILnet/minecraft/client/Minecraft;"
                    + "Lcom/cobblemon/mod/common/client/gui/startselection/StarterSelectionScreen;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void cobblemonFlexibleStarters$fixRandomStarterRow(int paneWidth, int paneHeight,
                                                  List<RenderableStarterCategory> categories,
                                                  int listX, int listY, Minecraft minecraft,
                                                  StarterSelectionScreen screen, CallbackInfo ci) {
        VariableObjectSelectionList list = (VariableObjectSelectionList) (Object) this;

        // createEntries() maps categories to Category entries in order, so walking the entries and
        // the source list together pairs them without reaching into Category's private state.
        int categoryIndex = 0;
        boolean changed = false;
        for (VariableObjectSelectionList.Entry entry : list.getChildren()) {
            if (!(entry instanceof CategoryList.Category)) {
                continue;
            }
            if (categoryIndex >= categories.size()) {
                break;
            }
            RenderableStarterCategory category = categories.get(categoryIndex++);
            int slots = category.getPokemon().size() + (category.getRandomStarter() ? 1 : 0);
            int rows = (slots - 1) / COLUMNS + 1;
            int height = SLOT_SIZE * rows + SLOT_GAP * (rows + 1) + ENTRY_CHROME;
            if (entry.getHeight() != height) {
                entry.setHeight(height);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        int total = 0;
        for (VariableObjectSelectionList.Entry entry : list.getChildren()) {
            total += entry.getHeight();
        }
        list.setListHeight(total);
        list.updatePositions();
    }
}
