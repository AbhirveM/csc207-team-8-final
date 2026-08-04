package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomentumConfigurationTest {

    @Test
    void validConfigurationIsStored() {
        final MomentumConfiguration configuration =
                new MomentumConfiguration(14, 30.0, 70.0);

        assertEquals(14, configuration.getRsiPeriod());
        assertEquals(30.0, configuration.getBuyThreshold());
        assertEquals(70.0, configuration.getSellThreshold());
    }

    @Test
    void rejectsZeroRsiPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(0, 30.0, 70.0));
    }

    @Test
    void rejectsNegativeRsiPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(-1, 30.0, 70.0));
    }

    @Test
    void rejectsBuyThresholdBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, -1.0, 70.0));
    }

    @Test
    void rejectsBuyThresholdAboveOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 101.0, 70.0));
    }

    @Test
    void rejectsSellThresholdBelowZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 30.0, -1.0));
    }

    @Test
    void rejectsSellThresholdAboveOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 30.0, 101.0));
    }

    @Test
    void rejectsEqualThresholds() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 50.0, 50.0));
    }

    @Test
    void rejectsBuyThresholdGreaterThanSellThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new MomentumConfiguration(14, 70.0, 30.0));
    }
}
