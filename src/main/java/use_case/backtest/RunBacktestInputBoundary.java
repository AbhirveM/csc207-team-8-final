package use_case.backtest;

/**
 * Input boundary for running a backtest.
 */
public interface RunBacktestInputBoundary {

    void execute(RunBacktestInputData inputData);
}