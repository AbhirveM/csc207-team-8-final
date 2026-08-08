package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import entity.DailyPrice;
import entity.MomentumConfiguration;
import entity.MovingAverageConfiguration;
import entity.MovingAverageCrossoverStrategy;
import entity.RSIMomentumStrategy;
import entity.Stock;
import entity.Ticker;
import entity.TradingStrategy;
import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestViewModel;
import use_case.watchlist.StockRepository;

/**
 * The screen that lets a user run a backtest and see its result.
 *
 * <p>This is the integration seam between three otherwise-disconnected verticals:
 * the watchlist supplies price history (through {@link StockRepository}), a strategy
 * turns that history into signals, and {@link entity.BacktestEngine} turns the signals
 * into a {@link entity.BacktestResult}. Before this view existed every one of those
 * pieces was constructed and tested but nothing in the UI called them, so the
 * Compare-Strategies screen only ever rendered its empty state.
 *
 * <p>The panel owns only its two input controls - a ticker chooser and a strategy
 * chooser - and delegates the run itself to {@link BacktestController}. The result is
 * rendered by an embedded {@link BacktestResultsView} bound to the same
 * {@link BacktestViewModel} the controller's presenter writes to, so this class never
 * touches a {@code BacktestResult} directly.
 */
public class BacktestView extends JPanel {

    /** The strategy options offered in the dropdown, in the order they appear. */
    private static final String MOVING_AVERAGE = "Moving Average Crossover (5 / 20)";
    private static final String RSI_MOMENTUM = "RSI Momentum (period 14, 30 / 70)";

    private final StockRepository stockRepository;
    private final BacktestController controller;

    private final JComboBox<String> tickerBox = new JComboBox<>();
    private final JComboBox<String> strategyBox =
            new JComboBox<>(new String[] {MOVING_AVERAGE, RSI_MOMENTUM});
    private final JButton runButton = new JButton("Run backtest");
    private final JLabel statusLabel = new JLabel(" ");

    /**
     * Builds the panel and wires its controls to the run-backtest use case.
     *
     * @param viewModel       the observable the embedded results view paints; must be non-null
     * @param controller      the boundary the Run button calls; must be non-null
     * @param stockRepository where price history is read from by symbol; must be non-null
     * @throws NullPointerException if any argument is null
     */
    public BacktestView(BacktestViewModel viewModel,
                        BacktestController controller,
                        StockRepository stockRepository) {
        this.controller = Objects.requireNonNull(controller, "Controller cannot be null");
        this.stockRepository =
                Objects.requireNonNull(stockRepository, "Stock repository cannot be null");
        Objects.requireNonNull(viewModel, "View model cannot be null");

        setLayout(new BorderLayout(8, 8));
        add(buildControls(), BorderLayout.NORTH);
        add(new BacktestResultsView(viewModel), BorderLayout.CENTER);

        runButton.addActionListener(event -> onRun());

        // The repository is empty at construction (prices are loaded later from the
        // watchlist), so the ticker list is refreshed every time this card is shown.
        // CardLayout.show makes the target component visible, which fires componentShown.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent event) {
                reloadTickers();
            }
        });
        reloadTickers();
    }

    /**
     * Builds the northern control strip: ticker chooser, strategy chooser, Run button
     * and a status line for problems this view resolves itself (such as a ticker with
     * no loaded price history).
     *
     * @return the assembled panel
     */
    private JPanel buildControls() {
        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

        final JLabel tickerLabel = new JLabel("Ticker:");
        tickerLabel.setLabelFor(tickerBox);
        tickerLabel.setDisplayedMnemonic('K');
        tickerBox.getAccessibleContext().setAccessibleName("Ticker to backtest");

        final JLabel strategyLabel = new JLabel("Strategy:");
        strategyLabel.setLabelFor(strategyBox);
        strategyLabel.setDisplayedMnemonic('S');
        strategyBox.getAccessibleContext().setAccessibleName("Strategy to apply");

        runButton.setMnemonic('B');
        runButton.setToolTipText(
                "Run the chosen strategy over the chosen ticker's loaded price history.");

        statusLabel.setFocusable(true);
        statusLabel.getAccessibleContext().setAccessibleName("Backtest status");

        controls.add(tickerLabel);
        controls.add(tickerBox);
        controls.add(strategyLabel);
        controls.add(strategyBox);
        controls.add(runButton);

        final JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        north.add(controls, BorderLayout.CENTER);
        north.add(statusLabel, BorderLayout.SOUTH);
        return north;
    }

    /**
     * Repopulates the ticker dropdown from the stock repository, preserving the current
     * selection when it still has price history. Only tickers with loaded prices appear,
     * because those are the only ones a backtest can run against.
     */
    private void reloadTickers() {
        final Object previous = tickerBox.getSelectedItem();
        tickerBox.removeAllItems();
        for (final Stock stock : stockRepository.findAll()) {
            tickerBox.addItem(stock.getTicker().getSymbol());
        }
        if (previous != null) {
            tickerBox.setSelectedItem(previous);
        }
        final boolean hasTickers = tickerBox.getItemCount() > 0;
        runButton.setEnabled(hasTickers);
        if (!hasTickers) {
            statusLabel.setText(
                    "No prices loaded yet. Add a ticker on the Watchlist screen and click "
                            + "\"Load prices\", then return here.");
        }
        else {
            statusLabel.setText(" ");
        }
    }

    /**
     * Runs the chosen strategy over the chosen ticker. Problems that this view can see
     * for itself - no selection, or a symbol whose prices are no longer loaded - are
     * reported on the local status line and short-circuit the call; everything else is
     * left to the interactor and surfaces through the embedded results view.
     */
    private void onRun() {
        final Object selected = tickerBox.getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Choose a ticker to backtest.");
            return;
        }

        final Stock stock = stockRepository.findBySymbol((String) selected).orElse(null);
        if (stock == null) {
            statusLabel.setText(
                    "No loaded prices for " + selected + ". Load its prices on the Watchlist "
                            + "screen first.");
            reloadTickers();
            return;
        }

        statusLabel.setText(" ");
        final Ticker ticker = stock.getTicker();
        final List<DailyPrice> prices = stock.getDailyPrices();
        final TradingStrategy strategy = buildStrategy();
        controller.runBacktest(ticker, strategy, prices);
    }



    /**
     * Builds the strategy the dropdown currently names, with the parameters shown in its
     * label. Both defaults sit comfortably inside the free tier's ~100-trading-day
     * compact response, so neither throws on a freshly loaded ticker.
     *
     * @return the chosen, fully configured strategy
     */
    private TradingStrategy buildStrategy() {
        if (RSI_MOMENTUM.equals(strategyBox.getSelectedItem())) {
            return new RSIMomentumStrategy(
                    new MomentumConfiguration(14, 30.0, 70.0));
        }
        return new MovingAverageCrossoverStrategy(
                new MovingAverageConfiguration(5, 20));
    }
}
