package net.tapacywka.teyvatcartographia.compat;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Other mods: the cave map and the theme's palette in the Aether follow its settings, elsewhere they are untouched. */
class ModCompatTest {
    @Test
    void noCavesInTheAetherUnlessAskedFor() {
        assertFalse(ModCompat.caves(ModCompat.AETHER_DIMENSION, false));
        assertTrue(ModCompat.caves(ModCompat.AETHER_DIMENSION, true));
        assertTrue(ModCompat.caves(Level.OVERWORLD, false), "the Aether setting leaves other dimensions alone");
    }

    @Test
    void onlyTheAetherIsDrawnFromItsTexturesInTheModsPalette() {
        assertTrue(ModCompat.texturedWorld(ModCompat.AETHER_DIMENSION));
        assertFalse(ModCompat.texturedWorld(Level.OVERWORLD));
        assertFalse(ModCompat.texturedWorld(Level.NETHER));
    }

    @Test
    void modNamesStartEveryWordWithACapital() {
        assertEquals("Xaero's Minimap", ModCompat.titleCase("xaero's minimap"));
        assertEquals("The Aether", ModCompat.titleCase("The aether"));
        assertEquals("JourneyMap", ModCompat.titleCase("JourneyMap"), "capitals inside a word stay");
        assertEquals("", ModCompat.titleCase(""));
    }
}
