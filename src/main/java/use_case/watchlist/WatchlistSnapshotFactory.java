package use_case.watchlist;

import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;
import entity.WatchlistEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Builds the {@link WatchlistSnapshot} that every watchlist use case returns.
 *
 * <p>Shared by the add, remove, and refresh interactors so the three cannot drift
 * apart in how they present the same tables.
 */
final class WatchlistSnapshotFactory {

    private WatchlistSnapshotFactory() {
    }

    /**
     * Assembles the current state of the watchlist for display.
     *
     * @param watchlist      the watchlist to read
     * @param stocks         where price history is stored
     * @param selectedSymbol the symbol whose prices should be shown; may be null
     * @return a display-ready snapshot
     */
    static WatchlistSnapshot build(Watchlist watchlist, StockRepository stocks, String selectedSymbol) {
        final List<WatchlistSnapshot.TickerRow> tickerRows = new ArrayList<>();

        /*
         * Watchlist.getEntries() hands back its own internal list, so this only ever
         * reads it. Do not "optimize" this into an add or remove - mutating it here
         * would bypass the entity's duplicate check.
         */
        for (WatchlistEntry entry : watchlist.getEntries()) {
            final Ticker ticker = entry.getTicker();
            final Optional<Stock> stock = stocks.findBySymbol(ticker.getSymbol());

            tickerRows.add(new WatchlistSnapshot.TickerRow(
                    ticker.getSymbol(),
                    displayName(ticker),
                    stock.map(Stock::getPriceCount).orElse(0),
                    stock.flatMap(Stock::getLatestPrice)
                            .map(price -> String.valueOf(price.getDate()))
                            .orElse(""),
                    stock.flatMap(Stock::getLatestPrice)
                            .map(price -> money(price.getClose()))
                            .orElse("")));
        }

        return new WatchlistSnapshot(tickerRows, selectedSymbol, priceRowsFor(stocks, selectedSymbol));
    }

    /** Newest first, because that is the order a user expects to read prices in. */
    private static List<WatchlistSnapshot.PriceRow> priceRowsFor(StockRepository stocks, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }

        final Optional<Stock> stock = stocks.findBySymbol(symbol);
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
