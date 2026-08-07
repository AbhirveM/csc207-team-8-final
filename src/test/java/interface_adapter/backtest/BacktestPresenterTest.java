package interface_adapter.backtest;

import entity.BacktestResult;
import entity.Ticker;
import org.junit.jupiter.api.Test;
import use_case.backtest.RunBacktestOutputData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BacktestPresenterTest {

    @Test
    void successViewStoresBacktestResult() {
        final BacktestViewModel viewModel =
                new BacktestViewModel();

        final BacktestPresenter presenter =
                new BacktestPresenter(viewModel);

        final Ticker ticker =
                new Ticker(
                        "TEST",
                        "Test Company");

        final BacktestResult result =
                new BacktestResult(
                        ticker,
                        "Test Strategy",
                        List.of(),
                        11000.0,
                        10.0,
                        0,
                        0.0);

        presenter.prepareSuccessView(
                new RunBacktestOutputData(result));

        assertEquals(
                result,
                viewModel.getResult());

        assertEquals(
                "",
                viewModel.getErrorMessage());
    }

    @Test
    void failViewStoresErrorMessage() {
        final BacktestViewModel viewModel =
                new BacktestViewModel();

        final BacktestPresenter presenter =
                new BacktestPresenter(viewModel);

        presenter.prepareFailView(
                "Backtest failed");

        assertEquals(
                "Backtest failed",
                viewModel.getErrorMessage());

        assertNull(
                viewModel.getResult());
    }
}