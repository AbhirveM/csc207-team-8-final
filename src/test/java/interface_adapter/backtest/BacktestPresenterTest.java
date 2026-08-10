package interface_adapter.backtest;

import entity.BacktestResult;
import entity.Ticker;
import entity.Trade;
import interface_adapter.chart.ChartTick;
import org.junit.jupiter.api.Test;
import use_case.backtest.RunBacktestOutputData;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacktestPresenterTest {

    private static BacktestResult resultWith(List<Trade> tradeLog) {
        return resultWith(tradeLog, List.of(), 10.0);
    }

    private static BacktestResult resultWith(List<Trade> tradeLog, List<Double> equityCurve,
                                             double totalReturn) {
        return new BacktestResult(
                new Ticker("TEST", "Test Company"),
                "Test Strategy",
                tradeLog,
                11000.0,
                totalReturn,
                tradeLog.size(),
                0.0,
                equityCurve,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 9));
    }

    @Test
    void successViewFormatsTheSummary() {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestPresenter presenter = new BacktestPresenter(viewModel);

        presenter.prepareSuccessView(new RunBacktestOutputData(resultWith(List.of())));

        final BacktestViewModel.Summary summary = viewModel.getSummary();
        assertEquals("TEST", summary.ticker());
        assertEquals("Test Strategy", summary.strategyName());
        assertEquals("$11000.00", summary.finalCapital());
        assertEquals("10.00%", summary.totalReturn());
        assertEquals("0", summary.numberOfTrades());
        assertEquals("0.00%", summary.winRate());
        assertEquals("", viewModel.getErrorMessage());
        assertTrue(viewModel.getTradeRows().isEmpty());
    }

    @Test
    void successViewFormatsEachTradeRow() {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestPresenter presenter = new BacktestPresenter(viewModel);

        final Trade trade = new Trade(
                new Ticker("TEST", "Test Company"),
                LocalDate.of(2026, 1, 5), 100.0,
                LocalDate.of(2026, 1, 9), 110.0,
                7);

        presenter.prepareSuccessView(new RunBacktestOutputData(resultWith(List.of(trade))));

        assertEquals(1, viewModel.getTradeRows().size());
        final BacktestViewModel.TradeRow row = viewModel.getTradeRows().get(0);
        assertEquals("2026-01-05", row.entryDate());
        assertEquals("$100.00", row.entryPrice());
        assertEquals("7", row.quantity());
        assertEquals("2026-01-09", row.exitDate());
        assertEquals("$110.00", row.exitPrice());
        assertEquals("10.00", row.returnPercent());
    }

    @Test
    void failViewStoresErrorMessageAndClearsTheResult() {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestPresenter presenter = new BacktestPresenter(viewModel);

        presenter.prepareSuccessView(new RunBacktestOutputData(resultWith(List.of())));
        presenter.prepareFailView("Backtest failed");

        assertEquals("Backtest failed", viewModel.getErrorMessage());
        assertNull(viewModel.getSummary());
        assertTrue(viewModel.getTradeRows().isEmpty());
        assertTrue(viewModel.getEquityCurve().values().isEmpty(),
                "a failure must not leave the previous run's curve on screen");
    }

    @Test
    void theEquityCurveCarriesRoundedBoundsAndFormattedValueTicks() {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestPresenter presenter = new BacktestPresenter(viewModel);

        presenter.prepareSuccessView(new RunBacktestOutputData(
                resultWith(List.of(), List.of(10000.0, 9500.0, 11000.0), 10.0)));

        final BacktestViewModel.EquityCurve curve = viewModel.getEquityCurve();
        assertEquals(List.of(10000.0, 9500.0, 11000.0), curve.values());

        // Rounded outwards past the data, so the curve clears the frame at both ends.
        assertTrue(curve.lowerBound() < 9500.0, "bound was " + curve.lowerBound());
        assertTrue(curve.upperBound() > 11000.0, "bound was " + curve.upperBound());

        // Every gridline is labelled, and the labels carry this presenter's dollar mark.
        assertTrue(curve.valueTicks().size() >= 3);
        for (final ChartTick tick : curve.valueTicks()) {
            assertTrue(tick.label().startsWith("$"), "tick label was " + tick.label());
        }
        assertEquals(curve.lowerBound(), curve.valueTicks().get(0).value(), 0.0);
        assertEquals(curve.upperBound(),
                curve.valueTicks().get(curve.valueTicks().size() - 1).value(), 0.0);
    }

    @Test
    void theEquityCurveLabelsOnlyItsTwoEndsInTime() {
        // BacktestResult carries the run's start and end but no per-point dates, so there is
        // nothing truthful to label the middle of the time axis with. Two ticks, not five.
        final BacktestViewModel viewModel = new BacktestViewModel();
        new BacktestPresenter(viewModel).prepareSuccessView(new RunBacktestOutputData(
                resultWith(List.of(), List.of(10000.0, 9500.0, 11000.0), 10.0)));

        final List<ChartTick> timeTicks = viewModel.getEquityCurve().timeTicks();
        assertEquals(2, timeTicks.size());
        assertEquals(0.0, timeTicks.get(0).value(), 0.0);
        assertEquals("2026-01-05", timeTicks.get(0).label());
        assertEquals(2.0, timeTicks.get(1).value(), 0.0);
        assertEquals("2026-01-09", timeTicks.get(1).label());
    }

    @Test
    void theCurveSummaryStatesItsDirectionInWordsAndWithASign() {
        // The plotted line is coloured by direction, so the direction has to be readable
        // without the colour. A gain takes an explicit "+", exactly as a signed table cell does.
        final BacktestViewModel gained = new BacktestViewModel();
        new BacktestPresenter(gained).prepareSuccessView(new RunBacktestOutputData(
                resultWith(List.of(), List.of(10000.0, 11000.0), 10.0)));
        assertEquals("Portfolio value over 2 days, $10000.00 to $11000.00, +10.00%.",
                gained.getEquityCurve().summary());

        final BacktestViewModel lost = new BacktestViewModel();
        new BacktestPresenter(lost).prepareSuccessView(new RunBacktestOutputData(
                resultWith(List.of(), List.of(10000.0, 9000.0), -10.0)));
        assertEquals("Portfolio value over 2 days, $10000.00 to $9000.00, -10.00%.",
                lost.getEquityCurve().summary());
    }

    @Test
    void aResultCarryingNoPathYieldsAnEmptyCurveRatherThanThrowing() {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestPresenter presenter = new BacktestPresenter(viewModel);

        presenter.prepareSuccessView(new RunBacktestOutputData(resultWith(List.of())));

        assertTrue(viewModel.getEquityCurve().values().isEmpty());
        assertEquals("No data.", viewModel.getEquityCurve().summary());
    }
}
