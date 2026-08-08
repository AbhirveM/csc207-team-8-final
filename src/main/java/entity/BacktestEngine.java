package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs a trading strategy against historical price data.
 */
public class BacktestEngine {

    public static final double INITIAL_CAPITAL = 10000.0;

    /**
     * Runs one strategy against one ticker's historical prices.
     *
     * @param ticker the ticker being backtested
     * @param strategy the trading strategy
     * @param prices historical prices ordered oldest to newest
     * @return the completed backtest result
     */
    public BacktestResult run(
            Ticker ticker,
            TradingStrategy strategy,
            List<DailyPrice> prices) {

        Objects.requireNonNull(
                ticker,
                "Ticker cannot be null");

        Objects.requireNonNull(
                strategy,
                "Strategy cannot be null");

        Objects.requireNonNull(
                prices,
                "Prices cannot be null");

        if (prices.isEmpty()) {
            throw new IllegalArgumentException(
                    "Price history cannot be empty");
        }

        validatePrices(prices);

        final List<TradingSignal> signals =
                strategy.generateSignals(prices);

        if (signals == null
                || signals.size() != prices.size()) {
            throw new IllegalStateException(
                    "Strategy must produce one signal per price");
        }

        double cash = INITIAL_CAPITAL;

        int shares = 0;

        LocalDate entryDate = null;
        double entryPrice = 0.0;

        final List<Trade> tradeLog =
                new ArrayList<>();

        /*
         * A signal produced on day i is executed using
         * the opening price on day i + 1.
         *
         * Therefore, the final day's signal cannot be executed.
         */
        for (int index = 0;
                index < signals.size() - 1;
                index++) {

            final TradingSignal signal =
                    Objects.requireNonNull(
                            signals.get(index),
                            "Signal entries cannot be null");

            final DailyPrice executionDay =
                    prices.get(index + 1);

            final double executionPrice =
                    executionDay.getOpen();

            if (executionPrice <= 0.0) {
                throw new IllegalArgumentException(
                        "Execution price must be positive");
            }

            if (signal.getSignalType() == SignalType.BUY
                    && shares == 0) {

                final int quantity =
                        (int) (cash / executionPrice);

                if (quantity > 0) {
                    shares = quantity;

                    cash -= shares * executionPrice;

                    entryDate = executionDay.getDate();
                    entryPrice = executionPrice;
                }
            }
            else if (signal.getSignalType() == SignalType.SELL
                    && shares > 0) {

                cash += shares * executionPrice;

                tradeLog.add(
                        new Trade(
                                ticker,
                                entryDate,
                                entryPrice,
                                executionDay.getDate(),
                                executionPrice));

                shares = 0;
                entryDate = null;
                entryPrice = 0.0;
            }
        }

        /*
         * If a position is still open when the data ends,
         * liquidate it using the final day's closing price.
         */
        if (shares > 0) {
            final DailyPrice finalDay =
                    prices.get(prices.size() - 1);

            final double exitPrice =
                    finalDay.getClose();

            if (exitPrice <= 0.0) {
                throw new IllegalArgumentException(
                        "Final closing price must be positive");
            }

            cash += shares * exitPrice;

            tradeLog.add(
                    new Trade(
                            ticker,
                            entryDate,
                            entryPrice,
                            finalDay.getDate(),
                            exitPrice));
        }

        final double finalCapital = cash;

        final double totalReturn =
                (finalCapital - INITIAL_CAPITAL)
                        / INITIAL_CAPITAL
                        * 100.0;

        final int numberOfTrades =
                tradeLog.size();

        final double winRate =
                calculateWinRate(tradeLog);

        return new BacktestResult(
                ticker,
                strategy.getName(),
                tradeLog,
                finalCapital,
                totalReturn,
                numberOfTrades,
                winRate);
    }

    /**
     * Calculates the percentage of completed trades
     * that made a positive return.
     *
     * @param tradeLog completed trades
     * @return win rate as a percentage
     */
    private double calculateWinRate(
            List<Trade> tradeLog) {

        if (tradeLog.isEmpty()) {
            return 0.0;
        }

        int winningTrades = 0;

        for (Trade trade : tradeLog) {
            if (trade.getReturnPercent() > 0.0) {
                winningTrades++;
            }
        }

        return (double) winningTrades
                / tradeLog.size()
                * 100.0;
    }

    /**
     * Validates historical price data.
     *
     * @param prices historical prices
     */
    private void validatePrices(
            List<DailyPrice> prices) {

        for (int index = 0;
                index < prices.size();
                index++) {

            final DailyPrice price =
                    Objects.requireNonNull(
                            prices.get(index),
                            "Price entries cannot be null");

            if (price.getOpen() <= 0.0
                    || price.getClose() <= 0.0) {
                throw new IllegalArgumentException(
                        "Opening and closing prices must be positive");
            }

            if (index > 0
                    && !price.getDate().isAfter(
                    prices.get(index - 1).getDate())) {

                throw new IllegalArgumentException(
                        "Prices must be ordered oldest to newest");
            }
        }
    }
}
