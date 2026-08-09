package view.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic behind a chart, with no drawing in it.
 *
 * <p>Deliberately free of Swing and {@code java.awt}: mapping a value onto a pixel, choosing
 * readable axis ticks and deciding where the zero line sits are all decisions that can be reasoned
 * about - and checked - without a display. Keeping them here leaves each chart panel as a thin
 * {@code paintComponent} that only knows how to draw what this class has already worked out, which
 * is also what keeps the chart code runnable in a headless build.
 *
 * <p>Instances are immutable. Construct one per repaint from the current series and the current
 * component size; they are cheap and hold no reference to any widget.
 */
public final class ChartScale {

    /**
     * Candidate tick steps within one power of ten.
     *
     * <p>1, 2 and 5 are the steps people read fluently - 0.2, 5, 50, 200. A step of 3 or 7 is
     * arithmetically fine and unreadable on an axis.
     */
    private static final double[] NICE_STEPS = {1.0, 2.0, 5.0, 10.0};

    /** Fallback half-range when every value in a series is identical. */
    private static final double FLAT_SERIES_PADDING = 0.5;

    private static final int DEFAULT_TICK_COUNT = 5;

    private final double minimum;
    private final double maximum;
    private final int pixelOrigin;
    private final int pixelLength;

    /**
     * Creates a scale mapping a value range onto a pixel range.
     *
     * @param minimum     the lowest value the axis must show
     * @param maximum     the highest value the axis must show
     * @param pixelOrigin the pixel coordinate the minimum maps to
     * @param pixelLength the length in pixels available to the axis; must be positive
     */
    public ChartScale(double minimum, double maximum, int pixelOrigin, int pixelLength) {
        if (pixelLength <= 0) {
            throw new IllegalArgumentException("Pixel length must be positive");
        }
        if (Double.isNaN(minimum) || Double.isNaN(maximum)) {
            throw new IllegalArgumentException("Scale bounds cannot be NaN");
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum cannot exceed maximum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.pixelOrigin = pixelOrigin;
        this.pixelLength = pixelLength;
    }

    /**
     * Builds a scale that covers a series, padded out to readable bounds.
     *
     * <p>A series that is entirely flat - which a backtest with no trades produces - would give a
     * zero-height range and divide by zero on every mapping, so it is widened by a fixed padding
     * and drawn as a line through the middle.
     *
     * @param values      the data to cover; must not be null or empty
     * @param pixelOrigin the pixel coordinate the minimum maps to
     * @param pixelLength the length in pixels available to the axis; must be positive
     * @return a scale covering the series
     */
    public static ChartScale forSeries(List<Double> values, int pixelOrigin, int pixelLength) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Series cannot be null or empty");
        }
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (final Double value : values) {
            if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("Series cannot contain null, NaN or infinite values");
            }
            low = Math.min(low, value);
            high = Math.max(high, value);
        }
        if (low == high) {
            low -= FLAT_SERIES_PADDING;
            high += FLAT_SERIES_PADDING;
        }
        return new ChartScale(low, high, pixelOrigin, pixelLength);
    }

    /**
     * Builds a scale covering a series and zero.
     *
     * <p>For anything drawn as bars growing from a baseline - total return per strategy, most
     * obviously - an axis that excludes zero makes a small loss look like a large one, because the
     * bar is measured from the bottom of the panel rather than from no-change.
     *
     * @param values      the data to cover; must not be null or empty
     * @param pixelOrigin the pixel coordinate the minimum maps to
     * @param pixelLength the length in pixels available to the axis; must be positive
     * @return a scale covering the series and zero
     */
    public static ChartScale forSeriesIncludingZero(List<Double> values, int pixelOrigin, int pixelLength) {
        final ChartScale bare = forSeries(values, pixelOrigin, pixelLength);
        return new ChartScale(
                Math.min(0.0, bare.minimum),
                Math.max(0.0, bare.maximum),
                pixelOrigin,
                pixelLength);
    }

    /**
     * Maps a value onto its pixel coordinate, increasing away from the origin.
     *
     * <p>Values outside the range are not clamped: a caller drawing into a clipped region wants the
     * real coordinate, and silently pinning it to an edge would draw a false flat line.
     *
     * @param value the value to place
     * @return the pixel coordinate
     */
    public int toPixel(double value) {
        final double fraction = (value - minimum) / (maximum - minimum);
        return pixelOrigin + (int) Math.round(fraction * pixelLength);
    }

    /**
     * Maps a value onto its pixel coordinate for an axis that grows upward.
     *
     * <p>Screen y grows downward, so a chart drawn with {@link #toPixel} alone comes out upside
     * down. This is the version a vertical axis wants.
     *
     * @param value the value to place
     * @return the pixel coordinate, measured down from the top of the axis
     */
    public int toPixelInverted(double value) {
        final double fraction = (value - minimum) / (maximum - minimum);
        return pixelOrigin + pixelLength - (int) Math.round(fraction * pixelLength);
    }

    /**
     * Spaces the points of a series evenly across the axis.
     *
     * <p>Trading days are not evenly spaced in calendar time - weekends and holidays leave gaps -
     * but a price chart is read by sequence, not by date arithmetic, so positions come from the
     * index. A single-point series is placed at the origin rather than dividing by zero.
     *
     * @param index the zero-based position of the point
     * @param count how many points the series holds; must be positive
     * @return the pixel coordinate for that point
     */
    public int toPixelForIndex(int index, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Point count must be positive");
        }
        if (count == 1) {
            return pixelOrigin;
        }
        return pixelOrigin + (int) Math.round((double) index / (count - 1) * pixelLength);
    }

    /**
     * Chooses readable tick values covering the range.
     *
     * <p>The step is rounded up to 1, 2 or 5 times a power of ten, so ticks land on numbers a
     * reader recognises. Because the step is rounded rather than fitted exactly, the count returned
     * is close to the requested count rather than equal to it.
     *
     * @param approximateCount roughly how many ticks are wanted; must be positive
     * @return the tick values, ascending, spanning at least the full range
     */
    public List<Double> ticks(int approximateCount) {
        if (approximateCount <= 0) {
            throw new IllegalArgumentException("Tick count must be positive");
        }
        final double step = niceStep((maximum - minimum) / approximateCount);
        final double first = Math.floor(minimum / step) * step;
        final List<Double> ticks = new ArrayList<>();
        // Compare against a value nudged by half a step so a tick landing exactly on the maximum
        // survives the floating-point error accumulated by repeated addition.
        final double limit = maximum + step / 2.0;
        for (double tick = first; tick <= limit; tick += step) {
            ticks.add(tick);
        }
        return ticks;
    }

    /**
     * Chooses readable tick values using a default count.
     *
     * @return the tick values, ascending
     */
    public List<Double> ticks() {
        return ticks(DEFAULT_TICK_COUNT);
    }

    /**
     * Reports whether zero falls inside the range, so a baseline is worth drawing.
     *
     * @return true when zero lies within the scale's bounds
     */
    public boolean includesZero() {
        return minimum <= 0.0 && maximum >= 0.0;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    /**
     * Rounds a raw step up to the nearest 1, 2 or 5 times a power of ten.
     *
     * @param rawStep the unrounded step
     * @return the rounded step, always positive
     */
    private static double niceStep(double rawStep) {
        if (rawStep <= 0.0) {
            return 1.0;
        }
        final double magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)));
        final double normalized = rawStep / magnitude;
        double chosen = NICE_STEPS[NICE_STEPS.length - 1];
        for (final double candidate : NICE_STEPS) {
            if (normalized <= candidate) {
                chosen = candidate;
                break;
            }
        }
        return chosen * magnitude;
    }
}
