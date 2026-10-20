package net.tapacywka.teyvatcartographia.marker;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The kinds of pins: custom for everyone, interactive for operators only (shared on a server), automatic points. */
class MarkerTypeTest {
    @Test
    void interactiveTypesAreForOperatorsOnly() {
        Set<MarkerType> interactive = EnumSet.noneOf(MarkerType.class);
        for (MarkerType t : MarkerType.values()) if (t.operatorOnly()) interactive.add(t);
        assertEquals(EnumSet.of(MarkerType.WAYPOINT, MarkerType.STATUE, MarkerType.DOMAIN), interactive);
    }

    @Test
    void sevenCustomTypesForEveryone() {
        Set<MarkerType> custom = EnumSet.noneOf(MarkerType.class);
        for (MarkerType t : MarkerType.values()) if (t.category == MarkerType.Category.EXPLORATION) custom.add(t);
        assertEquals(EnumSet.of(MarkerType.BOSS, MarkerType.QUEST, MarkerType.STAR, MarkerType.FOOD, MarkerType.CLOVER,
                MarkerType.MINE, MarkerType.FISHING), custom);
        for (MarkerType t : custom) assertFalse(t.operatorOnly(), t.name());
    }

    @Test
    void automaticPointsAreNotPins() {
        assertFalse(MarkerType.SPAWN.custom);
        assertFalse(MarkerType.DEATH.custom);
        assertFalse(MarkerType.SPAWN.operatorOnly());
        assertTrue(MarkerType.STAR.custom);
        assertTrue(MarkerType.WAYPOINT.custom);
    }

    @Test
    void everyTypeHasAnIconAndAKind() {
        for (MarkerType t : MarkerType.values()) {
            assertNotNull(t.icon, t.name());
            assertNotNull(t.category, t.name());
        }
    }

    @Test
    void sectionHeadersHaveTexts() {
        assertEquals("teyvatcartographia.pin.category.exploration", MarkerType.Category.EXPLORATION.langKey());
        assertEquals("teyvatcartographia.pin.category.interaction", MarkerType.Category.INTERACTION.langKey());
    }
}
