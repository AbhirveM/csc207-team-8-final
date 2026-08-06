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
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        for (String symbol : List.of("AAPL", "MSFT", "TSLA")) {
            assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                    gateway.fetchDailyPrices(symbol).size(), symbol);
            assertTrue(gateway.fetchCompanyName(symbol).isPresent(), symbol);
        }
    }

    @Test
    void lookupIsCaseInsensitive() throws Exception {
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                gateway.fetchDailyPrices("aapl").size());
    }

    @Test
    void unknownSymbolIsReportedAsInvalid() {
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        MarketDataException thrown = assertThrows(MarketDataException.class,
                () -> gateway.fetchDailyPrices("ZZZZ"));
        assertEquals(MarketDataException.Kind.INVALID_SYMBOL, thrown.getKind());
    }

    @Test
    void unknownCompanyNameIsEmptyRatherThanAFailure() throws Exception {
        InMemoryMarketDataGateway gateway = new InMemoryMarketDataGateway()
                .putPrices("VOO", InMemoryMarketDataGateway.syntheticSeries(
                        "VOO", LocalDate.of(2026, 8, 5), 10));

        assertTrue(gateway.fetchCompanyName("VOO").isEmpty());
    }

    @Test
    void configuredFailuresAreThrown() {
        MarketDataException quota = new MarketDataException(
                MarketDataException.Kind.RATE_LIMIT, "AAPL", "quota used up");
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData()
                .failPricesWith("AAPL", quota)
                .failCompanyNameWith("MSFT", quota);

        assertEquals(quota, assertThrows(MarketDataException.class,
                () -> gateway.fetchDailyPrices("AAPL")));
        assertEquals(quota, assertThrows(MarketDataException.class,
                () -> gateway.fetchCompanyName("MSFT")));
    }

    @Test
    void callCountsAreRecordedPerSymbol() throws Exception {
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("aapl");
        gateway.fetchCompanyName("AAPL");

        assertEquals(2, gateway.getPriceCallCount("AAPL"));
        assertEquals(1, gateway.getCompanyNameCallCount("AAPL"));
        assertEquals(0, gateway.getPriceCallCount("MSFT"));
    }

    @Test
    void fetchDailyPricesFreshDelegatesWhenNotCaching() throws Exception {
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();

        assertEquals(InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT,
                gateway.fetchDailyPricesFresh("AAPL").size());
        assertEquals(1, gateway.getPriceCallCount("AAPL"));
    }

    @Test
    void syntheticSeriesIsOldestToNewestAndSkipsWeekends() {
        List<DailyPrice> prices = InMemoryMarketDataGateway.syntheticSeries(
                "AAPL", InMemoryMarketDataGateway.SAMPLE_LAST_TRADING_DAY, 60);

        assertEquals(60, prices.size());
        assertEquals(InMemoryMarketDataGateway.SAMPLE_LAST_TRADING_DAY,
                prices.get(prices.size() - 1).getDate());

        for (int index = 0; index < prices.size(); index++) {
            DayOfWeek day = prices.get(index).getDate().getDayOfWeek();
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
        LocalDate lastDay = LocalDate.of(2026, 8, 5);

        assertEquals(InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20),
                InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20));
        assertTrue(!InMemoryMarketDataGateway.syntheticSeries("AAPL", lastDay, 20)
                        .equals(InMemoryMarketDataGateway.syntheticSeries("TSLA", lastDay, 20)),
                "Different symbols should produce different series");
    }

    @Test
    void syntheticSeriesHasHighLowBoundingOpenAndClose() {
        for (DailyPrice price : InMemoryMarketDataGateway.syntheticSeries(
                "MSFT", LocalDate.of(2026, 8, 5), 40)) {
            assertTrue(price.getHigh() >= Math.max(price.getOpen(), price.getClose()));
            assertTrue(price.getLow() <= Math.min(price.getOpen(), price.getClose()));
            assertTrue(price.getVolume() > 0);
        }
    }

    /**
     * The sample data exists so the offline demo and the strategy hand-off are not
     * vacuous. A flat or monotonic series would yield nothing but HOLD, so this pins
     * down that a short moving average really does cross a long one.
     */
    @Test
    void sampleDataProducesRealBuyAndSellSignals() throws Exception {
        InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();
        List<DailyPrice> prices = gateway.fetchDailyPrices("AAPL");

        List<TradingSignal> signals = new MovingAverageCrossoverStrategy(
                new MovingAverageConfiguration(10, 50)).generateSignals(prices);

        long buys = signals.stream().filter(s -> s.getSignalType() == SignalType.BUY).count();
        long sells = signals.stream().filter(s -> s.getSignalType() == SignalType.SELL).count();

        assertEquals(prices.size(), signals.size());
        assertTrue(buys > 0, "Expected at least one BUY signal, got " + buys);
        assertTrue(sells > 0, "Expected at least one SELL signal, got " + sells);
    }
}
