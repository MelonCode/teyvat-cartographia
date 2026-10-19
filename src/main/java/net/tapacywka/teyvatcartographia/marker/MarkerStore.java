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
 * Custom markers and the navigation target of one world, persisted as
 * {@code <gameDir>/teyvatcartographia/<world>/markers.json}. Main thread only.
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

    public static List<Marker> inDimension(ResourceKey<Level> dim) {
        List<Marker> out = new ArrayList<>();
        for (Marker m : markers) if (m.dimension == dim) out.add(m);
        return out;
    }

    /** Index-based access for per-frame loops (no list or iterator allocation); 0 ≤ i < {@link #count()}. */
    public static Marker get(int i) {
        return markers.get(i);
    }

    public static Marker find(UUID id) {
        for (Marker m : markers) if (m.id.equals(id)) return m;
        return null;
    }

    public static boolean add(Marker m) {
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
     * its id unless that id is taken.
     */
    public static Added addAll(List<Marker> in) {
        boolean operator = MarkerType.isOperator();
        int added = 0, duplicates = 0, overLimit = 0, operatorOnly = 0;
        for (Marker m : in) {
            if (contains(m)) duplicates++;
            else if (m.type.operatorOnly() && !operator) operatorOnly++;
            else if (markers.size() >= MAX_MARKERS) overLimit++;
            else {
                Marker copy = new Marker(find(m.id) == null ? m.id : UUID.randomUUID(), m.name, m.type, m.dimension, m.x, m.y, m.z, m.created, false);
                copy.band = m.band;
                markers.add(copy);
                added++;
            }
        }
        if (added > 0) save();
        return new Added(added, duplicates, overLimit, operatorOnly);
    }

    private static boolean contains(Marker m) {
        for (Marker o : markers) {
            if (o.dimension == m.dimension && o.x == m.x && o.y == m.y && o.z == m.z && o.name.equals(m.name)) return true;
        }
        return false;
    }

    /** All saved pins of this world, for export. */
    public static List<Marker> all() {
        return List.copyOf(markers);
    }

    public static void remove(Marker m) {
        markers.remove(m);
        if (Navigation.isTracking(m)) Navigation.clear();
        save();
    }

    public static void update(Marker m, String name, MarkerType type) {
        m.name = name;
        m.type = type;
        save();
    }

    public static int count() {
        return markers.size();
    }

    /** Loads markers for the given world (no-op if already loaded). {@code null} unloads. */
    public static void switchWorld(String id) {
        if (id == null ? worldId == null : id.equals(worldId)) return;
        version++;
        markers.clear();
        Navigation.restore(null);
        worldId = id;
        if (id == null) return;
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
