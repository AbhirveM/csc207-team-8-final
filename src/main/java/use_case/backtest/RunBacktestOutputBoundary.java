package use_case.backtest;

/**
 * Describes how the run-backtest use case reports its result.
 */
public interface RunBacktestOutputBoundary {

    void prepareSuccessView(
            RunBacktestOutputData outputData);

    void prepareFailView(
            String errorMessage);
}