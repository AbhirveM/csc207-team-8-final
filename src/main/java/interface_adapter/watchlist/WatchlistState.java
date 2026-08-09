package interface_adapter.watchlist;

import java.util.List;
import java.util.Objects;

/**
 * Everything {@code WatchlistView} needs in order to paint itself, and nothing else.
 *
 * <p>Every field is a display-ready {@code String}, including {@code priceCount}. The
 * presenter has already substituted {@code "Not loaded"} for a zero price count and an em
 * dash for an absent date or close, so the view performs no formatting whatsoever - no
 * {@code String.format}, no {@code NumberFormat}, no branching on emptiness. That is what
 * keeps the view dumb enough that its uncovered lines stay cheap.
 *
 * <p>This class declares its own {@link TickerRow} and {@link PriceRow} records rather
 * than re-exporting the ones on {@code WatchlistSnapshot}. The handful of duplicated lines
 * buys a view with zero {@code use_case} imports, and gives the presenter the place it
 * needs to substitute human-readable placeholders.
 *
 * <p>Instances are immutable and have value semantics so a test can assert on a whole state
 * at once. Note that {@link WatchlistViewModel} deliberately does <em>not</em> use that
 * equality to suppress a repaint - re-emitting an identical result must still fire an
 * event, or the view silently misses a redraw. See that class's {@code setState}.
 *
 * <p>Deep immutability is also what makes the seam thread-safe: the presenter constructs a
 * state on the background worker and the view reads it on the event dispatch thread, and
 * every field being final means the reader can never observe a half-built instance.
 */
public final class WatchlistState {

    /** The status line shown before any use case has run. */
    private static final String READY = "Ready.";

    /**
     * One row of the watchlist table, already formatted for display.
     *
     * @param symbol      the ticker symbol
     * @param companyName the company name, or a placeholder when it is unknown
     * @param priceCount  how many days of price history are held, as text
     * @param latestDate  the date of the most recent price, as text
     * @param latestClose the most recent closing price, as text
     */
    public record TickerRow(String symbol, String companyName, String priceCount,
                            String latestDate, String latestClose) {
    }

    /**
     * One row of the daily-price table, already formatted for display.
     *
     * @param date   the trading date, as text
     * @param open   the opening price, as text
     * @param high   the session high, as text
     * @param low    the session low, as text
     * @param close  the closing price, as text
     * @param volume the traded volume, as text
     */
    public record PriceRow(String date, String open, String high, String low,
                           String close, String volume) {
    }

    /**
     * The close-price series for the selected ticker, together with the axis labels and the
     * spoken summary the presenter has already formatted for it.
     *
     * <p>The same deliberate duplication as {@link TickerRow} and {@link PriceRow}: this
     * mirrors {@code LineChart.Series} component for component so the view can hand it straight
     * over without either layer importing the other's types.
     *
     * @param closes     the closing prices, <em>oldest first</em> - the opposite order to
     *                   {@link #getPriceRows()}, because a line runs forwards in time
     * @param lowLabel   the lowest close, formatted
     * @param highLabel  the highest close, formatted
     * @param startLabel the date of the oldest close
     * @param endLabel   the date of the newest close
     * @param summary    one sentence describing the series, shown beside the chart and spoken
     *                   as its accessible description
     */
    public record PriceChart(List<Double> closes, String lowLabel, String highLabel,
                             String startLabel, String endLabel, String summary) {

        /** @return the chart shown when nothing is selected or the selection has no prices. */
        public static PriceChart empty() {
            return new PriceChart(List.of(), "", "", "", "", "No data.");
        }
    }

    private final List<TickerRow> tickerRows;
    private final List<PriceRow> priceRows;
    private final String selectedSymbol;
    private final String statusMessage;
    private final String errorMessage;
    private final String tickerFieldText;
    private final PriceChart priceChart;

    /**
     * @param tickerRows      one row per watchlist ticker, copied defensively
     * @param priceRows       the daily prices for {@code selectedSymbol}, copied defensively
     * @param selectedSymbol  the symbol whose prices are shown, or {@code ""} for none
     * @param statusMessage   what the status label reads; must not be blank, because an
     *                        empty status label collapses and makes the layout jump
     * @param errorMessage    what the error label reads, or {@code ""} when there is no error
     * @param tickerFieldText what the ticker field should contain; {@code ""} after a
     *                        success, and the text the user typed after a failure
     * @param priceChart      the close-price series for {@code selectedSymbol}
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code statusMessage} is blank
     */
    public WatchlistState(List<TickerRow> tickerRows, List<PriceRow> priceRows,
                          String selectedSymbol, String statusMessage,
                          String errorMessage, String tickerFieldText,
                          PriceChart priceChart) {
        this.priceChart = Objects.requireNonNull(priceChart, "Price chart cannot be null");
        Objects.requireNonNull(tickerRows, "Ticker rows cannot be null");
        Objects.requireNonNull(priceRows, "Price rows cannot be null");
        Objects.requireNonNull(statusMessage, "Status message cannot be null");
        if (statusMessage.isBlank()) {
            throw new IllegalArgumentException("Status message cannot be blank");
        }
        this.tickerRows = List.copyOf(tickerRows);
        this.priceRows = List.copyOf(priceRows);
        this.selectedSymbol = Objects.requireNonNull(selectedSymbol,
                "Selected symbol cannot be null");
        this.statusMessage = statusMessage;
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
        this.tickerFieldText = Objects.requireNonNull(tickerFieldText,
                "Ticker field text cannot be null");
    }

    /**
     * The states that carry no chart.
     *
     * <p>Kept as an overload rather than folded into the canonical constructor because the
     * great majority of the call sites - every one that is asserting on rows, prose or the
     * ticker field - has nothing to say about a series, and threading an explicit
     * {@code PriceChart.empty()} through all of them would be churn that hides the handful of
     * places the chart is actually the point.
     *
     * @param tickerRows      one row per watchlist ticker, copied defensively
     * @param priceRows       the daily prices for {@code selectedSymbol}, copied defensively
     * @param selectedSymbol  the symbol whose prices are shown, or {@code ""} for none
     * @param statusMessage   what the status label reads; must not be blank
     * @param errorMessage    what the error label reads, or {@code ""} when there is no error
     * @param tickerFieldText what the ticker field should contain
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code statusMessage} is blank
     */
    public WatchlistState(List<TickerRow> tickerRows, List<PriceRow> priceRows,
                          String selectedSymbol, String statusMessage,
                          String errorMessage, String tickerFieldText) {
        this(tickerRows, priceRows, selectedSymbol, statusMessage, errorMessage,
                tickerFieldText, PriceChart.empty());
    }

    /**
     * The state the view model holds before any use case has run.
     *
     * @return an empty watchlist with nothing selected and no error
     */
    public static WatchlistState initial() {
        return new WatchlistState(List.of(), List.of(), "", READY, "", "", PriceChart.empty());
    }

    /** @return one row per watchlist ticker. Never null; empty when the watchlist is. */
    public List<TickerRow> getTickerRows() {
        return tickerRows;
    }

    /** @return the daily prices for the selected ticker. Never null; may be empty. */
    public List<PriceRow> getPriceRows() {
        return priceRows;
    }

    /** @return the symbol whose prices are shown, or "" when nothing is selected. */
    public String getSelectedSymbol() {
        return selectedSymbol;
    }

    /** @return what the status label reads. Never blank. */
    public String getStatusMessage() {
        return statusMessage;
    }

    /** @return what the error label reads, or "" when there is no error. */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return whether an error should be shown. Equivalent to
     *         {@code !getErrorMessage().isEmpty()}, and the only thing the view branches
     *         on - errors are conveyed in words, never colour alone.
     */
    public boolean isErrorPresent() {
        return !errorMessage.isEmpty();
    }

    /**
     * @return what the ticker field should contain: "" after a success, and the text the
     *         user typed after a failure, so a typo does not have to be retyped.
     */
    public String getTickerFieldText() {
        return tickerFieldText;
    }

    /**
     * @return the close-price series for the selected ticker. Never null; carries an empty
     *         list of closes when there is nothing to plot.
     */
    public PriceChart getPriceChart() {
        return priceChart;
    }

    /**
     * Value equality, so a presenter test can assert on a whole state at once.
     *
     * <p>This is not used to skip repaints - see the class javadoc.
     *
     * @param other the object to compare with
     * @return whether {@code other} is a state with the same rows, selection and prose
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WatchlistState)) {
            return false;
        }
        final WatchlistState that = (WatchlistState) other;
        return tickerRows.equals(that.tickerRows)
                && priceRows.equals(that.priceRows)
                && selectedSymbol.equals(that.selectedSymbol)
                && statusMessage.equals(that.statusMessage)
                && errorMessage.equals(that.errorMessage)
                && tickerFieldText.equals(that.tickerFieldText)
                && priceChart.equals(that.priceChart);
    }

    /** @return a hash consistent with {@link #equals(Object)} over all seven fields. */
    @Override
    public int hashCode() {
        return Objects.hash(tickerRows, priceRows, selectedSymbol, statusMessage,
                errorMessage, tickerFieldText, priceChart);
    }

    /**
     * @return a readable form. The row lists are summarised by size rather than printed,
     *         so a failing assertion names the prose without a hundred price rows of noise.
     */
    @Override
    public String toString() {
        return "WatchlistState{tickerRows=" + tickerRows.size()
                + ", priceRows=" + priceRows.size()
                + ", selectedSymbol='" + selectedSymbol + "'"
                + ", statusMessage='" + statusMessage + "'"
                + ", errorMessage='" + errorMessage + "'"
                + ", tickerFieldText='" + tickerFieldText + "'"
                + ", priceChart=" + priceChart.closes().size() + "}";
    }
}
