package view;

import interface_adapter.backtest.BacktestViewModel;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;

/**
 * Displays the result of a completed backtest.
 *
 * <p>Every value arrives from {@link BacktestViewModel} already formatted, so this panel imports
 * no entities and does no formatting of its own.
 */
public class BacktestResultsView extends JPanel {

    private static final String TICKER_LABEL = "Ticker";
    private static final String STRATEGY_LABEL = "Strategy";
    private static final String FINAL_CAPITAL_LABEL = "Final Capital";
    private static final String TOTAL_RETURN_LABEL = "Total Return";
    private static final String TRADES_LABEL = "Number of Trades";
    private static final String WIN_RATE_LABEL = "Win Rate";
    private static final String BLANK_LINE = " ";

    /** Shown in a metric slot that has no value yet. */
    private static final String NO_VALUE = "\u2014";

    /** The trade-log column holding a signed return, which gets the direction renderer. */
    private static final int RETURN_COLUMN = 5;

    /** The trade-log columns holding plain figures. */
    private static final int[] TRADE_NUMERIC_COLUMNS = {1, 2, 4};

    /** Relative trade-log column widths; dates are the widest thing in the row. */
    private static final int[] TRADE_COLUMN_WIDTHS = {90, 85, 70, 90, 85, 80};

    private final BacktestViewModel viewModel;

    // Each metric is now a caption and a value in their own cells rather than one label
    // holding "Caption: value". Two columns of figures only line up if the values share a
    // left edge, which a single concatenated label cannot give. The caption travels with the
    // value as its accessible name, so a screen reader still reads the pair.
    private final JLabel tickerLabel = valueLabel(TICKER_LABEL, Theme.FONT_UI);
    private final JLabel strategyLabel = valueLabel(STRATEGY_LABEL, Theme.FONT_UI);
    private final JLabel finalCapitalLabel = valueLabel(FINAL_CAPITAL_LABEL, Theme.FONT_MONO);
    private final JLabel totalReturnLabel = valueLabel(TOTAL_RETURN_LABEL, Theme.FONT_MONO);
    private final JLabel numberOfTradesLabel = valueLabel(TRADES_LABEL, Theme.FONT_MONO);
    private final JLabel winRateLabel = valueLabel(WIN_RATE_LABEL, Theme.FONT_MONO);
    private final JLabel statusLabel = new JLabel(BLANK_LINE);

    private final LineChart equityChart = new LineChart("Portfolio value");
    private final JLabel equityMeta = new JLabel(BLANK_LINE);

    private final DefaultTableModel tableModel;

    public BacktestResultsView(BacktestViewModel viewModel) {
        this.viewModel = viewModel;

        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);

        final JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setBackground(Theme.BG);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.MD, 0));
        addMetric(summaryPanel, 0, 0, TICKER_LABEL, tickerLabel);
        addMetric(summaryPanel, 1, 0, STRATEGY_LABEL, strategyLabel);
        addMetric(summaryPanel, 0, 1, FINAL_CAPITAL_LABEL, finalCapitalLabel);
        addMetric(summaryPanel, 1, 1, TOTAL_RETURN_LABEL, totalReturnLabel);
        addMetric(summaryPanel, 0, 2, TRADES_LABEL, numberOfTradesLabel);
        addMetric(summaryPanel, 1, 2, WIN_RATE_LABEL, winRateLabel);
        // The curve sits between the headline figures and the trade log: it explains the former
        // and is explained by the latter, so it belongs between them rather than under both.
        final JPanel head = new JPanel(new BorderLayout(0, Theme.MD));
        head.setBackground(Theme.BG);
        head.add(PanelHeader.region(new JLabel("Result"), null, summaryPanel), BorderLayout.NORTH);
        final JLabel equityHeading = new JLabel("Portfolio value");
        equityHeading.setLabelFor(equityChart);
        head.add(PanelHeader.region(equityHeading, equityMeta, equityChart), BorderLayout.CENTER);
        // A BorderLayout NORTH slot takes its child's preferred height, and the chart's is
        // Theme.CHART_HEIGHT, so the trade log below keeps everything left over.
        add(head, BorderLayout.NORTH);

        final String[] columns = {"Entry Date", "Entry Price", "Quantity", "Exit Date",
                "Exit Price", "Return %"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        final JTable table = new JTable(tableModel);
        table.getAccessibleContext().setAccessibleName("Completed trades");
        // Release Tab / Shift+Tab so keyboard focus can leave the trade-log table rather
        // than cycling its cells forever. Arrow keys still move between cells. Same fix
        // WatchlistView applies to its tables.
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        TableStyler.style(table);
        TableStyler.numericColumns(table, TRADE_NUMERIC_COLUMNS);
        TableStyler.signedColumns(table, RETURN_COLUMN);
        TableStyler.preferredWidths(table, TRADE_COLUMN_WIDTHS);
        final JLabel tradeLogHeading = new JLabel("Trade log");
        tradeLogHeading.setLabelFor(table);
        add(PanelHeader.region(tradeLogHeading, PanelHeader.rowCount(table), TableStyler.wrap(table)),
                BorderLayout.CENTER);

        statusLabel.setFont(Theme.FONT_UI);
        statusLabel.setForeground(Theme.FG_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(Theme.SM, 0, 0, 0));
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        add(statusLabel, BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
    }

    /**
     * Updates the displayed result whenever the view model changes.
     *
     * @param event the property-change event
     */
    private void onViewModelChanged(PropertyChangeEvent event) {
        tableModel.setRowCount(0);

        if (!viewModel.getErrorMessage().isEmpty()) {
            clearSummary();
            statusLabel.setText(viewModel.getErrorMessage());
            return;
        }

        final BacktestViewModel.Summary summary = viewModel.getSummary();
        if (summary == null) {
            clearSummary();
            statusLabel.setText(BLANK_LINE);
            return;
        }

        statusLabel.setText(BLANK_LINE);
        setValue(tickerLabel, summary.ticker());
        setValue(strategyLabel, summary.strategyName());
        setValue(finalCapitalLabel, summary.finalCapital());
        setValue(totalReturnLabel, summary.totalReturn());
        setValue(numberOfTradesLabel, summary.numberOfTrades());
        setValue(winRateLabel, summary.winRate());

        // The summary goes on the chart, where it becomes the accessible description, and into
        // the band's meta slot, where it is visible. The line's colour repeats what the signed
        // figure in that sentence already says.
        final BacktestViewModel.EquityCurve curve = viewModel.getEquityCurve();
        equityChart.setSeries(new LineChart.Series(curve.values(), curve.lowLabel(),
                curve.highLabel(), curve.startLabel(), curve.endLabel(), curve.summary()));
        equityMeta.setText(curve.summary());

        for (final BacktestViewModel.TradeRow row : viewModel.getTradeRows()) {
            tableModel.addRow(new Object[] {row.entryDate(), row.entryPrice(), row.quantity(),
                    row.exitDate(), row.exitPrice(), row.returnPercent()});
        }
    }

    /**
     * Clears all displayed summary information, including the curve.
     *
     * <p>Reached on both the error path and the nothing-has-run path. The curve is cleared
     * rather than left standing: a plotted run under an error message is a picture of something
     * that is no longer on screen anywhere else.
     */
    private void clearSummary() {
        equityChart.setSeries(LineChart.Series.empty());
        equityMeta.setText(BLANK_LINE);
        setValue(tickerLabel, NO_VALUE);
        setValue(strategyLabel, NO_VALUE);
        setValue(finalCapitalLabel, NO_VALUE);
        setValue(totalReturnLabel, NO_VALUE);
        setValue(numberOfTradesLabel, NO_VALUE);
        setValue(winRateLabel, NO_VALUE);
    }

    /**
     * Writes a metric value and keeps the spoken description in step with it.
     *
     * @param label the value label to write
     * @param value the display-ready value
     */
    private static void setValue(JLabel label, String value) {
        label.setText(value);
        label.getAccessibleContext().setAccessibleDescription(
                label.getAccessibleContext().getAccessibleName() + ": " + value);
    }

    /**
     * Builds the label that holds a metric value.
     *
     * <p>The font is the caller's choice because only four of these six metrics are figures.
     * A strategy name set in a figures font reads as a stock symbol, and monospace buys
     * nothing for a value nothing lines up against.
     *
     * @param caption the metric name, which becomes the label accessible name
     * @param font the font this metric renders in
     * @return the value label
     */
    private static JLabel valueLabel(String caption, Font font) {
        final JLabel label = new JLabel(NO_VALUE);
        label.setFont(font);
        label.setForeground(Theme.FG);
        label.getAccessibleContext().setAccessibleName(caption);
        return label;
    }

    /**
     * Places one caption and value pair into the summary grid.
     *
     * @param panel the summary panel being built
     * @param pair which of the two metric pairs on a row this is, 0 or 1
     * @param row the grid row
     * @param caption the metric name
     * @param value the label holding the metric value
     */
    private static void addMetric(JPanel panel, int pair, int row, String caption, JLabel value) {
        final JLabel captionLabel = Controls.fieldLabel(new JLabel(caption));
        captionLabel.setLabelFor(value);

        final int leadingGap;
        if (pair == 0) {
            leadingGap = 0;
        }
        else {
            leadingGap = Theme.XL;
        }

        final GridBagConstraints captionConstraints = new GridBagConstraints();
        captionConstraints.gridx = pair * 2;
        captionConstraints.gridy = row;
        captionConstraints.anchor = GridBagConstraints.LINE_END;
        captionConstraints.insets = new Insets(0, leadingGap, Theme.XS, Theme.SM);
        panel.add(captionLabel, captionConstraints);

        final GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = pair * 2 + 1;
        valueConstraints.gridy = row;
        valueConstraints.anchor = GridBagConstraints.LINE_START;
        valueConstraints.insets = new Insets(0, 0, Theme.XS, 0);
        valueConstraints.weightx = pair;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, valueConstraints);
    }
}
