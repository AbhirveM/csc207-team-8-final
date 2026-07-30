package entity;

import java.io.Serializable;
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

    public BacktestResult(Ticker ticker, String strategyName, List<Trade> tradeLog,
                           double finalCapital, double totalReturn, int numberOfTrades, double winRate) {
        this.ticker = ticker;
        this.strategyName = strategyName;
        this.tradeLog = tradeLog;
        this.finalCapital = finalCapital;
        this.totalReturn = totalReturn;
        this.numberOfTrades = numberOfTrades;
        this.winRate = winRate;
    }

    public Ticker getTicker() { return ticker; }
    public String getStrategyName() { return strategyName; }
    public List<Trade> getTradeLog() { return tradeLog; }
    public double getFinalCapital() { return finalCapital; }
    public double getTotalReturn() { return totalReturn; }
    public int getNumberOfTrades() { return numberOfTrades; }
    public double getWinRate() { return winRate; }
}
