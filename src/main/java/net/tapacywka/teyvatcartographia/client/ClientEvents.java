package net.tapacywka.teyvatcartographia.client;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.gui.WorldMapScreen;
import net.tapacywka.teyvatcartographia.hud.WorldMarkerLayer;
import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.map.data.MapDataManager;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerStore;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import net.tapacywka.teyvatcartographia.marker.SharedPins;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.nav.FarPath;
import net.tapacywka.teyvatcartographia.nav.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        SharedPins.tickOffer();
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
        while (Keybinds.QUICK_PIN.consumeClick()) quickPin(mc.player);
        while (Keybinds.PULSE_TARGET.consumeClick()) WorldMarkerLayer.pulse();
    }

    /** One zoom step in or out (MapView.ZOOM_STEPS, the same as on the world map). */
    private static void zoomMinimap(int dir) {
        MapConfig.MINIMAP_ZOOM.set(MapView.stepZoom(MapConfig.get(MapConfig.MINIMAP_ZOOM), dir));
        MapConfig.save();
    }

    private static void quickPin(LocalPlayer player) {
        ResourceKey<Level> dim = player.level().dimension();
        Marker m = Marker.custom(Component.translatable("teyvatcartographia.marker.default_name", MarkerStore.localCount() + 1).getString(),
                MarkerType.STAR, dim, player.getBlockX(), player.getBlockY(), player.getBlockZ());
        m.band = net.tapacywka.teyvatcartographia.map.data.MapDataManager.get().undergroundBand(); // underground: on that cave layer
        if (MarkerStore.add(m)) {
            player.displayClientMessage(Component.translatable("teyvatcartographia.marker.added", m.name), true);
        } else {
            player.displayClientMessage(Component.translatable("teyvatcartographia.marker.limit", MarkerStore.MAX_MARKERS), true);
        }
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
