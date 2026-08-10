package interface_adapter.backtest;

import entity.BacktestResult;
import entity.Trade;
import use_case.backtest.RunBacktestOutputBoundary;
import use_case.backtest.RunBacktestOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Presenter for the run-backtest use case.
 *
 * <p>All formatting lives here rather than in {@code BacktestResultsView}, so the view copies
 * strings into widgets and never imports an entity.
 */
public class BacktestPresenter implements RunBacktestOutputBoundary {

    private static final String MONEY_FORMAT = "$%.2f";
    private static final String PERCENT_FORMAT = "%.2f%%";
    private static final String PLAIN_FORMAT = "%.2f";

    private final BacktestViewModel viewModel;

    public BacktestPresenter(BacktestViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(RunBacktestOutputData outputData) {
        final BacktestResult result = outputData.getBacktestResult();

        final BacktestViewModel.Summary summary = new BacktestViewModel.Summary(
                result.getTicker().getSymbol(),
                result.getStrategyName(),
                String.format(MONEY_FORMAT, result.getFinalCapital()),
                String.format(PERCENT_FORMAT, result.getTotalReturn()),
                String.valueOf(result.getNumberOfTrades()),
                String.format(PERCENT_FORMAT, result.getWinRate()));

        final List<BacktestViewModel.TradeRow> tradeRows = new ArrayList<>();
        for (final Trade trade : result.getTradeLog()) {
            tradeRows.add(new BacktestViewModel.TradeRow(
                    String.valueOf(trade.getEntryDate()),
                    String.format(MONEY_FORMAT, trade.getEntryPrice()),
                    String.valueOf(trade.getQuantity()),
                    String.valueOf(trade.getExitDate()),
                    String.format(MONEY_FORMAT, trade.getExitPrice()),
                    String.format(PLAIN_FORMAT, trade.getReturnPercent())));
        }

        viewModel.setResult(summary, tradeRows);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }

    @Override
    public void presentAvailableTickers(List<String> tickerSymbols) {
        viewModel.setAvailableTickers(tickerSymbols);
    }
}
