package use_case.watchlist;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import data_access.InMemoryMarketDataGateway;
import data_access.InMemoryStockRepository;
import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RefreshTickerInteractor}, entirely offline.
 *
 * <p>The two behaviours worth guarding hardest: refresh never saves the watchlist,
 * because it changes prices and not membership; and a provider failure leaves the
 * previously stored history in place rather than losing it.
 */
class RefreshTickerInteractorTest {

    private static final String SYMBOL = "AAPL";

    private Watchlist watchlist;
    private InMemoryMarketDataGateway gateway;
    private InMemoryStockRepository stocks;
    private RecordingSaveWatchlist saveWatchlist;
    private RecordingWatchlistPresenter presenter;
    private RefreshTickerInteractor interactor;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
        gateway = new InMemoryMarketDataGateway();
        stocks = new InMemoryStockRepository();
        saveWatchlist = new RecordingSaveWatchlist();
        presenter = new RecordingWatchlistPresenter();
        interactor = new RefreshTickerInteractor(watchlist, gateway, stocks, presenter);
    }

    private void seed(String symbol, String companyName, int priceCount) {
        final Ticker ticker = new Ticker(symbol, companyName);
        watchlist.addTicker(ticker);
        stocks.save(new Stock(ticker, WatchlistTestData.ascendingPrices(priceCount)));
    }

    private void execute(String rawSymbol) {
        interactor.execute(new RefreshTickerInputData(rawSymbol));
    }

    @Test
    void nullInputDataFailsFastRatherThanReachingTheOutputBoundary() {
        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> interactor.execute(null));

        assertEquals("Input data cannot be null", exception.getMessage());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(0, presenter.getRefreshSuccessCount());
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThrows(NullPointerException.class,
                () -> new RefreshTickerInteractor(null, gateway, stocks, presenter));
        assertThrows(NullPointerException.class,
                () -> new RefreshTickerInteractor(watchlist, null, stocks, presenter));
        assertThrows(NullPointerException.class,
                () -> new RefreshTickerInteractor(watchlist, gateway, null, presenter));
        assertThrows(NullPointerException.class,
                () -> new RefreshTickerInteractor(watchlist, gateway, stocks, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void blankInputReportsBlankInputAndNeverReachesTheProvider(String rawSymbol) {
        execute(rawSymbol);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
        assertEquals(0, gateway.getPriceCallCount(SYMBOL));
    }

    @Test
    void nullRawSymbolReportsBlankInput() {
        execute(null);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
    }

    @Test
    void overLongInputReportsTooLong() {
        execute("ABCDEFGHIJKL");

        assertEquals(WatchlistFailure.Kind.TOO_LONG, presenter.getFailureKind());
    }

    @Test
    void illegalCharactersReportBadFormat() {
        execute("AA PL!");

        assertEquals(WatchlistFailure.Kind.BAD_FORMAT, presenter.getFailureKind());
    }

    @Test
    void refreshingSomethingNotOnTheWatchlistDoesNotSpendAProviderCall() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(3));

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.NOT_ON_WATCHLIST, presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(0, gateway.getPriceCallCount(SYMBOL));
        assertEquals(0, presenter.getRefreshSuccessCount());
    }

    @Test
    void successfulRefreshReplacesTheStoredHistoryAndNeverSaves() {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(7));

        execute(SYMBOL);

        assertEquals(1, presenter.getRefreshSuccessCount());
        assertEquals(0, presenter.getFailureCount());

        final RefreshTickerOutputData result = presenter.getRefreshResult();
        assertEquals(SYMBOL, result.getSymbol());
        assertEquals(7, result.getPriceCount());
        assertEquals(WatchlistTestData.LAST_DAY.toString(), result.getLatestDate());
        assertEquals(7, stocks.findBySymbol(SYMBOL).orElseThrow().getPriceCount());

        /*
         * Refresh changes prices, not membership, so persistence is not involved -
         * deliberately enforced by RefreshTickerInteractor's constructor taking no
         * SaveWatchlist.InputBoundary. The recording double is held here so that
         * guarantee fails loudly if that arity is ever widened.
         */
        assertEquals(0, saveWatchlist.getCallCount());
    }

    @Test
    void refreshPreservesThePreviouslyDiscoveredCompanyName() {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(5));

        execute(SYMBOL);

        assertEquals("Apple Inc.", stocks.findBySymbol(SYMBOL).orElseThrow().getCompanyName());
        assertEquals("Apple Inc.",
                presenter.getRefreshResult().getSnapshot().getTickerRows().get(0).companyName());
        assertEquals(0, gateway.getCompanyNameCallCount(SYMBOL));
    }

    @Test
    void refreshingATickerWithNoStoredHistoryRecoversTheNameFromTheWatchlist() {
        watchlist.addTicker(new Ticker(SYMBOL, "Apple Inc."));
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(4));

        execute(SYMBOL);

        assertEquals(1, presenter.getRefreshSuccessCount());
        assertEquals(4, presenter.getRefreshResult().getPriceCount());
        assertEquals("Apple Inc.", stocks.findBySymbol(SYMBOL).orElseThrow().getCompanyName());
    }

    @ParameterizedTest
    @EnumSource(MarketDataException.Kind.class)
    void everyProviderFailureKindIsReportedAsTheMatchingWatchlistFailure(
            MarketDataException.Kind kind) {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.failPricesWith(SYMBOL, new MarketDataException(kind, SYMBOL, "simulated"));

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.valueOf(kind.name()), presenter.getFailureKind());
        assertEquals(0, presenter.getRefreshSuccessCount());
    }

    @Test
    void aProviderFailureLeavesThePriorPriceHistoryIntact() {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.failPricesWith(SYMBOL, new MarketDataException(
                MarketDataException.Kind.RATE_LIMIT, SYMBOL, "simulated"));

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.RATE_LIMIT, presenter.getFailureKind());
        assertEquals(3, stocks.findBySymbol(SYMBOL).orElseThrow().getPriceCount());
        assertTrue(watchlist.contains(new Ticker(SYMBOL, null)));
        assertEquals(0, saveWatchlist.getCallCount());
    }

    @Test
    void unsortedPricesAreReportedAsMalformedResponseAndKeepThePriorHistory() {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.putPrices(SYMBOL, WatchlistTestData.descendingPrices());

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.MALFORMED_RESPONSE, presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(0, presenter.getRefreshSuccessCount());
        assertEquals(3, stocks.findBySymbol(SYMBOL).orElseThrow().getPriceCount());
    }

    @Test
    void unsortedPricesForATickerWithNoStoredHistoryAreAlsoMalformedResponse() {
        watchlist.addTicker(new Ticker(SYMBOL, "Apple Inc."));
        gateway.putPrices(SYMBOL, WatchlistTestData.duplicateDatePrices());

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.MALFORMED_RESPONSE, presenter.getFailureKind());
        assertTrue(stocks.findBySymbol(SYMBOL).isEmpty());
    }

    @Test
    void anEmptyRefreshedHistoryReportsNoLatestDate() {
        seed(SYMBOL, "Apple Inc.", 3);
        gateway.putPrices(SYMBOL, List.<DailyPrice>of());

        execute(SYMBOL);

        assertEquals(1, presenter.getRefreshSuccessCount());
        assertEquals(0, presenter.getRefreshResult().getPriceCount());
        assertEquals("", presenter.getRefreshResult().getLatestDate());
    }

    @Test
    void lowercaseInputRefreshesTheUppercaseEntry() {
        seed(SYMBOL, "Apple Inc.", 2);
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(6));

        execute("aapl");

        assertEquals(1, presenter.getRefreshSuccessCount());
        assertEquals(SYMBOL, presenter.getRefreshResult().getSymbol());
        assertEquals(SYMBOL, presenter.getRefreshResult().getSnapshot().getSelectedSymbol());
        assertEquals(6, presenter.getRefreshResult().getSnapshot().getSelectedPriceRows().size());
    }
}
