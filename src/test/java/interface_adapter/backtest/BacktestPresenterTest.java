package interface_adapter.backtest;

import entity.BacktestResult;
import entity.Ticker;
import entity.Trade;
import org.junit.jupiter.api.Test;
import use_case.backtest.RunBacktestOutputData;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacktestPresenterTest {

    private static BacktestResult resultWith(List<Trade> tradeLog) {
        return new BacktestResult(
                new Ticker("TEST", "Test Company"),
                "Test Strategy",
                tradeLog,
                11000.0,
                10.0,
                tradeLog.size(),
                0.0);
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
                LocalDate.of(2026, 1, 9), 110.0);

        presenter.prepareSuccessView(new RunBacktestOutputData(resultWith(List.of(trade))));

        assertEquals(1, viewModel.getTradeRows().size());
        final BacktestViewModel.TradeRow row = viewModel.getTradeRows().get(0);
        assertEquals("2026-01-05", row.entryDate());
        assertEquals("$100.00", row.entryPrice());
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
    }
}
