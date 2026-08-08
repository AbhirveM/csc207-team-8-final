package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import data_access.InMemoryStockRepository;
import entity.MomentumConfiguration;
import entity.MovingAverageConfiguration;
import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.momentum.MomentumController;
import interface_adapter.momentum.MomentumPresenter;
import interface_adapter.momentum.MomentumViewModel;
import interface_adapter.moving_average.MovingAverageController;
import interface_adapter.moving_average.MovingAveragePresenter;
import interface_adapter.moving_average.MovingAverageViewModel;
import use_case.backtest.RunBacktestInputBoundary;
import use_case.backtest.RunBacktestInteractor;
import use_case.momentum.ConfigureMomentumInteractor;
import use_case.moving_average.ConfigureMovingAverageInteractor;
import use_case.watchlist.StockRepository;

/**
 * The parameters a backtest runs with come from the two configuration screens, not from
 * numbers hardcoded in {@link BacktestView}. These tests read the strategy configurations the
 * view would hand to the controller and assert they follow whatever was saved on those screens,
 * falling back to a default only until the user configures one.
 *
 * <p>The view's config-resolution methods are exercised directly (they are package-private for
 * this reason); the project has no other Swing-view tests, so this stays below the interaction
 * layer and needs no display.
 */
class BacktestViewTest {

    /** A backtest controller whose interactor never runs the engine - the run is not under test. */
    private static BacktestController noopController() {
        final RunBacktestInputBoundary noop = inputData -> {
            // intentionally empty: these tests inspect the configuration, not the run
        };
        return new BacktestController(noop);
    }

    private static BacktestView viewWith(MomentumViewModel momentum,
                                         MovingAverageViewModel movingAverage) {
        final StockRepository stockRepository = new InMemoryStockRepository();
        final BacktestViewModel backtestViewModel = new BacktestViewModel();
        return new BacktestView(
                backtestViewModel, noopController(), stockRepository, momentum, movingAverage);
    }

    @Test
    void momentumConfigurationUsesTheValuesSavedOnTheMomentumScreen() {
        final MomentumViewModel momentum = new MomentumViewModel();
        // Save a configuration through the real Momentum stack, exactly as the screen does.
        new MomentumController(new ConfigureMomentumInteractor(new MomentumPresenter(momentum)))
                .execute("21", "25", "80");

        final BacktestView view = viewWith(momentum, new MovingAverageViewModel());
        final MomentumConfiguration used = view.momentumConfiguration();

        assertEquals(21, used.getPeriod());
        assertEquals(25.0, used.getOversoldThreshold());
        assertEquals(80.0, used.getOverboughtThreshold());
        assertSame(momentum.getState().getConfiguration(), used,
                "the view should hand over the saved configuration itself");
    }

    @Test
    void movingAverageConfigurationUsesTheWindowsSavedOnTheMovingAverageScreen() {
        final MovingAverageViewModel movingAverage = new MovingAverageViewModel();
        new MovingAverageController(
                new ConfigureMovingAverageInteractor(new MovingAveragePresenter(movingAverage)))
                .configure("8", "34");

        final BacktestView view = viewWith(new MomentumViewModel(), movingAverage);
        final MovingAverageConfiguration used = view.movingAverageConfiguration();

        assertEquals(8, used.getShortWindow());
        assertEquals(34, used.getLongWindow());
    }

    @Test
    void unconfiguredStrategiesFallBackToTheirDefaults() {
        // Nothing saved on either screen: the view still produces runnable configurations.
        final BacktestView view = viewWith(new MomentumViewModel(), new MovingAverageViewModel());

        final MomentumConfiguration momentum = view.momentumConfiguration();
        assertEquals(14, momentum.getPeriod());
        assertEquals(30.0, momentum.getOversoldThreshold());
        assertEquals(70.0, momentum.getOverboughtThreshold());

        final MovingAverageConfiguration movingAverage = view.movingAverageConfiguration();
        assertEquals(5, movingAverage.getShortWindow());
        assertEquals(20, movingAverage.getLongWindow());
    }
}
