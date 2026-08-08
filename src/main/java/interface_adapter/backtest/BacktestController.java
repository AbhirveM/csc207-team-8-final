package interface_adapter.backtest;

import entity.DailyPrice;
import entity.Ticker;
import entity.TradingStrategy;
import use_case.backtest.RunBacktestInputBoundary;
import use_case.backtest.RunBacktestInputData;

import java.util.List;

/**
 * Controller for running a backtest.
 */
public class BacktestController {

    private final RunBacktestInputBoundary interactor;

    public BacktestController(
            RunBacktestInputBoundary interactor) {

        this.interactor = interactor;
    }

    public void runBacktest(
            Ticker ticker,
            TradingStrategy strategy,
            List<DailyPrice> prices) {

        final RunBacktestInputData inputData =
                new RunBacktestInputData(
                        ticker,
                        strategy,
                        prices);

        interactor.execute(inputData);
    }
}
