package games.finn.dualstarters.client;

import java.util.HashMap;
import java.util.Map;

import games.finn.dualstarters.net.StarterDetailsPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-side cache of the authoritative ball/nature details sent by the server
 * ({@link StarterDetailsPacket}). Keyed by "category#index" string.
 *
 * <p>The cache is just display state - it is never consulted for anything security-relevant. The
 * starter screen reads it every frame for whichever entry is currently selected; if details for a
 * given pick have not arrived yet (or the entry is a "random" slot, which has no configured
 * properties), it shows "Random" rather than inventing a value.</p>
 */
public final class ClientStarterDetails {

    private static Map<String, StarterDetailsPacket.Entry> details = Map.of();

    private ClientStarterDetails() {}

    /** NeoForge payload handler entry point (runs on the network thread). */
    public static void handle(StarterDetailsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> apply(packet));
    }

    private static void apply(StarterDetailsPacket packet) {
        Map<String, StarterDetailsPacket.Entry> map = new HashMap<>();
        for (StarterDetailsPacket.Entry entry : packet.entries()) {
            map.put(key(entry.category(), entry.index()), entry);
        }
        details = map;
    }

    /** Null if details for this pick have not arrived (yet, or never will for a "random" slot). */
    public static StarterDetailsPacket.Entry get(String category, int index) {
        return details.get(key(category, index));
    }

    private static String key(String category, int index) {
        return category + '#' + index;
    }
}
