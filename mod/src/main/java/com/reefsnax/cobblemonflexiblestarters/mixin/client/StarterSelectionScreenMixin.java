package com.reefsnax.cobblemonflexiblestarters.mixin.client;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.client.gui.startselection.widgets.SelectionButton;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;

import com.reefsnax.cobblemonflexiblestarters.Config;
import com.reefsnax.cobblemonflexiblestarters.client.ClientStarterDetails;
import com.reefsnax.cobblemonflexiblestarters.client.NativeGui;
import com.reefsnax.cobblemonflexiblestarters.client.StarterActionButton;
import com.reefsnax.cobblemonflexiblestarters.client.StarterSelectionState;
import com.reefsnax.cobblemonflexiblestarters.client.TeamTrayWidget;
import com.reefsnax.cobblemonflexiblestarters.net.SelectStartersPacket;
import com.reefsnax.cobblemonflexiblestarters.net.StarterDetailsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns Cobblemon's own starter screen into a multi-pick screen, in place.
 *
 * <p>Cobblemon's {@code StarterSelectionScreen} is {@code final}, so this add-on augments it with a
 * mixin rather than replacing it. Everything the native screen owns is left untouched and keeps
 * working: the 239x197 frame and its layered chrome, the animated poke-ball backdrop, the lit
 * {@code ModelWidget} preview and its type-coloured platform, the species name / dex number /
 * type icon, the pokedex blurb (already hard-clipped by Cobblemon to five lines inside a fixed
 * box), the scrolling {@code CategoryList} + {@code VariableObjectSelectionScrollbar} (which is
 * what makes arbitrary category/starter counts work), the native {@code ExitButton}, and the
 * unblurred world background.</p>
 *
 * <p>Exactly one native behaviour is replaced: the {@code SelectionButton} that fires
 * {@code SelectStarterPacket} for a single pick and closes. It is removed <em>by type</em> after
 * {@code init} (rather than by intercepting its synthetic lambda, which would be name-fragile) and
 * replaced with a Choose / Undo / Confirm row in the same footprint. Picks accumulate in
 * {@link #cobblemonFlexibleStarters$picks} and are submitted once, together, through this add-on's own
 * server-validated {@link SelectStartersPacket}.</p>
 *
 * <p>The additions are a left-docked {@link TeamTrayWidget} showing the chosen team in native
 * starter slots, and a ball/nature readout drawn into the previously empty strip under the
 * category list. Both are built from Cobblemon assets and draw calls.</p>
 */
@Mixin(value = StarterSelectionScreen.class, remap = false)
public abstract class StarterSelectionScreenMixin extends Screen {

    /** Native {@code StarterSelectionScreen.BASE_WIDTH/BASE_HEIGHT}; both are private constants. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$BASE_WIDTH = 239;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$BASE_HEIGHT = 197;

    /** Native Choose-button footprint: x + 13, y + 180, 106 x 14. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$BUTTON_ROW_X = 13;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$BUTTON_ROW_Y = 180;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$GAP = 2;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$CHOOSE_W = 40;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$UNDO_W = 24;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$CONFIRM_W = 38;

    /** Empty strip under the category list, left of the native ExitButton at x + 210. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$DETAIL_X = 135;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$DETAIL_Y = 178;
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$DETAIL_LINE = 7;
    /** 0.5-scale text, so the raw-width budget is twice the ~70px of strip available. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$DETAIL_MAX = 140;
    /** Center of Cobblemon's native Pokemon preview viewport. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$PREVIEW_CENTER_X = 65;
    /** Top of the preview viewport, below the native name / dex-number chrome. */
    @Unique private static final int COBBLEMON_FLEXIBLE_STARTERS$FEEDBACK_Y = 22;

    @Shadow @Final private List<RenderableStarterCategory> categories;
    @Shadow private int currentSelection;
    @Shadow public RenderableStarterCategory currentCategory;

    private StarterSelectionScreenMixin(Component title) {
        super(title);
    }

    /** Ordered picks, exactly as they will be sent. Survives init() re-runs (window resize). */
    @Unique private List<SelectStartersPacket.Pick> cobblemonFlexibleStarters$picks;
    /** Parallel to {@link #cobblemonFlexibleStarters$picks}; null entries are "random within category" picks. */
    @Unique private List<RenderablePokemon> cobblemonFlexibleStarters$previews;
    @Unique private int cobblemonFlexibleStarters$max;
    @Unique private StarterActionButton cobblemonFlexibleStarters$chooseButton;
    @Unique private StarterActionButton cobblemonFlexibleStarters$undoButton;
    @Unique private StarterActionButton cobblemonFlexibleStarters$confirmButton;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void cobblemonFlexibleStarters$addMultiPickControls(CallbackInfo ci) {
        if (this.categories == null || this.categories.isEmpty()) {
            // Cobblemon logs and adds nothing in this case; there is nothing to augment.
            return;
        }
        if (this.cobblemonFlexibleStarters$picks == null) {
            this.cobblemonFlexibleStarters$picks = new ArrayList<>();
            this.cobblemonFlexibleStarters$previews = new ArrayList<>();
        }
        // Server-authoritative count: SERVER-type configs are synced to the client on connect, and
        // StarterGrantHandler re-validates the submitted size regardless of what we show here.
        this.cobblemonFlexibleStarters$max = Config.starterCount();
        while (this.cobblemonFlexibleStarters$picks.size() > this.cobblemonFlexibleStarters$max) {
            this.cobblemonFlexibleStarters$picks.remove(this.cobblemonFlexibleStarters$picks.size() - 1);
            this.cobblemonFlexibleStarters$previews.remove(this.cobblemonFlexibleStarters$previews.size() - 1);
        }
        StarterSelectionState.update((StarterSelectionScreen) (Object) this, this.cobblemonFlexibleStarters$previews);

        List<GuiEventListener> existing = new ArrayList<>(this.children());
        for (GuiEventListener child : existing) {
            if (child instanceof SelectionButton) {
                this.removeWidget(child);
            }
        }

        int x = (this.width - COBBLEMON_FLEXIBLE_STARTERS$BASE_WIDTH) / 2;
        int y = (this.height - COBBLEMON_FLEXIBLE_STARTERS$BASE_HEIGHT) / 2;
        int row = y + COBBLEMON_FLEXIBLE_STARTERS$BUTTON_ROW_Y;
        int chooseX = x + COBBLEMON_FLEXIBLE_STARTERS$BUTTON_ROW_X;
        int undoX = chooseX + COBBLEMON_FLEXIBLE_STARTERS$CHOOSE_W + COBBLEMON_FLEXIBLE_STARTERS$GAP;
        int confirmX = undoX + COBBLEMON_FLEXIBLE_STARTERS$UNDO_W + COBBLEMON_FLEXIBLE_STARTERS$GAP;

        this.cobblemonFlexibleStarters$chooseButton = this.addRenderableWidget(new StarterActionButton(
                chooseX, row, COBBLEMON_FLEXIBLE_STARTERS$CHOOSE_W,
                Component.translatable("cobblemon_flexible_starters.screen.choose"),
                button -> this.cobblemonFlexibleStarters$addCurrentPick()));
        this.cobblemonFlexibleStarters$undoButton = this.addRenderableWidget(new StarterActionButton(
                undoX, row, COBBLEMON_FLEXIBLE_STARTERS$UNDO_W,
                Component.translatable("cobblemon_flexible_starters.screen.undo"),
                button -> this.cobblemonFlexibleStarters$undoLastPick()));
        this.cobblemonFlexibleStarters$confirmButton = this.addRenderableWidget(new StarterActionButton(
                confirmX, row, COBBLEMON_FLEXIBLE_STARTERS$CONFIRM_W,
                Component.translatable("cobblemon_flexible_starters.screen.confirm"),
                button -> this.cobblemonFlexibleStarters$confirm()));

        // Docked left of the native frame. Minecraft guarantees a scaled width of at least 320, so
        // frame (239) + tray (35) + gap still fits; the clamp only guards exotic scale overrides.
        int trayHeight = TeamTrayWidget.heightFor(this.cobblemonFlexibleStarters$max);
        int trayX = Math.max(1, x - TeamTrayWidget.WIDTH - 4);
        int trayY = y + (COBBLEMON_FLEXIBLE_STARTERS$BASE_HEIGHT - trayHeight) / 2;
        this.addRenderableWidget(new TeamTrayWidget(
                trayX, trayY, this.cobblemonFlexibleStarters$max, () -> this.cobblemonFlexibleStarters$previews));

        this.cobblemonFlexibleStarters$refreshButtons();
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cobblemonFlexibleStarters$renderSelectionDetails(GuiGraphics context, int mouseX, int mouseY,
                                                     float delta, CallbackInfo ci) {
        if (this.categories == null || this.categories.isEmpty() || this.currentCategory == null) {
            return;
        }
        this.cobblemonFlexibleStarters$refreshButtons(); // selection changes are native-screen state, so refresh every frame.
        int x = (this.width - COBBLEMON_FLEXIBLE_STARTERS$BASE_WIDTH) / 2;
        int y = (this.height - COBBLEMON_FLEXIBLE_STARTERS$BASE_HEIGHT) / 2;
        NativeGui.text(context, null, Component.literal("Select up to " + this.cobblemonFlexibleStarters$max
                        + " Pokémon to join you on your journey!"),
                x + COBBLEMON_FLEXIBLE_STARTERS$BASE_WIDTH / 2.0, y - 18, 0.80F, 290, NativeGui.WHITE, true, true);
        StringBuilder limits = new StringBuilder("Special Limits: ");
        boolean hasEnabledGroup = false;
        for (com.reefsnax.cobblemonflexiblestarters.StarterSelectionRules.Group group : Config.groups()) {
            if (!group.enabled()) continue;
            if (hasEnabledGroup) limits.append(", ");
            limits.append(StarterSelectionState.groupLabel(group)).append(" (")
                    .append(StarterSelectionState.count(group)).append('/').append(group.max()).append(')');
            hasEnabledGroup = true;
        }
        if (hasEnabledGroup) {
            NativeGui.text(context, null, Component.literal(limits.toString()),
                    x + COBBLEMON_FLEXIBLE_STARTERS$BASE_WIDTH / 2.0, y - 10, 0.65F, 350, NativeGui.WHITE, true, true);
        }
        String violation = this.cobblemonFlexibleStarters$currentViolation();
        if (violation != null) {
            int colour = "Selected".equals(violation) ? 0xFF79E38A : 0xFFFFA0A0;
            // Keep feedback inside the upper preview viewport: visible while browsing, clear of
            // the native description, category list, and the Ball/Nature footer. The compact
            // scale accommodates messages such as "Pseudo-Legendary limit reached" without spill.
            NativeGui.text(context, null, Component.literal(violation),
                    x + COBBLEMON_FLEXIBLE_STARTERS$PREVIEW_CENTER_X, y + COBBLEMON_FLEXIBLE_STARTERS$FEEDBACK_Y,
                    0.65F, 180, colour, true, true);
        }

        // Ball/nature are whatever the server said the configured PokemonProperties hold; an empty
        // value means the property is genuinely unset, which Cobblemon rolls at grant time.
        StarterDetailsPacket.Entry entry = ClientStarterDetails.get(
                this.currentCategory.getName(), this.currentSelection);
        MutableComponent ball = Component.translatable("cobblemon_flexible_starters.screen.ball",
                cobblemonFlexibleStarters$ballName(entry == null ? null : entry.ball()));
        MutableComponent nature = Component.translatable("cobblemon_flexible_starters.screen.nature",
                cobblemonFlexibleStarters$natureName(entry == null ? null : entry.nature()));

        // Replace Cobblemon's fixed top-left Poké Ball (rendered at x+4/y+3) with the actual
        // configured ball.  This is the same native item sprite layer, at the same half-scale,
        // so it stays where the player expects instead of adding a second ball near the controls.
        NativeGui.blitScaled(context.pose(), cobblemonFlexibleStarters$topLeftBallTexture(entry == null ? null : entry.ball()),
                x + 4, y + 3, 16, 16, 0.5F);
        NativeGui.text(context, null, ball, x + COBBLEMON_FLEXIBLE_STARTERS$DETAIL_X, y + COBBLEMON_FLEXIBLE_STARTERS$DETAIL_Y,
                0.5F, COBBLEMON_FLEXIBLE_STARTERS$DETAIL_MAX, NativeGui.WHITE, false, true);
        NativeGui.text(context, null, nature, x + COBBLEMON_FLEXIBLE_STARTERS$DETAIL_X,
                y + COBBLEMON_FLEXIBLE_STARTERS$DETAIL_Y + COBBLEMON_FLEXIBLE_STARTERS$DETAIL_LINE,
                0.5F, COBBLEMON_FLEXIBLE_STARTERS$DETAIL_MAX, NativeGui.WHITE, false, true);
    }

    /** Adds whatever the native screen currently has selected, including a "random" slot. */
    @Unique
    private void cobblemonFlexibleStarters$addCurrentPick() {
        if (this.currentCategory == null || this.cobblemonFlexibleStarters$picks.size() >= this.cobblemonFlexibleStarters$max
                || this.cobblemonFlexibleStarters$currentViolation() != null) {
            return;
        }
        List<RenderablePokemon> pokemon = this.currentCategory.getPokemon();
        int index = this.currentSelection;
        if (index < 0 || index > pokemon.size()) {
            return;
        }
        // index == size is Cobblemon's "random within this category" slot; the server resolves it.
        boolean random = index == pokemon.size();
        if (random && !this.currentCategory.getRandomStarter()) {
            return;
        }
        this.cobblemonFlexibleStarters$picks.add(
                new SelectStartersPacket.Pick(this.currentCategory.getName(), index));
        this.cobblemonFlexibleStarters$previews.add(random ? null : pokemon.get(index));
        StarterSelectionState.update((StarterSelectionScreen) (Object) this, this.cobblemonFlexibleStarters$previews);
        this.cobblemonFlexibleStarters$playClick();
        this.cobblemonFlexibleStarters$refreshButtons();
    }

    @Unique
    private void cobblemonFlexibleStarters$undoLastPick() {
        if (this.cobblemonFlexibleStarters$picks.isEmpty()) {
            return;
        }
        this.cobblemonFlexibleStarters$picks.remove(this.cobblemonFlexibleStarters$picks.size() - 1);
        this.cobblemonFlexibleStarters$previews.remove(this.cobblemonFlexibleStarters$previews.size() - 1);
        StarterSelectionState.update((StarterSelectionScreen) (Object) this, this.cobblemonFlexibleStarters$previews);
        this.cobblemonFlexibleStarters$playClick();
        this.cobblemonFlexibleStarters$refreshButtons();
    }

    @Unique
    private void cobblemonFlexibleStarters$confirm() {
        if (this.cobblemonFlexibleStarters$picks.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(
                new SelectStartersPacket(List.copyOf(this.cobblemonFlexibleStarters$picks)));
        this.cobblemonFlexibleStarters$playClick();
        this.onClose();
    }

    @Unique
    private void cobblemonFlexibleStarters$refreshButtons() {
        int chosen = this.cobblemonFlexibleStarters$picks.size();
        this.cobblemonFlexibleStarters$chooseButton.active = chosen < this.cobblemonFlexibleStarters$max
                && this.cobblemonFlexibleStarters$currentViolation() == null;
        this.cobblemonFlexibleStarters$undoButton.active = chosen > 0;
        this.cobblemonFlexibleStarters$confirmButton.active = chosen > 0;
    }

    /** Client polish only; StarterGrantHandler remains the authoritative validation. */
    @Unique
    private String cobblemonFlexibleStarters$currentViolation() {
        return StarterSelectionState.violation(this.currentCategory, this.currentSelection);
    }

    /** Cobblemon's UI click, matching every other control on this screen. */
    @Unique
    private void cobblemonFlexibleStarters$playClick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F));
    }

    /** Resolves the 16px native item sprite that Cobblemon places next to the species name. */
    @Unique
    private static ResourceLocation cobblemonFlexibleStarters$topLeftBallTexture(String raw) {
        String path = "poke_ball";
        if (raw != null && !raw.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(raw.indexOf(':') >= 0 ? raw : "cobblemon:" + raw);
            if (id != null && PokeBalls.getPokeBall(id) != null) {
                path = id.getPath();
            }
        }
        return NativeGui.cobblemon("textures/item/poke_balls/" + path + ".png");
    }

    @Unique
    private static Component cobblemonFlexibleStarters$ballName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.translatable("cobblemon_flexible_starters.screen.random");
        }
        ResourceLocation id = ResourceLocation.tryParse(raw.indexOf(':') >= 0 ? raw : "cobblemon:" + raw);
        if (id != null) {
            PokeBall ball = PokeBalls.getPokeBall(id);
            if (ball != null && ball.item != null) {
                return Component.translatable(ball.item.getDescriptionId());
            }
        }
        return Component.literal(raw);
    }

    @Unique
    private static Component cobblemonFlexibleStarters$natureName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.translatable("cobblemon_flexible_starters.screen.random");
        }
        // 1.8.4's Natures.getNature(String) always applies the Cobblemon namespace itself.  The
        // configured property can already be stored as cobblemon:modest, so pass only its path to
        // avoid constructing the invalid identifier cobblemon:cobblemon:modest.
        ResourceLocation id = ResourceLocation.tryParse(raw);
        String lookup = id == null ? raw : id.getPath();
        Nature nature = Natures.getNature(lookup);
        return nature == null ? Component.literal(raw)
                : Component.translatable(nature.getDisplayName());
    }
}
