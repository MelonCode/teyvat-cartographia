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
        assertEquals(Theme.DEFAULT, MapConfig.THEME.getDefault(), "the built-in theme by default");
        assertEquals("default", Theme.DEFAULT);
        // Square minimap and rectangular interface, as the Minecraft theme of 0.1.x; the shapes start that way too.
        assertEquals(MapConfig.MinimapShape.SQUARE, MapConfig.MINIMAP_SHAPE.getDefault());
        assertEquals(MapConfig.InterfaceStyle.RECTANGULAR, MapConfig.INTERFACE_STYLE.getDefault());
        assertTrue(Theme.Style.squareMap);
        assertTrue(Theme.Style.squareUi);
        assertEquals(1.0, MapConfig.MAP_DEFAULT_ZOOM.getDefault());
        assertFalse(MapConfig.MAP_SHOW_LABELS.getDefault());
        assertFalse(MapConfig.MAP_SHOW_GRID.getDefault());
    }

    @Test
    void minimap() {
        assertTrue(MapConfig.MINIMAP_ENABLED.getDefault());
        assertEquals(MapConfig.Corner.TOP_LEFT, MapConfig.MINIMAP_CORNER.getDefault());
        assertEquals(4, MapConfig.MINIMAP_OFFSET_X.getDefault());
        assertEquals(4, MapConfig.MINIMAP_OFFSET_Y.getDefault());
        assertEquals(0.85, MapConfig.MINIMAP_OPACITY.getDefault());
        assertEquals(48, MapConfig.MINIMAP_RADIUS.getDefault());
        assertEquals(1.0, MapConfig.MINIMAP_ZOOM.getDefault());
        assertEquals(MapConfig.Orientation.NORTH_UP, MapConfig.MINIMAP_ORIENTATION.getDefault());
        assertEquals(MapConfig.UpdateRate.RATE_30, MapConfig.MINIMAP_UPDATE_RATE.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_COORDS.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_BIOME.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_TIME.getDefault());
        assertEquals(MapConfig.TimeSource.GAME_TIME, MapConfig.MINIMAP_TIME_SOURCE.getDefault());
        assertTrue(MapConfig.MINIMAP_SHOW_DAY.getDefault(), "the day counter is on");
        // Since 0.1.3: no inner padding, the outer 15% of the radius fading out completely, pills 2 units apart.
        assertEquals(0, MapConfig.MINIMAP_PADDING.getDefault());
        assertEquals(15, MapConfig.MINIMAP_FADE_WIDTH.getDefault());
        assertEquals(100, MapConfig.MINIMAP_FADE_STRENGTH.getDefault());
        assertEquals(2, MapConfig.MINIMAP_INFO_GAP.getDefault());
        assertEquals(0, MapConfig.MINIMAP_TEXT_SIZE.getDefault(), "Auto");
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
