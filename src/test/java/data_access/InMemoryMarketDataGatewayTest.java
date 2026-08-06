package data_access;

import entity.DailyPrice;
import entity.MovingAverageConfiguration;
import entity.MovingAverageCrossoverStrategy;
import entity.SignalType;
import entity.TradingSignal;
import org.junit.jupiter.api.Test;
import use_case.watchlist.MarketDataException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMarketDataGatewayTest {

    @Test
    void sampleDataProvidesPricesAndNamesForTheKnownSymbols() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        for (final String symbol : List.of("AAPL", "MSFT", "TSLA")) {
            assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                    gateway.fetchDailyPrices(symbol).size(), symbol);
            assertTrue(gateway.fetchCompanyName(symbol).isPresent(), symbol);
        }
    }

    @Test
    void lookupIsCaseInsensitive() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                gateway.fetchDailyPrices("aapl").size());
    }

    @Test
    void unknownSymbolIsReportedAsInvalid() {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        final MarketDataException thrown = assertThrows(MarketDataException.class,
                () -> gateway.fetchDailyPrices("ZZZZ"));
        assertEquals(MarketDataException.Kind.INVALID_SYMBOL, thrown.getKind());
    }

    @Test
    void unknownCompanyNameIsEmptyRatherThanAFailure() throws Exception {
        final InMemoryMarketDataGateway gateway = new InMemoryMarketDataGateway()
                .putPrices("VOO", InMemoryMarketDataGateway.syntheticSeries(
                        "VOO", LocalDate.of(2026, 8, 5), 10));

        assertTrue(gateway.fetchCompanyName("VOO").isEmpty());
    }

    // --- Symbol contract (MarketDataGateway, orchestrator 5.1) -----------------

    @Test
    void aNullSymbolIsRejectedWithANullPointerException() {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        assertThrows(NullPointerException.class, () -> gateway.fetchDailyPrices(null));
        assertThrows(NullPointerException.class, () -> gateway.fetchDailyPricesFresh(null));
        assertThrows(NullPointerException.class, () -> gateway.fetchCompanyName(null));
    }

    @Test
    void aBlankSymbolIsReportedAsInvalidAndNotRecorded() {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        for (final String blank : List.of("", "   ", "\t")) {
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchDailyPrices(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchDailyPricesFresh(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchCompanyName(blank)).getKind(), blank);
        }

        assertEquals(0, gateway.getPriceCallCount(""));
        assertEquals(0, gateway.getCompanyNameCallCount(""));
    }

    @Test
    void configuredFailuresAreThrown() {
        final MarketDataException quota = new MarketDataException(
                MarketDataException.Kind.RATE_LIMIT, "AAPL", "quota used up");
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData()
                .failPricesWith("AAPL", quota)
                .failCompanyNameWith("MSFT", quota);

        assertEquals(quota, assertThrows(MarketDataException.class,
                () -> gateway.fetchDailyPrices("AAPL")));
        assertEquals(quota, assertThrows(MarketDataException.class,
                () -> gateway.fetchCompanyName("MSFT")));
    }

    @Test
    void callCountsAreRecordedPerSymbol() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("aapl");
        gateway.fetchCompanyName("AAPL");

        assertEquals(2, gateway.getPriceCallCount("AAPL"));
        assertEquals(1, gateway.getCompanyNameCallCount("AAPL"));
        assertEquals(0, gateway.getPriceCallCount("MSFT"));
    }

    @Test
    void fetchDailyPricesFreshDelegatesWhenNotCaching() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                gateway.fetchDailyPricesFresh("AAPL").size());
        assertEquals(1, gateway.getPriceCallCount("AAPL"));
    }

    @Test
    void syntheticSeriesIsOldestToNewestAndSkipsWeekends() {
        final List<DailyPrice> prices = InMemoryMarketDataGateway.syntheticSeries(
                "AAPL", InMemoryMarketDataGateway.SAMPLE_LAST_TRADING_DAY, 60);

        assertEquals(60, prices.size());
        assertEquals(InMemoryMarketDataGateway.SAMPLE_LAST_TRADING_DAY,
                prices.get(prices.size() - 1).getDate());

        for (int index = 0; index < prices.size(); index++) {
            final DayOfWeek day = prices.get(index).getDate().getDayOfWeek();
            assertTrue(day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY,
                    "Weekend date at index " + index);
            if (index > 0) {
                assertTrue(prices.get(index - 1).getDate().isBefore(prices.get(index).getDate()),
                        "Dates must strictly increase at index " + index);
            }
        }
    }

    @Test
    void syntheticSeriesIsDeterministicAndVariesBySymbol() {
        final LocalDate lastDay = LocalDate.of(2026, 8, 5);

        assertEquals(InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20),
                InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20));
        assertTrue(!InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20)
                        .equals(InMemoryMarketDataGateway.syntheticSeries("TSLA", lastDay, 20)),
                "Different symbols should produce different series");
    }

    @Test
    void syntheticSeriesHasHighLowBoundingOpenAndClose() {
        for (final DailyPrice price : InMemoryMarketDataGateway.syntheticSeries(
                "MSFT", LocalDate.of(2026, 8, 5), 40)) {
            assertTrue(price.getHigh() >= Math.max(price.getOpen(), price.getClose()));
            assertTrue(price.getLow() <= Math.min(price.getOpen(), price.getClose()));
            assertTrue(price.getVolume() > 0);
        }
    }

    /**
     * The Phase 5 hand-off test runs at the default 5/20 windows, not the 10/50 pair
     * checked below, so the series has to oscillate fast enough for the short average to
     * cross the long one at that shorter pair too - for every sample symbol.
     */
    @Test
    void sampleDataCrossesAtTheDefaultFiveAndTwentyWindows() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        for (final String symbol : List.of("AAPL", "MSFT", "TSLA")) {
            final List<TradingSignal> signals = new MovingAverageCrossoverStrategy(
                    new MovingAverageConfiguration(5, 20))
                    .generateSignals(gateway.fetchDailyPrices(symbol));

            final long buys = signals.stream()
                    .filter(signal -> signal.getSignalType() == SignalType.BUY).count();
            final long sells = signals.stream()
                    .filter(signal -> signal.getSignalType() == SignalType.SELL).count();

            assertTrue(buys > 0, symbol + " produced no BUY signal at 5/20");
            assertTrue(sells > 0, symbol + " produced no SELL signal at 5/20");
        }
    }

    /**
     * The sample data exists so the offline demo and the strategy hand-off are not
     * vacuous. A flat or monotonic series would yield nothing but HOLD, so this pins
     * down that a short moving average really does cross a long one.
     */
    @Test
    void sampleDataProducesRealBuyAndSellSignals() throws Exception {
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();
        final List<DailyPrice> prices = gateway.fetchDailyPrices("AAPL");

        final List<TradingSignal> signals = new MovingAverageCrossoverStrategy(
                new MovingAverageConfiguration(10, 50)).generateSignals(prices);

        final long buys = signals.stream().filter(s -> s.getSignalType() == SignalType.BUY).count();
        final long sells = signals.stream().filter(s -> s.getSignalType() == SignalType.SELL).count();

        assertEquals(prices.size(), signals.size());
        assertTrue(buys > 0, "Expected at least one BUY signal, got " + buys);
        assertTrue(sells > 0, "Expected at least one SELL signal, got " + sells);
    }
}
