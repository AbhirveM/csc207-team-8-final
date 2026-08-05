package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomentumConfigurationTest {

    @Test
    void validConfigurationIsStored() {
        final MomentumConfiguration configuration =
                new MomentumConfiguration(14, 30.0, 70.0);

        assertEquals(14, configuration.getPeriod());
        assertEquals(30.0, configuration.getOversoldThreshold());
        assertEquals(70.0, configuration.getOverboughtThreshold());
    }

    @Test
    void rejectsPeriodOfOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(1, 30.0, 70.0));
    }

    @Test
    void rejectsZeroPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(0, 30.0, 70.0));
    }

    @Test
    void rejectsNegativePeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(-1, 30.0, 70.0));
    }

    @Test
    void rejectsOversoldThresholdBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, -1.0, 70.0));
    }

    @Test
    void rejectsOversoldThresholdAboveOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 101.0, 70.0));
    }

    @Test
    void rejectsOverboughtThresholdBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 30.0, -1.0));
    }

    @Test
    void rejectsOverboughtThresholdAboveOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 30.0, 101.0));
    }

    @Test
    void rejectsEqualThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 50.0, 50.0));
    }

    @Test
    void rejectsReversedThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 70.0, 30.0));
    }
}