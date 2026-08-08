package use_case.backtest;

import entity.BacktestEngine;
import entity.BacktestResult;

import java.util.Objects;

/**
 * Runs a backtest and sends its result to the presenter.
 */
public final class RunBacktestInteractor
        implements RunBacktestInputBoundary {

    private final BacktestEngine backtestEngine;
    private final RunBacktestOutputBoundary presenter;

    public RunBacktestInteractor(
            BacktestEngine backtestEngine,
            RunBacktestOutputBoundary presenter) {

        this.backtestEngine =
                Objects.requireNonNull(
                        backtestEngine,
                        "Backtest engine cannot be null");

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

        try {
            final BacktestResult result =
                    backtestEngine.run(
                            inputData.getTicker(),
                            inputData.getStrategy(),
                            inputData.getPrices());

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
}
