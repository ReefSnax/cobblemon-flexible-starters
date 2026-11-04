package games.finn.dualstarters;

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
 * more starters than the server permits.</p>
 */
public final class Config {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue STARTER_COUNT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        STARTER_COUNT = builder
                .comment("Number of starter Pokemon a new player chooses at once (clamped 1-6).",
                        "This is a server-authoritative value: it is synced to every connecting client.")
                .translation("dualstarters.config.starter_count")
                .defineInRange("starterCount", 2, 1, 6);
        SPEC = builder.build();
    }

    private Config() {}

    /** The authoritative starter count, defensively re-clamped in case of odd config IO. */
    public static int starterCount() {
        return Math.max(1, Math.min(6, STARTER_COUNT.get()));
    }
}
