package interface_adapter.backtest;

import entity.BacktestResult;
import entity.Trade;
import interface_adapter.chart.AxisScale;
import interface_adapter.chart.ChartTick;
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

    /**
     * Roughly how many gaps the value axis is divided into. A target rather than a count: the
     * rounding can land one either side of it.
     */
    private static final int AXIS_INTERVALS = 4;

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

        // Rounded bounds rather than the raw low and high, so the curve clears the frame and the
        // gridlines land on figures worth printing. AxisScale does the rounding.
        final AxisScale scale = AxisScale.forRange(low, high, AXIS_INTERVALS);
        final List<ChartTick> valueTicks = new ArrayList<>();
        for (final Double value : scale.tickValues()) {
            valueTicks.add(new ChartTick(value, String.format(MONEY_FORMAT, value)));
        }

        // Two date ticks and no more: BacktestResult carries the run's start and end but no
        // per-point dates, so there is nothing truthful to label the middle of the axis with.
        final List<ChartTick> timeTicks = List.of(
                new ChartTick(0, String.valueOf(result.getStartDate())),
                new ChartTick(values.size() - 1, String.valueOf(result.getEndDate())));

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

        // The band's meta slot is a few words wide. It carries the one thing the plotted line
        // must not be trusted to convey alone - the signed direction - and the sentence above
        // goes to the accessible description instead.
        final String meta = String.format("%dD %s", values.size(), signedReturn);

        return new BacktestViewModel.EquityCurve(values, scale.lowerBound(), scale.upperBound(),
                valueTicks, timeTicks, meta, summary);
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
