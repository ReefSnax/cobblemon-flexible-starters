package games.finn.cobblemonflexiblestarters.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen;
import com.cobblemon.mod.common.config.starter.RenderableStarterCategory;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;

import games.finn.cobblemonflexiblestarters.Config;
import games.finn.cobblemonflexiblestarters.StarterSelectionRules.DuplicateIdentityMode;
import games.finn.cobblemonflexiblestarters.StarterSelectionRules.Group;

/**
 * A small client-only mirror of the starter tray for native category-list rendering.
 *
 * <p>This state never grants a Pokemon or decides what the server accepts. It exists solely so
 * Cobblemon's individual {@code CategoryList.Category} entries can colour their own native slot
 * backgrounds from the same picks shown in the add-on's tray.</p>
 */
public final class StarterSelectionState {

    public enum SlotState {
        NORMAL,
        SELECTED,
        GROUP_BLOCKED
    }

    private static StarterSelectionScreen screen;
    private static List<RenderablePokemon> selected = List.of();

    private StarterSelectionState() {}

    public static void update(StarterSelectionScreen owner, List<RenderablePokemon> previews) {
        screen = owner;
        selected = List.copyOf(previews);
    }

    public static SlotState slotState(StarterSelectionScreen owner, RenderableStarterCategory category, int index) {
        if (owner != screen || index < 0 || index >= category.getPokemon().size()) {
            // A random slot has no known species and must not claim a green/red state.
            return SlotState.NORMAL;
        }
        RenderablePokemon candidate = category.getPokemon().get(index);
        for (RenderablePokemon chosen : selected) {
            if (chosen != null && samePokemon(candidate, chosen)) {
                return SlotState.SELECTED;
            }
        }
        String species = species(candidate);
        for (Group group : Config.groups()) {
            if (group.enabled() && group.contains(species) && count(group) >= group.max()) {
                return SlotState.GROUP_BLOCKED;
            }
        }
        return SlotState.NORMAL;
    }

    /** Returns the current button feedback, while preserving honest handling for Random slots. */
    public static String violation(RenderableStarterCategory category, int index) {
        if (category == null || index < 0 || index >= category.getPokemon().size()) {
            return null;
        }
        RenderablePokemon candidate = category.getPokemon().get(index);
        String species = species(candidate);
        for (RenderablePokemon chosen : selected) {
            if (chosen == null || !species.equals(species(chosen))) {
                continue;
            }
            if (Config.duplicateIdentity() == DuplicateIdentityMode.SPECIES
                    || (Config.duplicateIdentity() == DuplicateIdentityMode.SPECIES_AND_VARIANT
                    && variant(candidate).equals(variant(chosen)))) {
                return "Selected";
            }
        }
        for (Group group : Config.groups()) {
            if (group.enabled() && group.contains(species) && count(group) >= group.max()) {
                return groupLabel(group) + " limit reached";
            }
        }
        return null;
    }

    public static int count(Group group) {
        int count = 0;
        for (RenderablePokemon chosen : selected) {
            if (chosen != null && group.contains(species(chosen))) {
                count++;
            }
        }
        return count;
    }

    public static String groupLabel(Group group) {
        return switch (group.name()) {
            case "pseudoLegendaries" -> "Pseudo-Legendary";
            case "fanFavorites" -> "Fan Favorites";
            case "traditionalStarters" -> "Traditional Starters";
            default -> group.name();
        };
    }

    private static boolean samePokemon(RenderablePokemon left, RenderablePokemon right) {
        return species(left).equals(species(right)) && variant(left).equals(variant(right));
    }

    private static String species(RenderablePokemon pokemon) {
        return pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
    }

    private static String variant(RenderablePokemon pokemon) {
        Set<String> aspects = pokemon.getAspects();
        String form = pokemon.getForm() == null ? "" : pokemon.getForm().getName();
        return form.toLowerCase(Locale.ROOT) + "|" + aspects.stream()
                .map(aspect -> aspect.toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(","));
    }
}
