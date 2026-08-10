package use_case.watchlist;

import java.util.List;

import org.junit.jupiter.api.Test;

import entity.Ticker;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the small boundary types the interactors pass around.
 *
 * <p>They are mostly holders, but three things here are load-bearing and worth pinning:
 * {@code AddTickerOutputData}'s derived {@code isCompanyNameAvailable}, the
 * normalization {@code ShowWatchlistInputData} performs in its constructor, and the
 * membership rules {@link WatchlistInputSupport} applies on behalf of all three
 * mutating use cases.
 */
class WatchlistBoundaryTypesTest {

    private static final WatchlistSnapshot EMPTY_SNAPSHOT =
            new WatchlistSnapshot(List.of(), "", List.of());

    @Test
    void addTickerOutputDataDerivesCompanyNameAvailabilityFromTheNameItself() {
        final AddTickerOutputData named =
                new AddTickerOutputData("AAPL", "Apple Inc.", 5, EMPTY_SNAPSHOT);
        final AddTickerOutputData unnamed =
                new AddTickerOutputData("AAPL", "", 5, EMPTY_SNAPSHOT);

        assertTrue(named.isCompanyNameAvailable());
        assertEquals("Apple Inc.", named.getCompanyName());
        assertFalse(unnamed.isCompanyNameAvailable());
        assertEquals("", unnamed.getCompanyName());

        assertEquals("AAPL", named.getAddedSymbol());
        assertEquals(5, named.getPriceCount());
        assertEquals(EMPTY_SNAPSHOT, named.getSnapshot());
    }

    @Test
    void theFourArgumentConstructorMeansNoDiagnosisRatherThanNoFailure() {
        final AddTickerOutputData undiagnosed =
                new AddTickerOutputData("AAPL", "", 5, EMPTY_SNAPSHOT);

        assertNull(undiagnosed.getCompanyNameFailureKind());
        assertFalse(undiagnosed.isCompanyNameAvailable());
    }

    @Test
    void aCompanyNameFailureKindTravelsWithAnOtherwiseSuccessfulAdd() {
        final AddTickerOutputData diagnosed = new AddTickerOutputData(
                "AAPL", "", 5, EMPTY_SNAPSHOT, MarketDataException.Kind.RATE_LIMIT);

        assertEquals(MarketDataException.Kind.RATE_LIMIT, diagnosed.getCompanyNameFailureKind());
        assertFalse(diagnosed.isCompanyNameAvailable());
        assertEquals(5, diagnosed.getPriceCount());
    }

    @Test
    void addTickerOutputDataRejectsNulls() {
        assertThrows(NullPointerException.class,
                () -> new AddTickerOutputData(null, "Apple Inc.", 1, EMPTY_SNAPSHOT));
        assertThrows(NullPointerException.class,
                () -> new AddTickerOutputData("AAPL", null, 1, EMPTY_SNAPSHOT));
        assertThrows(NullPointerException.class,
                () -> new AddTickerOutputData("AAPL", "Apple Inc.", 1, null));
    }

    @Test
    void removeAndRefreshOutputDataCarryTheirGetters() {
        final RemoveTickerOutputData removed =
                new RemoveTickerOutputData("AAPL", EMPTY_SNAPSHOT);
        assertEquals("AAPL", removed.getRemovedSymbol());
        assertEquals(EMPTY_SNAPSHOT, removed.getSnapshot());

        final RefreshTickerOutputData refreshed =
                new RefreshTickerOutputData("AAPL", 7, "2026-03-10", EMPTY_SNAPSHOT);
        assertEquals("AAPL", refreshed.getSymbol());
        assertEquals(7, refreshed.getPriceCount());
        assertEquals("2026-03-10", refreshed.getLatestDate());
        assertEquals(EMPTY_SNAPSHOT, refreshed.getSnapshot());
    }

    @Test
    void showWatchlistOutputDataCarriesTheCountAndRejectsANullSnapshot() {
        final ShowWatchlistOutputData result = new ShowWatchlistOutputData(3, EMPTY_SNAPSHOT);

        assertEquals(3, result.getTickerCount());
        assertEquals(EMPTY_SNAPSHOT, result.getSnapshot());
        assertThrows(NullPointerException.class, () -> new ShowWatchlistOutputData(3, null));
    }

    @Test
    void showWatchlistInputDataNormalizesNullToNoSelection() {
        assertEquals("",
                new ShowWatchlistInputData(null, ChartPeriod.ALL).getSelectedSymbol());
        assertEquals("AAPL",
                new ShowWatchlistInputData("AAPL", ChartPeriod.ALL).getSelectedSymbol());
    }

    @Test
    void showWatchlistInputDataNormalizesANullPeriodToTheWholeHistory() {
        // A caller with no opinion gets everything, which is what every path except the period
        // combo is. Leaving it null would put a null into the snapshot and out to the view.
        assertEquals(ChartPeriod.ALL, new ShowWatchlistInputData("AAPL", null).getChartPeriod());
        assertEquals(ChartPeriod.THREE_MONTHS,
                new ShowWatchlistInputData("AAPL", ChartPeriod.THREE_MONTHS).getChartPeriod());
    }

    @Test
    void chartPeriodsAreOrderedShortestFirstAndOnlyAllIsUnbounded() {
        // The combo shows them in declaration order, so the order is part of the contract.
        assertEquals(List.of(ChartPeriod.ONE_MONTH, ChartPeriod.THREE_MONTHS,
                        ChartPeriod.SIX_MONTHS, ChartPeriod.ONE_YEAR, ChartPeriod.ALL),
                List.of(ChartPeriod.values()));
        assertEquals(21, ChartPeriod.ONE_MONTH.tradingDays());
        assertEquals(252, ChartPeriod.ONE_YEAR.tradingDays());
        assertTrue(ChartPeriod.ALL.isAll());
        assertFalse(ChartPeriod.ONE_YEAR.isAll());
        assertEquals("3M", ChartPeriod.THREE_MONTHS.toString());
    }

    @Test
    void theThreeInputDataTypesCarryTheRawSymbolUntouched() {
        assertEquals("  aapl ", new AddTickerInputData("  aapl ").getRawSymbol());
        assertEquals("  aapl ", new RemoveTickerInputData("  aapl ").getRawSymbol());
        assertEquals("  aapl ", new RefreshTickerInputData("  aapl ").getRawSymbol());
        assertNull(new AddTickerInputData(null).getRawSymbol());
    }

    @Test
    void marketDataExceptionCarriesItsKindSymbolAndDetailButNeverAKey() {
        final Throwable cause = new IllegalStateException("underlying");
        final MarketDataException exception = new MarketDataException(
                MarketDataException.Kind.RATE_LIMIT, "AAPL", "OVERVIEW quota reached", cause);

        assertEquals(MarketDataException.Kind.RATE_LIMIT, exception.getKind());
        assertEquals("AAPL", exception.getSymbol());
        assertEquals("OVERVIEW quota reached", exception.getTechnicalDetail());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("RATE_LIMIT"));
        assertTrue(exception.getMessage().contains("AAPL"));
    }

    @Test
    void marketDataExceptionRejectsANullKind() {
        assertThrows(NullPointerException.class,
                () -> new MarketDataException(null, "AAPL", "detail"));
    }

    @Test
    void resolveReportsDuplicateOnlyWhenTheSymbolMustBeAbsent() {
        final Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        final WatchlistInputSupport.Resolution absent = WatchlistInputSupport.resolve(
                "aapl", watchlist, WatchlistInputSupport.Membership.MUST_BE_ABSENT);
        assertFalse(absent.isResolved());
        assertNull(absent.getSymbol());
        assertEquals(WatchlistFailure.Kind.DUPLICATE, absent.getFailure().getKind());

        final WatchlistInputSupport.Resolution present = WatchlistInputSupport.resolve(
                "aapl", watchlist, WatchlistInputSupport.Membership.MUST_BE_PRESENT);
        assertTrue(present.isResolved());
        assertEquals("AAPL", present.getSymbol());
        assertNull(present.getFailure());
    }

    @Test
    void resolveReportsNotOnWatchlistOnlyWhenTheSymbolMustBePresent() {
        final Watchlist watchlist = new Watchlist();

        final WatchlistInputSupport.Resolution present = WatchlistInputSupport.resolve(
                "MSFT", watchlist, WatchlistInputSupport.Membership.MUST_BE_PRESENT);
        assertFalse(present.isResolved());
        assertEquals(WatchlistFailure.Kind.NOT_ON_WATCHLIST, present.getFailure().getKind());

        final WatchlistInputSupport.Resolution absent = WatchlistInputSupport.resolve(
                "MSFT", watchlist, WatchlistInputSupport.Membership.MUST_BE_ABSENT);
        assertTrue(absent.isResolved());
        assertEquals("MSFT", absent.getSymbol());
    }

    @Test
    void resolveRejectsNullCollaborators() {
        assertThrows(NullPointerException.class, () -> WatchlistInputSupport.resolve(
                "AAPL", null, WatchlistInputSupport.Membership.MUST_BE_ABSENT));
        assertThrows(NullPointerException.class,
                () -> WatchlistInputSupport.resolve("AAPL", new Watchlist(), null));
    }

    @Test
    void aLookupKeyMatchesOnSymbolAloneAndCarriesNoCompanyName() {
        final Ticker key = WatchlistInputSupport.lookupKey("AAPL");

        assertEquals("AAPL", key.getSymbol());
        assertNull(key.getCompanyName());
        assertEquals(new Ticker("aapl", "Apple Inc."), key);
    }
}
