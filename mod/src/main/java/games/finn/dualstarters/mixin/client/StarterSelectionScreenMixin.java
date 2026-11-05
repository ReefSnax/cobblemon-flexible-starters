package games.finn.dualstarters.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.client.gui.startselection.widgets.SelectionButton;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;

import games.finn.dualstarters.Config;
import games.finn.dualstarters.client.ClientStarterDetails;
import games.finn.dualstarters.client.NativeGui;
import games.finn.dualstarters.client.StarterActionButton;
import games.finn.dualstarters.client.TeamTrayWidget;
import games.finn.dualstarters.net.SelectStartersPacket;
import games.finn.dualstarters.net.StarterDetailsPacket;
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
 * {@link #dualstarters$picks} and are submitted once, together, through this add-on's own
 * server-validated {@link SelectStartersPacket}.</p>
 *
 * <p>The additions are a left-docked {@link TeamTrayWidget} showing the chosen team in native
 * starter slots, and a ball/nature readout drawn into the previously empty strip under the
 * category list. Both are built from Cobblemon assets and draw calls.</p>
 */
@Mixin(value = StarterSelectionScreen.class, remap = false)
public abstract class StarterSelectionScreenMixin extends Screen {

    /** Native {@code StarterSelectionScreen.BASE_WIDTH/BASE_HEIGHT}; both are private constants. */
    @Unique private static final int DUALSTARTERS$BASE_WIDTH = 239;
    @Unique private static final int DUALSTARTERS$BASE_HEIGHT = 197;

    /** Native Choose-button footprint: x + 13, y + 180, 106 x 14. */
    @Unique private static final int DUALSTARTERS$BUTTON_ROW_X = 13;
    @Unique private static final int DUALSTARTERS$BUTTON_ROW_Y = 180;
    @Unique private static final int DUALSTARTERS$GAP = 2;
    @Unique private static final int DUALSTARTERS$CHOOSE_W = 40;
    @Unique private static final int DUALSTARTERS$UNDO_W = 24;
    @Unique private static final int DUALSTARTERS$CONFIRM_W = 38;

    /** Empty strip under the category list, left of the native ExitButton at x + 210. */
    @Unique private static final int DUALSTARTERS$DETAIL_X = 135;
    @Unique private static final int DUALSTARTERS$DETAIL_Y = 178;
    @Unique private static final int DUALSTARTERS$DETAIL_LINE = 7;
    /** 0.5-scale text, so the raw-width budget is twice the ~70px of strip available. */
    @Unique private static final int DUALSTARTERS$DETAIL_MAX = 140;

    @Shadow @Final private List<RenderableStarterCategory> categories;
    @Shadow private int currentSelection;
    @Shadow public RenderableStarterCategory currentCategory;

    private StarterSelectionScreenMixin(Component title) {
        super(title);
    }

    /** Ordered picks, exactly as they will be sent. Survives init() re-runs (window resize). */
    @Unique private List<SelectStartersPacket.Pick> dualstarters$picks;
    /** Parallel to {@link #dualstarters$picks}; null entries are "random within category" picks. */
    @Unique private List<RenderablePokemon> dualstarters$previews;
    @Unique private int dualstarters$max;
    @Unique private StarterActionButton dualstarters$chooseButton;
    @Unique private StarterActionButton dualstarters$undoButton;
    @Unique private StarterActionButton dualstarters$confirmButton;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void dualstarters$addMultiPickControls(CallbackInfo ci) {
        if (this.categories == null || this.categories.isEmpty()) {
            // Cobblemon logs and adds nothing in this case; there is nothing to augment.
            return;
        }
        if (this.dualstarters$picks == null) {
            this.dualstarters$picks = new ArrayList<>();
            this.dualstarters$previews = new ArrayList<>();
        }

        // Server-authoritative count: SERVER-type configs are synced to the client on connect, and
        // StarterGrantHandler re-validates the submitted size regardless of what we show here.
        this.dualstarters$max = Config.starterCount();
        while (this.dualstarters$picks.size() > this.dualstarters$max) {
            this.dualstarters$picks.remove(this.dualstarters$picks.size() - 1);
            this.dualstarters$previews.remove(this.dualstarters$previews.size() - 1);
        }

        List<GuiEventListener> existing = new ArrayList<>(this.children());
        for (GuiEventListener child : existing) {
            if (child instanceof SelectionButton) {
                this.removeWidget(child);
            }
        }

        int x = (this.width - DUALSTARTERS$BASE_WIDTH) / 2;
        int y = (this.height - DUALSTARTERS$BASE_HEIGHT) / 2;
        int row = y + DUALSTARTERS$BUTTON_ROW_Y;
        int chooseX = x + DUALSTARTERS$BUTTON_ROW_X;
        int undoX = chooseX + DUALSTARTERS$CHOOSE_W + DUALSTARTERS$GAP;
        int confirmX = undoX + DUALSTARTERS$UNDO_W + DUALSTARTERS$GAP;

        this.dualstarters$chooseButton = this.addRenderableWidget(new StarterActionButton(
                chooseX, row, DUALSTARTERS$CHOOSE_W,
                Component.translatable("dualstarters.screen.choose"),
                button -> this.dualstarters$addCurrentPick()));
        this.dualstarters$undoButton = this.addRenderableWidget(new StarterActionButton(
                undoX, row, DUALSTARTERS$UNDO_W,
                Component.translatable("dualstarters.screen.undo"),
                button -> this.dualstarters$undoLastPick()));
        this.dualstarters$confirmButton = this.addRenderableWidget(new StarterActionButton(
                confirmX, row, DUALSTARTERS$CONFIRM_W,
                Component.translatable("dualstarters.screen.confirm"),
                button -> this.dualstarters$confirm()));

        // Docked left of the native frame. Minecraft guarantees a scaled width of at least 320, so
        // frame (239) + tray (35) + gap still fits; the clamp only guards exotic scale overrides.
        int trayHeight = TeamTrayWidget.heightFor(this.dualstarters$max);
        int trayX = Math.max(1, x - TeamTrayWidget.WIDTH - 4);
        int trayY = y + (DUALSTARTERS$BASE_HEIGHT - trayHeight) / 2;
        this.addRenderableWidget(new TeamTrayWidget(
                trayX, trayY, this.dualstarters$max, () -> this.dualstarters$previews));

        this.dualstarters$refreshButtons();
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void dualstarters$renderSelectionDetails(GuiGraphics context, int mouseX, int mouseY,
                                                     float delta, CallbackInfo ci) {
        if (this.categories == null || this.categories.isEmpty() || this.currentCategory == null) {
            return;
        }
        this.dualstarters$refreshButtons(); // selection changes are native-screen state, so refresh every frame.
        int x = (this.width - DUALSTARTERS$BASE_WIDTH) / 2;
        int y = (this.height - DUALSTARTERS$BASE_HEIGHT) / 2;
        StringBuilder limits = new StringBuilder("Total Pokémon selected: ")
                .append(this.dualstarters$picks.size()).append('/').append(this.dualstarters$max);
        for (games.finn.dualstarters.StarterSelectionRules.Group group : Config.groups()) {
            if (group.enabled()) limits.append("  |  ").append(dualstarters$groupLabel(group)).append(": ")
                    .append(this.dualstarters$countGroup(group)).append('/').append(group.max());
        }
        NativeGui.text(context, null, Component.literal(limits.toString()),
                x + DUALSTARTERS$BASE_WIDTH / 2.0, y - 11, 0.65F, 360, NativeGui.WHITE, true, true);
        String violation = this.dualstarters$currentViolation();
        if (violation != null) {
            NativeGui.text(context, null, Component.literal(violation), x + DUALSTARTERS$BASE_WIDTH / 2.0,
                    y + 22, 0.65F, 360, 0xFFFFA0A0, true, true);
        }

        // Ball/nature are whatever the server said the configured PokemonProperties hold; an empty
        // value means the property is genuinely unset, which Cobblemon rolls at grant time.
        StarterDetailsPacket.Entry entry = ClientStarterDetails.get(
                this.currentCategory.getName(), this.currentSelection);
        MutableComponent ball = Component.translatable("dualstarters.screen.ball",
                dualstarters$ballName(entry == null ? null : entry.ball()));
        MutableComponent nature = Component.translatable("dualstarters.screen.nature",
                dualstarters$natureName(entry == null ? null : entry.nature()));

        // Replace Cobblemon's fixed top-left Poké Ball (rendered at x+4/y+3) with the actual
        // configured ball.  This is the same native item sprite layer, at the same half-scale,
        // so it stays where the player expects instead of adding a second ball near the controls.
        NativeGui.blitScaled(context.pose(), dualstarters$topLeftBallTexture(entry == null ? null : entry.ball()),
                x + 4, y + 3, 16, 16, 0.5F);
        NativeGui.text(context, null, ball, x + DUALSTARTERS$DETAIL_X, y + DUALSTARTERS$DETAIL_Y,
                0.5F, DUALSTARTERS$DETAIL_MAX, NativeGui.WHITE, false, true);
        NativeGui.text(context, null, nature, x + DUALSTARTERS$DETAIL_X,
                y + DUALSTARTERS$DETAIL_Y + DUALSTARTERS$DETAIL_LINE,
                0.5F, DUALSTARTERS$DETAIL_MAX, NativeGui.WHITE, false, true);
    }

    /** Adds whatever the native screen currently has selected, including a "random" slot. */
    @Unique
    private void dualstarters$addCurrentPick() {
        if (this.currentCategory == null || this.dualstarters$picks.size() >= this.dualstarters$max
                || this.dualstarters$currentViolation() != null) {
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
        this.dualstarters$picks.add(
                new SelectStartersPacket.Pick(this.currentCategory.getName(), index));
        this.dualstarters$previews.add(random ? null : pokemon.get(index));
        this.dualstarters$playClick();
        this.dualstarters$refreshButtons();
    }

    @Unique
    private void dualstarters$undoLastPick() {
        if (this.dualstarters$picks.isEmpty()) {
            return;
        }
        this.dualstarters$picks.remove(this.dualstarters$picks.size() - 1);
        this.dualstarters$previews.remove(this.dualstarters$previews.size() - 1);
        this.dualstarters$playClick();
        this.dualstarters$refreshButtons();
    }

    @Unique
    private void dualstarters$confirm() {
        if (this.dualstarters$picks.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(
                new SelectStartersPacket(List.copyOf(this.dualstarters$picks)));
        this.dualstarters$playClick();
        this.onClose();
    }

    @Unique
    private void dualstarters$refreshButtons() {
        int chosen = this.dualstarters$picks.size();
        this.dualstarters$chooseButton.active = chosen < this.dualstarters$max
                && this.dualstarters$currentViolation() == null;
        this.dualstarters$undoButton.active = chosen > 0;
        this.dualstarters$confirmButton.active = chosen > 0;
    }

    /** Client polish only; StarterGrantHandler remains the authoritative validation. */
    @Unique
    private String dualstarters$currentViolation() {
        if (this.currentCategory == null) return null;
        List<RenderablePokemon> candidates = this.currentCategory.getPokemon();
        if (this.currentSelection < 0 || this.currentSelection >= candidates.size()) {
            return null; // Random is resolved safely by the server and cannot promise a species here.
        }
        RenderablePokemon candidate = candidates.get(this.currentSelection);
        String species = dualstarters$species(candidate);
        for (RenderablePokemon selected : this.dualstarters$previews) {
            if (selected == null || !species.equals(dualstarters$species(selected))) continue;
            if (Config.duplicateIdentity() == games.finn.dualstarters.StarterSelectionRules.DuplicateIdentityMode.SPECIES
                    || (Config.duplicateIdentity() == games.finn.dualstarters.StarterSelectionRules.DuplicateIdentityMode.SPECIES_AND_VARIANT
                    && dualstarters$variant(candidate).equals(dualstarters$variant(selected)))) {
                return "Already selected";
            }
        }
        for (games.finn.dualstarters.StarterSelectionRules.Group group : Config.groups()) {
            if (group.enabled() && group.contains(species) && dualstarters$countGroup(group) >= group.max()) {
                return dualstarters$groupLabel(group) + " limit reached";
            }
        }
        return null;
    }

    @Unique
    private int dualstarters$countGroup(games.finn.dualstarters.StarterSelectionRules.Group group) {
        int count = 0;
        for (RenderablePokemon selected : this.dualstarters$previews) {
            if (selected != null && group.contains(dualstarters$species(selected))) count++;
        }
        return count;
    }

    @Unique
    private static String dualstarters$groupLabel(games.finn.dualstarters.StarterSelectionRules.Group group) {
        return switch (group.name()) {
            case "pseudoLegendaries" -> "Pseudo-Legendary";
            case "fanFavorites" -> "Fan Favorites";
            case "traditionalStarters" -> "Traditional Starters";
            default -> group.name();
        };
    }

    @Unique
    private static String dualstarters$species(RenderablePokemon pokemon) {
        return pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
    }

    @Unique
    private static String dualstarters$variant(RenderablePokemon pokemon) {
        Set<String> aspects = pokemon.getAspects();
        return (pokemon.getForm() == null ? "" : pokemon.getForm().getName()) + "|"
                + aspects.stream().map(s -> s.toLowerCase(Locale.ROOT)).sorted().collect(java.util.stream.Collectors.joining(","));
    }

    /** Cobblemon's UI click, matching every other control on this screen. */
    @Unique
    private void dualstarters$playClick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F));
    }

    /** Resolves the 16px native item sprite that Cobblemon places next to the species name. */
    @Unique
    private static ResourceLocation dualstarters$topLeftBallTexture(String raw) {
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
    private static Component dualstarters$ballName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.translatable("dualstarters.screen.random");
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
    private static Component dualstarters$natureName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.translatable("dualstarters.screen.random");
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
