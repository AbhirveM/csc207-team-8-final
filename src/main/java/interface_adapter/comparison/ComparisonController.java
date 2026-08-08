package interface_adapter.comparison;

import use_case.comparison.CompareStrategies;

/**
 * The boundary the Compare Strategies screen calls.
 *
 * <p>The controller reads the completed backtests from {@link CompletedBacktestStore} itself, so
 * the view can trigger a comparison without ever naming an entity type.
 */
public class ComparisonController {

    private final CompareStrategies.InputBoundary interactor;
    private final CompletedBacktestStore completedBacktests;

    public ComparisonController(CompareStrategies.InputBoundary interactor,
                                CompletedBacktestStore completedBacktests) {
        this.interactor = interactor;
        this.completedBacktests = completedBacktests;
    }

    /** Compares every backtest completed so far. */
    public void compare() {
        interactor.execute(completedBacktests.getCompletedResults());
    }
}
