package dev.sharkwire.cobblemonflexiblestarters.mixin.client;

import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.client.gui.startselection.widgets.CategoryList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives the nested category-entry mixin its owning native starter screen. */
@Mixin(value = CategoryList.class, remap = false)
public interface CategoryListAccessor {

    @Accessor("starterSelectionScreen")
    StarterSelectionScreen cobblemonFlexibleStarters$getStarterSelectionScreen();
}
