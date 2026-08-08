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
import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AddTickerInteractor}, entirely offline.
 *
 * <p>Everything runs against {@link InMemoryMarketDataGateway} and
 * {@link InMemoryStockRepository}; no test here opens a socket.
 */
class AddTickerInteractorTest {

    private static final String SYMBOL = "AAPL";

    private Watchlist watchlist;
    private InMemoryMarketDataGateway gateway;
    private InMemoryStockRepository stocks;
    private RecordingSaveWatchlist saveWatchlist;
    private RecordingWatchlistPresenter presenter;
    private AddTickerInteractor interactor;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
        gateway = new InMemoryMarketDataGateway();
        stocks = new InMemoryStockRepository();
        saveWatchlist = new RecordingSaveWatchlist();
        presenter = new RecordingWatchlistPresenter();
        interactor = new AddTickerInteractor(watchlist, gateway, stocks, saveWatchlist, presenter);
    }

    private void execute(String rawSymbol) {
        interactor.execute(new AddTickerInputData(rawSymbol));
    }

    @Test
    void nullInputDataFailsFastRatherThanReachingTheOutputBoundary() {
        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> interactor.execute(null));

        assertEquals("Input data cannot be null", exception.getMessage());
        assertEquals(0, presenter.getFailureCount());
        assertEquals(0, presenter.getAddSuccessCount());
    }

    @Test
    void constructorRejectsNullCollaborators() {
        assertThrows(NullPointerException.class,
                () -> new AddTickerInteractor(null, gateway, stocks, saveWatchlist, presenter));
        assertThrows(NullPointerException.class,
                () -> new AddTickerInteractor(watchlist, null, stocks, saveWatchlist, presenter));
        assertThrows(NullPointerException.class,
                () -> new AddTickerInteractor(watchlist, gateway, null, saveWatchlist, presenter));
        assertThrows(NullPointerException.class,
                () -> new AddTickerInteractor(watchlist, gateway, stocks, null, presenter));
        assertThrows(NullPointerException.class,
                () -> new AddTickerInteractor(watchlist, gateway, stocks, saveWatchlist, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void blankInputReportsBlankInputAndNeverReachesTheProvider(String rawSymbol) {
        execute(rawSymbol);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
        assertEquals(0, presenter.getAddSuccessCount());
        assertEquals(0, gateway.getPriceCallCount(SYMBOL));
        assertTrue(watchlist.getEntries().isEmpty());
    }

    @Test
    void nullRawSymbolReportsBlankInput() {
        execute(null);

        assertEquals(WatchlistFailure.Kind.BLANK_INPUT, presenter.getFailureKind());
        assertEquals("", presenter.getFailure().getSymbol());
    }

    @Test
    void overLongInputReportsTooLongAndQuotesBackTheRawText() {
        execute("ABCDEFGHIJK");

        assertEquals(WatchlistFailure.Kind.TOO_LONG, presenter.getFailureKind());
        assertEquals("ABCDEFGHIJK", presenter.getFailure().getSymbol());
        assertTrue(watchlist.getEntries().isEmpty());
    }

    @Test
    void illegalCharactersReportBadFormat() {
        execute("AA$PL");

        assertEquals(WatchlistFailure.Kind.BAD_FORMAT, presenter.getFailureKind());
        assertEquals("AA$PL", presenter.getFailure().getSymbol());
    }

    @Test
    void aLongRunOfIllegalCharactersPrefersTheMoreSpecificBadFormat() {
        execute("$$$$$$$$$$$$$$$");

        assertEquals(WatchlistFailure.Kind.BAD_FORMAT, presenter.getFailureKind());
    }

    @Test
    void successfulAddStoresPricesSavesTheWatchlistAndReportsTheSnapshot() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(5))
                .putCompanyName(SYMBOL, "Apple Inc.");

        execute(SYMBOL);

        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(0, presenter.getFailureCount());

        final AddTickerOutputData result = presenter.getAddResult();
        assertEquals(SYMBOL, result.getAddedSymbol());
        assertEquals("Apple Inc.", result.getCompanyName());
        assertTrue(result.isCompanyNameAvailable());
        assertEquals(5, result.getPriceCount());

        assertTrue(watchlist.contains(new Ticker(SYMBOL, null)));
        assertEquals(5, stocks.findBySymbol(SYMBOL).orElseThrow().getPriceCount());

        final WatchlistSnapshot snapshot = result.getSnapshot();
        assertEquals(1, snapshot.getTickerRows().size());
        assertEquals(SYMBOL, snapshot.getSelectedSymbol());
        assertEquals(5, snapshot.getSelectedPriceRows().size());

        final WatchlistSnapshot.TickerRow row = snapshot.getTickerRows().get(0);
        assertEquals(SYMBOL, row.symbol());
        assertEquals("Apple Inc.", row.companyName());
        assertEquals(5, row.priceCount());
        assertEquals(WatchlistTestData.LAST_DAY.toString(), row.latestDate());
        assertEquals("104.00", row.latestClose());

        // Newest first, which is presentational only.
        assertEquals(WatchlistTestData.LAST_DAY.toString(),
                snapshot.getSelectedPriceRows().get(0).date());
    }

    @Test
    void saveWatchlistIsCalledExactlyOnceWithTheWatchlistItself() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(3));

        execute(SYMBOL);

        assertEquals(1, saveWatchlist.getCallCount());
        assertEquals(watchlist, saveWatchlist.getLastSaved());
    }

    @Test
    void lowercaseInputIsNormalizedToUppercaseEverywhereItLands() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(3));

        execute("  aa pl ");

        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(SYMBOL, presenter.getAddResult().getAddedSymbol());
        assertEquals(SYMBOL, presenter.getAddResult().getSnapshot().getSelectedSymbol());
        assertEquals(SYMBOL, presenter.getAddResult().getSnapshot().getTickerRows().get(0).symbol());
        assertTrue(stocks.findBySymbol(SYMBOL).isPresent());
    }

    @Test
    void addingTheSameSymbolTwiceReportsDuplicateAndDoesNotSpendAProviderCall() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(3));

        execute(SYMBOL);
        execute("aapl");

        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(WatchlistFailure.Kind.DUPLICATE, presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(1, gateway.getPriceCallCount(SYMBOL));
        assertEquals(1, saveWatchlist.getCallCount());
        assertEquals(1, watchlist.getEntries().size());
    }

    @ParameterizedTest
    @EnumSource(MarketDataException.Kind.class)
    void everyProviderFailureKindIsReportedAsTheMatchingWatchlistFailure(
            MarketDataException.Kind kind) {
        gateway.failPricesWith(SYMBOL, new MarketDataException(kind, SYMBOL, "simulated"));

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.valueOf(kind.name()), presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(0, presenter.getAddSuccessCount());
    }

    @Test
    void aProviderFailureLeavesTheWatchlistCompletelyUnchanged() {
        gateway.failPricesWith(SYMBOL, new MarketDataException(
                MarketDataException.Kind.NETWORK, SYMBOL, "simulated"));

        execute(SYMBOL);

        // Fetch before mutate: nothing is added, nothing is stored, nothing is saved.
        assertTrue(watchlist.getEntries().isEmpty());
        assertTrue(stocks.findBySymbol(SYMBOL).isEmpty());
        assertEquals(0, saveWatchlist.getCallCount());
        assertEquals(0, gateway.getCompanyNameCallCount(SYMBOL));
    }

    @Test
    void anUnknownSymbolIsReportedAsInvalidSymbolByTheFake() {
        execute("ZZZZ");

        assertEquals(WatchlistFailure.Kind.INVALID_SYMBOL, presenter.getFailureKind());
        assertTrue(watchlist.getEntries().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(MarketDataException.Kind.class)
    void aCompanyNameFailureIsNeverFatal(MarketDataException.Kind kind) {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(4))
                .failCompanyNameWith(SYMBOL, new MarketDataException(kind, SYMBOL, "simulated"));

        execute(SYMBOL);

        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertTrue(watchlist.contains(new Ticker(SYMBOL, null)));
        assertEquals("", presenter.getAddResult().getCompanyName());
        assertFalse(presenter.getAddResult().isCompanyNameAvailable());
        assertEquals(4, presenter.getAddResult().getPriceCount());

        // Non-fatal, but not silent: the reason travels with the successful outcome.
        assertEquals(kind, presenter.getAddResult().getCompanyNameFailureKind());
    }

    @Test
    void aRateLimitedNameLookupStillAddsTheTickerAndSaysWhyTheNameIsMissing() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(4))
                .failCompanyNameWith(SYMBOL, new MarketDataException(
                        MarketDataException.Kind.RATE_LIMIT, SYMBOL, "OVERVIEW quota reached"));

        execute(SYMBOL);

        // The add succeeds - vision.md principle 7 - and the ticker really is on the list.
        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(0, presenter.getFailureCount());
        assertTrue(watchlist.contains(new Ticker(SYMBOL, null)));
        assertEquals(1, saveWatchlist.getCallCount());
        assertEquals(4, stocks.findBySymbol(SYMBOL).orElseThrow().getPriceCount());

        /*
         * The distinguishing assertion: a rate-limited lookup and a symbol with no
         * company record both produce an empty name, so only the kind lets the
         * presenter say "unavailable - provider quota reached" rather than staying
         * silent.
         */
        assertEquals("", presenter.getAddResult().getCompanyName());
        assertFalse(presenter.getAddResult().isCompanyNameAvailable());
        assertEquals(MarketDataException.Kind.RATE_LIMIT,
                presenter.getAddResult().getCompanyNameFailureKind());
    }

    @Test
    void aCleanAddReportsNoCompanyNameFailureKind() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(4))
                .putCompanyName(SYMBOL, "Apple Inc.");

        execute(SYMBOL);

        assertEquals("Apple Inc.", presenter.getAddResult().getCompanyName());
        assertTrue(presenter.getAddResult().isCompanyNameAvailable());
        assertNull(presenter.getAddResult().getCompanyNameFailureKind());
    }

    @Test
    void aSymbolWithNoCompanyRecordIsDistinguishableFromAFailedLookup() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(4));

        execute(SYMBOL);

        // Nothing failed - the provider simply has no name - so there is no kind.
        assertEquals("", presenter.getAddResult().getCompanyName());
        assertFalse(presenter.getAddResult().isCompanyNameAvailable());
        assertNull(presenter.getAddResult().getCompanyNameFailureKind());
    }

    @Test
    void anAbsentCompanyNameFallsBackToTheSymbolInTheTable() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(2));

        execute(SYMBOL);

        assertEquals("", presenter.getAddResult().getCompanyName());
        assertEquals(SYMBOL, presenter.getAddResult().getSnapshot().getTickerRows().get(0).companyName());
    }

    @Test
    void aBlankCompanyNameIsTreatedAsNoNameAtAll() {
        gateway.putPrices(SYMBOL, WatchlistTestData.ascendingPrices(2))
                .putCompanyName(SYMBOL, "   ");

        execute(SYMBOL);

        assertEquals("", presenter.getAddResult().getCompanyName());
        assertFalse(presenter.getAddResult().isCompanyNameAvailable());
    }

    @Test
    void unsortedPricesAreReportedAsMalformedResponseRatherThanCrashing() {
        gateway.putPrices(SYMBOL, WatchlistTestData.descendingPrices());

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.MALFORMED_RESPONSE, presenter.getFailureKind());
        assertEquals(SYMBOL, presenter.getFailure().getSymbol());
        assertEquals(0, presenter.getAddSuccessCount());
    }

    @Test
    void duplicateDatesAreReportedAsMalformedResponse() {
        gateway.putPrices(SYMBOL, WatchlistTestData.duplicateDatePrices());

        execute(SYMBOL);

        assertEquals(WatchlistFailure.Kind.MALFORMED_RESPONSE, presenter.getFailureKind());
    }

    @Test
    void aMalformedResponseAlsoLeavesTheWatchlistUnchanged() {
        gateway.putPrices(SYMBOL, WatchlistTestData.descendingPrices());

        execute(SYMBOL);

        assertTrue(watchlist.getEntries().isEmpty());
        assertTrue(stocks.findBySymbol(SYMBOL).isEmpty());
        assertEquals(0, saveWatchlist.getCallCount());
        assertNull(saveWatchlist.getLastSaved());
    }

    @Test
    void anEmptyButWellFormedHistoryIsStillAnAdd() {
        gateway.putPrices(SYMBOL, List.<DailyPrice>of());

        execute(SYMBOL);

        assertEquals(1, presenter.getAddSuccessCount());
        assertEquals(0, presenter.getAddResult().getPriceCount());

        final WatchlistSnapshot.TickerRow row =
                presenter.getAddResult().getSnapshot().getTickerRows().get(0);
        assertEquals("", row.latestDate());
        assertEquals("", row.latestClose());
        assertNotNull(presenter.getAddResult().getSnapshot().getSelectedPriceRows());
        assertTrue(presenter.getAddResult().getSnapshot().getSelectedPriceRows().isEmpty());
    }
}
