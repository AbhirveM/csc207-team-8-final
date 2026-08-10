package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.momentum.MomentumController;
import interface_adapter.momentum.MomentumPresenter;
import interface_adapter.momentum.MomentumViewModel;
import interface_adapter.moving_average.MovingAverageController;
import interface_adapter.moving_average.MovingAveragePresenter;
import interface_adapter.moving_average.MovingAverageViewModel;
import use_case.backtest.RunBacktestInputBoundary;
import use_case.backtest.RunBacktestInputData;
import use_case.momentum.ConfigureMomentumInteractor;
import use_case.moving_average.ConfigureMovingAverageInteractor;

/**
 * The parameters a backtest runs with come from the two configuration screens, not from
 * numbers hardcoded in {@link BacktestView}. These tests read the parameters the view would
 * hand to the controller and assert they follow whatever was saved on those screens, falling
 * back to a default only until the user configures one.
 *
 * <p>The last test presses the button for real and inspects the {@link RunBacktestInputData}
 * that reaches the boundary. That is the assertion that matters after the layering fix: what
 * leaves this screen is a symbol and a few numbers, never a ticker, a price list or a
 * constructed strategy.
 *
 * <p>The config-resolution methods are exercised directly (they are package-private for this
 * reason); the project has no other Swing-view tests, so this stays below the interaction
 * layer and needs no display.
 */
class BacktestViewTest {

    /** A boundary that records what it was asked to run instead of running it. */
    private static final class RecordingBoundary implements RunBacktestInputBoundary {

        private RunBacktestInputData lastRun;
        private int loadCalls;

        @Override
        public void execute(RunBacktestInputData inputData) {
            this.lastRun = inputData;
        }

        @Override
        public void loadAvailableTickers() {
            this.loadCalls++;
        }
    }

    private static BacktestView viewWith(MomentumViewModel momentum,
                                         MovingAverageViewModel movingAverage) {
        return new BacktestView(
                new BacktestViewModel(),
                new BacktestController(new RecordingBoundary()),
                momentum,
                movingAverage);
    }

    @Test
    void momentumParametersUseTheValuesSavedOnTheMomentumScreen() {
        final MomentumViewModel momentum = new MomentumViewModel();
        // Save a configuration through the real Momentum stack, exactly as the screen does.
        new MomentumController(new ConfigureMomentumInteractor(new MomentumPresenter(momentum)))
                .execute("21", "25", "80");

        final double[] used = viewWith(momentum, new MovingAverageViewModel())
                .momentumParameters();

        assertEquals(21.0, used[0]);
        assertEquals(25.0, used[1]);
        assertEquals(80.0, used[2]);
    }

    @Test
    void movingAverageWindowsUseTheValuesSavedOnTheMovingAverageScreen() {
        final MovingAverageViewModel movingAverage = new MovingAverageViewModel();
        new MovingAverageController(
                new ConfigureMovingAverageInteractor(new MovingAveragePresenter(movingAverage)))
                .configure("8", "34");

        final int[] used = viewWith(new MomentumViewModel(), movingAverage)
                .movingAverageWindows();

        assertEquals(8, used[0]);
        assertEquals(34, used[1]);
    }

    @Test
    void unconfiguredStrategiesFallBackToTheirDefaults() {
        // Nothing saved on either screen: the view still produces runnable parameters.
        final BacktestView view = viewWith(new MomentumViewModel(), new MovingAverageViewModel());

        final double[] momentum = view.momentumParameters();
        assertEquals(14.0, momentum[0]);
        assertEquals(30.0, momentum[1]);
        assertEquals(70.0, momentum[2]);

        final int[] movingAverage = view.movingAverageWindows();
        assertEquals(5, movingAverage[0]);
        assertEquals(20, movingAverage[1]);
    }

    @Test
    void theScreenAsksForItsTickerListRatherThanReadingARepository() {
        final RecordingBoundary boundary = new RecordingBoundary();
        new BacktestView(
                new BacktestViewModel(),
                new BacktestController(boundary),
                new MomentumViewModel(),
                new MovingAverageViewModel());

        // Construction alone must populate the chooser: the card is shown for the first time
        // without a componentShown ever having fired.
        assertEquals(1, boundary.loadCalls);
    }

    @Test
    void theChooserIsFilledFromTheViewModelAndTheRunButtonFollowsIt() throws Exception {
        final BacktestViewModel viewModel = new BacktestViewModel();
        final BacktestView view = new BacktestView(
                viewModel,
                new BacktestController(new RecordingBoundary()),
                new MomentumViewModel(),
                new MovingAverageViewModel());

        // Nothing loaded yet: a run is impossible and the screen says why in words.
        assertFalse(buttonNamed(view, "Run backtest").isEnabled());

        viewModel.setAvailableTickers(List.of("AAPL", "NVDA"));
        flushEventQueue();

        assertTrue(buttonNamed(view, "Run backtest").isEnabled());
    }

    @Test
    void pressingRunSendsASymbolAndNumbersAndNothingElse() throws Exception {
        final RecordingBoundary boundary = new RecordingBoundary();
        final BacktestViewModel viewModel = new BacktestViewModel();
        final MovingAverageViewModel movingAverage = new MovingAverageViewModel();
        new MovingAverageController(
                new ConfigureMovingAverageInteractor(new MovingAveragePresenter(movingAverage)))
                .configure("8", "34");

        final BacktestView view = new BacktestView(
                viewModel,
                new BacktestController(boundary),
                new MomentumViewModel(),
                movingAverage);

        viewModel.setAvailableTickers(List.of("AAPL"));
        flushEventQueue();

        buttonNamed(view, "Run backtest").doClick();

        assertNotNull(boundary.lastRun, "the run never reached the boundary");
        assertEquals("AAPL", boundary.lastRun.getTickerSymbol());
        assertEquals(RunBacktestInputData.Strategy.MOVING_AVERAGE_CROSSOVER,
                boundary.lastRun.getStrategy());
        assertEquals(8, boundary.lastRun.getMovingAverageShortWindow());
        assertEquals(34, boundary.lastRun.getMovingAverageLongWindow());
    }

    /**
     * Waits for anything queued with invokeLater to have run.
     *
     * @throws Exception if the wait is interrupted
     */
    private static void flushEventQueue() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    /**
     * Finds the button carrying the given accessible name or exact label.
     *
     * @param root the container to search
     * @param name the name to match
     * @return the button
     */
    private static AbstractButton buttonNamed(Container root, String name) {
        for (AbstractButton button : descendants(root, AbstractButton.class)) {
            if (name.equals(button.getAccessibleContext().getAccessibleName())
                    || name.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("no button named " + name);
    }

    /**
     * Collects every descendant of a given type, depth first.
     *
     * @param root the container to search
     * @param type the component type to collect
     * @param <T> the component type
     * @return the matching descendants, in traversal order
     */
    private static <T> List<T> descendants(Container root, Class<T> type) {
        final List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(descendants(container, type));
            }
        }
        return found;
    }
}
