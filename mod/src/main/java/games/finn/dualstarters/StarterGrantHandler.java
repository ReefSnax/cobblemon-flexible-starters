package games.finn.dualstarters;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreManager;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes;
import com.cobblemon.mod.common.config.starter.StarterCategory;
import com.cobblemon.mod.common.pokemon.Pokemon;

import games.finn.dualstarters.net.SelectStartersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-side logic for Dual Starters.
 *
 * <p>Receives {@link SelectStartersPacket} from the picker screen and grants between 1 and
 * {@link Config#starterCount()} independent starter Pokemon (the player's choice - they are not
 * required to fill every slot), but ONLY to a player who has never chosen a starter. The
 * authoritative "new player" gate is {@code !starterSelected &&
 * !starterLocked} on Cobblemon's {@link GeneralPlayerData} - re-checked here so a spoofed or
 * replayed packet cannot grant twice (the data object is fetched fresh per packet and mutated on
 * grant, and since the server processes both the gate check and the mutation inside a single
 * {@code enqueueWork} on the main thread, two packets arriving back-to-back cannot race each
 * other into double-granting).</p>
 */
public final class StarterGrantHandler {

    private StarterGrantHandler() {}

    /** NeoForge payload handler entry point (runs on the network thread). */
    public static void handle(SelectStartersPacket packet, IPayloadContext context) {
        // Hop onto the server thread before touching world/player state.
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                grant(player, packet);
            }
        });
    }

    /**
     * Grants all starters if (and only if) the player has never chosen one and the submitted
     * picks are well-formed. Returns true if the grant happened.
     */
    public static boolean grant(ServerPlayer player, SelectStartersPacket packet) {
        PlayerInstancedDataStoreManager dataManager = Cobblemon.INSTANCE.getPlayerDataManager();
        GeneralPlayerData data = dataManager.getGenericData(player);

        // --- New-player gate (authoritative, server-side). ---
        if (!isNewPlayer(data)) {
            DualStarters.LOGGER.info("[Dual Starters] {} already has a starter (selected={}, locked={}); ignoring pick.",
                    player.getGameProfile().getName(), data.getStarterSelected(), data.getStarterLocked());
            // Re-sync so a stale client screen closes cleanly.
            data.sendToPlayer(player);
            return false;
        }

        // --- Count validation (authoritative, server-side; config is also synced to clients).
        // The player may choose anywhere from 1 up to the configured max - they are not required
        // to fill every slot. ---
        List<SelectStartersPacket.Pick> picks = packet.picks();
        int max = Config.starterCount();
        if (picks == null || picks.isEmpty() || picks.size() > max) {
            DualStarters.LOGGER.warn("[Dual Starters] {} submitted {} pick(s) but the server allows 1-{}; ignoring.",
                    player.getGameProfile().getName(), picks == null ? -1 : picks.size(), max);
            return false;
        }

        List<StarterCategory> starterList = Cobblemon.INSTANCE.getStarterHandler().getStarterList(player);

        // Roll every pick before granting any of them: one invalid pick invalidates the whole
        // submission (no partial grants). Each roll() call is an independent create(), so picking
        // the same species more than once still yields separately-rolled Pokemon.
        List<Pokemon> rolled = new ArrayList<>(picks.size());
        for (SelectStartersPacket.Pick pick : picks) {
            Pokemon mon = roll(starterList, pick.category(), pick.index(), player);
            if (mon == null) {
                DualStarters.LOGGER.warn("[Dual Starters] invalid pick from {}: [{}#{}]; granting nothing.",
                        player.getGameProfile().getName(), pick.category(), pick.index());
                return false;
            }
            rolled.add(mon);
        }

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        for (Pokemon mon : rolled) {
            party.add(mon);
        }

        // Mark the player as having chosen a starter so this never fires again.
        data.setStarterSelected(true);
        data.setStarterUUID(rolled.get(0).getUuid());
        dataManager.saveSingle(data, PlayerInstancedDataStoreTypes.INSTANCE.getGENERAL());
        data.sendToPlayer(player);

        DualStarters.LOGGER.info("[Dual Starters] granted {} {} starter(s).",
                player.getGameProfile().getName(), rolled.size());
        return true;
    }

    /** A "new player" has never chosen a starter and is not otherwise locked out. */
    public static boolean isNewPlayer(GeneralPlayerData data) {
        return !data.getStarterSelected() && !data.getStarterLocked();
    }

    /**
     * Resolves one (categoryName, index) pick into a freshly created Pokemon.
     * index == category size means "random within that category" (Cobblemon convention).
     * Returns null for an invalid pick (unknown category or out-of-range index).
     */
    private static Pokemon roll(List<StarterCategory> starterList, String categoryName, int index, ServerPlayer player) {
        StarterCategory category = null;
        for (StarterCategory c : starterList) {
            if (c.getName().equals(categoryName)) {
                category = c;
                break;
            }
        }
        if (category == null) {
            return null;
        }
        List<? extends PokemonProperties> pokemon = category.getPokemon();
        if (index < 0 || index > pokemon.size()) {
            return null;
        }
        PokemonProperties properties;
        if (index == pokemon.size()) {
            if (pokemon.isEmpty()) {
                return null;
            }
            properties = pokemon.get((int) (Math.random() * pokemon.size()));
        } else {
            properties = pokemon.get(index);
        }
        return properties.create(player);
    }
}
