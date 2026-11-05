package games.finn.dualstarters.net;

import java.util.ArrayList;
import java.util.List;

import games.finn.dualstarters.DualStarters;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client packet carrying the ball and nature of every Pokemon in every starter
 * category, keyed by (categoryName, index) so the client can look details up for whichever entry
 * it is previewing.
 *
 * <p>This exists purely for display. The client never sends ball/nature back - picks are still
 * just (category, index) in {@link SelectStartersPacket}, and the server re-resolves the actual
 * Pokemon from its own authoritative starter list ({@code StarterGrantHandler.roll}). This packet
 * is sent from {@code StarterHandlerMixin} (server side) built from
 * {@code Cobblemon.INSTANCE.getStarterHandler().getStarterList(player)} - the same list and the
 * same order Cobblemon uses to build the categories the client already renders - so indices line
 * up exactly.</p>
 *
 * <p>An empty {@link Entry#ball()} or {@link Entry#nature()} means "unset" - the picker shows
 * "Random" for those rather than inventing a value.</p>
 */
public record StarterDetailsPacket(List<Entry> entries) implements CustomPacketPayload {

    /** Generous bound protecting decode from unbounded allocation; no realistic datapack is this large. */
    public static final int MAX_ENTRIES = 4096;

    public record Entry(String category, int index, String ball, String nature) {}

    public static final Type<StarterDetailsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DualStarters.MODID, "starter_details"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::category,
            ByteBufCodecs.VAR_INT, Entry::index,
            ByteBufCodecs.STRING_UTF8, Entry::ball,
            ByteBufCodecs.STRING_UTF8, Entry::nature,
            Entry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StarterDetailsPacket> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Entry, List<Entry>>collection(ArrayList::new, ENTRY_CODEC, MAX_ENTRIES)
                    .map(StarterDetailsPacket::new, StarterDetailsPacket::entries);

    @Override
    public Type<StarterDetailsPacket> type() {
        return TYPE;
    }
}
