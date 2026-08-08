package interface_adapter.watchlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.watchlist.AddTickerInputBoundary;
import use_case.watchlist.AddTickerInputData;
import use_case.watchlist.RefreshTickerInputBoundary;
import use_case.watchlist.RefreshTickerInputData;
import use_case.watchlist.RemoveTickerInputBoundary;
import use_case.watchlist.RemoveTickerInputData;
import use_case.watchlist.ShowWatchlistInputBoundary;
import use_case.watchlist.ShowWatchlistInputData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the controller is a pass-through and nothing more.
 *
 * <p>The load-bearing assertions are the ones about lowercase and surrounding whitespace
 * <em>surviving</em>. Normalization belongs to {@code TickerSymbolValidator} inside the
 * use-case layer, where it is already tested; a controller that trimmed would duplicate
 * that coverage while hiding the regression if the two ever disagreed.
 */
class WatchlistControllerTest {

    /** A raw input that is neither trimmed nor upper-cased anywhere in this layer. */
    private static final String MESSY = "  aapl  ";

    private RecordingAddTicker addTicker;
    private RecordingRemoveTicker removeTicker;
    private RecordingRefreshTicker refreshTicker;
    private RecordingShowWatchlist showWatchlist;
    private WatchlistController controller;

    @BeforeEach
    void setUp() {
        addTicker = new RecordingAddTicker();
        removeTicker = new RecordingRemoveTicker();
        refreshTicker = new RecordingRefreshTicker();
        showWatchlist = new RecordingShowWatchlist();
        controller = new WatchlistController(addTicker, removeTicker, refreshTicker,
                showWatchlist);
    }

    @Test
    void addTickerPassesTheRawSymbolThroughWithItsCaseAndWhitespaceIntact() {
        controller.addTicker(MESSY);

        assertEquals(MESSY, addTicker.rawSymbol);
        assertEquals(1, addTicker.calls);
    }

    @Test
    void removeTickerPassesTheRawSymbolThroughWithItsCaseAndWhitespaceIntact() {
        controller.removeTicker(MESSY);

        assertEquals(MESSY, removeTicker.rawSymbol);
        assertEquals(1, removeTicker.calls);
    }

    @Test
    void refreshTickerPassesTheRawSymbolThroughWithItsCaseAndWhitespaceIntact() {
        controller.refreshTicker(MESSY);

        assertEquals(MESSY, refreshTicker.rawSymbol);
        assertEquals(1, refreshTicker.calls);
    }

    @Test
    void showWatchlistPassesTheSelectedSymbolThroughWithItsCaseAndWhitespaceIntact() {
        controller.showWatchlist(MESSY);

        assertEquals(MESSY, showWatchlist.selectedSymbol);
        assertEquals(1, showWatchlist.calls);
    }

    @Test
    void showWatchlistPassesTheEmptySelectionThroughUnchanged() {
        controller.showWatchlist("");

        assertEquals("", showWatchlist.selectedSymbol);
    }

    @Test
    void aBlankSymbolIsNotRejectedHereBecauseValidationIsTheInteractorsJob() {
        controller.addTicker("   ");

        assertEquals("   ", addTicker.rawSymbol);
        assertEquals(1, addTicker.calls);
    }

    @Test
    void eachMethodCallsOnlyItsOwnBoundary() {
        controller.addTicker("AAPL");

        assertEquals(1, addTicker.calls);
        assertEquals(0, removeTicker.calls);
        assertEquals(0, refreshTicker.calls);
        assertEquals(0, showWatchlist.calls);
    }

    @Test
    void theConstructorRejectsANullAddTickerBoundary() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistController(null, removeTicker, refreshTicker, showWatchlist));
        assertEquals("Add ticker input boundary cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullRemoveTickerBoundary() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistController(addTicker, null, refreshTicker, showWatchlist));
        assertEquals("Remove ticker input boundary cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullRefreshTickerBoundary() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistController(addTicker, removeTicker, null, showWatchlist));
        assertEquals("Refresh ticker input boundary cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullShowWatchlistBoundary() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistController(addTicker, removeTicker, refreshTicker, null));
        assertEquals("Show watchlist input boundary cannot be null", exception.getMessage());
    }

    /** Records the raw symbol Add Ticker was invoked with. */
    private static final class RecordingAddTicker implements AddTickerInputBoundary {
        private String rawSymbol;
        private int calls;

        @Override
        public void execute(AddTickerInputData inputData) {
            rawSymbol = inputData.getRawSymbol();
            calls++;
        }
    }

    /** Records the raw symbol Remove Ticker was invoked with. */
    private static final class RecordingRemoveTicker implements RemoveTickerInputBoundary {
        private String rawSymbol;
        private int calls;

        @Override
        public void execute(RemoveTickerInputData inputData) {
            rawSymbol = inputData.getRawSymbol();
            calls++;
        }
    }

    /** Records the raw symbol Refresh Ticker was invoked with. */
    private static final class RecordingRefreshTicker implements RefreshTickerInputBoundary {
        private String rawSymbol;
        private int calls;

        @Override
        public void execute(RefreshTickerInputData inputData) {
            rawSymbol = inputData.getRawSymbol();
            calls++;
        }
    }

    /** Records the symbol Show Watchlist was invoked with. */
    private static final class RecordingShowWatchlist implements ShowWatchlistInputBoundary {
        private String selectedSymbol;
        private int calls;

        @Override
        public void execute(ShowWatchlistInputData inputData) {
            selectedSymbol = inputData.getSelectedSymbol();
            calls++;
        }
    }
}
