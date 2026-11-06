package com.reefsnax.cobblemonflexiblestarters.mixin;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.config.starter.StarterCategory;
import com.cobblemon.mod.common.starter.CobblemonStarterHandler;

import com.reefsnax.cobblemonflexiblestarters.StarterGrantHandler;
import com.reefsnax.cobblemonflexiblestarters.net.StarterDetailsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends {@link StarterDetailsPacket} right after Cobblemon's own
 * {@link CobblemonStarterHandler#requestStarterChoice} sends {@code OpenStarterUIPacket}, so the
 * picker screen can show each starter's actual ball/nature without the client ever having to
 * supply (or be trusted for) that data itself.
 *
 * <p>{@code requestStarterChoice} is server-only logic, but the class itself is common (not
 * client-gated) because it also runs inside a single-player integrated server. This mixin is
 * registered as a common mixin (applies on both the dedicated server and the client's integrated
 * server) for exactly that reason.</p>
 *
 * <p>Injected at {@code TAIL} rather than replacing anything: Cobblemon's own gate
 * (already evaluated earlier in the same method) decides whether {@code OpenStarterUIPacket} was
 * actually sent. We re-check the same new-player condition ourselves before sending our own
 * packet, both because the TAIL injection point can't see Cobblemon's local branch outcome and
 * because re-checking server-side is the same defensive pattern used everywhere else in this
 * add-on.</p>
 */
@Mixin(value = CobblemonStarterHandler.class, remap = false)
public class StarterHandlerMixin {

    @Inject(
            method = "requestStarterChoice(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void cobblemonFlexibleStarters$sendDetails(ServerPlayer player, CallbackInfo ci) {
        GeneralPlayerData data = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player);
        if (!StarterGrantHandler.isNewPlayer(data)) {
            // Cobblemon itself didn't send OpenStarterUIPacket in this case; nothing to detail.
            return;
        }

        List<StarterCategory> starterList = Cobblemon.INSTANCE.getStarterHandler().getStarterList(player);
        List<StarterDetailsPacket.Entry> entries = new ArrayList<>();
        for (StarterCategory category : starterList) {
            List<? extends PokemonProperties> pokemon = category.getPokemon();
            for (int i = 0; i < pokemon.size() && entries.size() < StarterDetailsPacket.MAX_ENTRIES; i++) {
                PokemonProperties properties = pokemon.get(i);
                String ball = properties.getPokeball();
                String nature = properties.getNature();
                entries.add(new StarterDetailsPacket.Entry(
                        category.getName(), i,
                        ball == null ? "" : ball,
                        nature == null ? "" : nature
                ));
            }
        }

        PacketDistributor.sendToPlayer(player, new StarterDetailsPacket(entries));
    }
}
