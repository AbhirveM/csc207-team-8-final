package interface_adapter.backtest;

import use_case.backtest.RunBacktestInputBoundary;
import use_case.backtest.RunBacktestInputData;

import java.util.Objects;

/**
 * Controller for running a backtest.
 *
 * <p>Every parameter is text or a number. The screen names a ticker and a strategy; the
 * interactor resolves the symbol to its price history and builds the strategy. This class
 * imports no entity, and neither does the view that calls it.
 */
public class BacktestController {

    private final RunBacktestInputBoundary interactor;

    public BacktestController(
            RunBacktestInputBoundary interactor) {

        this.interactor = Objects.requireNonNull(
                interactor,
                "Interactor cannot be null");
    }

    /**
     * Runs the Moving Average Crossover strategy over a ticker's loaded price history.
     *
     * @param tickerSymbol the symbol to run against
     * @param shortWindow  the short moving-average window
     * @param longWindow   the long moving-average window
     */
    public void runMovingAverageBacktest(
            String tickerSymbol,
            int shortWindow,
            int longWindow) {

        interactor.execute(RunBacktestInputData.movingAverageCrossover(
                tickerSymbol, shortWindow, longWindow));
    }

    /**
     * Runs the RSI Momentum strategy over a ticker's loaded price history.
     *
     * @param tickerSymbol        the symbol to run against
     * @param period              the RSI period
     * @param oversoldThreshold   the RSI value at or below which the strategy buys
     * @param overboughtThreshold the RSI value at or above which the strategy sells
     */
    public void runMomentumBacktest(
            String tickerSymbol,
            int period,
            double oversoldThreshold,
            double overboughtThreshold) {

        interactor.execute(RunBacktestInputData.rsiMomentum(
                tickerSymbol, period, oversoldThreshold, overboughtThreshold));
    }

    /**
     * Asks which tickers have price history a backtest could run against. The answer
     * arrives on the view model, not as a return value.
     */
    public void loadAvailableTickers() {
        interactor.loadAvailableTickers();
    }
}
