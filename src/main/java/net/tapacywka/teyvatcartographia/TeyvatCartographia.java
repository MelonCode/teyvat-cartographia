package net.tapacywka.teyvatcartographia;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tapacywka.teyvatcartographia.client.ClientSetup;
import org.slf4j.Logger;

/**
 * Teyvat Cartographia: a Genshin Impact styled world map and minimap, client side only (the server side that shared
 * operators' interactive pins with every player was removed on 24.10.2026). On a dedicated server it does nothing.
 */
@Mod(TeyvatCartographia.MOD_ID)
public final class TeyvatCartographia {
    public static final String MOD_ID = "teyvatcartographia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TeyvatCartographia(IEventBus modBus, ModContainer container) {
        // The client classes are only touched on the client (a dedicated server has none of them).
        if (FMLEnvironment.dist.isClient()) ClientSetup.init(modBus, container);
    }
}
