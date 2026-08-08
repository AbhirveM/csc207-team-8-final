package use_case.backtest;

import entity.BacktestEngine;
import entity.BacktestResult;
import entity.DailyPrice;
import entity.SignalType;
import entity.Ticker;
import entity.TradingSignal;
import entity.TradingStrategy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunBacktestInteractorTest {

    private static final double DELTA = 0.0001;

    @Test
    void successfulBacktestPreparesSuccessView() {
        final TestPresenter presenter = new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        presenter);

        final Ticker ticker =
                new Ticker(
                        "TEST",
                        "Test Company");

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 50.0, 50.0),
                createPrice(3, 60.0, 60.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final RunBacktestInputData inputData =
                new RunBacktestInputData(
                        ticker,
                        strategy,
                        prices);

        interactor.execute(inputData);

        assertNotNull(presenter.successData);
        assertNull(presenter.errorMessage);

        final BacktestResult result =
                presenter.successData.getBacktestResult();

        assertEquals(
                ticker,
                result.getTicker());

        assertEquals(
                "Fixed Test Strategy",
                result.getStrategyName());

        assertEquals(
                12000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                20.0,
                result.getTotalReturn(),
                DELTA);

        assertEquals(
                1,
                result.getNumberOfTrades());

        assertEquals(
                100.0,
                result.getWinRate(),
                DELTA);
    }

    @Test
    void nullInputPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        presenter);

        interactor.execute(null);

        assertNull(presenter.successData);

        assertEquals(
                "Backtest input cannot be null",
                presenter.errorMessage);
    }

    @Test
    void emptyPriceHistoryPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        presenter);

        final RunBacktestInputData inputData =
                new RunBacktestInputData(
                        new Ticker(
                                "TEST",
                                "Test Company"),
                        new FixedSignalStrategy(),
                        List.of());

        interactor.execute(inputData);

        assertNull(presenter.successData);

        assertEquals(
                "Price history cannot be empty",
                presenter.errorMessage);
    }

    @Test
    void nullTickerPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        presenter);

        final List<DailyPrice> prices =
                List.of(
                        createPrice(
                                1,
                                100.0,
                                100.0));

        final RunBacktestInputData inputData =
                new RunBacktestInputData(
                        null,
                        new FixedSignalStrategy(
                                SignalType.HOLD),
                        prices);

        interactor.execute(inputData);

        assertNull(presenter.successData);

        assertEquals(
                "Ticker cannot be null",
                presenter.errorMessage);
    }

    @Test
    void nullStrategyPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        presenter);

        final List<DailyPrice> prices =
                List.of(
                        createPrice(
                                1,
                                100.0,
                                100.0));

        final RunBacktestInputData inputData =
                new RunBacktestInputData(
                        new Ticker(
                                "TEST",
                                "Test Company"),
                        null,
                        prices);

        interactor.execute(inputData);

        assertNull(presenter.successData);

        assertEquals(
                "Strategy cannot be null",
                presenter.errorMessage);
    }

    @Test
    void constructorRejectsNullEngine() {
        final TestPresenter presenter =
                new TestPresenter();

        assertThrows(
                NullPointerException.class,
                () -> new RunBacktestInteractor(
                        null,
                        presenter));
    }

    @Test
    void constructorRejectsNullPresenter() {
        assertThrows(
                NullPointerException.class,
                () -> new RunBacktestInteractor(
                        new BacktestEngine(),
                        null));
    }

    private DailyPrice createPrice(
            int day,
            double open,
            double close) {

        return new DailyPrice(
                LocalDate.of(2026, 1, day),
                open,
                Math.max(open, close),
                Math.min(open, close),
                close,
                1000L);
    }

    /**
     * Presenter used only for testing the interactor.
     */
    private static class TestPresenter
            implements RunBacktestOutputBoundary {

        private RunBacktestOutputData successData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(
                RunBacktestOutputData outputData) {

            this.successData = outputData;
            this.errorMessage = null;
        }

        @Override
        public void prepareFailView(
                String errorMessage) {

            this.errorMessage = errorMessage;
            this.successData = null;
        }
    }

    /**
     * Deterministic strategy used only for testing.
     */
    private static class FixedSignalStrategy
            implements TradingStrategy {

        private final List<SignalType> signalTypes;

        FixedSignalStrategy(
                SignalType... signalTypes) {

            this.signalTypes =
                    List.of(signalTypes);
        }

        @Override
        public String getName() {
            return "Fixed Test Strategy";
        }

        @Override
        public List<TradingSignal> generateSignals(
                List<DailyPrice> prices) {

            final List<TradingSignal> signals =
                    new ArrayList<>();

            for (int index = 0;
                 index < signalTypes.size();
                 index++) {

                signals.add(
                        new TradingSignal(
                                prices.get(index).getDate(),
                                signalTypes.get(index)));
            }

            return signals;
        }
    }
}