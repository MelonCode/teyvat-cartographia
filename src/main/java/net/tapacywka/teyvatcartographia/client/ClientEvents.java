package net.tapacywka.teyvatcartographia.client;

import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.WorldMapScreen;
import net.tapacywka.teyvatcartographia.hud.WorldMarkerLayer;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.nav.FarPath;
import net.tapacywka.teyvatcartographia.nav.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Game-bus client event handlers (registered from {@link net.tapacywka.teyvatcartographia.TeyvatCartographia}). */
public final class ClientEvents {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MapDataManager data = MapDataManager.get();
        data.tick();
        MarkerStore.switchWorld(data.worldId());
        Navigation.tick();
        PathFinder.tick();
        FarPath.tick();
        if (mc.player == null) return;

        while (Keybinds.OPEN_MAP.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new WorldMapScreen());
        }
        while (Keybinds.TOGGLE_MINIMAP.consumeClick()) {
            MapConfig.MINIMAP_ENABLED.set(!MapConfig.get(MapConfig.MINIMAP_ENABLED));
            MapConfig.save();
        }
        while (Keybinds.MINIMAP_ZOOM_IN.consumeClick()) zoomMinimap(1);
        while (Keybinds.MINIMAP_ZOOM_OUT.consumeClick()) zoomMinimap(-1);
        while (Keybinds.CLEAR_TARGET.consumeClick()) {
            if (Navigation.target() != null) {
                Navigation.clear();
                mc.player.displayClientMessage(Component.translatable("teyvatcartographia.nav.cleared"), true);
            }
        }
        while (Keybinds.QUICK_PIN.consumeClick()) quickPin(mc);
        while (Keybinds.PULSE_TARGET.consumeClick()) WorldMarkerLayer.pulse();
    }

    /** One zoom step in or out (MapView.ZOOM_STEPS, the same as on the world map). */
    private static void zoomMinimap(int dir) {
        MapConfig.MINIMAP_ZOOM.set(MapView.stepZoom(MapConfig.get(MapConfig.MINIMAP_ZOOM), dir));
        MapConfig.save();
    }

    /** The quick pin key: the world map with the new pin editor where the player stands (no star pin made at once). */
    private static void quickPin(Minecraft mc) {
        if (mc.screen == null) mc.setScreen(WorldMapScreen.newPinHere());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            MapDataManager.get().onChunkLoaded(chunk);
        }
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MapDataManager.get().closeAll(true);
        MarkerStore.switchWorld(null);
    }
}
