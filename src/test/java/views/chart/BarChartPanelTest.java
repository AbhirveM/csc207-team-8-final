package views.chart;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart is drawn rather than composed from widgets, so the usual "was the component
 * added" assertions say nothing about it. These paint it headless instead: a layout mistake
 * in {@code paintComponent} throws, and the accessible description is the text a screen
 * reader gets in place of the picture, so it has to hold the real figures.
 */
class BarChartPanelTest {

    private static final int WIDTH = 760;

    @Test
    void describesEveryBarWithItsSignedValue() {
        final BarChartPanel panel = new BarChartPanel("Total return by strategy");

        panel.setBars(List.of(
                new BarChartPanel.Bar("AAPL Moving Average Crossover", 12.43, "12.43"),
                new BarChartPanel.Bar("TSLA RSI Momentum", -8.92, "-8.92")));

        assertEquals("AAPL Moving Average Crossover, +12.43. TSLA RSI Momentum, -8.92.",
                panel.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void leavesZeroUnsigned() {
        final BarChartPanel panel = new BarChartPanel("Total return by strategy");

        panel.setBars(List.of(new BarChartPanel.Bar("AAPL RSI Momentum", 0.0, "0.00")));

        assertEquals("AAPL RSI Momentum, 0.00.",
                panel.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void saysInWordsWhenThereIsNothingToChart() {
        final BarChartPanel panel = new BarChartPanel("Total return by strategy");

        final String description = panel.getAccessibleContext().getAccessibleDescription();

        assertFalse(description.isBlank());
        assertTrue(description.toLowerCase().contains("no completed backtests"));
    }

    @Test
    void keepsItselfOutOfTheKeyboardOrder() {
        assertFalse(new BarChartPanel("Total return by strategy").isFocusable());
    }

    @Test
    void growsTallerAsBarsAreAdded() {
        final BarChartPanel one = new BarChartPanel("Total return by strategy");
        final BarChartPanel four = new BarChartPanel("Total return by strategy");

        one.setBars(List.of(new BarChartPanel.Bar("AAPL RSI Momentum", 1.0, "1.00")));
        four.setBars(List.of(
                new BarChartPanel.Bar("A", 1.0, "1.00"),
                new BarChartPanel.Bar("B", 2.0, "2.00"),
                new BarChartPanel.Bar("C", 3.0, "3.00"),
                new BarChartPanel.Bar("D", 4.0, "4.00")));

        assertTrue(four.getPreferredSize().height > one.getPreferredSize().height);
    }

    @Test
    void paintsMixedGainsAndLossesWithoutFailing() {
        paint(List.of(
                new BarChartPanel.Bar("AAPL Moving Average Crossover", 12.43, "12.43"),
                new BarChartPanel.Bar("TSLA RSI Momentum", -8.92, "-8.92")));
    }

    @Test
    void paintsAnAllPositiveChartWithoutFailing() {
        paint(List.of(
                new BarChartPanel.Bar("AAPL Moving Average Crossover", 12.43, "12.43"),
                new BarChartPanel.Bar("MSFT RSI Momentum", 2.0, "2.00")));
    }

    @Test
    void paintsAFlatChartWithoutDividingByZero() {
        paint(List.of(
                new BarChartPanel.Bar("AAPL RSI Momentum", 0.0, "0.00"),
                new BarChartPanel.Bar("MSFT RSI Momentum", 0.0, "0.00")));
    }

    @Test
    void paintsTheEmptyStateWithoutFailing() {
        paint(Collections.emptyList());
    }

    @Test
    void survivesBeingPaintedTooNarrowToPlot() {
        final BarChartPanel panel = new BarChartPanel("Total return by strategy");
        panel.setBars(List.of(
                new BarChartPanel.Bar("A very long strategy label indeed", -50.0, "-50.00")));
        panel.setSize(40, panel.getPreferredSize().height);

        final BufferedImage image =
                new BufferedImage(40, panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D canvas = image.createGraphics();
        try {
            panel.paint(canvas);
        }
        finally {
            canvas.dispose();
        }
    }

    /**
     * Paints a chart at a realistic width, failing the test if drawing throws.
     *
     * @param bars the bars to draw
     */
    private static void paint(List<BarChartPanel.Bar> bars) {
        final BarChartPanel panel = new BarChartPanel("Total return by strategy");
        panel.setBars(bars);
        panel.setSize(WIDTH, panel.getPreferredSize().height);

        final BufferedImage image =
                new BufferedImage(WIDTH, panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D canvas = image.createGraphics();
        try {
            panel.paint(canvas);
        }
        finally {
            canvas.dispose();
        }
    }
}
