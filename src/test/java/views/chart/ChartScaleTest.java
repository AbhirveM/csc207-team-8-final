package views.chart;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the chart arithmetic. No Swing is constructed anywhere here, which is the point of
 * extracting this class - the numbers behind a chart can be checked without a display.
 */
class ChartScaleTest {

    private static final double DELTA = 1e-9;

    @Test
    void rejectsNonPositivePixelLength() {
        assertThrows(IllegalArgumentException.class, () -> new ChartScale(0.0, 10.0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChartScale(0.0, 10.0, 0, -5));
    }

    @Test
    void rejectsNaNBounds() {
        assertThrows(IllegalArgumentException.class, () -> new ChartScale(Double.NaN, 10.0, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> new ChartScale(0.0, Double.NaN, 0, 100));
    }

    @Test
    void rejectsInvertedBounds() {
        assertThrows(IllegalArgumentException.class, () -> new ChartScale(10.0, 0.0, 0, 100));
    }

    @Test
    void exposesItsBounds() {
        final ChartScale scale = new ChartScale(-2.5, 7.5, 0, 100);

        assertEquals(-2.5, scale.getMinimum(), DELTA);
        assertEquals(7.5, scale.getMaximum(), DELTA);
    }

    @Test
    void mapsBoundsAndMidpointOntoPixels() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 10, 200);

        assertEquals(10, scale.toPixel(0.0));
        assertEquals(210, scale.toPixel(100.0));
        assertEquals(110, scale.toPixel(50.0));
    }

    @Test
    void invertedMappingRunsTheOtherWay() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 10, 200);

        assertEquals(210, scale.toPixelInverted(0.0));
        assertEquals(10, scale.toPixelInverted(100.0));
        assertEquals(110, scale.toPixelInverted(50.0));
    }

    @Test
    void doesNotClampValuesOutsideTheRange() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 0, 100);

        assertEquals(150, scale.toPixel(150.0));
        assertEquals(-50, scale.toPixel(-50.0));
    }

    @Test
    void spacesSeriesPointsEvenlyByIndex() {
        final ChartScale scale = new ChartScale(0.0, 1.0, 10, 200);

        assertEquals(10, scale.toPixelForIndex(0, 5));
        assertEquals(110, scale.toPixelForIndex(2, 5));
        assertEquals(210, scale.toPixelForIndex(4, 5));
    }

    @Test
    void placesASinglePointAtTheOrigin() {
        final ChartScale scale = new ChartScale(0.0, 1.0, 10, 200);

        assertEquals(10, scale.toPixelForIndex(0, 1));
    }

    @Test
    void rejectsNonPositivePointCount() {
        final ChartScale scale = new ChartScale(0.0, 1.0, 0, 100);

        assertThrows(IllegalArgumentException.class, () -> scale.toPixelForIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> scale.toPixelForIndex(0, -3));
    }

    @Test
    void coversASeriesWithItsOwnBounds() {
        final ChartScale scale = ChartScale.forSeries(Arrays.asList(3.0, 11.0, 7.0), 0, 100);

        assertEquals(3.0, scale.getMinimum(), DELTA);
        assertEquals(11.0, scale.getMaximum(), DELTA);
    }

    @Test
    void widensAFlatSeriesSoItDoesNotDivideByZero() {
        final ChartScale scale = ChartScale.forSeries(Arrays.asList(7.0, 7.0, 7.0), 0, 100);

        assertEquals(6.5, scale.getMinimum(), DELTA);
        assertEquals(7.5, scale.getMaximum(), DELTA);
        assertEquals(50, scale.toPixel(7.0));
    }

    @Test
    void rejectsAnAbsentOrEmptySeries() {
        assertThrows(IllegalArgumentException.class, () -> ChartScale.forSeries(null, 0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> ChartScale.forSeries(Collections.emptyList(), 0, 100));
    }

    @Test
    void rejectsUnplottableValuesInASeries() {
        final List<Double> withNull = Arrays.asList(1.0, null, 3.0);
        final List<Double> withNaN = Arrays.asList(1.0, Double.NaN);
        final List<Double> withInfinity = Arrays.asList(1.0, Double.POSITIVE_INFINITY);

        assertThrows(IllegalArgumentException.class, () -> ChartScale.forSeries(withNull, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> ChartScale.forSeries(withNaN, 0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> ChartScale.forSeries(withInfinity, 0, 100));
    }

    @Test
    void pullsAnAllPositiveSeriesDownToZero() {
        final ChartScale scale =
                ChartScale.forSeriesIncludingZero(Arrays.asList(10.0, 20.0), 0, 100);

        assertEquals(0.0, scale.getMinimum(), DELTA);
        assertEquals(20.0, scale.getMaximum(), DELTA);
    }

    @Test
    void pullsAnAllNegativeSeriesUpToZero() {
        final ChartScale scale =
                ChartScale.forSeriesIncludingZero(Arrays.asList(-30.0, -10.0), 0, 100);

        assertEquals(-30.0, scale.getMinimum(), DELTA);
        assertEquals(0.0, scale.getMaximum(), DELTA);
    }

    @Test
    void leavesASeriesThatAlreadySpansZeroAlone() {
        final ChartScale scale =
                ChartScale.forSeriesIncludingZero(Arrays.asList(-5.0, 12.0), 0, 100);

        assertEquals(-5.0, scale.getMinimum(), DELTA);
        assertEquals(12.0, scale.getMaximum(), DELTA);
    }

    @Test
    void reportsWhetherZeroIsOnTheAxis() {
        assertTrue(new ChartScale(-5.0, 5.0, 0, 100).includesZero());
        assertTrue(new ChartScale(0.0, 5.0, 0, 100).includesZero());
        assertTrue(new ChartScale(-5.0, 0.0, 0, 100).includesZero());
        assertFalse(new ChartScale(1.0, 5.0, 0, 100).includesZero());
        assertFalse(new ChartScale(-5.0, -1.0, 0, 100).includesZero());
    }

    @Test
    void choosesRoundTicksAcrossAWholeRange() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 0, 200);

        assertEquals(Arrays.asList(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), rounded(scale.ticks(5)));
    }

    @Test
    void choosesRoundTicksBelowOne() {
        final ChartScale scale = new ChartScale(0.0, 1.0, 0, 200);

        assertEquals(Arrays.asList(0.0, 0.2, 0.4, 0.6, 0.8, 1.0), rounded(scale.ticks(5)));
    }

    @Test
    void choosesRoundTicksAcrossZero() {
        final ChartScale scale = new ChartScale(-5.0, 15.0, 0, 200);

        assertEquals(Arrays.asList(-5.0, 0.0, 5.0, 10.0, 15.0), rounded(scale.ticks(4)));
    }

    @Test
    void ticksSpanTheFullRange() {
        final ChartScale scale = new ChartScale(3.0, 97.0, 0, 200);
        final List<Double> ticks = scale.ticks(5);

        assertTrue(ticks.get(0) <= scale.getMinimum());
        assertTrue(ticks.get(ticks.size() - 1) >= scale.getMaximum());
    }

    @Test
    void defaultTickCountProducesTicks() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 0, 200);

        assertEquals(rounded(scale.ticks(5)), rounded(scale.ticks()));
    }

    @Test
    void rejectsNonPositiveTickCount() {
        final ChartScale scale = new ChartScale(0.0, 100.0, 0, 200);

        assertThrows(IllegalArgumentException.class, () -> scale.ticks(0));
        assertThrows(IllegalArgumentException.class, () -> scale.ticks(-2));
    }

    @Test
    void handlesAZeroWidthRangeWhenChoosingTicks() {
        final ChartScale scale = new ChartScale(5.0, 5.0, 0, 100);

        assertEquals(Arrays.asList(5.0), rounded(scale.ticks(5)));
    }

    /**
     * Rounds tick values so accumulated floating-point error does not break equality.
     *
     * @param ticks the values to round
     * @return the same values rounded to six decimal places
     */
    private static List<Double> rounded(List<Double> ticks) {
        return ticks.stream()
                .map(tick -> Math.round(tick * 1_000_000.0) / 1_000_000.0)
                .toList();
    }
}
