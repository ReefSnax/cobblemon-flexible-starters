package com.reefsnax.cobblemonflexiblestarters;

import com.mojang.logging.LogUtils;
import com.reefsnax.cobblemonflexiblestarters.client.ClientPayloads;
import com.reefsnax.cobblemonflexiblestarters.net.SelectStartersPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * Cobblemon Flexible Starters - a Cobblemon add-on that lets brand-new players pick several
 * starter Pokemon at once from the normal starter flow. The count is a server-authoritative
 * config ({@link Config#STARTER_COUNT}, clamped 1-6, default 2).
 *
 * <p>The value here must match the modId in {@code neoforge.mods.toml}.</p>
 */
@Mod(CobblemonFlexibleStarters.MODID)
public class CobblemonFlexibleStarters {
    public static final String MODID = "cobblemon_flexible_starters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CobblemonFlexibleStarters(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[Cobblemon Flexible Starters] loading - configurable multi-starter Cobblemon add-on");
        // SERVER config type: values live per-world/per-server and are synced to every
        // connecting client, so the starter count a client can submit is always the server's.
        // The explicit filename keeps the server-editable TOML alongside the public JAR name.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC,
                "cobblemon-flexible-starters-server.toml");
        modEventBus.addListener(CobblemonFlexibleStarters::registerPayloads);
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("2");
        // Client -> server: the player's starter picks from the picker screen.
        registrar.playToServer(
                SelectStartersPacket.TYPE,
                SelectStartersPacket.STREAM_CODEC,
                StarterGrantHandler::handle
        );
        // Server -> client: authoritative ball/nature details for display. This registration must
        // happen on both physical sides: the dedicated server owns the packet type when it sends,
        // while ClientPayloads gates its client-only implementation at delivery time.
        ClientPayloads.register(registrar);
        LOGGER.info("[Cobblemon Flexible Starters] registered starter packets");
    }
}
