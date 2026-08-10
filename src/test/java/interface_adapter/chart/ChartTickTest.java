package interface_adapter.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link ChartTick} is a two-field carrier, so there is little to test beyond the null guard - but
 * that guard is what stops a missing label reaching {@code LineChart}, where it would print the
 * word "null" in the gutter of a chart on screen.
 */
class ChartTickTest {

    private static final double EXACT = 0.0;

    @Test
    void aTickCarriesItsPositionAndItsFinishedLabel() {
        ChartTick tick = new ChartTick(240.0, "240.00");

        assertEquals(240.0, tick.value(), EXACT);
        assertEquals("240.00", tick.label());
    }

    @Test
    void aTimeTickCarriesAPointIndexRatherThanAValue() {
        // The same record does both axes; on the time axis the value is an index into the series.
        ChartTick tick = new ChartTick(63, "2026-03-11");

        assertEquals(63.0, tick.value(), EXACT);
        assertEquals("2026-03-11", tick.label());
    }

    @Test
    void aNullLabelIsRejectedAtTheBoundaryRatherThanPrintedOnTheChart() {
        assertThrows(NullPointerException.class, () -> new ChartTick(1.0, null));
    }

    @Test
    void ticksHaveValueSemanticsSoASeriesCanBeComparedWhole() {
        assertEquals(new ChartTick(1.0, "one"), new ChartTick(1.0, "one"));
        assertEquals(new ChartTick(1.0, "one").hashCode(), new ChartTick(1.0, "one").hashCode());
        assertNotEquals(new ChartTick(1.0, "one"), new ChartTick(2.0, "one"));
        assertNotEquals(new ChartTick(1.0, "one"), new ChartTick(1.0, "two"));
    }
}
