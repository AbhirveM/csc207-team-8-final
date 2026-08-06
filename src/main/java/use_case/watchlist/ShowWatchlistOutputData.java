package use_case.watchlist;

import java.util.Objects;

/** Result of a successful Show Watchlist. */
public final class ShowWatchlistOutputData {

    private final int tickerCount;
    private final WatchlistSnapshot snapshot;

    /**
     * @param tickerCount how many tickers are on the watchlist
     * @param snapshot    the display-ready watchlist, with the selection applied
     * @throws NullPointerException if {@code snapshot} is null
     */
    public ShowWatchlistOutputData(int tickerCount, WatchlistSnapshot snapshot) {
        this.tickerCount = tickerCount;
        this.snapshot = Objects.requireNonNull(snapshot, "Snapshot cannot be null");
    }

    public int getTickerCount() {
        return tickerCount;
    }

    public WatchlistSnapshot getSnapshot() {
        return snapshot;
    }
}
