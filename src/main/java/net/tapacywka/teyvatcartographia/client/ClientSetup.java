package net.tapacywka.teyvatcartographia.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;
import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.TeyvatConfigScreen;
import net.tapacywka.teyvatcartographia.hud.MinimapLayer;
import net.tapacywka.teyvatcartographia.hud.WorldMarkerLayer;
import net.tapacywka.teyvatcartographia.render.ThemeLoader;

/** Everything the mod sets up on the client: config, keys, HUD layers, themes, the built-in pack, events. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, MapConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new TeyvatConfigScreen(parent));
        modBus.addListener(Keybinds::register);
        modBus.addListener(ClientSetup::registerLayers);
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new ThemeLoader()));
        modBus.addListener(ClientSetup::addPacks);
        NeoForge.EVENT_BUS.register(new ClientEvents());
        NeoForge.EVENT_BUS.addListener(WorldMarkerLayer::onRenderLevelStage);
        // Development tests (package devtest) are not in the JAR: loaded by name, only when a test run asks.
        if (System.getProperty("teyvatcartographia.devtest") != null) {
            String test = System.getProperty("teyvatcartographia.devtest.class", "ResourcepackTest");
            try {
                NeoForge.EVENT_BUS.register(Class.forName("net.tapacywka.teyvatcartographia.devtest." + test)
                        .getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                TeyvatCartographia.LOGGER.error("The development test is not available in this build", e);
            }
        }
    }

    /**
     * The Genshin Impact theme as a built-in resource pack inside the JAR (resourcepacks/genshin), off by default: its
     * theme shows up in the settings once the player enables the pack.
     */
    private static void addPacks(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(TeyvatCartographia.MOD_ID, "resourcepacks/genshin"), PackType.CLIENT_RESOURCES,
                Component.literal("Genshin Impact"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        ResourceLocation minimap = ResourceLocation.fromNamespaceAndPath(TeyvatCartographia.MOD_ID, "minimap");
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, minimap, new MinimapLayer());
        // Just under the minimap, which it goes around (and which covers it if they ever meet).
        event.registerBelow(minimap, ResourceLocation.fromNamespaceAndPath(TeyvatCartographia.MOD_ID, "world_marker"), new WorldMarkerLayer());
    }
}
