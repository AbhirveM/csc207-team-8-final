package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates trading signals using a Moving Average Crossover strategy.
 *
 * Prices are expected to be ordered from oldest to newest. The strategy
 * calculates moving averages using closing prices.
 */
public class MovingAverageCrossoverStrategy
        implements TradingStrategy {

    private static final String STRATEGY_NAME =
            "Moving Average Crossover";

    private final MovingAverageConfiguration configuration;

    /**
     * Creates a Moving Average Crossover strategy.
     *
     * @param configuration the short- and long-window configuration
     */
    public MovingAverageCrossoverStrategy(
            MovingAverageConfiguration configuration) {
        this.configuration = Objects.requireNonNull(
                configuration,
                "Configuration cannot be null");
    }

    @Override
    public String getName() {
        return STRATEGY_NAME;
    }

    @Override
    public List<TradingSignal> generateSignals(
            List<DailyPrice> prices) {

        Objects.requireNonNull(prices, "Prices cannot be null");

        for (DailyPrice price : prices) {
            Objects.requireNonNull(
                    price,
                    "Prices cannot contain null values");
        }

        final int shortWindow = configuration.getShortWindow();
        final int longWindow = configuration.getLongWindow();

        /*
         * Detecting a crossover requires the moving averages for both
         * the current date and the previous date.
         */
        if (prices.size() < longWindow + 1) {
            throw new IllegalArgumentException(
                    "Not enough price history to calculate a crossover");
        }

        final List<TradingSignal> signals =
                new ArrayList<>(prices.size());

        /*
         * No crossover can be calculated during these early dates,
         * so the strategy returns HOLD.
         */
        for (int index = 0; index < longWindow; index++) {
            signals.add(new TradingSignal(
                    prices.get(index).getDate(),
                    SignalType.HOLD));
        }

        for (int index = longWindow;
             index < prices.size();
             index++) {

            final double previousShortAverage =
                    calculateAverage(
                            prices,
                            index - shortWindow,
                            index);

            final double previousLongAverage =
                    calculateAverage(
                            prices,
                            index - longWindow,
                            index);

            final double currentShortAverage =
                    calculateAverage(
                            prices,
                            index - shortWindow + 1,
                            index + 1);

            final double currentLongAverage =
                    calculateAverage(
                            prices,
                            index - longWindow + 1,
                            index + 1);

            final SignalType signalType;

            if (previousShortAverage <= previousLongAverage
                    && currentShortAverage > currentLongAverage) {
                signalType = SignalType.BUY;
            }
            else if (previousShortAverage >= previousLongAverage
                    && currentShortAverage < currentLongAverage) {
                signalType = SignalType.SELL;
            }
            else {
                signalType = SignalType.HOLD;
            }

            signals.add(new TradingSignal(
                    prices.get(index).getDate(),
                    signalType));
        }

        return signals;
    }

    /**
     * Calculates the average closing price over the specified range.
     *
     * @param prices the historical prices
     * @param startInclusive first index included in the average
     * @param endExclusive first index not included in the average
     * @return the average closing price
     */
    private double calculateAverage(
            List<DailyPrice> prices,
            int startInclusive,
            int endExclusive) {

        double total = 0.0;

        for (int index = startInclusive;
             index < endExclusive;
             index++) {
            total += prices.get(index).getClose();
        }

        return total / (endExclusive - startInclusive);
    }
}