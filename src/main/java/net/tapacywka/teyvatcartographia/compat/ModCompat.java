package net.tapacywka.teyvatcartographia.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.tapacywka.teyvatcartographia.config.MapConfig;

/**
 * Other mods the map knows about, and their settings (the Compatibility section of the settings shows a mod's
 * sub-section only while that mod is in the game):
 * <ul>
 *   <li>The Aether: its islands float over the void, so the cave map (under an island counts as underground) is off
 *       there unless asked for; the map draws it from its block textures, as its map colours look nothing like it.</li>
 *   <li>Xaero's Minimap: the import of its waypoints (which type they get, hidden ones, death points).</li>
 * </ul>
 */
public final class ModCompat {
    private ModCompat() {}

    public static final String AETHER = "aether";
    public static final ResourceKey<Level> AETHER_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(AETHER, "the_aether"));

    public static boolean aether() {
        return loaded(AETHER);
    }

    /** Xaero's Minimap or Xaero's World Map in the game, or the waypoints Xaero's Minimap left in it. */
    public static boolean xaero() {
        return loaded("xaerominimap") || loaded("xaeroworldmap") || !XaeroWaypoints.roots().isEmpty();
    }

    /** Whether the cave map works in this dimension (the general switch aside). */
    public static boolean caves(ResourceKey<Level> dimension) {
        return caves(dimension, MapConfig.get(MapConfig.AETHER_CAVES));
    }

    static boolean caves(ResourceKey<Level> dimension, boolean aetherCaves) {
        return dimension != AETHER_DIMENSION || aetherCaves;
    }

    /**
     * Whether the mod's own palette draws this dimension from its block textures (softened like the rest of the map)
     * rather than the blocks' map colours: the Aether, whose map colours look nothing like it.
     */
    public static boolean texturedWorld(ResourceKey<Level> dimension) {
        return dimension == AETHER_DIMENSION;
    }

    /**
     * A mod's name as its sub-section shows it: every word starts with a capital letter, the rest stays as written
     * ("xaero's minimap": "Xaero's Minimap", "The Aether" unchanged).
     */
    public static String titleCase(String name) {
        StringBuilder out = new StringBuilder(name.length());
        boolean start = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            out.append(start ? Character.toUpperCase(c) : c);
            start = Character.isWhitespace(c);
        }
        return out.toString();
    }

    private static boolean loaded(String modId) {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded(modId);
    }
}
