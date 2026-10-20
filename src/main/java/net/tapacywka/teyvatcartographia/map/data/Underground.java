package net.tapacywka.teyvatcartographia.map.data;

import java.util.Objects;

/**
 * Whether the player is underground, and in which cave band ({@link MapDataManager}): at least {@code ROOF} solid
 * blocks (leaves left out) above the head, so a forest or the roof of a house does not count. A new state (or band)
 * holds only once it stayed the same for {@code TICKS} ticks, so the minimap does not flicker at a cave mouth.
 */
final class Underground {
    static final int ROOF = 4, TICKS = 10;

    private Integer current, candidate;
    private int ticks;

    /** The cave band for a player at block height {@code y} with {@code solidAbove} solid blocks counted over the head, or null. */
    static Integer band(int solidAbove, int y) {
        return solidAbove >= ROOF ? CaveScanner.band(y) : null;
    }

    /** The state this tick ({@code now}: a band or null); returns the state that holds. */
    Integer update(Integer now) {
        if (Objects.equals(now, current)) {
            ticks = 0;
        } else if (Objects.equals(now, candidate)) {
            if (++ticks >= TICKS) {
                current = now;
                ticks = 0;
            }
        } else {
            candidate = now;
            ticks = 0;
        }
        return current;
    }

    /** The state that holds: a cave band, or null above ground. */
    Integer current() {
        return current;
    }

    void reset() {
        current = candidate = null;
        ticks = 0;
    }
}
