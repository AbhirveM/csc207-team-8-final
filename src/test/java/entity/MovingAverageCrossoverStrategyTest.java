package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingAverageCrossoverStrategyTest {

    @Test
    void generatesSignalsOnlyWhenAveragesCross() {
        final MovingAverageConfiguration configuration =
                new MovingAverageConfiguration(2, 3);

        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(configuration);

        final List<DailyPrice> prices = List.of(
                createPrice(1, 3.0),
                createPrice(2, 2.0),
                createPrice(3, 1.0),
                createPrice(4, 4.0),
                createPrice(5, 5.0),
                createPrice(6, 0.0)
        );

        final List<SignalType> expectedTypes = List.of(
                SignalType.HOLD,
                SignalType.HOLD,
                SignalType.HOLD,
                SignalType.BUY,
                SignalType.HOLD,
                SignalType.SELL
        );

        final List<TradingSignal> actualSignals =
                strategy.generateSignals(prices);

        assertEquals(expectedTypes.size(), actualSignals.size());

        for (int index = 0;
             index < expectedTypes.size();
             index++) {

            assertEquals(
                    prices.get(index).getDate(),
                    actualSignals.get(index).getDate(),
                    "Incorrect date at index " + index);

            assertEquals(
                    expectedTypes.get(index),
                    actualSignals.get(index).getSignalType(),
                    "Incorrect signal at index " + index);
        }
    }

    @Test
    void returnsStrategyName() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        assertEquals(
                "Moving Average Crossover",
                strategy.getName());
    }

    @Test
    void rejectsNullConfiguration() {
        assertThrows(
                NullPointerException.class,
                () -> new MovingAverageCrossoverStrategy(null));
    }

    @Test
    void rejectsNullPriceList() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        assertThrows(
                NullPointerException.class,
                () -> strategy.generateSignals(null));
    }

    @Test
    void rejectsNullPriceWithinList() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        final List<DailyPrice> prices = new ArrayList<>();
        prices.add(createPrice(1, 3.0));
        prices.add(createPrice(2, 2.0));
        prices.add(null);
        prices.add(createPrice(4, 4.0));

        assertThrows(
                NullPointerException.class,
                () -> strategy.generateSignals(prices));
    }

    @Test
    void rejectsInsufficientPriceHistory() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 3.0),
                createPrice(2, 2.0),
                createPrice(3, 1.0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.generateSignals(prices));
    }

    @Test
    void calculatesAveragesUsingClosingPrices() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        final List<DailyPrice> prices = List.of(
                createPriceWithConstantOpen(1, 3.0),
                createPriceWithConstantOpen(2, 2.0),
                createPriceWithConstantOpen(3, 1.0),
                createPriceWithConstantOpen(4, 4.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(
                SignalType.BUY,
                signals.get(3).getSignalType());
    }

    @Test
    void generatesBuyAfterEqualAveragesCrossUpward() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 2.0),
                createPrice(2, 1.0),
                createPrice(3, 3.0),
                createPrice(4, 5.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(
                SignalType.BUY,
                signals.get(3).getSignalType());
    }

    @Test
    void generatesSellAfterEqualAveragesCrossDownward() {
        final TradingStrategy strategy =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(2, 3));

        final List<DailyPrice> prices = List.of(
                createPrice(1, 2.0),
                createPrice(2, 3.0),
                createPrice(3, 1.0),
                createPrice(4, 0.0)
        );

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        assertEquals(
                SignalType.SELL,
                signals.get(3).getSignalType());
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

    private DailyPrice createPriceWithConstantOpen(
            int day, double close) {
        return new DailyPrice(
                LocalDate.of(2026, 2, day),
                10.0,
                100.0,
                0.0,
                close,
                1000L);
    }
}