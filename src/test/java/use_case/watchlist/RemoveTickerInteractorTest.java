package use_case.watchlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import data_access.InMemoryStockRepository;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RemoveTickerInteractor}.
 *
 * <p>Removal has no market-data dependency at all, so there is no gateway here and no
 * provider failure to simulate - which is itself the point worth pinning.
 */
class RemoveTickerInteractorTest {

    private static final String SYMBOL = "AAPL";

    private Watchlist watchlist;
    private InMemoryStockRepository stocks;
    private RecordingSaveWatchlist saveWatchlist;
    private RecordingWatchlistPresenter presenter;
    private RemoveTickerInteractor interactor;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
        stocks = new InMemoryStockRepository();
        saveWatchlist = new RecordingSaveWatchlist();
        presenter = new RecordingWatchlistPresenter();
        interactor = new RemoveTickerInteractor(watchlist, stocks, saveWatchlist, presenter);
    }

    private void seed(String symbol, String companyName, int priceCount) {
        final Ticker ticker = new Ticker(symbol, companyName);
        watchlist.addTicker(ticker);
        stocks.save(new Stock(ticker, WatchlistTestData.ascendingPrices(priceCount)));
    }

    private void execute(String rawSymbol) {
        interactor.execute(new RemoveTickerInputData(rawSymbol));
    }

    @Test
    void nullInputDataFailsFastRatherThanReachingTheOutputBoundary() {
        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> interactor.execute(null));

        assertEquals("Input data cannot be null", exception.getMessage());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(0, presenter.getRemoveSuccessCount());
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThrows(NullPointerException.class,
                () -> new RemoveTickerInteractor(null, stocks, saveWatchlist, presenter));
        assertThrows(NullPointerException.class,
                () -> new RemoveTickerInteractor(watchlist, null, saveWatchlist, presenter));
        assertThrows(NullPointerException.class,
                () -> new RemoveTickerInteractor(watchlist, stocks, null, presenter));
        assertThrows(NullPointerException.class,
                () -> new RemoveTickerInteractor(watchlist, stocks, saveWatchlist, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankInputReportsBlankInput(String rawSymbol) {
        execute(rawSymbol);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
        assertEquals(0, presenter.getRemoveSuccessCount());
        assertEquals(0, saveWatchlist.getCallCount());
    }

    @Test
    void nullRawSymbolReportsBlankInput() {
        execute(null);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
    }

    @Test
    void overLongInputReportsTooLong() {
        execute("ABCDEFGHIJK");

        assertEquals(WatchlistFailure.Kind.TOO_LONG, presenter.getFailureKind());
        assertEquals("ABCDEFGHIJK", presenter.getFailure().getSymbol());
    }

    @Test
    void illegalCharactersReportBadFormat() {
        execute("AA/PL");

        assertEquals(WatchlistFailure.Kind.BAD_FORMAT, presenter.getFailureKind());
    }

    @Test
    void removingSomethingNeverAddedReportsNotOnWatchlist() {
        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.NOT_ON_WATCHLIST, presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(0, presenter.getRemoveSuccessCount());
        assertEquals(0, saveWatchlist.getCallCount());
    }

    @Test
    void successfulRemoveDropsTheTickerItsPricesAndSavesOnce() {
        seed(SYMBOL, "Apple Inc.", 4);

        execute(SYMBOL);

        assertEquals(1, presenter.getRemoveSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(SYMBOL, presenter.getRemoveResult().getRemovedSymbol());

        assertFalse(watchlist.contains(new Ticker(SYMBOL, null)));
        assertTrue(stocks.findBySymbol(SYMBOL).isEmpty());
        assertEquals(1, saveWatchlist.getCallCount());
        assertSame(watchlist, saveWatchlist.getLastSaved());
    }

    @Test
    void removeClearsTheSelectionAndThePriceRows() {
        seed(SYMBOL, "Apple Inc.", 4);
        seed("MSFT", "Microsoft Corporation", 3);

        execute(SYMBOL);

        final WatchlistSnapshot snapshot = presenter.getRemoveResult().getSnapshot();
        assertEquals("", snapshot.getSelectedSymbol());
        assertTrue(snapshot.getSelectedPriceRows().isEmpty());
        assertEquals(1, snapshot.getTickerRows().size());
        assertEquals("MSFT", snapshot.getTickerRows().get(0).symbol());
    }

    @Test
    void lowercaseInputRemovesTheUppercaseEntry() {
        seed(SYMBOL, "Apple Inc.", 2);

        execute(" aapl ");

        assertEquals(1, presenter.getRemoveSuccessCount());
        assertEquals(SYMBOL, presenter.getRemoveResult().getRemovedSymbol());
        assertTrue(watchlist.getEntries().isEmpty());
        assertTrue(stocks.findBySymbol(SYMBOL).isEmpty());
    }

    @Test
    void removingTheSameSymbolTwiceReportsNotOnWatchlistTheSecondTime() {
        seed(SYMBOL, "Apple Inc.", 2);

        execute(SYMBOL);
        execute(SYMBOL);

        assertEquals(1, presenter.getRemoveSuccessCount());
        assertEquals(1, presenter.getFailureCount());
        assertEquals(WatchlistFailure.Kind.NOT_ON_WATCHLIST, presenter.getFailureKind());
        assertEquals(1, saveWatchlist.getCallCount());
    }

    @Test
    void removingATickerWithNoStoredPricesIsStillASuccess() {
        watchlist.addTicker(new Ticker(SYMBOL, null));

        execute(SYMBOL);

        assertEquals(1, presenter.getRemoveSuccessCount());
        assertTrue(presenter.getRemoveResult().getSnapshot().getTickerRows().isEmpty());
    }
}
