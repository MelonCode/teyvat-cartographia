package net.tapacywka.teyvatcartographia.marker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;
import net.tapacywka.teyvatcartographia.map.WorldId;
import net.tapacywka.teyvatcartographia.nav.Navigation;
import net.tapacywka.teyvatcartographia.shared.SharedPinsServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The player's own pins and the navigation target of one world, persisted as
 * {@code <gameDir>/teyvatcartographia/<world>/markers.json}, plus the shared interactive pins of a server with the mod
 * ({@link SharedPins}, not saved here): {@link #count} and {@link #get} go over both. Main thread only.
 */
public final class MarkerStore {
    private MarkerStore() {}

    public static final int MAX_MARKERS = 100;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The contents of {@code markers.json}: the format version, the pins and the tracked target. */
    private record Saved(List<Marker> markers, Optional<Navigation.Target> target) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("version", 1).forGetter(s -> 1),
                Marker.CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(Saved::markers),
                Navigation.Target.CODEC.lenientOptionalFieldOf("target").forGetter(Saved::target)
        ).apply(i, (version, markers, target) -> new Saved(markers, target)));
    }

    private static final List<Marker> markers = new ArrayList<>();
    private static String worldId;
    /** Incremented on every change of the markers or the navigation target (all of them go through {@link #save()}). */
    private static int version;

    public static int version() {
        return version;
    }

    /** The player's own pins and the shared ones ({@link SharedPins}) of a dimension. */
    public static List<Marker> inDimension(ResourceKey<Level> dim) {
        List<Marker> out = new ArrayList<>();
        for (int i = 0, n = count(); i < n; i++) if (get(i).dimension == dim) out.add(get(i));
        return out;
    }

    /**
     * Index-based access for per-frame loops (no list or iterator allocation); 0 ≤ i < {@link #count()}: the
     * player's own pins first, then the shared ones.
     */
    public static Marker get(int i) {
        return i < markers.size() ? markers.get(i) : SharedPins.pins().get(i - markers.size());
    }

    public static Marker find(UUID id) {
        for (int i = 0, n = count(); i < n; i++) if (get(i).id.equals(id)) return get(i);
        return null;
    }

    /** True if a new pin of this type goes to the server's shared pins (interactive pins, when the server has the mod). */
    public static boolean sharedType(MarkerType type) {
        return type.operatorOnly() && SharedPins.available();
    }

    /**
     * An interactive pin only this player has and sees: not shared (a server without the mod, or not moved to the
     * shared pins), outside single player (where the integrated server shares them all).
     */
    public static boolean localOnly(MarkerType type, boolean shared) {
        return type.operatorOnly() && !shared && !net.minecraft.client.Minecraft.getInstance().isLocalServer();
    }

    /**
     * A new pin: an interactive one to the server's shared pins when it keeps them (it comes back with the server's
     * list), any other to the player's own. False when there is no room left.
     */
    public static boolean add(Marker m) {
        if (sharedType(m.type)) {
            if (SharedPins.count() >= SharedPinsServer.MAX_SHARED) return false;
            SharedPins.put(m);
            return true;
        }
        if (markers.size() >= MAX_MARKERS) return false;
        markers.add(m);
        save();
        return true;
    }

    /** What {@link #addAll} did with the pins it was given. */
    public record Added(int added, int duplicates, int overLimit, int operatorOnly) {}

    /**
     * Adds imported pins with one save. A pin with the same dimension, position and name as one already here is a
     * repeat and is left out; interactive pins only for operators; nothing beyond {@link #MAX_MARKERS}. A pin keeps
     * its id unless that id is taken. Interactive pins go to the server's shared pins when it keeps them (up to its
     * limit; the server checks again).
     */
    public static Added addAll(List<Marker> in) {
        return addAll(in, MarkerType.isOperator());
    }

    /** {@link #addAll(List)} for a player with or without operator rights. */
    static Added addAll(List<Marker> in, boolean operator) {
        int added = 0, duplicates = 0, overLimit = 0, operatorOnly = 0;
        List<Marker> toServer = new ArrayList<>();
        for (Marker m : in) {
            if (contains(m) || contains(toServer, m)) duplicates++;
            else if (m.type.operatorOnly() && !operator) operatorOnly++;
            else if (sharedType(m.type)) {
                if (SharedPins.count() + toServer.size() >= SharedPinsServer.MAX_SHARED) overLimit++;
                else {
                    toServer.add(m);
                    added++;
                }
            } else if (markers.size() >= MAX_MARKERS) overLimit++;
            else {
                Marker copy = new Marker(find(m.id) == null ? m.id : UUID.randomUUID(), m.name, m.type, m.dimension, m.x, m.y, m.z, m.created, false);
                copy.band = m.band;
                markers.add(copy);
                added++;
            }
        }
        SharedPins.putAll(toServer);
        if (added > toServer.size()) save();
        return new Added(added, duplicates, overLimit, operatorOnly);
    }

    /** The same dimension, position and name as one of the player's own or the shared pins. */
    private static boolean contains(Marker m) {
        for (int i = 0, n = count(); i < n; i++) if (same(get(i), m)) return true;
        return false;
    }

    private static boolean contains(List<Marker> list, Marker m) {
        for (Marker o : list) if (same(o, m)) return true;
        return false;
    }

    private static boolean same(Marker a, Marker b) {
        return a.dimension == b.dimension && a.x == b.x && a.y == b.y && a.z == b.z && a.name.equals(b.name);
    }

    /** The player's own pins of this world, for export (shared pins belong to the server). */
    public static List<Marker> all() {
        return List.copyOf(markers);
    }

    /** Removes a pin; a shared one on the server (for operators; it goes with the server's next list). */
    public static void remove(Marker m) {
        if (Navigation.isTracking(m)) Navigation.clear();
        if (m.shared) {
            SharedPins.remove(m);
            return;
        }
        markers.remove(m);
        save();
    }

    /** Renames a pin or changes its type; a shared one on the server (it changes with the server's next list). */
    public static void update(Marker m, String name, MarkerType type) {
        // An own pin given an interactive type on a server with shared pins becomes a shared one (same id: still tracked).
        if (m.shared || sharedType(type) && SharedPins.count() < SharedPinsServer.MAX_SHARED) {
            if (!m.shared) {
                markers.remove(m);
                save();
            }
            Marker changed = new Marker(m.id, name, type, m.dimension, m.x, m.y, m.z, m.created, false);
            changed.band = m.band;
            SharedPins.put(changed);
            return;
        }
        m.name = name;
        m.type = type;
        save();
    }

    /** The player's own pins and the shared ones, for loops with {@link #get}. */
    public static int count() {
        return markers.size() + SharedPins.pins().size();
    }

    /** The player's own pins (their limit is {@link #MAX_MARKERS}). */
    public static int localCount() {
        return markers.size();
    }

    /** The player's own interactive pins (for the offer to move them to the shared ones). */
    static List<Marker> localInteractive() {
        List<Marker> out = new ArrayList<>();
        for (Marker m : markers) if (m.type.operatorOnly()) out.add(m);
        return out;
    }

    /** After the offer: the player's own pins the server now lists (by id, or the same place and name) go. */
    static void dropUploaded(java.util.Set<UUID> uploading, List<Marker> shared) {
        boolean changed = markers.removeIf(m -> uploading.contains(m.id) && shared.stream().anyMatch(s -> s.id.equals(m.id) || same(s, m)));
        if (changed) save();
    }

    /** The server's list changed (the version makes the map, the minimap and the pin list redraw). */
    static void sharedChanged() {
        version++;
    }

    /** This world's folder (its {@code markers.json}), or null outside a world. */
    static Path worldFolder() {
        return worldId == null ? null : WorldId.root().resolve(worldId);
    }

    /** Loads markers for the given world (no-op if already loaded). {@code null} unloads. */
    public static void switchWorld(String id) {
        if (id == null ? worldId == null : id.equals(worldId)) return;
        version++;
        markers.clear();
        Navigation.restore(null);
        worldId = id;
        // The shared pins come from the server on joining, possibly before the world is known here: only leaving clears them.
        if (id == null) {
            SharedPins.reset();
            return;
        }
        Path file = file();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // A broken pin is left out and logged; the others load.
            Saved saved = Saved.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r))
                    .resultOrPartial(e -> TeyvatCartographia.LOGGER.error("Problem in {}: {}", file, e)).orElse(null);
            if (saved == null) return;
            markers.addAll(saved.markers());
            // A pin or an automatic point. Free points of older versions (neither) are dropped.
            saved.target().filter(t -> t.markerId() != null || t.auto() != null).ifPresent(Navigation::restore);
            TeyvatCartographia.LOGGER.info("Teyvat Cartographia: loaded {} markers for {}", markers.size(), id);
        } catch (IOException | RuntimeException e) {
            TeyvatCartographia.LOGGER.error("Could not read markers from {}", file, e);
        }
    }
    private static Path file() {
        return WorldId.root().resolve(worldId).resolve("markers.json");
    }

    public static void save() {
        version++;
        if (worldId == null) return;
        var root = Saved.CODEC.encodeStart(JsonOps.INSTANCE, new Saved(markers, Optional.ofNullable(Navigation.target())))
                .resultOrPartial(e -> TeyvatCartographia.LOGGER.error("Could not encode markers: {}", e)).orElse(null);
        if (root == null) return;        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("markers.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            TeyvatCartographia.LOGGER.error("Could not save markers to {}", file, e);
        }
    }
}
