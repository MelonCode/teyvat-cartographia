package net.tapacywka.teyvatcartographia.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both themes are complete: every colour of {@link Theme} in each {@code colors.json} and nothing unknown, every
 * {@link Icon} in each theme, and the built-in Genshin Impact pack in place.
 */
class ThemeAssetsTest {
    private static final Path RES = Path.of(System.getProperty("teyvatcartographia.projectDir", ".")).resolve("src/main/resources");
    private static final Path MINECRAFT = RES.resolve("assets/teyvatcartographia/themes/minecraft");
    private static final Path PACK = RES.resolve("resourcepacks/genshin");
    private static final Path GENSHIN = PACK.resolve("assets/teyvatcartographia/themes/genshin");

    @SuppressWarnings("unchecked")
    private static Set<String> themeKeys() throws ReflectiveOperationException {
        Field f = Theme.class.getDeclaredField("FIELDS");
        f.setAccessible(true);
        return ((Map<String, ?>) f.get(null)).keySet();
    }

    private static Set<String> colorKeys(Path theme) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(theme.resolve("colors.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject colors = root.has("colors") ? root.getAsJsonObject("colors") : root;
        Set<String> keys = new TreeSet<>();
        for (String k : colors.keySet()) keys.add(k.toLowerCase(Locale.ROOT));
        return keys;
    }

    private static void sameColours(Path theme) throws Exception {
        Set<String> expected = new TreeSet<>(themeKeys()), actual = colorKeys(theme);
        Set<String> missing = new TreeSet<>(expected), unknown = new TreeSet<>(actual);
        missing.removeAll(actual);
        unknown.removeAll(expected);
        assertTrue(missing.isEmpty(), theme.getFileName() + ": colours missing " + missing);
        assertTrue(unknown.isEmpty(), theme.getFileName() + ": unknown colours " + unknown);
    }

    @Test
    void minecraftColoursAreComplete() throws Exception {
        sameColours(MINECRAFT);
    }

    @Test
    void genshinColoursAreComplete() throws Exception {
        sameColours(GENSHIN);
    }

    @Test
    void everyIconInBothThemes() {
        List<String> missing = new ArrayList<>();
        for (Path theme : new Path[]{MINECRAFT, GENSHIN}) {
            for (Icon i : Icon.values()) {
                Path png = theme.resolve("icons/64").resolve(i.name().toLowerCase(Locale.ROOT) + ".png");
                if (!Files.isRegularFile(png)) missing.add(theme.getFileName() + "/" + png.getFileName());
            }
        }
        assertTrue(missing.isEmpty(), "icons missing: " + missing);
    }

    @Test
    void bothThemesHaveTheirFiles() {
        for (Path theme : new Path[]{MINECRAFT, GENSHIN}) {
            for (String f : new String[]{"theme.json", "colors.json", "fog.png"}) {
                assertTrue(Files.isRegularFile(theme.resolve(f)), theme.getFileName() + "/" + f);
            }
        }
    }

    @Test
    void theGenshinPackIsBuiltIn() throws IOException {
        JsonObject pack = JsonParser.parseString(Files.readString(PACK.resolve("pack.mcmeta"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("pack");
        assertEquals("Genshin Impact theme for Teyvat Cartographia", pack.getAsJsonObject("description").get("fallback").getAsString());
        // B16: a round pack icon of 256 x 256 (the size from the PNG header).
        try (InputStream in = Files.newInputStream(PACK.resolve("pack.png")); DataInputStream d = new DataInputStream(in)) {
            d.skipNBytes(16);
            assertEquals(256, d.readInt(), "pack.png width");
            assertEquals(256, d.readInt(), "pack.png height");
        }
    }
}
