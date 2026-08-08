package use_case.backtest;

import entity.BacktestResult;

import java.util.Objects;

/**
 * Output data produced by a successful backtest.
 */
public final class RunBacktestOutputData {

    private final BacktestResult backtestResult;

    public RunBacktestOutputData(
            BacktestResult backtestResult) {

        this.backtestResult =
                Objects.requireNonNull(
                        backtestResult,
                        "Backtest result cannot be null");
    }

    public BacktestResult getBacktestResult() {
        return backtestResult;
    }
}
