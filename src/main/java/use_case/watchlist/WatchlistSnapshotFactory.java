package use_case.watchlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;
import entity.WatchlistEntry;

/**
 * Builds the {@link WatchlistSnapshot} that every watchlist use case returns.
 *
 * <p>Shared by the add, remove, refresh, and show interactors so the four cannot drift
 * apart in how they present the same tables.
 */
final class WatchlistSnapshotFactory {

    private WatchlistSnapshotFactory() {
    }

    /**
     * Assembles the current state of the watchlist for display.
     *
     * @param watchlist      the watchlist to read; must be non-null
     * @param stocks         where price history is stored; must be non-null
     * @param selectedSymbol the symbol whose prices should be shown; may be null or ""
     *                       for no selection
     * @return a display-ready snapshot
     * @throws NullPointerException if {@code watchlist} or {@code stocks} is null
     */
    static WatchlistSnapshot build(Watchlist watchlist, StockRepository stocks, String selectedSymbol) {
        Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        Objects.requireNonNull(stocks, "Stock repository cannot be null");

        final List<WatchlistSnapshot.TickerRow> tickerRows = new ArrayList<>();
        Optional<Stock> selectedStock = Optional.empty();

        /*
         * Watchlist.getEntries() hands back its own internal list, so this only ever
         * reads it. Do not "optimize" this into an add or remove - mutating it here
         * would bypass the entity's duplicate check.
         *
         * The selected symbol's stock is captured on the way past rather than looked
         * up again afterwards: one findBySymbol per ticker, and one getLatestPrice per
         * row, is all this needs.
         */
        for (WatchlistEntry entry : watchlist.getEntries()) {
            final Ticker ticker = entry.getTicker();
            final Optional<Stock> stock = stocks.findBySymbol(ticker.getSymbol());
            final Optional<DailyPrice> latestPrice = stock.flatMap(Stock::getLatestPrice);

            if (isSelected(ticker.getSymbol(), selectedSymbol)) {
                selectedStock = stock;
            }

            tickerRows.add(new WatchlistSnapshot.TickerRow(
                    ticker.getSymbol(),
                    displayName(ticker),
                    stock.map(Stock::getPriceCount).orElse(0),
                    latestPrice.map(price -> String.valueOf(price.getDate())).orElse(""),
                    latestPrice.map(price -> money(price.getClose())).orElse("")));
        }

        return new WatchlistSnapshot(tickerRows, selectedSymbol,
                priceRowsFor(selectedStock), closesFor(selectedStock));
    }

    /** Symbols are compared case-insensitively, matching {@code Ticker.equals}. */
    private static boolean isSelected(String tickerSymbol, String selectedSymbol) {
        return selectedSymbol != null
                && !selectedSymbol.isBlank()
                && tickerSymbol.equalsIgnoreCase(selectedSymbol);
    }

    /** Newest first, because that is the order a user expects to read prices in. */
    private static List<WatchlistSnapshot.PriceRow> priceRowsFor(Optional<Stock> stock) {
        if (stock.isEmpty()) {
            return List.of();
        }

        final List<DailyPrice> prices = stock.get().getDailyPrices();
        final List<WatchlistSnapshot.PriceRow> rows = new ArrayList<>(prices.size());
        for (int index = prices.size() - 1; index >= 0; index--) {
            final DailyPrice price = prices.get(index);
            rows.add(new WatchlistSnapshot.PriceRow(
                    String.valueOf(price.getDate()),
                    money(price.getOpen()),
                    money(price.getHigh()),
                    money(price.getLow()),
                    money(price.getClose()),
                    String.valueOf(price.getVolume())));
        }
        return rows;
    }

    /**
     * Oldest first, the opposite order to {@link #priceRowsFor}: these are plotted along a time
     * axis rather than read down a table, and a series handed over newest-first draws backwards.
     */
    private static List<Double> closesFor(Optional<Stock> stock) {
        if (stock.isEmpty()) {
            return List.of();
        }

        final List<DailyPrice> prices = stock.get().getDailyPrices();
        final List<Double> closes = new ArrayList<>(prices.size());
        for (final DailyPrice price : prices) {
            closes.add(price.getClose());
        }
        return List.copyOf(closes);
    }

    /** Falls back to the symbol so the table never shows a blank name cell. */
    private static String displayName(Ticker ticker) {
        final String companyName = ticker.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            return ticker.getSymbol();
        }
        return companyName;
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
