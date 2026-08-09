package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * A single hand-painted line plot: one series, a framed plot area, and four axis labels.
 *
 * <p><strong>Why this is painted rather than pulled in.</strong> The app draws two charts, both
 * of them one line over a hundred-odd points. A charting library would add a dependency, a
 * theme to fight, and a default look - legends, tick marks, drop shadows, rounded plot frames -
 * built for a dashboard rather than for the terminal restraint the rest of this package
 * observes. Two hundred lines of {@code paintComponent} is cheaper than styling that away, and
 * it keeps every colour and every gap coming from {@link Theme} like the rest of {@code view}.
 *
 * <p><strong>The series colour is direction, not decoration.</strong> A line whose last value
 * is above its first is drawn in {@link Theme#UP}, below in {@link Theme#DOWN}, and equal in
 * {@link Theme#FG} - exactly the rule {@code TableStyler.SignedRenderer} applies to a signed
 * cell, lifted from a cell to a line. That colour is deliberately <em>redundant</em>: the same
 * direction is printed in words and with an explicit sign in the header band's meta slot beside
 * this chart, and again in {@link Series#summary()}, which becomes the accessible description.
 * Remove the colour entirely and no information is lost, which is the condition the project's
 * accessibility report commits to for every signal on screen.
 *
 * <p><strong>This class never formats a number.</strong> The four axis labels and the summary
 * arrive as finished strings from a presenter, in keeping with the rule that no class in
 * {@code view} composes user-facing text.
 *
 * <p><strong>Threading.</strong> {@link #setSeries(Series)} mutates Swing state and must be
 * called on the event dispatch thread, like any other component mutator.
 */
public final class LineChart extends JPanel {

    /** What the empty state prints in the middle of the plot. */
    private static final String NO_DATA_TEXT = "NO DATA";

    /** The spoken description of a chart with nothing to plot. */
    private static final String NO_DATA_SUMMARY = "No data.";

    /** Fewer points than this cannot make a line, so they render as the empty state. */
    private static final int MINIMUM_POINTS = 2;

    /** The span a flat series is given so the scaling divide stays finite. */
    private static final double FLAT_SERIES_SPAN = 1.0;

    /**
     * A series to plot, together with the axis labels and the spoken summary the presenter has
     * already formatted for it.
     *
     * @param values     the points to plot, oldest first; fewer than two renders the empty state
     * @param lowLabel   the smallest value, formatted, shown at the foot of the gutter
     * @param highLabel  the largest value, formatted, shown at the head of the gutter
     * @param startLabel the date of the first point, shown at the plot's left edge
     * @param endLabel   the date of the last point, shown at the plot's right edge
     * @param summary    one sentence describing the series, including its direction in words -
     *                   this becomes the chart's accessible description
     */
    public record Series(List<Double> values, String lowLabel, String highLabel,
                         String startLabel, String endLabel, String summary) {

        /**
         * Compact constructor, copying the values so a caller cannot mutate a plotted series.
         *
         * @throws NullPointerException if any component is null
         */
        public Series {
            values = List.copyOf(Objects.requireNonNull(values, "Values cannot be null"));
            Objects.requireNonNull(lowLabel, "Low label cannot be null");
            Objects.requireNonNull(highLabel, "High label cannot be null");
            Objects.requireNonNull(startLabel, "Start label cannot be null");
            Objects.requireNonNull(endLabel, "End label cannot be null");
            Objects.requireNonNull(summary, "Summary cannot be null");
        }

        /**
         * @return the series a chart shows when there is nothing selected or nothing has run
         */
        public static Series empty() {
            return new Series(List.of(), "", "", "", "", NO_DATA_SUMMARY);
        }
    }

    private Series series = Series.empty();

    /**
     * Builds an empty chart.
     *
     * @param accessibleName what a screen reader calls this chart, such as "Close price"; the
     *                       chart is focusable so a keyboard user can land on it and hear the
     *                       description {@link #setSeries(Series)} keeps current
     */
    public LineChart(String accessibleName) {
        setBackground(Theme.BG);
        setOpaque(true);
        // Zero width so the chart takes whatever its container gives it horizontally; the
        // height is the one dimension it insists on.
        setPreferredSize(new Dimension(0, Theme.CHART_HEIGHT));
        setFocusable(true);
        getAccessibleContext().setAccessibleName(accessibleName);
        getAccessibleContext().setAccessibleDescription(series.summary());
    }

    /**
     * Replaces what is plotted and repaints. Must be called on the event dispatch thread.
     *
     * @param newSeries the series to plot; its summary becomes the accessible description, so
     *                  the chart says in words what the line says in shape and colour
     * @throws NullPointerException if {@code newSeries} is null
     */
    public void setSeries(Series newSeries) {
        this.series = Objects.requireNonNull(newSeries, "Series cannot be null");
        getAccessibleContext().setAccessibleDescription(newSeries.summary());
        repaint();
    }

    /**
     * Paints the frame, the gridlines, and either the series or the empty state.
     *
     * @param graphics the surface to paint on
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        final Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            // Antialiasing is not on the house anti-pattern list, and a 1.5px diagonal without
            // it is a staircase rather than a line.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int plotX = Theme.CHART_GUTTER;
            final int plotY = 0;
            final int plotW = getWidth() - Theme.CHART_GUTTER - Theme.SM;
            final int plotH = getHeight() - Theme.CHART_FOOT;
            if (plotW < 1 || plotH < 1) {
                return;
            }

            paintFrame(g2, plotX, plotY, plotW, plotH);

            if (series.values().size() < MINIMUM_POINTS) {
                paintEmptyState(g2, plotX, plotY, plotW, plotH);
            }
            else {
                paintSeries(g2, plotX, plotY, plotW, plotH);
                paintAxisLabels(g2, plotX, plotY, plotW, plotH);
            }
        }
        finally {
            // The copy is ours; leaking it is invisible in a test and degrades a long session.
            g2.dispose();
        }
    }

    /**
     * Draws the plot's one-pixel boundary and its horizontal gridlines.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private static void paintFrame(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(Theme.RULE);
        g2.drawRect(plotX, plotY, plotW - 1, plotH - 1);
        for (int line = 1; line <= Theme.CHART_GRID_LINES; line++) {
            final int y = plotY + plotH * line / (Theme.CHART_GRID_LINES + 1);
            g2.drawLine(plotX + 1, y, plotX + plotW - 2, y);
        }
    }

    /**
     * Centres the empty-state word in the plot. This is what a watchlist with no selection and
     * a backtest that has not run both render.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private static void paintEmptyState(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.FG_FAINT);
        final FontMetrics metrics = g2.getFontMetrics();
        final int x = plotX + (plotW - metrics.stringWidth(NO_DATA_TEXT)) / 2;
        final int y = plotY + (plotH - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(NO_DATA_TEXT, x, y);
    }

    /**
     * Plots the series, scaled to fill the plot rectangle in both axes.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private void paintSeries(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        final List<Double> values = series.values();
        final int count = values.size();

        double min = values.get(0);
        double max = values.get(0);
        for (final Double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (max == min) {
            // A flat series has no span to scale against. Widening the top of the range keeps
            // the divide finite and draws the line along the bottom of the plot rather than
            // dividing by zero.
            max = min + FLAT_SERIES_SPAN;
        }

        final double spanX = plotW - 1.0;
        final double spanY = plotH - 1.0;
        final Path2D.Double path = new Path2D.Double();
        for (int index = 0; index < count; index++) {
            final double x = plotX + spanX * index / (count - 1.0);
            final double y = plotY + spanY - spanY * (values.get(index) - min) / (max - min);
            if (index == 0) {
                path.moveTo(x, y);
            }
            else {
                path.lineTo(x, y);
            }
        }

        g2.setColor(directionColour(values.get(0), values.get(count - 1)));
        g2.setStroke(new BasicStroke(Theme.CHART_STROKE));
        g2.draw(path);
    }

    /**
     * Draws the four supplied axis labels. The chart never composes one of these - it places
     * the strings the presenter handed it.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private void paintAxisLabels(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.FG_MUTED);
        final FontMetrics metrics = g2.getFontMetrics();
        final int gutterRight = plotX - Theme.XS;

        g2.drawString(series.highLabel(),
                gutterRight - metrics.stringWidth(series.highLabel()),
                plotY + metrics.getAscent());
        g2.drawString(series.lowLabel(),
                gutterRight - metrics.stringWidth(series.lowLabel()),
                plotY + plotH - metrics.getDescent());

        final int footBaseline = plotY + plotH + metrics.getAscent();
        g2.drawString(series.startLabel(), plotX, footBaseline);
        g2.drawString(series.endLabel(),
                plotX + plotW - metrics.stringWidth(series.endLabel()),
                footBaseline);
    }

    /**
     * Picks the series colour from the direction of the whole series.
     *
     * <p>The same rule {@code TableStyler.SignedRenderer} applies to a signed cell. It is
     * redundant with the summary printed beside the chart, by design - see the class javadoc.
     *
     * @param first the oldest value in the series
     * @param last the newest value in the series
     * @return the colour to stroke the line in
     */
    private static Color directionColour(double first, double last) {
        Color colour = Theme.FG;
        if (last > first) {
            colour = Theme.UP;
        }
        else if (last < first) {
            colour = Theme.DOWN;
        }
        return colour;
    }
}
