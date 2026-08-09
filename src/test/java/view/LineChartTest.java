package view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

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

        chart.setSeries(new LineChart.Series(List.of(1.0, 2.0), "1.00", "2.00",
                "2026-01-05", "2026-01-09", "Close price for AAPL, 2 days."));
        assertEquals("Close price for AAPL, 2 days.",
                chart.getAccessibleContext().getAccessibleDescription());

        chart.setSeries(LineChart.Series.empty());
        assertEquals("No data.", chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void aPlottedSeriesCannotBeMutatedThroughTheListItWasBuiltFrom() {
        List<Double> source = new ArrayList<>(List.of(1.0, 2.0));
        LineChart.Series plotted = series(source);
        source.add(3.0);
        assertEquals(2, plotted.values().size());
    }

    /**
     * Wraps values in a series with placeholder labels; the labels are not what these tests are
     * about.
     *
     * @param values the points to plot
     * @return the series
     */
    private static LineChart.Series series(List<Double> values) {
        return new LineChart.Series(values, "low", "high", "start", "end", "A summary.");
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
