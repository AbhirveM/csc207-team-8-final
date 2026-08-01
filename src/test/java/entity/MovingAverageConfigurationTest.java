package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingAverageConfigurationTest {

    @Test
    void validWindowsAreStored() {
        final MovingAverageConfiguration configuration =
                new MovingAverageConfiguration(10, 50);

        assertEquals(10, configuration.getShortWindow());
        assertEquals(50, configuration.getLongWindow());
    }

    @Test
    void rejectsZeroShortWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(0, 50));
    }

    @Test
    void rejectsNegativeShortWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(-1, 50));
    }

    @Test
    void rejectsZeroLongWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(10, 0));
    }

    @Test
    void rejectsNegativeLongWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(10, -1));
    }

    @Test
    void rejectsEqualWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(10, 10));
    }

    @Test
    void rejectsShortWindowGreaterThanLongWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovingAverageConfiguration(50, 10));
    }
}