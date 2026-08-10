package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JPanel;

import interface_adapter.chart.ChartTick;

/**
 * A single hand-painted line plot: one series, a framed plot area, a labelled grid, and a row of
 * dates along the foot.
 *
 * <p><strong>Why this is painted rather than pulled in.</strong> The app draws two charts, both
 * of them one line over a hundred-odd points. A charting library would add a dependency, a
 * theme to fight, and a default look - legends, tick marks, drop shadows, rounded plot frames -
 * built for a dashboard rather than for the terminal restraint the rest of this package
 * observes. Two hundred lines of {@code paintComponent} is cheaper than styling that away, and
 * it keeps every colour and every gap coming from {@link Theme} like the rest of {@code view}.
 *
 * <p><strong>The grid is data, not decoration.</strong> Every gridline sits on a value a presenter
 * chose and labelled, and the plot is scaled to the rounded bounds those values span rather than
 * to the series' own extremes. Scaling to the extremes made every series touch the top and the
 * bottom of the frame, so a stock that drifted 2% and one that halved looked the same; and it put
 * the gridlines on whatever numbers the pixel arithmetic happened to land on. See
 * {@code interface_adapter.chart.AxisScale}, which does the rounding.
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
 * <p><strong>This class never formats a number.</strong> Every tick label and the summary arrive
 * as finished strings from a presenter, in keeping with the rule that no class in {@code view}
 * composes user-facing text.
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

    /** The bounds an empty series carries, so the record's positive-span rule still holds. */
    private static final double EMPTY_UPPER_BOUND = 1.0;

    /**
     * Points per pixel column above which the path is decimated. Two allows a column to hold a
     * rise and a fall without any thinning at all.
     */
    private static final int POINTS_PER_COLUMN_LIMIT = 2;

    /**
     * A series to plot, together with the axis it is scaled against and the spoken summary the
     * presenter has already formatted for it.
     *
     * @param values      the points to plot, oldest first; fewer than two renders the empty state
     * @param lowerBound  the value at the bottom of the plot; below the smallest value, so the
     *                    line clears the frame
     * @param upperBound  the value at the top of the plot; must exceed {@code lowerBound}
     * @param valueTicks  the labelled marks down the left-hand gutter, each drawn as a gridline
     *                    at the height its value maps to
     * @param timeTicks   the labelled marks along the foot, each tick's value being a
     *                    <em>point index</em> into {@code values} rather than a value; the first
     *                    and last are drawn flush to the plot edges, and any label that would
     *                    collide with its neighbour is skipped
     * @param summary     one sentence describing the series, including its direction in words -
     *                    this becomes the chart's accessible description
     */
    public record Series(List<Double> values, double lowerBound, double upperBound,
                         List<ChartTick> valueTicks, List<ChartTick> timeTicks, String summary) {

        /**
         * Compact constructor, copying the lists so a caller cannot mutate a plotted series.
         *
         * @throws NullPointerException if any component is null
         * @throws IllegalArgumentException if the bounds do not enclose a positive span
         */
        public Series {
            values = List.copyOf(Objects.requireNonNull(values, "Values cannot be null"));
            valueTicks = List.copyOf(Objects.requireNonNull(valueTicks, "Value ticks cannot be null"));
            timeTicks = List.copyOf(Objects.requireNonNull(timeTicks, "Time ticks cannot be null"));
            Objects.requireNonNull(summary, "Summary cannot be null");
            if (!(upperBound > lowerBound)) {
                throw new IllegalArgumentException("Upper bound must exceed lower bound");
            }
        }

        /**
         * @return the series a chart shows when there is nothing selected or nothing has run
         */
        public static Series empty() {
            return new Series(List.of(), 0.0, EMPTY_UPPER_BOUND, List.of(), List.of(),
                    NO_DATA_SUMMARY);
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
     * Paints the frame, the labelled grid, and either the series or the empty state.
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
                paintValueLabels(g2, plotX, plotY, plotH);
                paintTimeLabels(g2, plotX, plotY, plotW, plotH);
            }
        }
        finally {
            // The copy is ours; leaking it is invisible in a test and degrades a long session.
            g2.dispose();
        }
    }

    /**
     * Draws the plot's one-pixel boundary and one gridline per value tick.
     *
     * <p>A series with no ticks - the empty state - gets the boundary and nothing else. An
     * unlabelled gridline is decoration, and the house style has no room for it.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private void paintFrame(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(Theme.RULE);
        g2.drawRect(plotX, plotY, plotW - 1, plotH - 1);
        for (final ChartTick tick : series.valueTicks()) {
            final int y = yFor(tick.value(), plotY, plotH);
            // The bounds land on the frame itself, which is already stroked.
            if (y > plotY && y < plotY + plotH - 1) {
                g2.drawLine(plotX + 1, y, plotX + plotW - 2, y);
            }
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
     * Plots the series against the axis bounds the presenter chose.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private void paintSeries(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        final List<Double> values = series.values();
        final Path2D.Double path = new Path2D.Double();
        boolean started = false;
        for (final int index : plottedIndices(values, plotW)) {
            final double x = xFor(index, values.size(), plotX, plotW);
            final double y = yFor(values.get(index), plotY, plotH);
            if (started) {
                path.lineTo(x, y);
            }
            else {
                path.moveTo(x, y);
                started = true;
            }
        }

        g2.setColor(directionColour(values.get(0), values.get(values.size() - 1)));
        g2.setStroke(new BasicStroke(Theme.CHART_STROKE));
        g2.draw(path);
    }

    /**
     * Chooses which points to put in the path.
     *
     * <p>Below {@value #POINTS_PER_COLUMN_LIMIT} points per pixel column the answer is "all of
     * them" and the path is exact, which is the case both charts are in today: 120 closes, or 252
     * for a one-year window, across four hundred-odd pixels. Above it a polyline with several
     * points in one column paints a blurred band rather than a line, so each column contributes
     * only its lowest and highest point, emitted in the order they occur.
     *
     * <p>Min/max rather than stride sampling, and the difference is not cosmetic: taking every
     * n-th point can drop a one-day spike entirely, so the chart would show a calm week that
     * never happened. Keeping both extremes of every column cannot lose one.
     *
     * @param values the whole series
     * @param plotW the plot's width in pixels; must be at least one
     * @return the indexes to plot, ascending, always including the first and last point
     */
    static List<Integer> plottedIndices(List<Double> values, int plotW) {
        final int count = values.size();
        final List<Integer> indices = new ArrayList<>();
        if (count <= plotW * POINTS_PER_COLUMN_LIMIT) {
            for (int index = 0; index < count; index++) {
                indices.add(index);
            }
            return indices;
        }

        int columnStart = 0;
        int lowest = 0;
        int highest = 0;
        for (int index = 0; index <= count; index++) {
            if (index == count || columnOf(index, count, plotW) != columnOf(columnStart, count, plotW)) {
                indices.add(Math.min(lowest, highest));
                if (lowest != highest) {
                    indices.add(Math.max(lowest, highest));
                }
                columnStart = index;
                lowest = index;
                highest = index;
            }
            if (index < count) {
                if (values.get(index) < values.get(lowest)) {
                    lowest = index;
                }
                if (values.get(index) > values.get(highest)) {
                    highest = index;
                }
            }
        }

        // The extremes of a column need not be its first and last point, so the endpoints of the
        // whole series are pinned separately: a line that starts a day late reads as a shorter
        // window, and the direction colour is taken from the true endpoints regardless.
        if (indices.get(0) != 0) {
            indices.add(0, 0);
        }
        if (indices.get(indices.size() - 1) != count - 1) {
            indices.add(count - 1);
        }
        return indices;
    }

    /**
     * @param index the point's position in the series
     * @param count how many points the series holds
     * @param plotW the plot's width
     * @return which pixel column the point falls in
     */
    private static int columnOf(int index, int count, int plotW) {
        return (int) ((long) index * plotW / count);
    }

    /**
     * Draws the value tick labels down the gutter, each beside its own gridline.
     *
     * <p>The top and bottom labels are nudged inside the component so neither is clipped by its
     * own edge; the rest sit centred on the line they belong to.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotH the plot's height
     */
    private void paintValueLabels(Graphics2D g2, int plotX, int plotY, int plotH) {
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.FG_MUTED);
        final FontMetrics metrics = g2.getFontMetrics();
        final int gutterRight = plotX - Theme.XS;
        final int halfInk = (metrics.getAscent() - metrics.getDescent()) / 2;

        for (final ChartTick tick : series.valueTicks()) {
            final int y = yFor(tick.value(), plotY, plotH);
            final int baseline = Math.max(plotY + metrics.getAscent(),
                    Math.min(plotY + plotH - metrics.getDescent(), y + halfInk));
            g2.drawString(tick.label(), gutterRight - metrics.stringWidth(tick.label()), baseline);
        }
    }

    /**
     * Draws the date labels along the foot, dropping any that would collide.
     *
     * <p>The first and last labels stay flush to the plot edges and the rest are centred on their
     * point. A label whose box overlaps the one before it, or the last one, is skipped: a narrow
     * window then shows fewer dates rather than a row of overprinted mush.
     *
     * @param g2 the surface to paint on
     * @param plotX the plot's left edge
     * @param plotY the plot's top edge
     * @param plotW the plot's width
     * @param plotH the plot's height
     */
    private void paintTimeLabels(Graphics2D g2, int plotX, int plotY, int plotW, int plotH) {
        final List<ChartTick> ticks = series.timeTicks();
        if (ticks.isEmpty()) {
            return;
        }
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.FG_MUTED);
        final FontMetrics metrics = g2.getFontMetrics();
        final int baseline = plotY + plotH + metrics.getAscent();
        final int count = series.values().size();

        final ChartTick last = ticks.get(ticks.size() - 1);
        final int lastLeft = plotX + plotW - metrics.stringWidth(last.label());

        int nextFree = plotX;
        for (int index = 0; index < ticks.size() - 1; index++) {
            final ChartTick tick = ticks.get(index);
            final int width = metrics.stringWidth(tick.label());
            int left = (int) Math.round(xFor((int) tick.value(), count, plotX, plotW)) - width / 2;
            if (index == 0) {
                left = plotX;
            }
            if (left >= nextFree && left + width + Theme.SM <= lastLeft) {
                g2.drawString(tick.label(), left, baseline);
                nextFree = left + width + Theme.SM;
            }
        }
        if (lastLeft >= nextFree) {
            g2.drawString(last.label(), lastLeft, baseline);
        }
    }

    /**
     * @param index the point's position in the series
     * @param count how many points the series holds
     * @param plotX the plot's left edge
     * @param plotW the plot's width
     * @return the x the point sits at
     */
    private static double xFor(int index, int count, int plotX, int plotW) {
        double x = plotX;
        if (count > 1) {
            x = plotX + (plotW - 1.0) * index / (count - 1.0);
        }
        return x;
    }

    /**
     * @param value the value to place
     * @param plotY the plot's top edge
     * @param plotH the plot's height
     * @return the y the value sits at, given the series' axis bounds
     */
    private int yFor(double value, int plotY, int plotH) {
        final double span = plotH - 1.0;
        final double fraction = (value - series.lowerBound())
                / (series.upperBound() - series.lowerBound());
        return (int) Math.round(plotY + span - span * fraction);
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
