package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Output of running one strategy against one ticker's history.
 * This is what Member 3's backtesting engine produces, and what
 * Member 4's comparison feature consumes - it's the seam between the two.
 */
public class BacktestResult implements Serializable {
    private final Ticker ticker;
    private final String strategyName;
    private final List<Trade> tradeLog;
    private final double finalCapital;
    private final double totalReturn;   // percent
    private final int numberOfTrades;
    private final double winRate;       // percent

    /** Portfolio value at every close, oldest first, one entry per trading day. */
    private final List<Double> equityCurve;

    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * @param ticker the ticker the run was against
     * @param strategyName the name of the strategy that was run
     * @param tradeLog the completed trades, oldest first
     * @param finalCapital the closing capital
     * @param totalReturn the total return, as a percentage
     * @param numberOfTrades how many trades the run produced
     * @param winRate the percentage of trades that made money
     * @param equityCurve the portfolio value at every close, oldest first, one entry per
     *                    trading day; its last entry is the final capital exactly
     * @param startDate the date of the first price in the run, or null when it is not known
     * @param endDate the date of the last price in the run, or null when it is not known
     */
    public BacktestResult(Ticker ticker, String strategyName, List<Trade> tradeLog,
                           double finalCapital, double totalReturn, int numberOfTrades, double winRate,
                           List<Double> equityCurve, LocalDate startDate, LocalDate endDate) {
        this.ticker = ticker;
        this.strategyName = strategyName;
        this.tradeLog = tradeLog;
        this.finalCapital = finalCapital;
        this.totalReturn = totalReturn;
        this.numberOfTrades = numberOfTrades;
        this.winRate = winRate;
        this.equityCurve = List.copyOf(equityCurve);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Ticker getTicker() { return ticker; }
    public String getStrategyName() { return strategyName; }
    public List<Trade> getTradeLog() { return tradeLog; }
    public double getFinalCapital() { return finalCapital; }
    public double getTotalReturn() { return totalReturn; }
    public int getNumberOfTrades() { return numberOfTrades; }
    public double getWinRate() { return winRate; }

    /**
     * @return the portfolio value at every close, oldest first. Never null; empty only for a
     *         result that was not produced by the engine.
     */
    public List<Double> getEquityCurve() { return equityCurve; }

    /**
     * @return the date of the first price in the run, or null when it is not known
     */
    public LocalDate getStartDate() { return startDate; }

    /**
     * @return the date of the last price in the run, or null when it is not known
     */
    public LocalDate getEndDate() { return endDate; }
}
