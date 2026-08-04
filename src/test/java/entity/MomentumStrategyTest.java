package entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MomentumStrategyTest {

    @Test
    void returnsCorrectName() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));

        assertEquals("Momentum RSI Strategy", strategy.getName());
    }

    @Test
    void increasingPricesGenerateSellSignals() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 11.0),
                createPrice(3, 12.0),
                createPrice(4, 13.0),
                createPrice(5, 14.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(5, signals.size());
        assertEquals(SignalType.HOLD, signals.get(0).getSignalType());
        assertEquals(SignalType.HOLD, signals.get(1).getSignalType());
        assertEquals(SignalType.HOLD, signals.get(2).getSignalType());
        assertEquals(SignalType.SELL, signals.get(3).getSignalType());
        assertEquals(SignalType.SELL, signals.get(4).getSignalType());
    }

    @Test
    void decreasingPricesGenerateBuySignals() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 14.0),
                createPrice(2, 13.0),
                createPrice(3, 12.0),
                createPrice(4, 11.0),
                createPrice(5, 10.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(SignalType.HOLD, signals.get(0).getSignalType());
        assertEquals(SignalType.HOLD, signals.get(1).getSignalType());
        assertEquals(SignalType.HOLD, signals.get(2).getSignalType());
        assertEquals(SignalType.BUY, signals.get(3).getSignalType());
        assertEquals(SignalType.BUY, signals.get(4).getSignalType());
    }

    @Test
    void unchangedPricesGenerateHoldSignals() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 10.0),
                createPrice(3, 10.0),
                createPrice(4, 10.0),
                createPrice(5, 10.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        for (TradingSignal signal : signals) {
            assertEquals(SignalType.HOLD, signal.getSignalType());
        }
    }

    @Test
    void signalDatesMatchPriceDates() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(2, 30.0, 70.0));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 11.0),
                createPrice(3, 12.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(prices.get(0).getDate(), signals.get(0).getDate());
        assertEquals(prices.get(1).getDate(), signals.get(1).getDate());
        assertEquals(prices.get(2).getDate(), signals.get(2).getDate());
    }

    @Test
    void rejectsNullConfiguration() {
        assertThrows(NullPointerException.class,
                () -> new MomentumStrategy(null));
    }

    @Test
    void rejectsNullPriceList() {
        final MomentumStrategy strategy = new MomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));

        assertThrows(NullPointerException.class,
                () -> strategy.generateSignals(null));
    }

    private DailyPrice createPrice(int day, double close) {
        return new DailyPrice(
                LocalDate.of(2026, 1, day),
                close,
                close,
                close,
                close,
                1000L);
    }
}