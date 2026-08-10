package interface_adapter.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link AxisScale} decides what numbers a user reads off a chart, so it is worth testing
 * directly rather than through the plot it feeds - a wrong bound there shows up as a line that
 * looks slightly wrong, which nothing can fail on.
 *
 * <p>The cases are the four ways the rounding can go badly: a realistic price range, a series with
 * no extent at all, a range small enough that the step has to go below 1.0, and a range straddling
 * zero, where an axis that skipped zero would be actively misleading.
 */
class AxisScaleTest {

    private static final double EXACT = 0.0;
    private static final int INTERVALS = 4;

    @Test
    void aRealisticPriceRangeRoundsToNumbersWorthPrinting() {
        // The range the offline AAPL series actually produces. Before this class the gridlines
        // landed on 225.88, 235.03, 244.17 and 253.32, and none of them was labelled.
        AxisScale scale = AxisScale.forRange(216.74, 262.46, INTERVALS);

        assertEquals(200.0, scale.lowerBound(), EXACT);
        assertEquals(280.0, scale.upperBound(), EXACT);
        assertEquals(20.0, scale.step(), EXACT);
        assertEquals(List.of(200.0, 220.0, 240.0, 260.0, 280.0), scale.tickValues());
    }

    @Test
    void theBoundsClearTheDataSoTheLineNeverTouchesTheFrame() {
        // This is the whole reason the padding exists: scaling to the raw extremes made a 2%
        // drift and a 50% crash fill the plot identically.
        AxisScale scale = AxisScale.forRange(216.74, 262.46, INTERVALS);

        assertTrue(scale.lowerBound() < 216.74, "the axis foot must sit below the lowest value");
        assertTrue(scale.upperBound() > 262.46, "the axis head must sit above the highest value");
    }

    @Test
    void aFlatSeriesGetsAnAxisRatherThanADivideByZero() {
        AxisScale scale = AxisScale.forRange(50.0, 50.0, INTERVALS);

        assertTrue(scale.upperBound() > scale.lowerBound());
        assertTrue(scale.step() > 0.0);
        assertTrue(scale.lowerBound() < 50.0 && scale.upperBound() > 50.0,
                "a flat series should sit inside its axis, not on an edge of it");
    }

    @Test
    void aSingleValueRangeBehavesLikeAFlatOne() {
        // Reached when a period narrows the window to one close.
        AxisScale scale = AxisScale.forRange(7.5, 7.5, INTERVALS);

        assertTrue(scale.upperBound() > scale.lowerBound());
        assertTrue(scale.tickValues().size() >= 2);
    }

    @Test
    void aRangeSmallerThanOneStillGetsSensibleSteps() {
        // A penny stock, or any series the step has to drop below 1.0 for. Rounding to whole
        // numbers here would collapse the axis onto two ticks.
        AxisScale scale = AxisScale.forRange(0.12, 0.38, INTERVALS);

        assertTrue(scale.step() < 1.0, "step was " + scale.step());
        assertTrue(scale.step() > 0.0);
        assertTrue(scale.lowerBound() < 0.12);
        assertTrue(scale.upperBound() > 0.38);
        assertTrue(scale.tickValues().size() >= 3);
    }

    @Test
    void aRangeSpanningZeroPutsATickOnZero() {
        // An axis that crossed zero without marking it would leave the reader guessing where
        // gains become losses.
        AxisScale scale = AxisScale.forRange(-5.0, 15.0, INTERVALS);

        assertTrue(scale.lowerBound() < 0.0);
        assertTrue(scale.upperBound() > 15.0);
        assertTrue(scale.tickValues().contains(0.0), "ticks were " + scale.tickValues());
    }

    @Test
    void anEntirelyNegativeRangeRoundsOutwardsInBothDirections() {
        AxisScale scale = AxisScale.forRange(-90.0, -40.0, INTERVALS);

        assertTrue(scale.lowerBound() < -90.0);
        assertTrue(scale.upperBound() > -40.0);
    }

    @Test
    void theLastTickLandsExactlyOnTheUpperBound() {
        // Computed as lowerBound + index * step rather than by repeated addition. Accumulated
        // floating-point error would drop the top tick, leaving the head of the axis unlabelled.
        AxisScale scale = AxisScale.forRange(0.1, 0.7, INTERVALS);
        List<Double> ticks = scale.tickValues();

        assertEquals(scale.lowerBound(), ticks.get(0), EXACT);
        assertEquals(scale.upperBound(), ticks.get(ticks.size() - 1), EXACT);
    }

    @Test
    void everyTickSitsInsideTheBoundsAndAscends() {
        AxisScale scale = AxisScale.forRange(216.74, 262.46, INTERVALS);
        List<Double> ticks = scale.tickValues();

        for (int index = 1; index < ticks.size(); index++) {
            assertTrue(ticks.get(index) > ticks.get(index - 1));
        }
        assertTrue(ticks.get(0) >= scale.lowerBound());
        assertTrue(ticks.get(ticks.size() - 1) <= scale.upperBound());
    }

    @Test
    void theBoundsAreAlwaysMultiplesOfTheStep() {
        // What makes the labels readable: a bound that is not a multiple of the step prints as
        // 216.74 rather than 200.
        AxisScale scale = AxisScale.forRange(1234.5, 9876.5, INTERVALS);

        assertEquals(0.0, Math.abs(scale.lowerBound() % scale.step()), 1e-9);
        assertEquals(0.0, Math.abs(scale.upperBound() % scale.step()), 1e-9);
    }

    @Test
    void aTargetOfOneIntervalIsAllowedAndZeroIsNot() {
        assertTrue(AxisScale.forRange(1.0, 2.0, 1).step() > 0.0);
        assertThrows(IllegalArgumentException.class, () -> AxisScale.forRange(1.0, 2.0, 0));
        assertThrows(IllegalArgumentException.class, () -> AxisScale.forRange(1.0, 2.0, -3));
    }

    @Test
    void anInfiniteOrNotANumberRangeIsRejectedRatherThanProducingAnUnpaintableAxis() {
        assertThrows(IllegalArgumentException.class,
                () -> AxisScale.forRange(Double.NaN, 10.0, INTERVALS));
        assertThrows(IllegalArgumentException.class,
                () -> AxisScale.forRange(0.0, Double.POSITIVE_INFINITY, INTERVALS));
    }

    @Test
    void theRecordItselfRefusesBoundsThatEncloseNothing() {
        assertThrows(IllegalArgumentException.class, () -> new AxisScale(5.0, 5.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new AxisScale(5.0, 4.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new AxisScale(0.0, 10.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new AxisScale(0.0, 10.0, -1.0));
    }
}
