package use_case.watchlist;

/** Result of a successful Remove Ticker. */
public final class RemoveTickerOutputData {

    private final String removedSymbol;
    private final WatchlistSnapshot snapshot;

    public RemoveTickerOutputData(String removedSymbol, WatchlistSnapshot snapshot) {
        this.removedSymbol = removedSymbol;
        this.snapshot = snapshot;
    }

    public String getRemovedSymbol() {
        return removedSymbol;
    }

    public WatchlistSnapshot getSnapshot() {
        return snapshot;
    }
}
