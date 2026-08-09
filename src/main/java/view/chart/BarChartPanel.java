package view.chart;

import view.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Horizontal bars growing from a zero baseline, for comparing signed figures.
 *
 * <p>Bars run from a zero line rather than from the left edge, because a chart whose axis
 * starts at the smallest value makes a small loss look like a large one - the bar gets
 * measured from the bottom of the panel instead of from no-change. {@link ChartScale} owns
 * that decision; this class only paints what the scale works out.
 *
 * <p>Nothing here is load-bearing on colour. Direction is carried three times over: which
 * side of the baseline the bar sits on, an explicit {@code +} or {@code -} on every value,
 * and only then the colour. The chart also never replaces the table beside it - a reader who
 * cannot use it still has every figure in text.
 *
 * <p>Values arrive already formatted from the presenter and are drawn verbatim. The only
 * numbers this class formats are its own axis ticks, which are chart furniture rather than
 * data.
 */
public class BarChartPanel extends JPanel {

    /** Shown in place of the chart when there is nothing to draw. */
    private static final String EMPTY_TEXT = "No completed backtests to chart yet.";

    /** Roughly how many axis ticks to aim for; {@link ChartScale} rounds to readable steps. */
    private static final int TICK_TARGET = 4;

    /** Widest the label gutter may grow, as a fraction of the panel. */
    private static final double MAX_GUTTER_FRACTION = 0.35;

    /** Minimum drawn length of a bar, so a zero value still leaves a mark. */
    private static final int MINIMUM_BAR_LENGTH = 2;

    /** Height reserved under the plot for tick labels. */
    private static final int AXIS_HEIGHT = 18;

    /** Fallback plot width used only when the panel has not been laid out yet. */
    private static final int FALLBACK_WIDTH = 320;

    private List<Bar> bars = Collections.emptyList();

    /**
     * Builds an empty chart.
     *
     * <p>Not focusable on purpose. The table beside it already carries every figure in the
     * keyboard order, so a focus stop here would make a keyboard user tab through a second
     * copy of the same data to reach the next control.
     *
     * @param accessibleName what this chart is, for assistive technology
     */
    public BarChartPanel(String accessibleName) {
        setBackground(Theme.BG);
        setFocusable(false);
        getAccessibleContext().setAccessibleName(accessibleName);
        setBars(Collections.emptyList());
    }

    /**
     * One bar.
     *
     * @param label     what the bar names, drawn in the gutter
     * @param value     the figure the bar length comes from
     * @param valueText the same figure as the presenter formatted it, drawn at the bar's end
     */
    public record Bar(String label, double value, String valueText) {
    }

    /**
     * Replaces the charted data and repaints.
     *
     * <p>The accessible description is rebuilt here rather than at paint time: a screen
     * reader user never triggers a repaint, so describing the chart only while drawing it
     * would leave them reading a stale sentence.
     *
     * @param bars the bars to draw, in the order they should appear; must not be null
     */
    public final void setBars(List<Bar> bars) {
        this.bars = List.copyOf(bars);
        getAccessibleContext().setAccessibleDescription(describe());
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        final int rows = Math.max(bars.size(), 1);
        final int height = Theme.SM * 2 + rows * (Theme.ROW_HEIGHT + Theme.XS) + AXIS_HEIGHT;
        return new Dimension(FALLBACK_WIDTH, height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        final Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (bars.isEmpty()) {
                paintEmptyState(canvas);
            }
            else {
                paintBars(canvas);
            }
        }
        finally {
            canvas.dispose();
        }
    }

    /**
     * Says in words that there is nothing to draw, rather than leaving a blank rectangle that
     * looks like a chart which failed to load.
     *
     * @param canvas the surface to draw on
     */
    private void paintEmptyState(Graphics2D canvas) {
        canvas.setFont(Theme.FONT_UI);
        canvas.setColor(Theme.FG_FAINT);
        final FontMetrics metrics = canvas.getFontMetrics();
        final int y = getHeight() / 2 + metrics.getAscent() / 2;
        canvas.drawString(EMPTY_TEXT, Theme.SM, y);
    }

    /**
     * Draws the axis, the baseline and every bar.
     *
     * @param canvas the surface to draw on
     */
    private void paintBars(Graphics2D canvas) {
        canvas.setFont(Theme.FONT_MONO);
        final FontMetrics metrics = canvas.getFontMetrics();

        final List<Double> values = new ArrayList<>();
        boolean anyNegative = false;
        for (final Bar bar : bars) {
            values.add(bar.value());
            anyNegative = anyNegative || bar.value() < 0;
        }

        // A negative bar puts its value to the left of where the bar starts, and the leftmost a
        // bar can start is the edge of the plot - so without reserving that room here, a long
        // loss overwrites the gutter label beside it. Only reserved when a loss is present,
        // because an all-positive chart would otherwise carry a permanent empty margin.
        final int valueWidth = widestValue(metrics);
        final int gutter = gutterWidth(metrics);
        final int leftReserve;
        if (anyNegative) {
            leftReserve = valueWidth + Theme.XS;
        }
        else {
            leftReserve = 0;
        }
        final int plotLeft = gutter + Theme.SM + leftReserve;
        final int plotWidth = getWidth() - plotLeft - Theme.SM - valueWidth - Theme.SM;
        if (plotWidth <= 0) {
            return;
        }
        final ChartScale scale = ChartScale.forSeriesIncludingZero(values, plotLeft, plotWidth);
        final int plotBottom = getHeight() - AXIS_HEIGHT;

        paintAxis(canvas, scale, metrics, plotBottom);

        final int zeroX = scale.toPixel(0.0);
        canvas.setColor(Theme.RULE_STRONG);
        canvas.drawLine(zeroX, Theme.SM, zeroX, plotBottom);

        int y = Theme.SM;
        for (final Bar bar : bars) {
            paintBar(canvas, metrics, bar, scale, zeroX, y);
            y += Theme.ROW_HEIGHT + Theme.XS;
        }
    }

    /**
     * Draws one bar, its gutter label, and its value.
     *
     * @param canvas  the surface to draw on
     * @param metrics the metrics of the current font
     * @param bar     the bar to draw
     * @param scale   the scale mapping values onto x
     * @param zeroX   the x coordinate of the baseline
     * @param top     the y coordinate of the bar's top edge
     */
    private void paintBar(Graphics2D canvas, FontMetrics metrics, Bar bar, ChartScale scale,
                          int zeroX, int top) {
        final int valueX = scale.toPixel(bar.value());
        final int left = Math.min(zeroX, valueX);
        final int length = Math.max(Math.abs(valueX - zeroX), MINIMUM_BAR_LENGTH);
        final int textBaseline = top + (Theme.ROW_HEIGHT + metrics.getAscent()) / 2;

        canvas.setColor(directionColour(bar.value()));
        canvas.fillRect(left, top, length, Theme.ROW_HEIGHT);

        canvas.setColor(Theme.FG_MUTED);
        final String label = bar.label();
        canvas.drawString(label, Theme.SM, textBaseline);

        // The value sits outside the bar, on the side the bar grew towards, so it never has to
        // be legible against the fill.
        canvas.setColor(Theme.FG);
        final String text = signed(bar.valueText(), bar.value());
        final int textX;
        if (bar.value() < 0) {
            textX = left - Theme.XS - metrics.stringWidth(text);
        }
        else {
            textX = left + length + Theme.XS;
        }
        canvas.drawString(text, textX, textBaseline);
    }

    /**
     * Draws the vertical gridlines and their tick labels.
     *
     * @param canvas     the surface to draw on
     * @param scale      the scale to take ticks from
     * @param metrics    the metrics of the current font
     * @param plotBottom the y coordinate where the plot ends and the axis begins
     */
    private void paintAxis(Graphics2D canvas, ChartScale scale, FontMetrics metrics, int plotBottom) {
        final List<Double> ticks = scale.ticks(TICK_TARGET);
        final int decimals = decimalsFor(ticks);
        for (final Double tick : ticks) {
            final int x = scale.toPixel(tick);
            canvas.setColor(Theme.RULE);
            canvas.drawLine(x, Theme.SM, x, plotBottom);
            canvas.setColor(Theme.FG_FAINT);
            final String text = String.format("%." + decimals + "f", tick);
            canvas.drawString(text, x - metrics.stringWidth(text) / 2, plotBottom + metrics.getAscent());
        }
    }

    /**
     * Chooses how many decimal places the tick labels need to stay distinct.
     *
     * @param ticks the tick values
     * @return the number of decimal places to show
     */
    private static int decimalsFor(List<Double> ticks) {
        int decimals = 0;
        if (ticks.size() > 1) {
            final double step = Math.abs(ticks.get(1) - ticks.get(0));
            if (step < 1.0) {
                decimals = 2;
            }
        }
        return decimals;
    }

    /**
     * Prefixes an explicit {@code +} to a positive value.
     *
     * <p>A negative value already carries its own sign from the presenter, and zero is
     * genuinely unsigned. This mirrors what the signed table column does, so a figure reads
     * the same way in the chart and in the table beside it.
     *
     * @param text  the formatted value
     * @param value the value it represents
     * @return the text with a leading sign where one is needed
     */
    private static String signed(String text, double value) {
        String result = text;
        if (value > 0) {
            result = "+" + text;
        }
        return result;
    }

    /**
     * Picks the fill for a direction. Redundant with the sign and the side of the baseline.
     *
     * @param value the bar's value
     * @return the colour to fill with
     */
    private static Color directionColour(double value) {
        Color colour = Theme.FG_MUTED;
        if (value > 0) {
            colour = Theme.UP;
        }
        else if (value < 0) {
            colour = Theme.DOWN;
        }
        return colour;
    }

    /**
     * Measures the gutter needed by the widest label, capped so the plot keeps most of the
     * panel even when a label is very long.
     *
     * @param metrics the metrics of the current font
     * @return the gutter width in pixels
     */
    private int gutterWidth(FontMetrics metrics) {
        int widest = 0;
        for (final Bar bar : bars) {
            widest = Math.max(widest, metrics.stringWidth(bar.label()));
        }
        return Math.min(widest + Theme.SM, (int) (getWidth() * MAX_GUTTER_FRACTION));
    }

    /**
     * Measures the widest value string, so the plot can reserve room for it outside the bars.
     *
     * @param metrics the metrics of the current font
     * @return the width in pixels
     */
    private int widestValue(FontMetrics metrics) {
        int widest = 0;
        for (final Bar bar : bars) {
            widest = Math.max(widest, metrics.stringWidth(signed(bar.valueText(), bar.value())));
        }
        return widest;
    }

    /**
     * Builds the sentence a screen reader gets instead of the picture.
     *
     * @return a description naming every bar and its value
     */
    private String describe() {
        if (bars.isEmpty()) {
            return EMPTY_TEXT;
        }
        final StringBuilder description = new StringBuilder();
        for (final Bar bar : bars) {
            if (description.length() > 0) {
                description.append(". ");
            }
            description.append(bar.label())
                    .append(", ")
                    .append(signed(bar.valueText(), bar.value()));
        }
        return description.append('.').toString();
    }
}
