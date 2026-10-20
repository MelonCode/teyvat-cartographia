package net.tapacywka.teyvatcartographia.marker;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Own pins and the import (no world open, so nothing is saved): the limit of 100, repeats left out, interactive pins
 * only for operators, ids kept unless taken.
 */
class ImportRulesTest {
    @BeforeEach
    @SuppressWarnings("unchecked")
    void noPins() throws ReflectiveOperationException {
        Field f = MarkerStore.class.getDeclaredField("markers");
        f.setAccessible(true);
        ((List<Marker>) f.get(null)).clear();
    }

    private static Marker pin(String name, MarkerType type, int x) {
        return Marker.custom(name, type, Level.OVERWORLD, x, 64, 0);
    }

    @Test
    void ownPinsStopAt100() {
        for (int i = 0; i < MarkerStore.MAX_MARKERS; i++) assertTrue(MarkerStore.add(pin("Pin " + i, MarkerType.STAR, i)));
        assertEquals(100, MarkerStore.localCount());
        assertFalse(MarkerStore.add(pin("One more", MarkerType.STAR, 500)), "the 101st is refused");
    }

    @Test
    void importLeavesOutRepeats() {
        MarkerStore.add(pin("Camp", MarkerType.BOSS, 10));
        List<Marker> file = List.of(pin("Camp", MarkerType.BOSS, 10), pin("Camp", MarkerType.BOSS, 11), pin("Other", MarkerType.BOSS, 10));
        MarkerStore.Added r = MarkerStore.addAll(file, false);
        assertEquals(2, r.added(), "same name elsewhere and another name here are new");
        assertEquals(1, r.duplicates(), "same place and name");
        assertEquals(3, MarkerStore.localCount());
    }

    @Test
    void repeatsInsideOneFileCountOnce() {
        MarkerStore.Added r = MarkerStore.addAll(List.of(pin("A", MarkerType.STAR, 1), pin("A", MarkerType.STAR, 1)), false);
        assertEquals(1, r.added());
        assertEquals(1, r.duplicates());
    }

    @Test
    void importStopsAtTheLimit() {
        for (int i = 0; i < 98; i++) MarkerStore.add(pin("Pin " + i, MarkerType.STAR, i));
        List<Marker> file = new ArrayList<>();
        for (int i = 0; i < 5; i++) file.add(pin("New " + i, MarkerType.STAR, 1000 + i));
        MarkerStore.Added r = MarkerStore.addAll(file, false);
        assertEquals(2, r.added());
        assertEquals(3, r.overLimit());
        assertEquals(100, MarkerStore.localCount());
    }

    @Test
    void interactivePinsOnlyForOperators() {
        List<Marker> file = List.of(pin("Statue", MarkerType.STATUE, 1), pin("Camp", MarkerType.BOSS, 2));
        MarkerStore.Added player = MarkerStore.addAll(file, false);
        assertEquals(1, player.added());
        assertEquals(1, player.operatorOnly());
        MarkerStore.Added op = MarkerStore.addAll(List.of(pin("Statue", MarkerType.STATUE, 1)), true);
        assertEquals(1, op.added(), "an operator gets it (as an own pin where the server keeps no shared pins)");
    }

    @Test
    void anImportedPinKeepsItsIdUnlessItIsTaken() {
        Marker mine = pin("Mine", MarkerType.MINE, 5);
        MarkerStore.add(mine);
        Marker sameId = new Marker(mine.id, "Elsewhere", MarkerType.MINE, Level.OVERWORLD, 900, 64, 900, 0, false);
        Marker fresh = pin("Fresh", MarkerType.STAR, 7);
        MarkerStore.addAll(List.of(sameId, fresh), false);
        assertTrue(MarkerStore.find(fresh.id) != null, "its own id");
        Marker copy = null;
        for (Marker m : MarkerStore.all()) if (m.name.equals("Elsewhere")) copy = m;
        assertTrue(copy != null);
        assertNotEquals(mine.id, copy.id, "a new id when the id is taken");
        assertEquals("Mine", MarkerStore.find(mine.id).name, "the pin already here is unchanged");
    }

    @Test
    void theCaveLayerComesAlong() {
        Marker cave = pin("Cave", MarkerType.STAR, 3);
        cave.band = -2;
        MarkerStore.addAll(List.of(cave), false);
        assertEquals(-2, MarkerStore.find(cave.id).band);
    }
}
