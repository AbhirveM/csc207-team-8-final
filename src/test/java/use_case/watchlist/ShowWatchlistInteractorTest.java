package use_case.watchlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import data_access.InMemoryStockRepository;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ShowWatchlistInteractor}.
 *
 * <p>The defining property is what this use case does <em>not</em> do: no gateway is
 * even constructible into it, and no save can happen, so re-rendering the watchlist can
 * never spend a request from the daily quota or rewrite the saved file.
 */
class ShowWatchlistInteractorTest {

    private static final String SYMBOL = "AAPL";

    private Watchlist watchlist;
    private InMemoryStockRepository stocks;
    private RecordingSaveWatchlist saveWatchlist;
    private RecordingWatchlistPresenter presenter;
    private ShowWatchlistInteractor interactor;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
        stocks = new InMemoryStockRepository();
        saveWatchlist = new RecordingSaveWatchlist();
        presenter = new RecordingWatchlistPresenter();
        interactor = new ShowWatchlistInteractor(watchlist, stocks, presenter);
    }

    private void seed(String symbol, String companyName, int priceCount) {
        final Ticker ticker = new Ticker(symbol, companyName);
        watchlist.addTicker(ticker);
        stocks.save(new Stock(ticker, WatchlistTestData.ascendingPrices(priceCount)));
    }

    private void execute(String selectedSymbol) {
        interactor.execute(new ShowWatchlistInputData(selectedSymbol, ChartPeriod.ALL));
    }

    private void execute(String selectedSymbol, ChartPeriod period) {
        interactor.execute(new ShowWatchlistInputData(selectedSymbol, period));
    }

    @Test
    void theRequestedPeriodReachesTheSnapshotAndNarrowsTheSeriesToItsTail() {
        // The tail, not the head. subList(0, days) compiles and runs and plots the oldest month
        // under a label reading "1M", which nothing on screen would give away.
        seed("AAPL", "Apple Inc.", 40);

        execute("AAPL", ChartPeriod.ONE_MONTH);

        final WatchlistSnapshot snapshot = presenter.getShowResult().getSnapshot();
        assertEquals(ChartPeriod.ONE_MONTH, snapshot.getChartPeriod());
        assertEquals(21, snapshot.getSelectedCloses().size());

        // The last close is the same either way; the first is what tells the two apart.
        execute("AAPL", ChartPeriod.ALL);
        final List<Double> all = presenter.getShowResult().getSnapshot().getSelectedCloses();
        execute("AAPL", ChartPeriod.ONE_MONTH);
        final List<Double> month = presenter.getShowResult().getSnapshot().getSelectedCloses();

        assertEquals(all.get(all.size() - 1), month.get(month.size() - 1),
                "the window must end on the most recent close");
        assertEquals(all.get(all.size() - 21), month.get(0),
                "the window must start 21 days back, not at the beginning of the history");
    }

    @Test
    void aPeriodLongerThanTheHistoryClampsToEverythingRatherThanFailing() {
        // 120 days of offline data means 6M and 1Y both show everything. That is the correct
        // answer to the question the user asked, not an error to report.
        seed("AAPL", "Apple Inc.", 30);

        execute("AAPL", ChartPeriod.ONE_YEAR);

        final WatchlistSnapshot snapshot = presenter.getShowResult().getSnapshot();
        assertEquals(30, snapshot.getSelectedCloses().size());
        assertEquals(0, presenter.getFailureCount());
    }

    @Test
    void narrowingTheChartLeavesTheDailyPriceTableWhole() {
        // The table is the audit trail behind the chart, and its row count is echoed by the
        // "Days of history" column beside it. Shortening one without the other would have the
        // same ticker report two different histories on one screen.
        seed("AAPL", "Apple Inc.", 40);

        execute("AAPL", ChartPeriod.ONE_MONTH);

        final WatchlistSnapshot snapshot = presenter.getShowResult().getSnapshot();
        assertEquals(21, snapshot.getSelectedCloses().size());
        assertEquals(40, snapshot.getSelectedPriceRows().size());
        assertEquals(40, snapshot.getTickerRows().get(0).priceCount());
    }

    @Test
    void nullInputDataFailsFastRatherThanReachingTheOutputBoundary() {
        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> interactor.execute(null));

        assertEquals("Input data cannot be null", exception.getMessage());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(0, presenter.getShowSuccessCount());
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThrows(NullPointerException.class,
                () -> new ShowWatchlistInteractor(null, stocks, presenter));
        assertThrows(NullPointerException.class,
                () -> new ShowWatchlistInteractor(watchlist, null, presenter));
        assertThrows(NullPointerException.class,
                () -> new ShowWatchlistInteractor(watchlist, stocks, null));
    }

    @Test
    void anEmptyWatchlistIsShownRatherThanReportedAsAFailure() {
        execute("");

        assertEquals(1, presenter.getShowSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(0, presenter.getShowResult().getTickerCount());
        assertTrue(presenter.getShowResult().getSnapshot().getTickerRows().isEmpty());
        assertEquals("", presenter.getShowResult().getSnapshot().getSelectedSymbol());
        assertTrue(presenter.getShowResult().getSnapshot().getSelectedPriceRows().isEmpty());
    }

    @Test
    void showingASelectedSymbolPopulatesItsPriceRows() {
        seed(SYMBOL, "Apple Inc.", 4);
        seed("MSFT", "Microsoft Corporation", 6);

        execute(SYMBOL);

        assertEquals(1, presenter.getShowSuccessCount());

        final ShowWatchlistOutputData result = presenter.getShowResult();
        assertEquals(2, result.getTickerCount());
        assertEquals(2, result.getSnapshot().getTickerRows().size());
        assertEquals(SYMBOL, result.getSnapshot().getSelectedSymbol());
        assertEquals(4, result.getSnapshot().getSelectedPriceRows().size());
        assertEquals(WatchlistTestData.LAST_DAY.toString(),
                result.getSnapshot().getSelectedPriceRows().get(0).date());
    }

    @Test
    void selectingNothingShowsTheTableWithNoPriceRows() {
        seed(SYMBOL, "Apple Inc.", 4);

        execute("");

        assertEquals(1, presenter.getShowResult().getTickerCount());
        assertEquals("", presenter.getShowResult().getSnapshot().getSelectedSymbol());
        assertTrue(presenter.getShowResult().getSnapshot().getSelectedPriceRows().isEmpty());
    }

    @Test
    void aNullSelectedSymbolIsNormalizedToNoSelection() {
        seed(SYMBOL, "Apple Inc.", 4);

        execute(null);

        assertEquals(1, presenter.getShowSuccessCount());
        assertEquals("", presenter.getShowResult().getSnapshot().getSelectedSymbol());
    }

    @Test
    void anUnknownSelectedSymbolDegradesSilentlyRatherThanFailing() {
        seed(SYMBOL, "Apple Inc.", 4);

        execute("MSFT");

        assertEquals(1, presenter.getShowSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertEquals("", presenter.getShowResult().getSnapshot().getSelectedSymbol());
        assertTrue(presenter.getShowResult().getSnapshot().getSelectedPriceRows().isEmpty());
        assertEquals(1, presenter.getShowResult().getSnapshot().getTickerRows().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDEFGHIJK", "AA$PL", "   "})
    void anUnusableSelectedSymbolDegradesSilentlyToo(String selectedSymbol) {
        seed(SYMBOL, "Apple Inc.", 4);

        execute(selectedSymbol);

        assertEquals(1, presenter.getShowSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertEquals("", presenter.getShowResult().getSnapshot().getSelectedSymbol());
    }

    @Test
    void lowercaseSelectionResolvesToTheUppercaseEntry() {
        seed(SYMBOL, "Apple Inc.", 5);

        execute(" aapl ");

        assertEquals(SYMBOL, presenter.getShowResult().getSnapshot().getSelectedSymbol());
        assertEquals(5, presenter.getShowResult().getSnapshot().getSelectedPriceRows().size());
    }

    /**
     * Show cannot persist anything: {@link ShowWatchlistInteractor}'s constructor takes
     * no {@code SaveWatchlist.InputBoundary} at all, so this is a structural guarantee
     * that the recording double is here to keep honest if that arity ever changes.
     */
    @Test
    void showNeverSavesTheWatchlist() {
        seed(SYMBOL, "Apple Inc.", 3);

        execute(SYMBOL);
        execute("");
        execute("MSFT");

        assertEquals(3, presenter.getShowSuccessCount());
        assertEquals(0, saveWatchlist.getCallCount());
    }

    @Test
    void aTickerRestoredFromDiskWithNoPricesStillRenders() {
        watchlist.addTicker(new Ticker(SYMBOL, "Apple Inc."));

        execute(SYMBOL);

        assertEquals(1, presenter.getShowSuccessCount());
        assertEquals(1, presenter.getShowResult().getTickerCount());

        final WatchlistSnapshot.TickerRow row =
                presenter.getShowResult().getSnapshot().getTickerRows().get(0);
        assertEquals(SYMBOL, row.symbol());
        assertEquals("Apple Inc.", row.companyName());
        assertEquals(0, row.priceCount());
        assertEquals("", row.latestDate());
        assertEquals("", row.latestClose());
        assertTrue(presenter.getShowResult().getSnapshot().getSelectedPriceRows().isEmpty());
    }

    @Test
    void aTickerWithNoCompanyNameFallsBackToItsSymbol() {
        watchlist.addTicker(new Ticker(SYMBOL, null));

        execute(SYMBOL);

        assertEquals(SYMBOL,
                presenter.getShowResult().getSnapshot().getTickerRows().get(0).companyName());
    }
}
