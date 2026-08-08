package use_case.watchlist;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryStockRepository;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct tests for {@link WatchlistSnapshotFactory}, pinning deviation D2-c (need A-N3).
 *
 * <p>The factory captures the selected stock while walking the watchlist entries rather
 * than resolving {@code stocks.findBySymbol(selectedSymbol)} a second time afterwards.
 * The observable consequence is that selected price rows follow <em>watchlist
 * membership</em>: a symbol whose prices are stored but whose ticker is not on the
 * watchlist yields no price rows at all.
 *
 * <p>None of the four public input boundaries can reach that branch — they all normalize
 * an unknown selection to {@code ""} first — so only a direct call can distinguish the
 * current behaviour from the older double-lookup. Without this class a refactor could
 * silently revert A-N3 and every other test would still pass, which is warning W2-8.
 */
class WatchlistSnapshotFactoryTest {

    private static final String ON_WATCHLIST = "AAPL";
    private static final String OFF_WATCHLIST = "MSFT";

    private Watchlist watchlist;
    private InMemoryStockRepository stocks;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
        stocks = new InMemoryStockRepository();
    }

    /**
     * Stores prices for a symbol without putting its ticker on the watchlist.
     *
     * @param symbol     the symbol to store prices under
     * @param priceCount how many days of prices to store
     */
    private void storePricesOnly(String symbol, int priceCount) {
        stocks.save(new Stock(new Ticker(symbol, symbol + " Inc."),
                WatchlistTestData.ascendingPrices(priceCount)));
    }

    /**
     * The regression guard for A-N3.
     *
     * <p>{@code MSFT} is provably in the repository and is provably not on the watchlist.
     * Under the old double-{@code findBySymbol} code this returned four price rows; under
     * the membership-driven code it returns none. Reverting the factory fails here.
     */
    @Test
    void aSelectedSymbolWithStoredPricesButNoWatchlistEntryYieldsNoPriceRows() {
        watchlist.addTicker(new Ticker(ON_WATCHLIST, "Apple Inc."));
        storePricesOnly(OFF_WATCHLIST, 4);

        // The repository really does hold the prices, so an empty result below cannot be
        // an artifact of the save never having happened.
        final Optional<Stock> stored = stocks.findBySymbol(OFF_WATCHLIST);
        assertTrue(stored.isPresent());
        assertEquals(4, stored.get().getPriceCount());
        assertFalse(watchlist.contains(new Ticker(OFF_WATCHLIST, null)));

        final WatchlistSnapshot snapshot =
                WatchlistSnapshotFactory.build(watchlist, stocks, OFF_WATCHLIST);

        assertTrue(snapshot.getSelectedPriceRows().isEmpty());
        assertEquals(OFF_WATCHLIST, snapshot.getSelectedSymbol());
        assertEquals(1, snapshot.getTickerRows().size());
        assertEquals(ON_WATCHLIST, snapshot.getTickerRows().get(0).symbol());
    }

    /**
     * The positive control for the test above.
     *
     * <p>Identical repository contents, identical selection; the only difference is that
     * the ticker is now on the watchlist. Without this, a factory that returned no price
     * rows unconditionally would still satisfy the regression guard.
     */
    @Test
    void theSameSelectionYieldsPriceRowsOnceItsTickerIsOnTheWatchlist() {
        watchlist.addTicker(new Ticker(ON_WATCHLIST, "Apple Inc."));
        watchlist.addTicker(new Ticker(OFF_WATCHLIST, OFF_WATCHLIST + " Inc."));
        storePricesOnly(OFF_WATCHLIST, 4);

        final Optional<Stock> stored = stocks.findBySymbol(OFF_WATCHLIST);
        assertTrue(stored.isPresent());
        assertEquals(4, stored.get().getPriceCount());

        final WatchlistSnapshot snapshot =
                WatchlistSnapshotFactory.build(watchlist, stocks, OFF_WATCHLIST);

        assertEquals(4, snapshot.getSelectedPriceRows().size());
        assertEquals(WatchlistTestData.LAST_DAY.toString(),
                snapshot.getSelectedPriceRows().get(0).date());
        assertEquals(2, snapshot.getTickerRows().size());
    }

    /**
     * Membership, not the repository, also decides a selection with no stored prices.
     *
     * <p>Pins the other half of the same branch: an entry restored from disk before any
     * refresh has run is selectable and simply has nothing to show.
     */
    @Test
    void aSelectedTickerOnTheWatchlistWithNoStoredPricesYieldsNoPriceRows() {
        watchlist.addTicker(new Ticker(ON_WATCHLIST, "Apple Inc."));

        assertTrue(stocks.findBySymbol(ON_WATCHLIST).isEmpty());

        final WatchlistSnapshot snapshot =
                WatchlistSnapshotFactory.build(watchlist, stocks, ON_WATCHLIST);

        assertTrue(snapshot.getSelectedPriceRows().isEmpty());
        assertEquals(1, snapshot.getTickerRows().size());
        assertEquals(0, snapshot.getTickerRows().get(0).priceCount());
    }

    @Test
    void buildRejectsNullCollaborators() {
        assertThrows(NullPointerException.class,
                () -> WatchlistSnapshotFactory.build(null, stocks, ON_WATCHLIST));
        assertThrows(NullPointerException.class,
                () -> WatchlistSnapshotFactory.build(watchlist, null, ON_WATCHLIST));
    }
}
