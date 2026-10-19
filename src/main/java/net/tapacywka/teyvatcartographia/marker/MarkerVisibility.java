package net.tapacywka.teyvatcartographia.marker;

import net.tapacywka.teyvatcartographia.config.MapConfig;
import net.tapacywka.teyvatcartographia.nav.Navigation;

/** Which pins the world map and the minimap draw (the pin list shows them all). */
public final class MarkerVisibility {
    private MarkerVisibility() {}

    /** The kind of pin {@code m}: its settings ({@link MapConfig#pins}). */
    public static MapConfig.PinKind kind(Marker m) {
        if (m.automatic) return MapConfig.PinKind.SYSTEM;
        return m.type.category == MarkerType.Category.EXPLORATION ? MapConfig.PinKind.CUSTOM : MapConfig.PinKind.INTERACTIVE;
    }

    /** The kind of the tracked pin {@code t}, or null without a target (or when its pin is gone). */
    public static MapConfig.PinKind kind(Navigation.Target t) {
        if (t == null) return null;
        if (t.auto() != null) return MapConfig.PinKind.SYSTEM;
        Marker m = MarkerStore.find(t.markerId());
        return m == null ? null : kind(m);
    }

    /** Settings of the tracked pin's kind, or null without a target. */
    public static MapConfig.PinOptions targetOptions() {
        MapConfig.PinKind k = kind(Navigation.target());
        return k == null ? null : MapConfig.pins(k);
    }

    /**
     * Whether pin {@code m} is drawn at {@code scale} (GUI pixels per block) on the minimap or the world map:
     * <ul>
     *   <li>the tracked target always;</li>
     *   <li>below scale 1 nothing else;</li>
     *   <li>at 1 and above: by the settings of its kind, separately for the map and the minimap.</li>
     * </ul>
     */
    public static boolean shown(Marker m, double scale, boolean minimap) {
        if (Navigation.isTracking(m)) return true;
        if (pinsHidden(scale)) return false;
        MapConfig.PinOptions o = MapConfig.pins(kind(m));
        return minimap ? o.onMinimap() : o.onMap();
    }
    /**
     * Where pin {@code m} lies against the shown layer {@code band} (null: the surface): 1 on a layer above it, -1 below,
     * 0 on the same layer. Cave bands from the sea level up are above the surface, the others below it.
     */
    public static int layerRelation(Marker m, Integer band) {
        if (java.util.Objects.equals(m.band, band)) return 0;
        return Integer.compare(layerHeight(m.band), layerHeight(band));
    }

    /** Height order of a layer (twice the band's lowest Y; the surface just under the first band above the sea). */
    private static int layerHeight(Integer band) {
        if (band != null) return 2 * band * net.tapacywka.teyvatcartographia.map.data.CaveScanner.BAND;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return 2 * (level == null ? 63 : level.getSeaLevel()) + 1;
    }

    /** Below scale 1 the map and the minimap show only the tracked target. */
    public static boolean pinsHidden(double scale) {
        return scale < 1 - 1e-6;
    }

    /** Everything {@link #shown} depends on apart from the pin itself, as one number (for redraw checks). */
    public static int state(double scale, boolean minimap) {
        int state = pinsHidden(scale) ? 1 : 0;
        for (MapConfig.PinKind k : MapConfig.PinKind.values()) {
            MapConfig.PinOptions o = MapConfig.pins(k);
            if (minimap ? o.onMinimap() : o.onMap()) state |= 2 << k.ordinal();
        }
        return state;
    }
}
