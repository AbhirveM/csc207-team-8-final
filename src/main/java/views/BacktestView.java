package views;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.momentum.MomentumState;
import interface_adapter.momentum.MomentumViewModel;
import interface_adapter.moving_average.MovingAverageState;
import interface_adapter.moving_average.MovingAverageViewModel;

/**
 * The screen that lets a user run a backtest and see its result.
 *
 * <p>This is the integration seam between three otherwise-disconnected verticals: the
 * watchlist supplies price history, a strategy turns that history into signals, and the
 * backtest engine turns the signals into a result. Before this view existed every one of
 * those pieces was constructed and tested but nothing in the UI called them, so the
 * Compare-Strategies screen only ever rendered its empty state.
 *
 * <p>The panel owns only its two input controls - a ticker chooser and a strategy chooser -
 * and names a run to {@link BacktestController} as a symbol plus a handful of numbers. It
 * looks nothing up and constructs nothing: resolving the symbol to its price history and
 * building the strategy are use-case work, so this class imports no {@code entity} and no
 * repository. Both the chooser's contents and the run's result arrive on
 * {@link BacktestViewModel}, already display-ready.
 *
 * <p>The strategy parameters are not hardcoded here: they are read from the two
 * configuration screens' view models ({@link MomentumViewModel} and
 * {@link MovingAverageViewModel}) so a run reflects whatever the user saved there. Until
 * they configure a strategy, each falls back to a default that fits the free tier's
 * ~100-day history.
 */
public class BacktestView extends JPanel {

    /** The strategy options offered in the dropdown, in the order they appear. */
    private static final String MOVING_AVERAGE = "Moving Average Crossover";
    private static final String RSI_MOMENTUM = "RSI Momentum";

    /** Defaults used when the user has not saved a configuration on the strategy screens. */
    private static final int DEFAULT_SHORT_WINDOW = 5;
    private static final int DEFAULT_LONG_WINDOW = 20;
    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final double DEFAULT_OVERSOLD = 30.0;
    private static final double DEFAULT_OVERBOUGHT = 70.0;

    private final BacktestController controller;
    private final MomentumViewModel momentumViewModel;
    private final MovingAverageViewModel movingAverageViewModel;

    private final JComboBox<String> tickerBox = new JComboBox<>();
    private final JComboBox<String> strategyBox =
            new JComboBox<>(new String[] {MOVING_AVERAGE, RSI_MOMENTUM});
    private final JButton runButton = new JButton("Run backtest");
    private final JLabel statusLabel = new JLabel(" ");

    /**
     * Builds the panel and wires its controls to the run-backtest use case.
     *
     * @param viewModel              the observable this screen paints - the chooser's tickers
     *                               and the embedded results view both read it; must be non-null
     * @param controller             the boundary the Run button calls; must be non-null
     * @param momentumViewModel      the Momentum configuration screen's view model, read for the
     *                               saved RSI parameters; must be non-null
     * @param movingAverageViewModel the Moving Average configuration screen's view model, read for
     *                               the saved window sizes; must be non-null
     * @throws NullPointerException if any argument is null
     */
    public BacktestView(BacktestViewModel viewModel,
                        BacktestController controller,
                        MomentumViewModel momentumViewModel,
                        MovingAverageViewModel movingAverageViewModel) {
        this.controller = Objects.requireNonNull(controller, "Controller cannot be null");
        this.momentumViewModel =
                Objects.requireNonNull(momentumViewModel, "Momentum view model cannot be null");
        this.movingAverageViewModel = Objects.requireNonNull(
                movingAverageViewModel, "Moving average view model cannot be null");
        Objects.requireNonNull(viewModel, "View model cannot be null");

        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.LG, Theme.LG, Theme.LG, Theme.LG));
        add(buildControls(), BorderLayout.NORTH);
        add(new BacktestResultsView(viewModel), BorderLayout.CENTER);

        runButton.addActionListener(event -> onRun());

        viewModel.addPropertyChangeListener(event -> onViewModelChanged(viewModel, event));

        // The repository behind the use case is empty at construction (prices are loaded
        // later from the watchlist), so the ticker list is asked for again every time this
        // card is shown. CardLayout.show makes the target component visible, which fires
        // componentShown.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent event) {
                controller.loadAvailableTickers();
            }
        });
        controller.loadAvailableTickers();
    }

    /**
     * Builds the northern control strip: ticker chooser, strategy chooser, Run button
     * and a status line for problems this view resolves itself (such as no ticker having
     * loaded price history yet).
     *
     * @return the assembled panel
     */
    private JPanel buildControls() {
        final JLabel tickerLabel = controlLabel("Ticker:");
        tickerLabel.setLabelFor(tickerBox);
        tickerLabel.setDisplayedMnemonic('K');
        tickerBox.getAccessibleContext().setAccessibleName("Ticker to backtest");

        final JLabel strategyLabel = controlLabel("Strategy:");
        strategyLabel.setLabelFor(strategyBox);
        strategyLabel.setDisplayedMnemonic('S');
        strategyBox.getAccessibleContext().setAccessibleName("Strategy to apply");

        runButton.setMnemonic('B');
        runButton.setEnabled(false);
        runButton.setToolTipText(
                "Run the chosen strategy over the chosen ticker's loaded price history.");

        statusLabel.setFocusable(true);
        statusLabel.setFont(Theme.FONT_UI);
        statusLabel.setForeground(Theme.FG_MUTED);
        statusLabel.getAccessibleContext().setAccessibleName("Backtest status");

        Controls.styleComboBox(tickerBox);
        Controls.styleComboBox(strategyBox);
        Controls.primary(runButton);

        // A box row instead of a FlowLayout: the controls stay on one line pinned left,
        // rather than centring themselves and re-wrapping as the window is resized.
        final Box controls = Box.createHorizontalBox();
        controls.add(tickerLabel);
        controls.add(Box.createHorizontalStrut(Theme.SM));
        controls.add(tickerBox);
        controls.add(Box.createHorizontalStrut(Theme.LG));
        controls.add(strategyLabel);
        controls.add(Box.createHorizontalStrut(Theme.SM));
        controls.add(strategyBox);
        controls.add(Box.createHorizontalStrut(Theme.LG));
        controls.add(runButton);
        controls.add(Box.createHorizontalGlue());

        final JPanel north = new JPanel(new BorderLayout(0, Theme.SM));
        north.setBackground(Theme.BG);
        north.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.MD, 0));
        north.add(controls, BorderLayout.CENTER);
        north.add(statusLabel, BorderLayout.SOUTH);
        return north;
    }

    /**
     * Builds a control-strip label in the house type.
     *
     * @param text the label text
     * @return the styled label
     */
    private static JLabel controlLabel(String text) {
        return Controls.fieldLabel(new JLabel(text));
    }

    /**
     * Handles updates from the backtest view model, marshalling onto the event thread.
     *
     * <p>Only the ticker list is handled here; the run's result is painted by the embedded
     * {@link BacktestResultsView}, which listens to the same view model itself.
     *
     * @param viewModel the view model that changed
     * @param event     the property change event
     */
    private void onViewModelChanged(BacktestViewModel viewModel, PropertyChangeEvent event) {
        if (!BacktestViewModel.TICKERS_PROPERTY.equals(event.getPropertyName())) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onViewModelChanged(viewModel, event));
            return;
        }
        showTickers(viewModel.getAvailableTickers());
    }

    /**
     * Repopulates the ticker dropdown, preserving the current selection when it is still
     * offered. Only tickers with loaded prices are listed, because those are the only ones
     * a backtest can run against.
     *
     * @param symbols the symbols to offer, in the order they should appear
     */
    private void showTickers(List<String> symbols) {
        final Object previous = tickerBox.getSelectedItem();
        tickerBox.removeAllItems();
        for (final String symbol : symbols) {
            tickerBox.addItem(symbol);
        }
        if (previous != null) {
            tickerBox.setSelectedItem(previous);
        }
        runButton.setEnabled(!symbols.isEmpty());
        if (symbols.isEmpty()) {
            statusLabel.setText(
                    "No prices loaded yet. Add a ticker on the Watchlist screen and click "
                            + "\"Load prices\", then return here.");
        }
        else {
            statusLabel.setText(" ");
        }
    }

    /**
     * Runs the chosen strategy over the chosen ticker. The one problem this view can see
     * for itself - nothing selected - is reported on the local status line and
     * short-circuits the call; everything else, including a symbol whose prices are no
     * longer loaded, is the interactor's to word and surfaces through the embedded results
     * view.
     */
    private void onRun() {
        final Object selected = tickerBox.getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Choose a ticker to backtest.");
            return;
        }

        statusLabel.setText(" ");
        final String symbol = (String) selected;
        if (RSI_MOMENTUM.equals(strategyBox.getSelectedItem())) {
            final double[] momentum = momentumParameters();
            controller.runMomentumBacktest(
                    symbol, (int) momentum[0], momentum[1], momentum[2]);
        }
        else {
            final int[] windows = movingAverageWindows();
            controller.runMovingAverageBacktest(symbol, windows[0], windows[1]);
        }
    }

    /**
     * The Moving Average windows the user saved on the Moving Average screen, or the
     * defaults when they have not configured any this session. The screen stores its
     * windows as separate integers, so both must be present before they are used.
     *
     * <p>Package-private so {@code BacktestViewTest} can assert the saved windows are read
     * rather than hardcoded ones.
     *
     * @return the short window followed by the long window
     */
    int[] movingAverageWindows() {
        final MovingAverageState state = movingAverageViewModel.getState();
        final Integer shortWindow = state.getConfiguredShortWindow();
        final Integer longWindow = state.getConfiguredLongWindow();
        final int[] windows;
        if (shortWindow != null && longWindow != null) {
            windows = new int[] {shortWindow, longWindow};
        }
        else {
            windows = new int[] {DEFAULT_SHORT_WINDOW, DEFAULT_LONG_WINDOW};
        }
        return windows;
    }

    /**
     * The Momentum parameters the user saved on the Momentum screen, or the defaults when
     * they have not configured any this session.
     *
     * <p>Package-private so {@code BacktestViewTest} can assert the saved parameters are
     * read rather than hardcoded ones.
     *
     * @return the RSI period, the oversold threshold and the overbought threshold
     */
    double[] momentumParameters() {
        final MomentumState state = momentumViewModel.getState();
        final double[] parameters;
        if (state.isConfigured()) {
            parameters = new double[] {
                    state.getConfiguredPeriod(),
                    state.getConfiguredOversoldThreshold(),
                    state.getConfiguredOverboughtThreshold()};
        }
        else {
            parameters = new double[] {
                    DEFAULT_RSI_PERIOD, DEFAULT_OVERSOLD, DEFAULT_OVERBOUGHT};
        }
        return parameters;
    }
}
