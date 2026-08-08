package entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The user's full watchlist of tickers (and their saved strategy configs).
 */
public class Watchlist implements Serializable {
    private final List<WatchlistEntry> entries = new ArrayList<>();

    public void addTicker(Ticker ticker) {
        if (contains(ticker)) {
            return;
        }
        entries.add(new WatchlistEntry(ticker));
    }

    public void removeTicker(Ticker ticker) {
        entries.removeIf(e -> e.getTicker().equals(ticker));
    }

    public boolean contains(Ticker ticker) {
        return entries.stream().anyMatch(e -> e.getTicker().equals(ticker));
    }

    public Optional<WatchlistEntry> findEntry(Ticker ticker) {
        return entries.stream().filter(e -> e.getTicker().equals(ticker)).findFirst();
    }

    public List<WatchlistEntry> getEntries() {
        return entries;
    }
}
