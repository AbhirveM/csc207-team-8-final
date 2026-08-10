package interface_adapter.chart;

import java.util.Objects;

/**
 * One labelled mark on a chart axis: where it sits, and the finished text beside it.
 *
 * <p>Shared by both presenters and by {@code view.LineChart} rather than mirrored on either side
 * of the boundary. The duplication that {@code WatchlistState.PriceChart} performs against
 * {@code use_case} exists because a view may not import {@code use_case}; a view importing
 * {@code interface_adapter} is the normal direction, and {@code WatchlistView} already does it.
 * Mirroring this record too would only add a mapping loop to two views.
 *
 * <p>The label is always composed by a presenter. Nothing in {@code view} formats a number, so a
 * chart places the string it was handed and never builds one.
 *
 * @param value where the tick sits: a value on the value axis, or a <em>point index</em> into the
 *              series on the time axis
 * @param label the finished text to draw beside it
 */
public record ChartTick(double value, String label) {

    /**
     * Compact constructor.
     *
     * @throws NullPointerException if {@code label} is null
     */
    public ChartTick {
        Objects.requireNonNull(label, "Label cannot be null");
    }
}
