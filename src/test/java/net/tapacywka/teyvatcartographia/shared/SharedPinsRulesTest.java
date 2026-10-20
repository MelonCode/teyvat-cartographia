package net.tapacywka.teyvatcartographia.shared;

import net.minecraft.world.level.Level;
import net.tapacywka.teyvatcartographia.marker.Marker;
import net.tapacywka.teyvatcartographia.marker.MarkerType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the server does with an operator's edit of the shared pins (the rights are checked before it). */
class SharedPinsRulesTest {
    private static Marker pin(String name, MarkerType type, int x) {
        return Marker.custom(name, type, Level.OVERWORLD, x, 70, 0);
    }

    private static boolean put(List<Marker> pins, Marker... in) {
        return SharedPinsServer.apply(pins, new SharedPinsNetwork.Edit(List.of(in), List.of()));
    }

    @Test
    void interactivePinsOnly() {
        List<Marker> pins = new ArrayList<>();
        assertFalse(put(pins, pin("Camp", MarkerType.BOSS, 1)), "a custom pin is not shared");
        assertTrue(put(pins, pin("Statue", MarkerType.STATUE, 2)));
        assertEquals(1, pins.size());
    }

    @Test
    void namesAreTrimmedAndNotEmpty() {
        List<Marker> pins = new ArrayList<>();
        assertFalse(put(pins, pin("   ", MarkerType.WAYPOINT, 1)), "an empty name");
        put(pins, pin("  Gate  ", MarkerType.WAYPOINT, 2), pin("x".repeat(80), MarkerType.DOMAIN, 3));
        assertEquals("Gate", pins.get(0).name);
        assertEquals(48, pins.get(1).name.length(), "at most 48 characters");
    }

    @Test
    void aChangeKeepsThePlace() {
        List<Marker> pins = new ArrayList<>();
        Marker statue = pin("Statue", MarkerType.STATUE, 10);
        put(pins, statue);
        Marker moved = new Marker(statue.id, "Statue of the Seven", MarkerType.WAYPOINT, Level.NETHER, 999, 5, 999, 0, false);
        assertTrue(put(pins, moved));
        Marker now = pins.get(0);
        assertEquals("Statue of the Seven", now.name);
        assertEquals(MarkerType.WAYPOINT, now.type);
        assertEquals(Level.OVERWORLD, now.dimension, "the place stays");
        assertEquals(10, now.x);
        assertEquals(70, now.y);
        assertFalse(put(pins, moved), "the same again changes nothing");
    }

    @Test
    void aRepeatIsNotAddedTwice() {
        List<Marker> pins = new ArrayList<>();
        put(pins, pin("Gate", MarkerType.WAYPOINT, 5));
        assertFalse(put(pins, pin("Gate", MarkerType.WAYPOINT, 5)), "same place and name, another id");
        assertEquals(1, pins.size());
    }

    @Test
    void atMost200() {
        List<Marker> pins = new ArrayList<>();
        for (int i = 0; i < SharedPinsServer.MAX_SHARED + 5; i++) put(pins, pin("Gate " + i, MarkerType.WAYPOINT, i));
        assertEquals(200, pins.size());
    }

    @Test
    void removing() {
        List<Marker> pins = new ArrayList<>();
        Marker a = pin("A", MarkerType.WAYPOINT, 1), b = pin("B", MarkerType.STATUE, 2);
        put(pins, a, b);
        assertTrue(SharedPinsServer.apply(pins, new SharedPinsNetwork.Edit(List.of(), List.of(a.id))));
        assertEquals(1, pins.size());
        assertEquals(b.id, pins.get(0).id, "the server keeps copies: compared by id");
        assertFalse(SharedPinsServer.apply(pins, new SharedPinsNetwork.Edit(List.of(), List.of(UUID.randomUUID()))), "an unknown id");
    }

    @Test
    void theCaveLayerIsKept() {
        List<Marker> pins = new ArrayList<>();
        Marker cave = pin("Deep gate", MarkerType.WAYPOINT, 1);
        cave.band = -3;
        put(pins, cave);
        assertEquals(-3, pins.get(0).band);
    }
}
