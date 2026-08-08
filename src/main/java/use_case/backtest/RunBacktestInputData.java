package use_case.backtest;

import entity.DailyPrice;
import entity.Ticker;
import entity.TradingStrategy;

import java.util.List;

/**
 * Input data required to run a backtest.
 */
public final class RunBacktestInputData {

    private final Ticker ticker;
    private final TradingStrategy strategy;
    private final List<DailyPrice> prices;

    public RunBacktestInputData(
            Ticker ticker,
            TradingStrategy strategy,
            List<DailyPrice> prices) {

        this.ticker = ticker;
        this.strategy = strategy;
        this.prices = prices;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public TradingStrategy getStrategy() {
        return strategy;
    }

    public List<DailyPrice> getPrices() {
        return prices;
    }
}
