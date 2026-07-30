package use_case.comparison;

import entity.BacktestResult;

import java.util.Comparator;
import java.util.List;

public class CompareStrategies {

    /** Plain data holder passed to the presenter - not a Swing object. */
    public static class ComparisonOutputData {
        public final List<BacktestResult> resultsRankedByReturn; // best first
        public final BacktestResult best;

        public ComparisonOutputData(List<BacktestResult> resultsRankedByReturn, BacktestResult best) {
            this.resultsRankedByReturn = resultsRankedByReturn;
            this.best = best;
        }
    }

    public interface InputBoundary {
        void execute(List<BacktestResult> results);
    }

    public interface OutputBoundary {
        void presentComparison(ComparisonOutputData outputData);
        void prepareFailView(String errorMessage);
    }

    public static class Interactor implements InputBoundary {
        private final OutputBoundary presenter;

        public Interactor(OutputBoundary presenter) {
            this.presenter = presenter;
        }

        @Override
        public void execute(List<BacktestResult> results) {
            if (results == null || results.isEmpty()) {
                presenter.prepareFailView("Run at least one backtest before comparing strategies.");
                return;
            }

            List<BacktestResult> ranked = results.stream()
                    .sorted(Comparator.comparingDouble(BacktestResult::getTotalReturn).reversed())
                    .toList();

            presenter.presentComparison(new ComparisonOutputData(ranked, ranked.get(0)));
        }
    }
}
