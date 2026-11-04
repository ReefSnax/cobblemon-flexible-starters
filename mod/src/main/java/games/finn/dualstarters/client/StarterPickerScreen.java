package games.finn.dualstarters.client;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.gui.TypeIcon;
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;

import games.finn.dualstarters.Config;
import games.finn.dualstarters.net.SelectStartersPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The starter picker: up to {@link Config#starterCount()} slots, filled from Cobblemon's normal
 * starter categories, with a "Confirm your team" step.
 *
 * <p>{@link com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen} (Cobblemon's
 * native screen) is {@code final}, so it cannot be extended directly. Rather than hand-paint a
 * panel, this screen mirrors the technique Cobblemon's own screen uses: the normal in-world
 * blurred/vignette background ({@link #renderBlurredBackground}/{@link #renderMenuBackground},
 * same as the vanilla pause screen) instead of a bespoke opaque overlay, with Cobblemon's
 * starter-viewport textures blitted at their native pixel size (never scaled) and plain vanilla
 * widgets/text for everything else. That keeps it visually clean and compatible without
 * depending on Cobblemon's internal (and non-extensible) screen class.</p>
 *
 * <p>Layout is computed from {@code this.width}/{@code this.height} in {@link #init()} so it
 * stays legible at any GUI scale and for any number of configured starter categories (the
 * category selector is a single prev/next row rather than one widget per category, so category
 * count never affects how much screen space is needed).</p>
 */
public class StarterPickerScreen extends Screen {

    private static ResourceLocation cb(String path) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/starterselection/" + path);
    }

    private static final ResourceLocation TEX_BACKGROUND = cb("background.png");   // 118x100 viewport
    private static final ResourceLocation TEX_PLATFORM = cb("platform_base.png");  // 113x32 platform
    private static final int VIEWPORT_W = 118;
    private static final int VIEWPORT_H = 96;
    private static final int PLATFORM_W = 113;
    private static final int PLATFORM_H = 32;

    private record Pick(String categoryName, int index, Component displayName) {}

    private final List<RenderableStarterCategory> categories;
    private final int slotCount;
    private int currentCategory = 0;
    private int currentIndex = 0;
    private int currentSlot = 0;

    private final Pick[] slots;

    private ModelWidget modelWidget;
    private Button confirmButton;
    private Button removeButton;

    // Slot grid geometry, computed once in init() and reused for both rendering and hit-testing.
    private final int[] slotX;
    private final int[] slotY;
    private int slotW, slotH;

    private int viewportX, viewportY, nameplateY, categoryRowY, pickRowY, slotsY, bottomRowY;

    public StarterPickerScreen(List<RenderableStarterCategory> categories) {
        super(Component.translatable("dualstarters.screen.title"));
        this.categories = categories != null ? categories : new ArrayList<>();
        this.slotCount = Config.starterCount();
        this.slots = new Pick[slotCount];
        this.slotX = new int[slotCount];
        this.slotY = new int[slotCount];
    }

    public boolean hasOptions() {
        for (RenderableStarterCategory c : categories) {
            if (c.getPokemon() != null && !c.getPokemon().isEmpty()) return true;
        }
        return false;
    }

    private List<RenderablePokemon> monsOf(int categoryIdx) {
        if (categories.isEmpty()) return List.of();
        List<RenderablePokemon> mons = categories.get(categoryIdx).getPokemon();
        return mons != null ? mons : List.of();
    }

    private List<RenderablePokemon> currentMons() {
        if (categories.isEmpty()) return List.of();
        return monsOf(currentCategory);
    }

    /** Move off an empty configured category before the first screen render. */
    private void normalizeCurrentCategory() {
        if (categories.isEmpty() || !monsOf(currentCategory).isEmpty()) return;
        for (int i = 0; i < categories.size(); i++) {
            if (!monsOf(i).isEmpty()) {
                currentCategory = i;
                currentIndex = 0;
                return;
            }
        }
    }

    private RenderablePokemon currentPokemon() {
        List<RenderablePokemon> mons = currentMons();
        if (mons.isEmpty()) return null;
        if (currentIndex < 0 || currentIndex >= mons.size()) currentIndex = 0;
        return mons.get(currentIndex);
    }

    private Component currentName() {
        RenderablePokemon p = currentPokemon();
        return p != null ? p.getSpecies().getTranslatedName() : Component.literal("-");
    }

    @Override
    protected void init() {
        normalizeCurrentCategory();
        int centerX = this.width / 2;
        int y = 8;

        y += this.font.lineHeight + 6; // title, drawn manually in render()
        categoryRowY = y;
        y += 20;

        viewportX = centerX - VIEWPORT_W / 2;
        viewportY = y;
        y += VIEWPORT_H;
        nameplateY = y + 2;
        y += this.font.lineHeight + 6;

        pickRowY = y;
        y += 20;

        slotsY = y;
        int columns = slotCount <= 3 ? slotCount : 3;
        int rows = (slotCount + columns - 1) / columns;
        slotW = 70;
        slotH = 24;
        int gridW = columns * slotW + (columns - 1) * 4;
        int gridX = centerX - gridW / 2;
        for (int i = 0; i < slotCount; i++) {
            int col = i % columns;
            int row = i / columns;
            slotX[i] = gridX + col * (slotW + 4);
            slotY[i] = slotsY + row * (slotH + 4);
        }
        y += rows * (slotH + 4);

        bottomRowY = y + 4;

        if (!hasOptions()) {
            addRenderableWidget(Button.builder(Component.translatable("dualstarters.screen.close"), b -> onClose())
                    .bounds(centerX - 50, this.height / 2 + 40, 100, 20).build());
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleCategory(-1))
                .bounds(centerX - 110, categoryRowY, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleCategory(1))
                .bounds(centerX + 90, categoryRowY, 20, 18).build());

        try {
            RenderablePokemon cur = currentPokemon();
            if (cur != null) {
                modelWidget = new ModelWidget(viewportX, viewportY, VIEWPORT_W, VIEWPORT_H - 4, cur, 2.0f, 0f, -8.0, true, false, 15);
                addRenderableWidget(modelWidget);
            }
        } catch (Throwable t) {
            modelWidget = null;
        }

        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleMon(-1))
                .bounds(centerX - 100, pickRowY, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleMon(1))
                .bounds(centerX + 80, pickRowY, 20, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("dualstarters.screen.choose"), b -> addOrReplace())
                .bounds(centerX - 76, pickRowY, 152, 18).build());

        removeButton = Button.builder(Component.translatable("dualstarters.screen.remove"), b -> removeCurrentSlot())
                .bounds(centerX - 110, bottomRowY, 100, 18).build();
        addRenderableWidget(removeButton);

        confirmButton = Button.builder(Component.translatable("dualstarters.screen.confirm"), b -> confirm())
                .bounds(centerX + 10, bottomRowY, 100, 18).build();
        addRenderableWidget(confirmButton);
        refreshButtons();
    }

    private void cycleCategory(int delta) {
        int count = categories.size();
        if (count == 0) return;
        for (int attempt = 0; attempt < count; attempt++) {
            currentCategory = (currentCategory + delta + count) % count;
            if (!monsOf(currentCategory).isEmpty()) break;
        }
        currentIndex = 0;
        refreshModel();
        rebuildWidgets();
    }

    private void cycleMon(int delta) {
        int size = currentMons().size();
        if (size == 0) return;
        currentIndex = (currentIndex + delta + size) % size;
        refreshModel();
    }

    private void refreshModel() {
        RenderablePokemon cur = currentPokemon();
        if (modelWidget != null && cur != null) {
            try { modelWidget.setPokemon(cur); } catch (Throwable ignored) {}
        }
    }

    private void addOrReplace() {
        RenderablePokemon cur = currentPokemon();
        if (cur == null) return;
        slots[currentSlot] = new Pick(categories.get(currentCategory).getName(), currentIndex, currentName());
        // Auto-advance to the next empty slot, if any, so repeated picks fill left-to-right.
        for (int i = 0; i < slotCount; i++) {
            if (slots[i] == null) {
                currentSlot = i;
                break;
            }
        }
        refreshButtons();
    }

    private void removeCurrentSlot() {
        slots[currentSlot] = null;
        refreshButtons();
    }

    private void refreshButtons() {
        if (confirmButton != null) {
            boolean allFilled = true;
            for (Pick p : slots) {
                if (p == null) { allFilled = false; break; }
            }
            confirmButton.active = allFilled;
        }
    }

    private void confirm() {
        List<SelectStartersPacket.Pick> picks = new ArrayList<>(slotCount);
        for (Pick slot : slots) {
            if (slot == null) return;
            picks.add(new SelectStartersPacket.Pick(slot.categoryName(), slot.index()));
        }
        PacketDistributor.sendToServer(new SelectStartersPacket(picks));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasOptions()) {
            for (int i = 0; i < slotCount; i++) {
                if (mouseX >= slotX[i] && mouseX < slotX[i] + slotW && mouseY >= slotY[i] && mouseY < slotY[i] + slotH) {
                    currentSlot = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Vanilla in-world blur/vignette (same technique Cobblemon's own starter screen and the
        // pause menu use) instead of a bespoke opaque overlay.
        this.renderBlurredBackground(partialTick);
        this.renderMenuBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        if (!hasOptions()) {
            g.drawCenteredString(this.font, Component.translatable("dualstarters.screen.no_starters"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        RenderableStarterCategory cat = categories.get(currentCategory);
        Component categoryLabel = Component.translatable("dualstarters.screen.category_counter",
                cat.getDisplayNameText(), currentCategory + 1, categories.size());
        g.drawCenteredString(this.font, categoryLabel, this.width / 2, categoryRowY + 4, 0xFFE0FFE6);

        // Viewport + platform, blitted at native pixel size (never scaled) so they stay crisp at
        // any GUI scale. (ModelWidget renders the live 3D model itself, over this.)
        g.blit(TEX_BACKGROUND, viewportX, viewportY, 0, 0, VIEWPORT_W, VIEWPORT_H, VIEWPORT_W, 100);
        g.blit(TEX_PLATFORM, viewportX + (VIEWPORT_W - PLATFORM_W) / 2, viewportY + VIEWPORT_H - 26, 0, 0, PLATFORM_W, PLATFORM_H, PLATFORM_W, PLATFORM_H);

        // Name + type badges, drawn as plain shadowed text (vanilla style) rather than a boxed
        // nameplate, so there's no extra opaque panel behind the model.
        RenderablePokemon cp = currentPokemon();
        int nameW = this.font.width(currentName());
        g.drawString(this.font, currentName(), this.width / 2 - nameW / 2 - 10, nameplateY, 0xFFFFFFFF);
        if (cp != null) {
            try {
                Species sp = cp.getSpecies();
                new TypeIcon(this.width / 2 - nameW / 2 - 10 + nameW + 4, nameplateY - 1, sp.getPrimaryType(), sp.getSecondaryType(),
                        false, true, 15F, 7.5F, 1F).render(g);
            } catch (Throwable ignored) {}
        }

        // Up to six team slots, in a wrapping grid; the current slot is highlighted and numbered
        // so it's unambiguous which slot "I choose you!"/"Remove" will affect.
        for (int i = 0; i < slotCount; i++) {
            drawSlot(g, slotX[i], slotY[i], i + 1, slots[i], i == currentSlot);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSlot(GuiGraphics g, int x, int y, int slotNumber, Pick pick, boolean selected) {
        int bg = pick != null ? 0x80284A30 : 0x60202020;
        g.fill(x, y, x + slotW, y + slotH, bg);
        int border = selected ? 0xFFFFFFFF : 0x40FFFFFF;
        g.fill(x, y, x + slotW, y + 1, border);
        g.fill(x, y + slotH - 1, x + slotW, y + slotH, border);
        g.fill(x, y, x + 1, y + slotH, border);
        g.fill(x + slotW - 1, y, x + slotW, y + slotH, border);
        Component label = Component.translatable("dualstarters.screen.slot", slotNumber);
        Component value = pick != null ? pick.displayName() : Component.translatable("dualstarters.screen.slot_empty");
        g.drawCenteredString(this.font, label, x + slotW / 2, y + 2, selected ? 0xFFFFFFFF : 0xFFC0C0C0);
        g.drawCenteredString(this.font, value, x + slotW / 2, y + slotH - this.font.lineHeight - 2,
                pick != null ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
