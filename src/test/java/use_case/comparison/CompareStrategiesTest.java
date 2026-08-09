package use_case.comparison;

import entity.BacktestResult;
import entity.Ticker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompareStrategiesTest {

    private BacktestResult makeResult(String symbol, String strategyName, double totalReturn) {
        Ticker ticker = new Ticker(symbol, symbol + " Inc.");
        return new BacktestResult(
                ticker,
                strategyName,
                Collections.emptyList(),
                10000 * (1 + totalReturn / 100.0),
                totalReturn,
                3,
                60.0,
                Collections.emptyList(),
                null,
                null
        );
    }

    /** Capturing presenter so tests can inspect what the interactor produced,
     *  without needing a real Swing view model. */
    private static class RecordingPresenter implements CompareStrategies.OutputBoundary {
        CompareStrategies.ComparisonOutputData lastSuccess;
        String lastError;

        @Override
        public void presentComparison(CompareStrategies.ComparisonOutputData outputData) {
            this.lastSuccess = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.lastError = errorMessage;
        }
    }

    @Test
    void emptyResultsListPresentsFailView() {
        RecordingPresenter presenter = new RecordingPresenter();
        CompareStrategies.Interactor interactor = new CompareStrategies.Interactor(presenter);

        interactor.execute(new ArrayList<>());

        assertNull(presenter.lastSuccess);
        assertNotNull(presenter.lastError);
    }

    @Test
    void nullResultsListPresentsFailView() {
        RecordingPresenter presenter = new RecordingPresenter();
        CompareStrategies.Interactor interactor = new CompareStrategies.Interactor(presenter);

        interactor.execute(null);

        assertNull(presenter.lastSuccess);
        assertNotNull(presenter.lastError);
    }

    @Test
    void singleResultIsItsOwnBest() {
        RecordingPresenter presenter = new RecordingPresenter();
        CompareStrategies.Interactor interactor = new CompareStrategies.Interactor(presenter);

        BacktestResult onlyResult = makeResult("AAPL", "MovingAverageCrossover", 12.5);
        interactor.execute(List.of(onlyResult));

        assertNotNull(presenter.lastSuccess);
        assertNull(presenter.lastError);
        assertEquals(onlyResult, presenter.lastSuccess.best);
        assertEquals(1, presenter.lastSuccess.resultsRankedByReturn.size());
    }

    @Test
    void multipleResultsAreRankedHighestReturnFirst() {
        RecordingPresenter presenter = new RecordingPresenter();
        CompareStrategies.Interactor interactor = new CompareStrategies.Interactor(presenter);

        BacktestResult worse = makeResult("TSLA", "RSIMomentum", 4.0);
        BacktestResult better = makeResult("TSLA", "MovingAverageCrossover", 18.0);
        BacktestResult middle = makeResult("TSLA", "BuyAndHold", 9.0);

        interactor.execute(List.of(worse, better, middle));

        assertNull(presenter.lastError);
        List<BacktestResult> ranked = presenter.lastSuccess.resultsRankedByReturn;
        assertEquals(3, ranked.size());
        assertEquals(better, ranked.get(0));
        assertEquals(middle, ranked.get(1));
        assertEquals(worse, ranked.get(2));
        assertEquals(better, presenter.lastSuccess.best);
        assertEquals("MovingAverageCrossover", presenter.lastSuccess.best.getStrategyName());
    }

    @Test
    void tiedReturnsStillProduceAValidBest() {
        RecordingPresenter presenter = new RecordingPresenter();
        CompareStrategies.Interactor interactor = new CompareStrategies.Interactor(presenter);

        BacktestResult tiedA = makeResult("NVDA", "MovingAverageCrossover", 10.0);
        BacktestResult tiedB = makeResult("NVDA", "RSIMomentum", 10.0);

        interactor.execute(List.of(tiedA, tiedB));

        assertNotNull(presenter.lastSuccess);
        assertNull(presenter.lastError);
        List<BacktestResult> ranked = presenter.lastSuccess.resultsRankedByReturn;
        assertEquals(2, ranked.size());
        // The sort is stable and reversing the comparator keeps ties comparing
        // equal, so tied results must come back in the order they went in.
        assertEquals(tiedA, ranked.get(0));
        assertEquals(tiedB, ranked.get(1));
        assertEquals(tiedA, presenter.lastSuccess.best);
        assertEquals(10.0, presenter.lastSuccess.best.getTotalReturn());
    }
}
