package use_case.backtest;

import entity.BacktestEngine;
import entity.BacktestResult;
import entity.MomentumConfiguration;
import entity.MovingAverageConfiguration;
import entity.MovingAverageCrossoverStrategy;
import entity.RSIMomentumStrategy;
import entity.Stock;
import entity.TradingStrategy;
import use_case.watchlist.StockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs a backtest and sends its result to the presenter.
 *
 * <p>Three steps that used to be spread across the backtest screen live here: resolving a
 * ticker symbol to its loaded price history, building the strategy the caller named, and
 * running the engine over the two. A caller supplies text and numbers only, which is what
 * keeps {@code entity} out of the view and the controller.
 */
public final class RunBacktestInteractor
        implements RunBacktestInputBoundary {

    private final BacktestEngine backtestEngine;
    private final StockRepository stockRepository;
    private final RunBacktestOutputBoundary presenter;

    public RunBacktestInteractor(
            BacktestEngine backtestEngine,
            StockRepository stockRepository,
            RunBacktestOutputBoundary presenter) {

        this.backtestEngine =
                Objects.requireNonNull(
                        backtestEngine,
                        "Backtest engine cannot be null");

        this.stockRepository =
                Objects.requireNonNull(
                        stockRepository,
                        "Stock repository cannot be null");

        this.presenter =
                Objects.requireNonNull(
                        presenter,
                        "Presenter cannot be null");
    }

    @Override
    public void execute(
            RunBacktestInputData inputData) {

        if (inputData == null) {
            presenter.prepareFailView(
                    "Backtest input cannot be null");
            return;
        }

        final Optional<Stock> stock =
                stockRepository.findBySymbol(
                        inputData.getTickerSymbol());

        if (stock.isEmpty()) {
            presenter.prepareFailView(
                    "No loaded prices for "
                            + inputData.getTickerSymbol()
                            + ". Add it on the Watchlist screen and click \"Load prices\" "
                            + "first.");
            return;
        }

        try {
            final BacktestResult result =
                    backtestEngine.run(
                            stock.get().getTicker(),
                            strategyFor(inputData),
                            stock.get().getDailyPrices());

            presenter.prepareSuccessView(
                    new RunBacktestOutputData(result));
        }
        catch (IllegalArgumentException
               | NullPointerException
               | IllegalStateException exception) {

            presenter.prepareFailView(
                    exception.getMessage());
        }
    }

    @Override
    public void loadAvailableTickers() {
        final List<String> symbols = new ArrayList<>();
        for (final Stock stock : stockRepository.findAll()) {
            symbols.add(stock.getTicker().getSymbol());
        }
        presenter.presentAvailableTickers(symbols);
    }

    /**
     * Builds the strategy the input data names, from the parameters it carries.
     *
     * <p>The configuration entities validate their own numbers, so an out-of-range window
     * or threshold throws here and is caught by {@link #execute} alongside the engine's own
     * complaints - one failure path, worded once.
     *
     * @param inputData the run being prepared
     * @return the strategy to run
     * @throws IllegalArgumentException if the parameters are outside the strategy's bounds
     */
    private static TradingStrategy strategyFor(RunBacktestInputData inputData) {
        final TradingStrategy strategy;
        if (inputData.getStrategy() == RunBacktestInputData.Strategy.RSI_MOMENTUM) {
            strategy = new RSIMomentumStrategy(new MomentumConfiguration(
                    inputData.getMomentumPeriod(),
                    inputData.getMomentumOversoldThreshold(),
                    inputData.getMomentumOverboughtThreshold()));
        }
        else {
            strategy = new MovingAverageCrossoverStrategy(new MovingAverageConfiguration(
                    inputData.getMovingAverageShortWindow(),
                    inputData.getMovingAverageLongWindow()));
        }
        return strategy;
    }
}
