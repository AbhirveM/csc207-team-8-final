package entity;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WatchlistTest {

    @Test
    void addTickerAddsAnEntry() {
        Watchlist watchlist = new Watchlist();
        Ticker aapl = new Ticker("AAPL", "Apple Inc.");

        watchlist.addTicker(aapl);

        assertEquals(1, watchlist.getEntries().size());
        assertTrue(watchlist.contains(aapl));
    }

    @Test
    void addTickerIgnoresExactDuplicate() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        assertEquals(1, watchlist.getEntries().size());
    }

    @Test
    void addTickerIsCaseInsensitiveWhenPreventingDuplicates() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("aapl", "Apple Inc."));
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        // Ticker equality uppercases the symbol, so these are the same ticker.
        assertEquals(1, watchlist.getEntries().size());
    }

    @Test
    void containsIsCaseInsensitive() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("aapl", "Apple Inc."));

        assertTrue(watchlist.contains(new Ticker("AAPL", "Apple Inc.")));
    }

    @Test
    void containsReturnsFalseForAbsentTicker() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        assertFalse(watchlist.contains(new Ticker("TSLA", "Tesla Inc.")));
    }

    @Test
    void removeTickerRemovesMatchingEntry() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        watchlist.removeTicker(new Ticker("AAPL", "Apple Inc."));

        assertFalse(watchlist.contains(new Ticker("AAPL", "Apple Inc.")));
        assertTrue(watchlist.getEntries().isEmpty());
    }

    @Test
    void removeTickerIsCaseInsensitive() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        watchlist.removeTicker(new Ticker("aapl", "Apple Inc."));

        assertTrue(watchlist.getEntries().isEmpty());
    }

    @Test
    void removeTickerLeavesOtherEntriesUntouched() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));
        watchlist.addTicker(new Ticker("TSLA", "Tesla Inc."));

        watchlist.removeTicker(new Ticker("AAPL", "Apple Inc."));

        assertFalse(watchlist.contains(new Ticker("AAPL", "Apple Inc.")));
        assertTrue(watchlist.contains(new Ticker("TSLA", "Tesla Inc.")));
        assertEquals(1, watchlist.getEntries().size());
    }

    @Test
    void removeTickerOnAbsentTickerIsNoOp() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        watchlist.removeTicker(new Ticker("TSLA", "Tesla Inc."));

        assertEquals(1, watchlist.getEntries().size());
    }

    @Test
    void findEntryReturnsMatchingEntry() {
        Watchlist watchlist = new Watchlist();
        Ticker aapl = new Ticker("AAPL", "Apple Inc.");
        watchlist.addTicker(aapl);

        Optional<WatchlistEntry> found = watchlist.findEntry(new Ticker("aapl", "Apple Inc."));

        assertTrue(found.isPresent());
        assertEquals(aapl, found.get().getTicker());
    }

    @Test
    void findEntryReturnsEmptyForAbsentTicker() {
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        Optional<WatchlistEntry> found = watchlist.findEntry(new Ticker("TSLA", "Tesla Inc."));

        assertTrue(found.isEmpty());
    }
}
