package net.tapacywka.teyvatcartographia.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads the waypoints of Xaero's Minimap (Xaero's World Map shows the same ones). Layout on disk:
 * {@code <gameDir>/xaero/minimap/<world>/<dimension>/<sub-world>.txt} ({@code XaeroWaypoints/} before Minimap 24.3), one
 * {@code waypoint:name:initials:x:y:z:color:disabled:type:set:...} line per waypoint. Names write ":" as "§§", an unknown
 * height is "~", type 1 and 2 are death points. Dimension folders: {@code dim%0}, {@code dim%-1}, {@code dim%1}, other
 * dimensions {@code dim%<namespace>$<path>} with "/" as "%" and trailing dots as ",".
 */
public final class XaeroWaypoints {
    private XaeroWaypoints() {}

    private static final String DIM_PREFIX = "dim%";

    /** The Xaero folders that may hold waypoints, newest layout first; only those that exist. */
    public static List<Path> roots() {
        Path game = FMLPaths.GAMEDIR.get();
        List<Path> out = new ArrayList<>();
        for (Path p : List.of(game.resolve("xaero").resolve("minimap"), game.resolve("XaeroWaypoints"))) {
            if (Files.isDirectory(p)) out.add(p);
        }
        return out;
    }

    /** The folder of the world the player is in, or null if Xaero's Minimap has none for it. */
    public static Path currentWorldFolder() {
        List<String> names = currentWorldNames();
        for (Path root : roots()) {
            try (Stream<Path> s = Files.list(root)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    String n = p.getFileName().toString();
                    if (Files.isDirectory(p) && names.stream().anyMatch(n::equalsIgnoreCase)) return p;
                }
            } catch (IOException ignored) {
                // Not readable: like no folder.
            }
        }
        return null;
    }

    /** The names Xaero gives the current world's folder (with and without the server port). */
    private static List<String> currentWorldNames() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            Path name = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            return name == null ? List.of() : List.of(escape(name.toString()));
        }
        ServerData data = mc.getCurrentServer();
        if (data == null || data.isRealm()) return List.of();
        String ip = data.ip.trim();
        List<String> out = new ArrayList<>(List.of(serverFolder(ip)));
        int port = ip.startsWith("[") ? ip.lastIndexOf("]:") + 1 : ip.indexOf(':') == ip.lastIndexOf(':') ? ip.indexOf(':') : -1;
        if (port > 0) out.add(serverFolder(ip.substring(0, port)));
        return out;
    }

    private static String serverFolder(String address) {
        String a = address.replace("[", "").replace("]", "").replace(":", "§");
        a = escape(a).trim();
        while (a.endsWith(".")) a = a.substring(0, a.length() - 1) + ",";
        return "Multiplayer_" + a;
    }

    private static String escape(String s) {
        return s.replace("_", "%us%").replace("/", "%fs%").replace("\\", "%bs%").replace("[", "%lb%").replace("]", "%rb%");
    }

    /**
     * All waypoints under a folder chosen by the player: a world folder (holding {@code dim%} folders) or one
     * dimension folder. Death points are left out; every waypoint becomes a custom star pin.
     */
    public static List<Marker> read(Path folder) throws IOException {
        List<Marker> out = new ArrayList<>();
        if (dimension(folder) != null) {
            readDimension(folder, out);
            return out;
        }
        try (Stream<Path> s = Files.list(folder)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isDirectory(p) && dimension(p) != null) readDimension(p, out);
            }
        }
        return out;
    }

    /** True if the folder is a Xaero world or dimension folder. */
    public static boolean isWaypointFolder(Path folder) {
        if (dimension(folder) != null) return true;
        try (Stream<Path> s = Files.list(folder)) {
            return s.anyMatch(p -> Files.isDirectory(p) && dimension(p) != null);
        } catch (IOException e) {
            return false;
        }
    }

    private static void readDimension(Path dir, List<Marker> out) throws IOException {
        ResourceKey<Level> dim = dimension(dir);
        try (Stream<Path> s = Files.list(dir)) {
            for (Path f : (Iterable<Path>) s::iterator) {
                if (!Files.isRegularFile(f) || !f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) continue;
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    Marker m = parse(line, dim);
                    if (m != null) out.add(m);
                }
            }
        }
    }

    /** One waypoint line, or null for other lines, death points and broken lines. */
    static Marker parse(String line, ResourceKey<Level> dim) {
        String[] f = line.split(":");
        if (f.length < 9 || !f[0].equalsIgnoreCase("waypoint")) return null;
        try {
            int type = Integer.parseInt(f[8]);
            if (type == 1 || type == 2) return null;
            String name = f[1].replace("§§", ":").trim();
            if (name.isEmpty()) name = f[2].replace("§§", ":").trim();
            int y = f[4].equals("~") ? Integer.MIN_VALUE : Integer.parseInt(f[4]);
            return Marker.custom(name, MarkerType.STAR, dim, Integer.parseInt(f[3]), y, Integer.parseInt(f[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The dimension of a Xaero dimension folder, or null if the folder is not one. */
    static ResourceKey<Level> dimension(Path dir) {
        Path fn = dir.getFileName();
        String n = fn == null ? "" : fn.toString();
        if (!n.startsWith(DIM_PREFIX)) return null;
        String id = n.substring(DIM_PREFIX.length());
        switch (id) {
            case "0": return Level.OVERWORLD;
            case "-1": return Level.NETHER;
            case "1": return Level.END;
            default: break;
        }
        String[] parts = id.split("\\$", 2);
        if (parts.length != 2) return null;
        ResourceLocation loc = ResourceLocation.tryBuild(parts[0], parts[1].replace('%', '/').replace(',', '.'));
        return loc == null ? null : ResourceKey.create(Registries.DIMENSION, loc);
    }
}
