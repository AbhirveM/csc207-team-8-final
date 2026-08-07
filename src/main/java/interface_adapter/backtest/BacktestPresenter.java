package interface_adapter.backtest;

import use_case.backtest.RunBacktestOutputBoundary;
import use_case.backtest.RunBacktestOutputData;

/**
 * Presenter for the run-backtest use case.
 */
public class BacktestPresenter
        implements RunBacktestOutputBoundary {

    private final BacktestViewModel viewModel;

    public BacktestPresenter(
            BacktestViewModel viewModel) {

        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(
            RunBacktestOutputData outputData) {

        viewModel.setResult(
                outputData.getBacktestResult());
    }

    @Override
    public void prepareFailView(
            String errorMessage) {

        viewModel.setError(errorMessage);
    }
}