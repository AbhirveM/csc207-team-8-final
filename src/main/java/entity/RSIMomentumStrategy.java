package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates trading signals using the Relative Strength Index.
 */
public class RSIMomentumStrategy implements TradingStrategy {

    private final MomentumConfiguration configuration;

    public RSIMomentumStrategy(MomentumConfiguration configuration) {
        this.configuration = Objects.requireNonNull(
                configuration,
                "Momentum configuration cannot be null");
    }

    @Override
    public String getName() {
        return "RSI Momentum Strategy";
    }

    /**
     * Returns the configuration this strategy runs with.
     *
     * @return the momentum configuration
     */
    public MomentumConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public List<TradingSignal> generateSignals(List<DailyPrice> prices) {
        Objects.requireNonNull(prices, "Prices cannot be null");

        for (DailyPrice price : prices) {
            Objects.requireNonNull(
                    price,
                    "Price entries cannot be null");
        }

        final int period = configuration.getPeriod();

        if (prices.size() < period + 1) {
            throw new IllegalArgumentException(
                    "Insufficient price history for RSI calculation");
        }

        final List<TradingSignal> signals = new ArrayList<>();

        for (int index = 0; index < prices.size(); index++) {
            final DailyPrice currentPrice = prices.get(index);
            final SignalType signalType;

            if (index < period) {
                signalType = SignalType.HOLD;
            }
            else {
                final double rsi = calculateRsi(prices, index, period);
                signalType = determineSignalType(rsi);
            }

            signals.add(new TradingSignal(
                    currentPrice.getDate(),
                    signalType));
        }

        return signals;
    }

    double calculateRsi(List<DailyPrice> prices,
                        int endIndex,
                        int period) {
        double totalGain = 0.0;
        double totalLoss = 0.0;

        final int startIndex = endIndex - period + 1;

        for (int index = startIndex; index <= endIndex; index++) {
            final double change =
                    prices.get(index).getClose()
                            - prices.get(index - 1).getClose();

            if (change > 0) {
                totalGain += change;
            }
            else if (change < 0) {
                totalLoss += -change;
            }
        }

        final double averageGain = totalGain / period;
        final double averageLoss = totalLoss / period;

        if (averageGain == 0.0 && averageLoss == 0.0) {
            return 50.0;
        }

        if (averageLoss == 0.0) {
            return 100.0;
        }

        if (averageGain == 0.0) {
            return 0.0;
        }

        final double relativeStrength = averageGain / averageLoss;

        return 100.0 - 100.0 / (1.0 + relativeStrength);
    }

    private SignalType determineSignalType(double rsi) {
        if (rsi <= configuration.getOversoldThreshold()) {
            return SignalType.BUY;
        }

        if (rsi >= configuration.getOverboughtThreshold()) {
            return SignalType.SELL;
        }

        return SignalType.HOLD;
    }
}
