package games.finn.cobblemonflexiblestarters.client;

import games.finn.cobblemonflexiblestarters.net.StarterDetailsPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the common-safe server-to-client payload handler. The packet type must be registered
 * on both physical sides: the server owns it when sending and the client owns it when receiving.
 * The delivery callback gates the reference to {@link ClientStarterDetails}, which touches
 * {@code net.minecraft.client.Minecraft}, so dedicated servers never load client classes.
 */
public final class ClientPayloads {

    private ClientPayloads() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                StarterDetailsPacket.TYPE,
                StarterDetailsPacket.STREAM_CODEC,
                ClientPayloads::handleStarterDetails
        );
    }

    private static void handleStarterDetails(StarterDetailsPacket packet, IPayloadContext context) {
        // This callback is registered play-to-client, therefore NeoForge only invokes it in a
        // client connection. Keeping the reference in this method (rather than a static field or
        // registration method-reference) lets a dedicated server register the payload type without
        // resolving the client-only cache class.
        ClientStarterDetails.handle(packet, context);
    }
}
