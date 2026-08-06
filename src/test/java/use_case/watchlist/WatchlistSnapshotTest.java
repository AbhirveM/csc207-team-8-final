package use_case.watchlist;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WatchlistSnapshot}'s value semantics and defensive copying.
 *
 * <p>The copying is what stops {@code Watchlist.getEntries()}'s live internal list, and
 * the background thread that builds a snapshot, from reaching the view.
 */
class WatchlistSnapshotTest {

    private static final WatchlistSnapshot.TickerRow ROW =
            new WatchlistSnapshot.TickerRow("AAPL", "Apple Inc.", 2, "2026-03-10", "101.00");
    private static final WatchlistSnapshot.PriceRow PRICE =
            new WatchlistSnapshot.PriceRow("2026-03-10", "100.00", "103.00", "99.00",
                    "101.00", "1001");

    @Test
    void equalSnapshotsAreEqualAndShareAHashCode() {
        final WatchlistSnapshot left = new WatchlistSnapshot(List.of(ROW), "AAPL", List.of(PRICE));
        final WatchlistSnapshot right = new WatchlistSnapshot(List.of(ROW), "AAPL", List.of(PRICE));

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals(left, left);
    }

    @Test
    void snapshotsDifferingInAnyPartAreNotEqual() {
        final WatchlistSnapshot base = new WatchlistSnapshot(List.of(ROW), "AAPL", List.of(PRICE));

        assertNotEquals(base, new WatchlistSnapshot(List.of(), "AAPL", List.of(PRICE)));
        assertNotEquals(base, new WatchlistSnapshot(List.of(ROW), "MSFT", List.of(PRICE)));
        assertNotEquals(base, new WatchlistSnapshot(List.of(ROW), "AAPL", List.of()));
        assertNotEquals(base, null);
        assertNotEquals(base, "not a snapshot");
    }

    @Test
    void aNullSelectionIsNormalizedToTheEmptyString() {
        final WatchlistSnapshot snapshot = new WatchlistSnapshot(List.of(), null, List.of());

        assertEquals("", snapshot.getSelectedSymbol());
        assertEquals(snapshot, new WatchlistSnapshot(List.of(), "", List.of()));
    }

    @Test
    void bothListsAreCopiedDefensivelyAndHandedOutUnmodifiable() {
        final List<WatchlistSnapshot.TickerRow> tickerRows = new ArrayList<>(List.of(ROW));
        final List<WatchlistSnapshot.PriceRow> priceRows = new ArrayList<>(List.of(PRICE));

        final WatchlistSnapshot snapshot = new WatchlistSnapshot(tickerRows, "AAPL", priceRows);

        tickerRows.clear();
        priceRows.clear();

        assertEquals(1, snapshot.getTickerRows().size());
        assertEquals(1, snapshot.getSelectedPriceRows().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getTickerRows().add(ROW));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getSelectedPriceRows().add(PRICE));
    }

    @Test
    void nullListsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> new WatchlistSnapshot(null, "AAPL", List.of()));
        assertThrows(NullPointerException.class,
                () -> new WatchlistSnapshot(List.of(), "AAPL", null));
    }

    @Test
    void theRowRecordsCarryEveryColumnTheTablesRender() {
        assertEquals("AAPL", ROW.symbol());
        assertEquals("Apple Inc.", ROW.companyName());
        assertEquals(2, ROW.priceCount());
        assertEquals("2026-03-10", ROW.latestDate());
        assertEquals("101.00", ROW.latestClose());

        assertEquals("2026-03-10", PRICE.date());
        assertEquals("100.00", PRICE.open());
        assertEquals("103.00", PRICE.high());
        assertEquals("99.00", PRICE.low());
        assertEquals("101.00", PRICE.close());
        assertEquals("1001", PRICE.volume());
        assertTrue(PRICE.toString().contains("2026-03-10"));
    }
}
