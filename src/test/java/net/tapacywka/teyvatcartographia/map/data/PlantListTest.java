package net.tapacywka.teyvatcartographia.map.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The list of plants the map shows (Settings > Map > Plants): switching one on and off by its id. */
class PlantListTest {
    private static final List<String> DEFAULTS = List.of("minecraft:cactus", "minecraft:dead_bush", "minecraft:chorus_plant", "minecraft:chorus_flower");

    @Test
    void switchingOnAddsItOnce() {
        List<String> on = Plants.toggled(DEFAULTS, "minecraft:poppy", true);
        assertEquals(5, on.size());
        assertEquals("minecraft:poppy", on.get(4));
        assertEquals(on, Plants.toggled(on, "minecraft:poppy", true), "on again: still once");
    }

    @Test
    void switchingOffRemovesEveryCopy() {
        List<String> twice = List.of("minecraft:cactus", "minecraft:poppy", "minecraft:poppy");
        assertEquals(List.of("minecraft:cactus"), Plants.toggled(twice, "minecraft:poppy", false));
        assertEquals(DEFAULTS.subList(1, 4), Plants.toggled(DEFAULTS, "minecraft:cactus", false));
    }

    @Test
    void anUnknownIdOffChangesNothing() {
        assertEquals(DEFAULTS, Plants.toggled(DEFAULTS, "biomesoplenty:wildflower", false));
    }
}
