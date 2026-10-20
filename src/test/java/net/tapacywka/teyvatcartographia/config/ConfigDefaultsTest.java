package net.tapacywka.teyvatcartographia.config;

import net.tapacywka.teyvatcartographia.map.MapView;
import net.tapacywka.teyvatcartographia.render.Theme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The settings a new game starts with (and "Reset" goes back to). */
class ConfigDefaultsTest {
    @Test
    void appearanceAndMap() {
        assertEquals(Theme.DEFAULT, MapConfig.THEME.getDefault(), "the Minecraft theme by default");
        assertEquals("minecraft", Theme.DEFAULT);
        assertEquals(1.0, MapConfig.MAP_DEFAULT_ZOOM.getDefault());
        assertFalse(MapConfig.MAP_SHOW_LABELS.getDefault());
        assertFalse(MapConfig.MAP_SHOW_GRID.getDefault());
    }

    @Test
    void minimap() {
        assertTrue(MapConfig.MINIMAP_ENABLED.getDefault());
        assertEquals(MapConfig.Corner.TOP_LEFT, MapConfig.MINIMAP_CORNER.getDefault());
        assertEquals(8, MapConfig.MINIMAP_OFFSET_X.getDefault());
        assertEquals(8, MapConfig.MINIMAP_OFFSET_Y.getDefault());
        assertEquals(48, MapConfig.MINIMAP_RADIUS.getDefault());
        assertEquals(1.0, MapConfig.MINIMAP_ZOOM.getDefault());
        assertEquals(MapConfig.Orientation.NORTH_UP, MapConfig.MINIMAP_ORIENTATION.getDefault());
        assertEquals(MapConfig.UpdateRate.RATE_30, MapConfig.MINIMAP_UPDATE_RATE.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_COORDS.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_BIOME.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_TIME.getDefault());
        assertEquals(MapConfig.TimeSource.GAME_TIME, MapConfig.MINIMAP_TIME_SOURCE.getDefault());
    }

    @Test
    void everyPinKindAndItsPathAreOn() {
        for (MapConfig.PinKind kind : MapConfig.PinKind.values()) {
            MapConfig.PinOptions o = MapConfig.pins(kind);
            for (var v : new net.neoforged.neoforge.common.ModConfigSpec.BooleanValue[]{o.enabled, o.map, o.minimap, o.beam, o.path, o.pathMap, o.pathMinimap, o.pathWorld}) {
                assertTrue(v.getDefault(), kind + " " + v.getPath());
            }
        }
    }

    @Test
    void cartography() {
        assertTrue(MapConfig.CAVES_ENABLED.getDefault());
        assertEquals(MapConfig.Detail.HIGH, MapConfig.DETAIL.getDefault());
        assertEquals(16, MapConfig.SCAN_RADIUS.getDefault());
        assertEquals(6, MapConfig.CHUNKS_PER_TICK.getDefault());
        assertEquals(160, MapConfig.MAX_CACHED_TILES.getDefault());
    }

    @Test
    void defaultZoomsAreZoomSteps() {
        assertEquals(MapConfig.MAP_DEFAULT_ZOOM.getDefault(), MapView.snapZoom(MapConfig.MAP_DEFAULT_ZOOM.getDefault()));
        assertEquals(MapConfig.MINIMAP_ZOOM.getDefault(), MapView.snapZoom(MapConfig.MINIMAP_ZOOM.getDefault()));
    }

    @Test
    void anUnloadedSettingReadsAsItsDefault() {
        // Before the config file is loaded (and in these tests) MapConfig.get must not throw.
        assertEquals(MapConfig.MINIMAP_CORNER.getDefault(), MapConfig.get(MapConfig.MINIMAP_CORNER));
    }
}
