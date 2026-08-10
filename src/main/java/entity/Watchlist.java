package entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The user's full watchlist of tickers (and their saved strategy configs).
 */
public class Watchlist implements Serializable {

    // Pinned to the value the JVM computed before it was declared, so save files written
    // by an earlier build still load. Declaring a fresh 1L here would have changed the UID
    // and made every existing watchlist.dat an InvalidClassException - which the DAO reads
    // as corruption and recovers from by resetting, the exact data loss this prevents.
    private static final long serialVersionUID = -5696186273907508955L;

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
