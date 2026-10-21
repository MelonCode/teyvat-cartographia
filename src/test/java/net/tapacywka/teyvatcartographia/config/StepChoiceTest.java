package net.tapacywka.teyvatcartographia.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** On/off settings with their parts as one choice slider: off, the map, both maps, everywhere; the clock likewise. */
class StepChoiceTest {
    @Test
    void eachStepSwitchesOnOneMorePart() {
        assertFalse(StepChoice.on(StepChoice.OFF));
        assertTrue(StepChoice.map(StepChoice.MAP) && !StepChoice.minimap(StepChoice.MAP) && !StepChoice.world(StepChoice.MAP));
        assertTrue(StepChoice.map(StepChoice.BOTH_MAPS) && StepChoice.minimap(StepChoice.BOTH_MAPS) && !StepChoice.world(StepChoice.BOTH_MAPS));
        assertTrue(StepChoice.map(StepChoice.EVERYWHERE) && StepChoice.minimap(StepChoice.EVERYWHERE) && StepChoice.world(StepChoice.EVERYWHERE));
    }

    @Test
    void theStepReadsBackFromItsParts() {
        for (int s = StepChoice.OFF; s <= StepChoice.EVERYWHERE; s++) {
            assertEquals(s, StepChoice.level(StepChoice.on(s), StepChoice.map(s), StepChoice.minimap(s), StepChoice.world(s)));
        }
    }

    @Test
    void offWinsAndOddCombinationsReadAsTheirLastPart() {
        assertEquals(StepChoice.OFF, StepChoice.level(false, true, true, true), "the old defaults with the kind off");
        assertEquals(StepChoice.EVERYWHERE, StepChoice.level(true, true, true, true), "the defaults");
        assertEquals(StepChoice.BOTH_MAPS, StepChoice.level(true, false, true, false), "minimap only");
        assertEquals(StepChoice.EVERYWHERE, StepChoice.level(true, false, false, true), "beam only");
    }

    @Test
    void theClockStepsAreOffThenEachSource() {
        assertEquals(0, StepChoice.clock(false, MapConfig.TimeSource.REAL_TIME));
        assertEquals(1, StepChoice.clock(true, MapConfig.TimeSource.GAME_TIME));
        assertEquals(2, StepChoice.clock(true, MapConfig.TimeSource.REAL_TIME));
        assertEquals(3, StepChoice.clock(true, MapConfig.TimeSource.BOTH));
        for (int s = 1; s <= 3; s++) assertEquals(s, StepChoice.clock(true, StepChoice.clockSource(s)));
    }
}
