package use_case.backtest;

import java.util.Objects;

/**
 * What a caller must supply to run a backtest: which ticker, which strategy, and the
 * numbers that strategy takes.
 *
 * <p>Every field is a {@code String}, an {@code int} or a {@code double}. The ticker is
 * named by symbol rather than carried as a {@code Ticker}, and the strategy by
 * {@link Strategy} plus its parameters rather than as an already-built
 * {@code TradingStrategy}. Resolving that symbol against the price repository and
 * constructing the strategy are use-case work, so both happen behind the input boundary.
 *
 * <p>That is what lets a screen ask for a run without importing {@code entity}: before
 * this shape, the backtest screen read a {@code Stock} out of the repository and built a
 * strategy itself, which is a jump from Frameworks &amp; Drivers straight past the adapter
 * layer into Entities.
 *
 * <p>The parameters are <em>not</em> validated here. Each strategy's bounds are an
 * enterprise rule owned by its configuration entity, so those constructors reject bad
 * numbers and the failure is worded through the output boundary.
 */
public final class RunBacktestInputData {

    /** The strategies a run can be asked for. */
    public enum Strategy {

        /** Buy and sell on a short/long moving average crossover. */
        MOVING_AVERAGE_CROSSOVER,

        /** Buy and sell on RSI crossing its oversold and overbought thresholds. */
        RSI_MOMENTUM
    }

    private final String tickerSymbol;
    private final Strategy strategy;
    private final int movingAverageShortWindow;
    private final int movingAverageLongWindow;
    private final int momentumPeriod;
    private final double momentumOversoldThreshold;
    private final double momentumOverboughtThreshold;

    private RunBacktestInputData(String tickerSymbol,
                                 Strategy strategy,
                                 int movingAverageShortWindow,
                                 int movingAverageLongWindow,
                                 int momentumPeriod,
                                 double momentumOversoldThreshold,
                                 double momentumOverboughtThreshold) {

        this.tickerSymbol = tickerSymbol;
        this.strategy = strategy;
        this.movingAverageShortWindow = movingAverageShortWindow;
        this.movingAverageLongWindow = movingAverageLongWindow;
        this.momentumPeriod = momentumPeriod;
        this.momentumOversoldThreshold = momentumOversoldThreshold;
        this.momentumOverboughtThreshold = momentumOverboughtThreshold;
    }

    /**
     * A run of the Moving Average Crossover strategy.
     *
     * @param tickerSymbol the symbol to run against; must be non-null
     * @param shortWindow  the short moving-average window
     * @param longWindow   the long moving-average window
     * @return the input data
     * @throws NullPointerException if {@code tickerSymbol} is null
     */
    public static RunBacktestInputData movingAverageCrossover(String tickerSymbol,
                                                              int shortWindow,
                                                              int longWindow) {
        return new RunBacktestInputData(
                Objects.requireNonNull(tickerSymbol, "Ticker symbol cannot be null"),
                Strategy.MOVING_AVERAGE_CROSSOVER,
                shortWindow, longWindow, 0, 0.0, 0.0);
    }

    /**
     * A run of the RSI Momentum strategy.
     *
     * @param tickerSymbol        the symbol to run against; must be non-null
     * @param period              the RSI period
     * @param oversoldThreshold   the RSI value at or below which the strategy buys
     * @param overboughtThreshold the RSI value at or above which the strategy sells
     * @return the input data
     * @throws NullPointerException if {@code tickerSymbol} is null
     */
    public static RunBacktestInputData rsiMomentum(String tickerSymbol,
                                                   int period,
                                                   double oversoldThreshold,
                                                   double overboughtThreshold) {
        return new RunBacktestInputData(
                Objects.requireNonNull(tickerSymbol, "Ticker symbol cannot be null"),
                Strategy.RSI_MOMENTUM,
                0, 0, period, oversoldThreshold, overboughtThreshold);
    }

    public String getTickerSymbol() {
        return tickerSymbol;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public int getMovingAverageShortWindow() {
        return movingAverageShortWindow;
    }

    public int getMovingAverageLongWindow() {
        return movingAverageLongWindow;
    }

    public int getMomentumPeriod() {
        return momentumPeriod;
    }

    public double getMomentumOversoldThreshold() {
        return momentumOversoldThreshold;
    }

    public double getMomentumOverboughtThreshold() {
        return momentumOverboughtThreshold;
    }
}
