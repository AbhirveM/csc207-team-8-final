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
 * <p>Instances are immutable and have value semantics, so the view model can compare an
 * incoming state with the outgoing one and a test can assert on a whole state at once.
 */
public final class WatchlistState {

    /** The status line shown before any use case has run. */
    private static final String READY = "Ready.";

    /** One row of the watchlist table, already formatted for display. */
    public record TickerRow(String symbol, String companyName, String priceCount,
                            String latestDate, String latestClose) {
    }

    /** One row of the daily-price table, already formatted for display. */
    public record PriceRow(String date, String open, String high, String low,
                           String close, String volume) {
    }

    private final List<TickerRow> tickerRows;
    private final List<PriceRow> priceRows;
    private final String selectedSymbol;
    private final String statusMessage;
    private final String errorMessage;
    private final String tickerFieldText;

    /**
     * @param tickerRows      one row per watchlist ticker, copied defensively
     * @param priceRows       the daily prices for {@code selectedSymbol}, copied defensively
     * @param selectedSymbol  the symbol whose prices are shown, or {@code ""} for none
     * @param statusMessage   what the status label reads; must not be blank, because an
     *                        empty status label collapses and makes the layout jump
     * @param errorMessage    what the error label reads, or {@code ""} when there is no error
     * @param tickerFieldText what the ticker field should contain; {@code ""} after a
     *                        success, and the text the user typed after a failure
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code statusMessage} is blank
     */
    public WatchlistState(List<TickerRow> tickerRows, List<PriceRow> priceRows,
                          String selectedSymbol, String statusMessage,
                          String errorMessage, String tickerFieldText) {
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
     * The state the view model holds before any use case has run.
     *
     * @return an empty watchlist with nothing selected and no error
     */
    public static WatchlistState initial() {
        return new WatchlistState(List.of(), List.of(), "", READY, "", "");
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
     * Value equality, so the view model can skip a repaint when the presenter re-emits an
     * identical state, and so a presenter test can assert on a whole state at once.
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
                && tickerFieldText.equals(that.tickerFieldText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickerRows, priceRows, selectedSymbol, statusMessage,
                errorMessage, tickerFieldText);
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
                + ", tickerFieldText='" + tickerFieldText + "'}";
    }
}
