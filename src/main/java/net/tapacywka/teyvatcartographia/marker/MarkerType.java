package net.tapacywka.teyvatcartographia.marker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.tapacywka.teyvatcartographia.render.Icon;

/**
 * Marker types in two pin categories plus automatic points that come from real world data (spawn, last death):
 * <ul>
 *   <li>{@link Category#EXPLORATION} (shown as "Custom"): the seven custom pin types of Genshin Impact, for every player;</li>
 *   <li>{@link Category#INTERACTION} (shown as "Interactive"): waypoint, statue and domain; only operators create, edit and
 *       teleport to them.</li>
 * </ul>
 */
public enum MarkerType {
    BOSS(Icon.BOSS, Category.EXPLORATION),
    QUEST(Icon.QUEST, Category.EXPLORATION),
    STAR(Icon.STAR, Category.EXPLORATION),
    FOOD(Icon.FOOD, Category.EXPLORATION),
    CLOVER(Icon.CLOVER, Category.EXPLORATION),
    MINE(Icon.PICKAXE, Category.EXPLORATION),
    FISHING(Icon.HOOK, Category.EXPLORATION),
    // Declaration order = order in the pin editor's grid.
    WAYPOINT(Icon.WAYPOINT, Category.INTERACTION),
    STATUE(Icon.STATUE, Category.INTERACTION),
    DOMAIN(Icon.DOMAIN, Category.INTERACTION),
    // Automatic points (not selectable for custom pins)
    SPAWN(Icon.SPAWN, Category.AUTO),
    DEATH(Icon.GRAVE, Category.AUTO);

    public enum Category {
        EXPLORATION, INTERACTION, AUTO;

        /** Translation key of the section header in the pin editor. */
        public String langKey() {
            return "teyvatcartographia.pin.category." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public final Icon icon;
    public final Category category;
    /** Selectable for pins (custom and system). */
    public final boolean custom;

    MarkerType(Icon icon, Category category) {
        this.icon = icon;
        this.category = category;
        this.custom = category != Category.AUTO;
    }

    /** System pins: created, edited and teleported to by operators only. */
    public boolean operatorOnly() {
        return category == Category.INTERACTION;
    }

    /**
     * Operator check: permission level 2+ (what /tp itself requires). The server sends the player's op level
     * to the client and updates it on /op and /deop; in singleplayer it is granted only with cheats enabled.
     */
    public static boolean isOperator() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.hasPermissions(2);
    }

    /** The type of a saved pin, by name: never fails ({@link #parse} maps old and unknown names). */
    public static final Codec<MarkerType> PIN_CODEC = Codec.STRING.xmap(MarkerType::parse, MarkerType::name);

    /** The type of a tracked automatic point, by name: an error for anything but an automatic type. */
    public static final Codec<MarkerType> AUTO_CODEC = Codec.STRING.comapFlatMap(s -> {
        for (MarkerType t : values()) if (!t.custom && t.name().equals(s)) return DataResult.success(t);
        return DataResult.error(() -> "Not an automatic point: " + s);
    }, MarkerType::name);

    /** Parses a saved type; types from earlier versions are mapped to the closest current one. */
    private static MarkerType parse(String s) {
        if (s == null) return STAR;
        return switch (s) {
            case "HERB" -> CLOVER;
            case "POI", "TELEPORT", "HOME", "CHEST", "NOTE", "FLAG" -> STAR;
            default -> {
                try {
                    MarkerType t = valueOf(s);
                    yield t.custom ? t : STAR;
                } catch (IllegalArgumentException e) {
                    yield STAR;
                }
            }
        };
    }
}
