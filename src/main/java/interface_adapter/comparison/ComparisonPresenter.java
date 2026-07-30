package interface_adapter.comparison;

import use_case.comparison.CompareStrategies;

public class ComparisonPresenter implements CompareStrategies.OutputBoundary {
    private final ComparisonViewModel viewModel;

    public ComparisonPresenter(ComparisonViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void presentComparison(CompareStrategies.ComparisonOutputData outputData) {
        viewModel.setResults(outputData.resultsRankedByReturn, outputData.best.getStrategyName());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }
}
