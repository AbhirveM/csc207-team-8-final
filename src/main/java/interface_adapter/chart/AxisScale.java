package interface_adapter.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * The rounded lower bound, upper bound, and step for an axis covering a range of values.
 *
 * <p><strong>Why this is not in {@code view}.</strong> It decides what numbers a user reads off a
 * chart, which is a presentation decision and therefore a presenter's. A chart that chose its own
 * tick values would be the first piece of formatting logic to cross that line.
 *
 * <p><strong>Why the bounds are not the data's own minimum and maximum.</strong> Scaling a plot to
 * the raw extremes makes the line touch the top and the bottom of the frame no matter how little
 * it moved, so a stock that drifted 2% and one that halved fill the box identically. Padding the
 * range and then rounding outwards gives the line headroom <em>and</em> lands the gridlines on
 * numbers worth printing.
 *
 * @param lowerBound the rounded bottom of the axis, at or below the padded minimum
 * @param upperBound the rounded top of the axis, at or above the padded maximum; always strictly
 *                   greater than {@code lowerBound}, so a caller may divide by the span
 * @param step       the distance between adjacent ticks; always strictly positive
 */
public record AxisScale(double lowerBound, double upperBound, double step) {

    /** Fraction of the raw range added at each end before rounding, so the line clears the frame. */
    private static final double PADDING_FRACTION = 0.05;

    /** The span given to a range with no extent, so a flat series still has an axis to sit on. */
    private static final double FLAT_RANGE_SPAN = 1.0;

    /**
     * The mantissas a step is allowed to take, in ascending order. The standard "nice numbers"
     * set: every one of them divides a decade into ticks a reader can add up in their head.
     */
    private static final double[] NICE_STEPS = {1.0, 2.0, 2.5, 5.0, 10.0};

    /**
     * Compact constructor.
     *
     * @throws IllegalArgumentException if the bounds do not enclose a positive span, or the step
     *                                  is not positive
     */
    public AxisScale {
        if (!(upperBound > lowerBound)) {
            throw new IllegalArgumentException("Upper bound must exceed lower bound");
        }
        if (!(step > 0.0)) {
            throw new IllegalArgumentException("Step must be positive");
        }
    }

    /**
     * Rounds an axis outwards from the range it has to cover.
     *
     * <p>The range is padded by {@value #PADDING_FRACTION} of its own extent at each end first, so
     * the plotted line never touches the frame. A range with no extent - one point, or a perfectly
     * flat series - is widened by {@value #FLAT_RANGE_SPAN} at each end instead, which keeps the
     * later divide finite.
     *
     * @param min             the smallest value the axis must show
     * @param max             the largest value the axis must show; may equal or precede
     *                        {@code min}, in which case the range is treated as flat
     * @param targetIntervals roughly how many gaps between ticks are wanted; the rounding means
     *                        the result may have one or two more or fewer
     * @return an axis whose bounds are multiples of its step
     * @throws IllegalArgumentException if {@code targetIntervals} is less than one, or either
     *                                  bound is not a finite number
     */
    public static AxisScale forRange(double min, double max, int targetIntervals) {
        if (targetIntervals < 1) {
            throw new IllegalArgumentException("Target intervals must be at least one");
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            throw new IllegalArgumentException("Range bounds must be finite");
        }

        final double range = max - min;
        final double paddedMin;
        final double paddedMax;
        if (range > 0.0) {
            final double padding = range * PADDING_FRACTION;
            paddedMin = min - padding;
            paddedMax = max + padding;
        }
        else {
            paddedMin = min - FLAT_RANGE_SPAN;
            paddedMax = min + FLAT_RANGE_SPAN;
        }

        final double step = niceStep((paddedMax - paddedMin) / targetIntervals);
        final double lower = Math.floor(paddedMin / step) * step;
        double upper = Math.ceil(paddedMax / step) * step;
        if (!(upper > lower)) {
            // Only reachable when the padded range collapses onto a single multiple of the step.
            upper = lower + step;
        }
        return new AxisScale(lower, upper, step);
    }

    /**
     * The tick values from the lower bound to the upper bound inclusive.
     *
     * <p>Each value is computed as {@code lowerBound + index * step} rather than by repeatedly
     * adding the step to a running total. Repeated addition accumulates floating-point error, and
     * the error shows up exactly where it is most visible: the last tick misses the upper bound
     * and is dropped, leaving the top of the axis unlabelled.
     *
     * @return the tick values, ascending, always at least two of them
     */
    public List<Double> tickValues() {
        final int intervals = (int) Math.round((upperBound - lowerBound) / step);
        final List<Double> values = new ArrayList<>(intervals + 1);
        for (int index = 0; index <= intervals; index++) {
            values.add(lowerBound + index * step);
        }
        return List.copyOf(values);
    }

    /**
     * Snaps a step up to the next value that reads well: one of {@link #NICE_STEPS} times a power
     * of ten.
     *
     * @param rawStep the exact step the target interval count asked for; must be positive
     * @return the smallest nice step that is at least {@code rawStep}
     */
    private static double niceStep(double rawStep) {
        final double magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)));
        final double normalized = rawStep / magnitude;
        double chosen = NICE_STEPS[NICE_STEPS.length - 1];
        for (final double candidate : NICE_STEPS) {
            if (candidate >= normalized) {
                chosen = candidate;
                break;
            }
        }
        return chosen * magnitude;
    }
}
