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

        viewModel.setResult(summary, tradeRows, curveFor(result));
    }

    /**
     * Builds the equity curve for the chart, with every label already formatted - the chart
     * itself composes no text.
     *
     * <p>The summary states the direction twice over, in words and with an explicit sign, which
     * is what lets the plotted line be coloured by direction without the colour carrying
     * anything on its own. The {@code +} on a gain is the same convention
     * {@code TableStyler.SignedRenderer} applies to a signed cell.
     *
     * @param result the completed run
     * @return the curve to plot, or an empty one for a result that carries no path
     */
    private static BacktestViewModel.EquityCurve curveFor(BacktestResult result) {
        final List<Double> values = result.getEquityCurve();
        if (values.isEmpty()) {
            return BacktestViewModel.EquityCurve.empty();
        }

        double low = values.get(0);
        double high = values.get(0);
        for (final Double value : values) {
            low = Math.min(low, value);
            high = Math.max(high, value);
        }

        final String returnText = String.format(PERCENT_FORMAT, result.getTotalReturn());
        final String signedReturn;
        if (result.getTotalReturn() > 0.0) {
            signedReturn = "+" + returnText;
        }
        else {
            signedReturn = returnText;
        }

        final String summary = String.format(
                "Portfolio value over %d days, %s to %s, %s.",
                values.size(),
                String.format(MONEY_FORMAT, values.get(0)),
                String.format(MONEY_FORMAT, values.get(values.size() - 1)),
                signedReturn);

        return new BacktestViewModel.EquityCurve(values,
                String.format(MONEY_FORMAT, low),
                String.format(MONEY_FORMAT, high),
                String.valueOf(result.getStartDate()),
                String.valueOf(result.getEndDate()),
                summary);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }
}
