package use_case.watchlist;

import java.util.List;
import java.util.Objects;

/**
 * A display-ready view of the watchlist, produced by every watchlist use case.
 *
 * <p>Only plain strings and numbers cross the output boundary - no entities. That
 * matters for three reasons: {@code Watchlist.getEntries()} exposes its internal
 * list, so letting it reach the view would leak the entity's invariants; the Swing
 * view refreshes on a background thread, so sharing a mutable entity list risks a
 * concurrent-modification failure; and it keeps the view model free of both Swing
 * and entity imports.
 *
 * <p>Prices here are formatted for display and ordered <em>newest first</em>, which
 * is what a user expects to read. That is purely presentational and does not weaken
 * the oldest-to-newest guarantee {@link entity.Stock} makes to the strategies.
 */
public final class WatchlistSnapshot {

    /**
     * One row of the watchlist table.
     *
     * @param symbol      the ticker symbol
     * @param companyName the company name, or a placeholder when it is unknown
     * @param priceCount  how many days of price history are held
     * @param latestDate  the date of the most recent price, as text
     * @param latestClose the most recent closing price, as text
     */
    public record TickerRow(String symbol, String companyName, int priceCount,
                            String latestDate, String latestClose) {
    }

    /**
     * One row of the daily-price table for the selected ticker.
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

    private final List<TickerRow> tickerRows;
    private final String selectedSymbol;
    private final List<PriceRow> selectedPriceRows;

    /**
     * @param tickerRows        one row per watchlist ticker, copied defensively
     * @param selectedSymbol    the symbol whose prices are carried, or null/"" for none
     * @param selectedPriceRows the price rows for {@code selectedSymbol}
     * @throws NullPointerException if either list is null
     */
    public WatchlistSnapshot(List<TickerRow> tickerRows, String selectedSymbol,
                             List<PriceRow> selectedPriceRows) {
        Objects.requireNonNull(tickerRows, "Ticker rows cannot be null");
        Objects.requireNonNull(selectedPriceRows, "Selected price rows cannot be null");
        this.tickerRows = List.copyOf(tickerRows);
        this.selectedSymbol = selectedSymbol == null ? "" : selectedSymbol;
        this.selectedPriceRows = List.copyOf(selectedPriceRows);
    }

    public List<TickerRow> getTickerRows() {
        return tickerRows;
    }

    /** @return the symbol whose prices are shown, or "" when nothing is selected. */
    public String getSelectedSymbol() {
        return selectedSymbol;
    }

    public List<PriceRow> getSelectedPriceRows() {
        return selectedPriceRows;
    }

    /**
     * Value equality, so the presenter can skip a repaint when a use case re-emits an
     * identical snapshot, and so tests can assert on a whole snapshot at once.
     *
     * @param other the object to compare with
     * @return whether {@code other} is a snapshot with the same rows and selection
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WatchlistSnapshot)) {
            return false;
        }
        final WatchlistSnapshot that = (WatchlistSnapshot) other;
        return tickerRows.equals(that.tickerRows)
                && selectedSymbol.equals(that.selectedSymbol)
                && selectedPriceRows.equals(that.selectedPriceRows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickerRows, selectedSymbol, selectedPriceRows);
    }
}
