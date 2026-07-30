package interface_adapter.comparison;

import entity.BacktestResult;
import use_case.comparison.CompareStrategies;

import java.util.List;

public class ComparisonController {
    private final CompareStrategies.InputBoundary interactor;

    public ComparisonController(CompareStrategies.InputBoundary interactor) {
        this.interactor = interactor;
    }

    public void compare(List<BacktestResult> completedResults) {
        interactor.execute(completedResults);
    }
}
