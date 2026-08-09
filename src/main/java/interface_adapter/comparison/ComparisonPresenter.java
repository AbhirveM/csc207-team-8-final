package interface_adapter.comparison;

import entity.BacktestResult;
import use_case.comparison.CompareStrategies;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a completed comparison into display-ready rows.
 *
 * <p>All formatting lives here rather than in the view, so the wording and the number of decimal
 * places are decided in one place and {@code ComparisonView} only copies strings into widgets.
 */
public class ComparisonPresenter implements CompareStrategies.OutputBoundary {

    private static final String PERCENT_FORMAT = "%.2f";

    private final ComparisonViewModel viewModel;

    public ComparisonPresenter(ComparisonViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void presentComparison(CompareStrategies.ComparisonOutputData outputData) {
        final List<ComparisonViewModel.ResultRow> rows = new ArrayList<>();
        for (final BacktestResult result : outputData.resultsRankedByReturn) {
            rows.add(new ComparisonViewModel.ResultRow(
                    result.getTicker().getSymbol(),
                    result.getStrategyName(),
                    String.format(PERCENT_FORMAT, result.getTotalReturn()),
                    String.valueOf(result.getNumberOfTrades()),
                    String.format(PERCENT_FORMAT, result.getWinRate()),
                    result.getTotalReturn(),
                    result.getWinRate()));
        }
        viewModel.setResults(rows, outputData.best.getStrategyName());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }
}
