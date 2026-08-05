package entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSIMomentumStrategyTest {

    @Test
    void returnsCorrectName() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        assertEquals("RSI Momentum Strategy", strategy.getName());
    }

    @Test
    void knownPricesProduceExpectedRsi() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 12.0),
                createPrice(3, 11.0),
                createPrice(4, 13.0)
        );

        final double rsi = strategy.calculateRsi(prices, 3, 3);

        assertEquals(80.0, rsi, 0.0001);
    }

    @Test
    void increasingPricesGenerateSellSignals() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 11.0),
                createPrice(3, 12.0),
                createPrice(4, 13.0),
                createPrice(5, 14.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(SignalType.HOLD,
                signals.get(0).getSignalType());
        assertEquals(SignalType.HOLD,
                signals.get(1).getSignalType());
        assertEquals(SignalType.HOLD,
                signals.get(2).getSignalType());
        assertEquals(SignalType.SELL,
                signals.get(3).getSignalType());
        assertEquals(SignalType.SELL,
                signals.get(4).getSignalType());
    }

    @Test
    void decreasingPricesGenerateBuySignals() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 14.0),
                createPrice(2, 13.0),
                createPrice(3, 12.0),
                createPrice(4, 11.0),
                createPrice(5, 10.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(SignalType.BUY,
                signals.get(3).getSignalType());
        assertEquals(SignalType.BUY,
                signals.get(4).getSignalType());
    }

    @Test
    void unchangedPricesGenerateHoldSignals() {
        final RSIMomentumStrategy strategy =
                createStrategy();

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
            assertEquals(
                    SignalType.HOLD,
                    signal.getSignalType());
        }
    }

    @Test
    void rejectsInsufficientPriceHistory() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 11.0),
                createPrice(3, 12.0)
        );

        assertThrows(IllegalArgumentException.class,
                () -> strategy.generateSignals(prices));
    }

    @Test
    void signalDatesMatchPriceDates() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 10.0),
                createPrice(2, 11.0),
                createPrice(3, 12.0),
                createPrice(4, 13.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        for (int index = 0; index < prices.size(); index++) {
            assertEquals(
                    prices.get(index).getDate(),
                    signals.get(index).getDate());
        }
    }

    @Test
    void rejectsNullConfiguration() {
        assertThrows(NullPointerException.class,
                () -> new RSIMomentumStrategy(null));
    }

    @Test
    void rejectsNullPriceList() {
        final RSIMomentumStrategy strategy =
                createStrategy();

        assertThrows(NullPointerException.class,
                () -> strategy.generateSignals(null));
    }

    private RSIMomentumStrategy createStrategy() {
        return new RSIMomentumStrategy(
                new MomentumConfiguration(3, 30.0, 70.0));
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