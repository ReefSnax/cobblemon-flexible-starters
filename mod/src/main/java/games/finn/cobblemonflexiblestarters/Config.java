package games.finn.cobblemonflexiblestarters;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import games.finn.cobblemonflexiblestarters.StarterSelectionRules.DuplicateIdentityMode;
import games.finn.cobblemonflexiblestarters.StarterSelectionRules.Group;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-authoritative configuration for Dual Starters.
 *
 * <p>Registered as a {@code SERVER}-type config. NeoForge automatically syncs the server's
 * values for this config type to every connecting client (see {@code ConfigSync}), overriding
 * whatever the client has locally for the duration of the connection. That means
 * {@link #STARTER_COUNT} is read the same way on both sides, and a client with a stale or
 * tampered local copy still only ever sees - and can only submit - the count the server allows.
 * {@link StarterGrantHandler} additionally re-validates the submitted pick count against this
 * value server-side, so even a client that ignores the synced config entirely cannot be granted
 * more starters than the server permits. The same is true of {@link #DUPLICATE_IDENTITY} and the
 * three exclusivity groups below: they are enforced authoritatively by
 * {@link StarterSelectionRules} inside {@link StarterGrantHandler}, not merely by the UI.</p>
 */
public final class Config {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue STARTER_COUNT;

    public static final ModConfigSpec.EnumValue<DuplicateIdentityMode> DUPLICATE_IDENTITY;

    public static final ModConfigSpec.BooleanValue PSEUDO_LEGENDARIES_ENABLED;
    public static final ModConfigSpec.IntValue PSEUDO_LEGENDARIES_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PSEUDO_LEGENDARIES_SPECIES;

    public static final ModConfigSpec.BooleanValue FAN_FAVORITES_ENABLED;
    public static final ModConfigSpec.IntValue FAN_FAVORITES_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FAN_FAVORITES_SPECIES;

    public static final ModConfigSpec.BooleanValue TRADITIONAL_STARTERS_ENABLED;
    public static final ModConfigSpec.IntValue TRADITIONAL_STARTERS_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TRADITIONAL_STARTERS_SPECIES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        STARTER_COUNT = builder
                .comment("Number of starter Pokemon a new player chooses at once (clamped 1-6).",
                        "This is a server-authoritative value: it is synced to every connecting client.")
                .translation("cobblemon_flexible_starters.config.starter_count")
                .defineInRange("starterCount", 2, 1, 6);

        DUPLICATE_IDENTITY = builder
                .comment("How strictly two picks in the same submission must match to be rejected",
                        "as duplicates of each other:",
                        "  SPECIES              - picking the same species twice is always rejected,",
                        "                         regardless of form/aspect (e.g. two Rotom of any form).",
                        "  SPECIES_AND_VARIANT  - only an exact species + form + aspect + region_bias",
                        "                         match is rejected; different forms/variants or regional",
                        "                         starter-line biases of the same species may both be picked",
                        "  DISABLED             - duplicate picks are always allowed.",
                        "This is independent of the exclusivity groups below.")
                .translation("cobblemon_flexible_starters.config.duplicate_identity")
                .defineEnum("duplicateIdentity", DuplicateIdentityMode.SPECIES_AND_VARIANT);

        builder.comment("Exclusivity groups: named caps on how many starters from a given species",
                        "list a single submission may contain. Each group is independent and is only",
                        "enforced while its own \"enabled\" flag is true. Species names are the",
                        "lowercase Cobblemon species identifier (e.g. \"charmander\").")
                .push("exclusivityGroups");

        builder.comment("Active by default: a new player may receive at most one pseudo-legendary",
                        "base-stage Pokemon across their whole submission, even split across",
                        "different starter categories.")
                .push("pseudoLegendaries");
        PSEUDO_LEGENDARIES_ENABLED = builder
                .comment("Whether this group is enforced.")
                .translation("cobblemon_flexible_starters.config.group.pseudo_legendaries.enabled")
                .define("enabled", true);
        PSEUDO_LEGENDARIES_MAX = builder
                .comment("Maximum number of these species a submission may contain.")
                .translation("cobblemon_flexible_starters.config.group.pseudo_legendaries.max")
                .defineInRange("max", 1, 1, 6);
        PSEUDO_LEGENDARIES_SPECIES = builder
                .comment("The active pseudo-legendary example pool. Edit this list freely; group limits",
                        "match base species regardless of regional form or aspect. The default list is",
                        "Dratini, Larvitar, Bagon, Beldum, Gible, Deino, Goomy, Jangmo-o, Dreepy, and Frigibax.")
                .translation("cobblemon_flexible_starters.config.group.pseudo_legendaries.species")
                .defineList("species", List.of(
                        "dratini", "larvitar", "bagon", "beldum", "gible", "deino",
                        "goomy", "jangmo-o", "dreepy", "frigibax"
                ), o -> o instanceof String);
        builder.pop();

        builder.comment("Disabled example: shows how to cap crossover-popular species like",
                        "Pikachu/Eevee to one pick per submission. Set enabled=true to turn it on.")
                .push("fanFavorites");
        FAN_FAVORITES_ENABLED = builder
                .comment("Whether this group is enforced.")
                .translation("cobblemon_flexible_starters.config.group.fan_favorites.enabled")
                .define("enabled", false);
        FAN_FAVORITES_MAX = builder
                .comment("Maximum number of these species a submission may contain.")
                .translation("cobblemon_flexible_starters.config.group.fan_favorites.max")
                .defineInRange("max", 1, 1, 6);
        FAN_FAVORITES_SPECIES = builder
                .comment("Example species list for this group.")
                .translation("cobblemon_flexible_starters.config.group.fan_favorites.species")
                .defineList("species", List.of("pikachu", "eevee"), o -> o instanceof String);
        builder.pop();

        builder.comment("Disabled example: shows how to cap classic first-partner starters to one",
                        "pick per submission. Set enabled=true to turn it on, and extend the species",
                        "list with any other generations' starters you want included.")
                .push("traditionalStarters");
        TRADITIONAL_STARTERS_ENABLED = builder
                .comment("Whether this group is enforced.")
                .translation("cobblemon_flexible_starters.config.group.traditional_starters.enabled")
                .define("enabled", false);
        TRADITIONAL_STARTERS_MAX = builder
                .comment("Maximum number of these species a submission may contain.")
                .translation("cobblemon_flexible_starters.config.group.traditional_starters.max")
                .defineInRange("max", 1, 1, 6);
        TRADITIONAL_STARTERS_SPECIES = builder
                .comment("Example species list for this group (generation I starters).")
                .translation("cobblemon_flexible_starters.config.group.traditional_starters.species")
                .defineList("species", List.of("bulbasaur", "charmander", "squirtle"), o -> o instanceof String);
        builder.pop();

        builder.pop(); // exclusivityGroups

        SPEC = builder.build();
    }

    private Config() {}

    /** The authoritative starter count, defensively re-clamped in case of odd config IO. */
    public static int starterCount() {
        return Math.max(1, Math.min(6, STARTER_COUNT.get()));
    }

    /** The authoritative duplicate-identity policy used by {@link StarterSelectionRules}. */
    public static DuplicateIdentityMode duplicateIdentity() {
        return DUPLICATE_IDENTITY.get();
    }

    /** The 3 configured exclusivity groups, built fresh from the current config values. */
    public static List<Group> groups() {
        return List.of(
                new Group("pseudoLegendaries", PSEUDO_LEGENDARIES_ENABLED.get(),
                        Math.max(1, PSEUDO_LEGENDARIES_MAX.get()), lowercaseSet(PSEUDO_LEGENDARIES_SPECIES.get())),
                new Group("fanFavorites", FAN_FAVORITES_ENABLED.get(),
                        Math.max(1, FAN_FAVORITES_MAX.get()), lowercaseSet(FAN_FAVORITES_SPECIES.get())),
                new Group("traditionalStarters", TRADITIONAL_STARTERS_ENABLED.get(),
                        Math.max(1, TRADITIONAL_STARTERS_MAX.get()), lowercaseSet(TRADITIONAL_STARTERS_SPECIES.get()))
        );
    }

    private static Set<String> lowercaseSet(List<? extends String> species) {
        return species.stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
