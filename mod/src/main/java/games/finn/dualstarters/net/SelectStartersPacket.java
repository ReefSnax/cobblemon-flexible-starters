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
 * Client -> server packet carrying the player's starter picks (1..6 of them).
 *
 * <p>Each pick is a (categoryName, index) pair identifying an entry in Cobblemon's normal
 * starter list (the same list the picker screen was populated from). An index equal to the
 * category size means "random within that category" (mirrors Cobblemon's own
 * {@code chooseStarter} convention). Picking the same species more than once is allowed; each
 * pick is rolled independently on the server.</p>
 *
 * <p>The list is hard-capped at {@link #MAX_PICKS} entries at the codec level, so a malformed or
 * hostile payload claiming a larger count is rejected during decode rather than ever reaching
 * handler logic. The server additionally verifies the list size matches the configured
 * {@link games.finn.dualstarters.Config#starterCount()} before granting anything.</p>
 */
public record SelectStartersPacket(List<Pick> picks) implements CustomPacketPayload {

    /** Hard ceiling matching the config's clamp range; protects decode from unbounded lists. */
    public static final int MAX_PICKS = 6;

    public record Pick(String category, int index) {}

    public static final Type<SelectStartersPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DualStarters.MODID, "select_starters"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Pick> PICK_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Pick::category,
            ByteBufCodecs.VAR_INT, Pick::index,
            Pick::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectStartersPacket> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Pick, List<Pick>>collection(ArrayList::new, PICK_CODEC, MAX_PICKS)
                    .map(SelectStartersPacket::new, SelectStartersPacket::picks);

    @Override
    public Type<SelectStartersPacket> type() {
        return TYPE;
    }
}
