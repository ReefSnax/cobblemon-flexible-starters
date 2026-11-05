package games.finn.cobblemonflexiblestarters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;

/**
 * Pure selection-rule logic used by the server grant path. It deliberately holds no
 * Minecraft/NeoForge state of its own, so future client pre-validation may reuse it without
 * changing the authoritative server behavior.
 *
 * <p>Two independent rule families are evaluated, in order, against the Pokemon already resolved
 * earlier in the same submission:</p>
 * <ol>
 *   <li><b>Duplicate identity</b> - rejects a pick whose {@link #identityKey} matches an earlier
 *   pick's, per {@link Config#duplicateIdentity()}.</li>
 *   <li><b>Exclusivity groups</b> - rejects a pick that would push an enabled {@link Group}'s
 *   member count (across the whole submission) past its configured maximum.</li>
 * </ol>
 */
public final class StarterSelectionRules {

    /** How strictly two picks must match to be considered "the same" Pokemon for duplicate checks. */
    public enum DuplicateIdentityMode {
        /** Same species (any form/aspect) counts as a duplicate. */
        SPECIES,
        /** Only an exact species + form + aspect match counts as a duplicate. */
        SPECIES_AND_VARIANT,
        /** Duplicate picks are always allowed. */
        DISABLED
    }

    /** A named cap on how many starters from {@code species} a single submission may contain. */
    public record Group(String name, boolean enabled, int max, Set<String> species) {
        public boolean contains(String normalizedSpecies) {
            return species.contains(normalizedSpecies);
        }
    }

    private final DuplicateIdentityMode duplicateIdentityMode;
    private final List<Group> groups;

    public StarterSelectionRules(DuplicateIdentityMode duplicateIdentityMode, List<Group> groups) {
        this.duplicateIdentityMode = duplicateIdentityMode;
        this.groups = groups;
    }

    /** Builds the live rule set from the currently loaded {@link Config} values. */
    public static StarterSelectionRules fromConfig() {
        return new StarterSelectionRules(Config.duplicateIdentity(), Config.groups());
    }

    /**
     * The key used to compare two picks for the duplicate-identity check, or {@code null} if
     * duplicate checking is disabled entirely.
     */
    public String identityKey(PokemonProperties properties) {
        if (this.duplicateIdentityMode == DuplicateIdentityMode.DISABLED) {
            return null;
        }
        String species = normalize(properties.getSpecies());
        if (this.duplicateIdentityMode == DuplicateIdentityMode.SPECIES) {
            return species;
        }
        // SPECIES_AND_VARIANT: distinguish meaningful configured form/aspect/region selectors.
        // getOriginalString retains selectors that Cobblemon represents as custom properties (such
        // as region_bias), while deliberately excluding ball, nature, moves, IVs, level and shiny.
        String form = normalize(properties.getForm());
        Set<String> aspects = properties.getAspects();
        String aspectPart = aspects == null || aspects.isEmpty() ? "" : aspects.stream()
                .map(StarterSelectionRules::normalize)
                .sorted()
                .collect(Collectors.joining(","));
        return species + "|" + form + "|" + aspectPart + "|" + regionBias(properties.getOriginalString());
    }

    /**
     * Returns a human-readable reason the candidate conflicts with the already-resolved picks, or
     * {@code null} if the candidate is fine to add.
     */
    public String violation(List<PokemonProperties> alreadyResolved, PokemonProperties candidate) {
        String candidateKey = identityKey(candidate);
        if (candidateKey != null) {
            for (PokemonProperties resolved : alreadyResolved) {
                if (candidateKey.equals(identityKey(resolved))) {
                    return "duplicate identity (" + candidateKey + ")";
                }
            }
        }

        String candidateSpecies = normalize(candidate.getSpecies());
        for (Group group : this.groups) {
            if (!group.enabled() || !group.contains(candidateSpecies)) {
                continue;
            }
            int countInGroup = 1; // the candidate itself
            for (PokemonProperties resolved : alreadyResolved) {
                if (group.contains(normalize(resolved.getSpecies()))) {
                    countInGroup++;
                }
            }
            if (countInGroup > group.max()) {
                return "exclusivity group \"" + group.name() + "\" allows at most " + group.max();
            }
        }
        return null;
    }

    public boolean isValid(List<PokemonProperties> alreadyResolved, PokemonProperties candidate) {
        return violation(alreadyResolved, candidate) == null;
    }

    /**
     * Validates a full proposed submission at once, checking each pick against every pick before
     * it. Returns {@code true} only if every pick is compatible with all earlier picks.
     */
    public boolean validateAll(List<PokemonProperties> proposed) {
        List<PokemonProperties> resolved = new ArrayList<>(proposed.size());
        for (PokemonProperties candidate : proposed) {
            if (!isValid(resolved, candidate)) {
                return false;
            }
            resolved.add(candidate);
        }
        return true;
    }

    /**
     * Filters a category's candidate list down to the entries still compatible with the picks
     * already resolved in this submission, for use when resolving a "random within category"
     * pick. The result is always finite (bounded by {@code candidates}) so random resolution
     * terminates without retry loops.
     */
    public <T extends PokemonProperties> List<T> compatibleCandidates(
            List<T> candidates, List<PokemonProperties> alreadyResolved) {
        List<T> compatible = new ArrayList<>(candidates.size());
        for (T candidate : candidates) {
            if (isValid(alreadyResolved, candidate)) {
                compatible.add(candidate);
            }
        }
        return compatible;
    }

    /** Extracts only the regional selector from Cobblemon's original properties string. */
    private static String regionBias(String original) {
        if (original == null || original.isBlank()) {
            return "";
        }
        for (String token : original.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (token.startsWith("region_bias=") || token.startsWith("regionbias=")) {
                return token.substring(token.indexOf('=') + 1);
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
