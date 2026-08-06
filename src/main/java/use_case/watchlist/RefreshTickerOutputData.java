package use_case.watchlist;

/** Result of a successful Refresh Ticker. */
public final class RefreshTickerOutputData {

    private final String symbol;
    private final int priceCount;
    private final String latestDate;
    private final WatchlistSnapshot snapshot;

    public RefreshTickerOutputData(String symbol, int priceCount, String latestDate,
                                   WatchlistSnapshot snapshot) {
        this.symbol = symbol;
        this.priceCount = priceCount;
        this.latestDate = latestDate;
        this.snapshot = snapshot;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getPriceCount() {
        return priceCount;
    }

    /** @return the newest date now held, or "" when the provider returned no history. */
    public String getLatestDate() {
        return latestDate;
    }

    public WatchlistSnapshot getSnapshot() {
        return snapshot;
    }
}
