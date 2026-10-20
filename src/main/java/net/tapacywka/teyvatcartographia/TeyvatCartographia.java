package net.tapacywka.teyvatcartographia;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.tapacywka.teyvatcartographia.client.ClientSetup;
import net.tapacywka.teyvatcartographia.shared.SharedPinsNetwork;
import net.tapacywka.teyvatcartographia.shared.SharedPinsServer;
import org.slf4j.Logger;

/**
 * Teyvat Cartographia: a Genshin Impact styled world map and minimap. Mostly client side; the optional server side
 * keeps the interactive pins operators share with every player ({@link SharedPinsServer}). Both sides are optional:
 * players without the mod can join a server with it, and the mod works on servers without it (all pins local then).
 */
@Mod(TeyvatCartographia.MOD_ID)
public final class TeyvatCartographia {
    public static final String MOD_ID = "teyvatcartographia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TeyvatCartographia(IEventBus modBus, ModContainer container) {
        modBus.addListener(SharedPinsNetwork::register);
        NeoForge.EVENT_BUS.register(new SharedPinsServer());
        // The client classes are only touched on the client (a dedicated server has none of them).
        if (FMLEnvironment.dist.isClient()) ClientSetup.init(modBus, container);
    }
}
