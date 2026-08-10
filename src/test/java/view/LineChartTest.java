package view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import interface_adapter.chart.AxisScale;
import interface_adapter.chart.ChartTick;

/**
 * {@link LineChart} is the only class in the project that paints, so it is the only one whose
 * arithmetic runs inside {@code paintComponent} where an exception is swallowed into a stack
 * trace on the console rather than failing anything. These tests paint it headless into an
 * image and assert it survives the shapes of data it will really be handed - nothing, a flat
 * line, two points, and far more points than it has pixels.
 *
 * <p>They deliberately assert on <em>not throwing</em> rather than on pixels: pixel assertions
 * on antialiased output are a maintenance burden and would pin the visual design in place,
 * which is the opposite of what this component is for.
 */
class LineChartTest {

    private static final int WIDTH = 400;

    @Test
    void anEmptySeriesPaintsTheEmptyStateWithoutThrowing() {
        LineChart chart = new LineChart("Close price");
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aSingleValuePaintsTheEmptyStateBecauseOnePointIsNotALine() {
        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(List.of(100.0)));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aFlatSeriesPaintsWithoutDividingByZero() {
        // Every value identical means max == min, which is the one input that can put a zero in
        // the scaling denominator.
        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(List.of(50.0, 50.0, 50.0, 50.0)));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aTwoPointSeriesPaints() {
        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(List.of(10.0, 20.0)));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aFallingSeriesPaints() {
        LineChart chart = new LineChart("Portfolio value");
        chart.setSeries(series(List.of(10000.0, 9500.0, 9000.0)));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aSeriesWithFarMorePointsThanPixelsPaints() {
        // A real run is around 120 points; this is an order of magnitude past that, painted
        // into a component narrower than the point count.
        LineChart chart = new LineChart("Close price");
        List<Double> values = new ArrayList<>();
        for (int index = 0; index < 5000; index++) {
            values.add(Math.sin(index / 10.0) * 20 + 200);
        }
        chart.setSeries(series(values));
        assertDoesNotThrow(() -> paint(chart, 120, Theme.CHART_HEIGHT));
    }

    @Test
    void aSeriesWithFarFewerPointsThanPixelsPaints() {
        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(List.of(1.0, 3.0, 2.0)));
        assertDoesNotThrow(() -> paint(chart, 1200, Theme.CHART_HEIGHT));
    }

    @Test
    void aChartSqueezedSmallerThanItsOwnGutterPaintsNothingRatherThanThrowing() {
        // The user can drag the split-pane divider to here. A negative plot width would throw
        // out of paintComponent on every repaint.
        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(List.of(10.0, 20.0, 15.0)));
        assertDoesNotThrow(() -> paint(chart, 1, 1));
        assertDoesNotThrow(() -> paint(chart, Theme.CHART_GUTTER, Theme.CHART_FOOT));
    }

    @Test
    void settingASeriesPutsItsSummaryOnTheAccessibleDescription() {
        // The summary is what a screen-reader user gets instead of the shape of the line, so it
        // has to follow the data rather than being set once at construction.
        LineChart chart = new LineChart("Close price");
        assertEquals("Close price", chart.getAccessibleContext().getAccessibleName());
        assertEquals("No data.", chart.getAccessibleContext().getAccessibleDescription());

        chart.setSeries(new LineChart.Series(List.of(1.0, 2.0), 0.0, 3.0,
                List.of(new ChartTick(0.0, "0.00"), new ChartTick(3.0, "3.00")),
                List.of(new ChartTick(0, "2026-01-05"), new ChartTick(1, "2026-01-09")),
                "Close price for AAPL, 2 days."));
        assertEquals("Close price for AAPL, 2 days.",
                chart.getAccessibleContext().getAccessibleDescription());

        chart.setSeries(LineChart.Series.empty());
        assertEquals("No data.", chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void aSeriesWhoseBoundsAreWiderThanItsDataStillPaints() {
        // The whole point of the rounded axis: the line no longer reaches either edge of the
        // frame, so no value maps to the very top or the very bottom pixel.
        LineChart chart = new LineChart("Close price");
        chart.setSeries(new LineChart.Series(List.of(216.74, 240.0, 262.46), 200.0, 280.0,
                List.of(new ChartTick(200.0, "200.00"), new ChartTick(240.0, "240.00"),
                        new ChartTick(280.0, "280.00")),
                List.of(new ChartTick(0, "2026-01-05"), new ChartTick(2, "2026-06-30")),
                "A summary."));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aSeriesWithNoTicksPaintsTheFrameAndNothingElse() {
        // An unlabelled gridline is decoration, so a series carrying no ticks must not invent
        // any. This is also what the empty state paints through.
        LineChart chart = new LineChart("Close price");
        chart.setSeries(new LineChart.Series(List.of(1.0, 2.0), 0.0, 3.0, List.of(), List.of(),
                "A summary."));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aNarrowChartDropsDateLabelsRatherThanOverprintingThem() {
        // Five dates in a chart barely wider than one of them. The test is that it survives and
        // that the label count is what decides, not that a particular date wins.
        LineChart chart = new LineChart("Close price");
        List<ChartTick> crowded = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            crowded.add(new ChartTick(index, "2026-01-0" + (index + 1)));
        }
        chart.setSeries(new LineChart.Series(List.of(1.0, 2.0, 3.0, 4.0, 5.0), 0.0, 6.0,
                List.of(new ChartTick(3.0, "3.00")), crowded, "A summary."));
        assertDoesNotThrow(() -> paint(chart, Theme.CHART_GUTTER + 40, Theme.CHART_HEIGHT));
        assertDoesNotThrow(() -> paint(chart, WIDTH, Theme.CHART_HEIGHT));
    }

    @Test
    void aShortSeriesIsPlottedPointForPointWithNoDecimation() {
        // The common case, and the one that must stay exact: 120 closes across 400 pixels.
        List<Double> values = new ArrayList<>();
        for (int index = 0; index < 120; index++) {
            values.add((double) index);
        }
        assertEquals(120, LineChart.plottedIndices(values, WIDTH).size());
        assertEquals(0, LineChart.plottedIndices(values, WIDTH).get(0));
        assertEquals(119, LineChart.plottedIndices(values, WIDTH).get(119));
    }

    @Test
    void aDecimatedSeriesStillPaintsItsSpike() {
        // A single one-day spike in a series far longer than the plot is wide. Stride sampling
        // would drop it and draw a calm week that never happened; min/max decimation cannot.
        List<Double> values = new ArrayList<>();
        for (int index = 0; index < 5000; index++) {
            values.add(100.0);
        }
        values.set(2500, 999.0);

        List<Integer> plotted = LineChart.plottedIndices(values, 120);
        assertTrue(plotted.size() < values.size(), "a 5000-point series should be decimated");
        assertTrue(plotted.contains(2500), "min/max decimation must keep the spike");
        assertEquals(0, plotted.get(0));
        assertEquals(4999, plotted.get(plotted.size() - 1));

        LineChart chart = new LineChart("Close price");
        chart.setSeries(series(values));
        assertDoesNotThrow(() -> paint(chart, 120, Theme.CHART_HEIGHT));
    }

    @Test
    void aDecimatedSeriesKeepsBothExtremesOfAColumnAndStaysInOrder() {
        // Two spikes, one high and one low, close enough together to share pixel columns.
        List<Double> values = new ArrayList<>();
        for (int index = 0; index < 4000; index++) {
            values.add(50.0);
        }
        values.set(1000, 0.5);
        values.set(1001, 99.5);

        List<Integer> plotted = LineChart.plottedIndices(values, 100);
        assertTrue(plotted.contains(1000), "the low extreme was dropped");
        assertTrue(plotted.contains(1001), "the high extreme was dropped");
        for (int index = 1; index < plotted.size(); index++) {
            assertTrue(plotted.get(index) > plotted.get(index - 1),
                    "decimated indexes must stay strictly ascending, or the line doubles back");
        }
    }

    @Test
    void aSeriesWhoseBoundsDoNotEncloseASpanIsRejected() {
        // Guarding here rather than in paintComponent: a zero span is a divide by zero on every
        // repaint, and the presenter that built it is where the mistake actually is.
        assertThrows(IllegalArgumentException.class,
                () -> new LineChart.Series(List.of(1.0), 5.0, 5.0, List.of(), List.of(), "A."));
        assertThrows(IllegalArgumentException.class,
                () -> new LineChart.Series(List.of(1.0), 5.0, 4.0, List.of(), List.of(), "A."));
    }

    @Test
    void aPlottedSeriesCannotBeMutatedThroughTheListItWasBuiltFrom() {
        List<Double> source = new ArrayList<>(List.of(1.0, 2.0));
        LineChart.Series plotted = series(source);
        source.add(3.0);
        assertEquals(2, plotted.values().size());
    }

    /**
     * Wraps values in a series scaled by the same rule a presenter uses, with placeholder tick
     * labels; the label text is not what these tests are about.
     *
     * @param values the points to plot
     * @return the series
     */
    private static LineChart.Series series(List<Double> values) {
        double low = 0.0;
        double high = 1.0;
        if (!values.isEmpty()) {
            low = Collections.min(values);
            high = Collections.max(values);
        }
        AxisScale scale = AxisScale.forRange(low, high, 4);
        List<ChartTick> valueTicks = new ArrayList<>();
        for (Double value : scale.tickValues()) {
            valueTicks.add(new ChartTick(value, "tick"));
        }
        List<ChartTick> timeTicks = new ArrayList<>();
        if (values.size() > 1) {
            timeTicks.add(new ChartTick(0, "2026-01-05"));
            timeTicks.add(new ChartTick(values.size() / 2, "2026-03-11"));
            timeTicks.add(new ChartTick(values.size() - 1, "2026-06-30"));
        }
        return new LineChart.Series(values, scale.lowerBound(), scale.upperBound(),
                valueTicks, timeTicks, "A summary.");
    }

    /**
     * Paints a chart at a given size into an off-screen image.
     *
     * @param chart the chart to paint
     * @param width the width to paint at
     * @param height the height to paint at
     */
    private static void paint(LineChart chart, int width, int height) {
        chart.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        try {
            chart.paint(graphics);
        }
        finally {
            graphics.dispose();
        }
    }
}
