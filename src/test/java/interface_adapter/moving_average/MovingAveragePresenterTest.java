package interface_adapter.moving_average;

import java.util.concurrent.atomic.AtomicInteger;

import entity.MovingAverageConfiguration;
import org.junit.jupiter.api.Test;
import use_case.moving_average.ConfigureMovingAverageOutputData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MovingAveragePresenterTest {

    @Test
    void successUpdatesStateAndNotifiesView() {
        final MovingAverageViewModel viewModel =
                new MovingAverageViewModel();

        final AtomicInteger notifications = new AtomicInteger();

        viewModel.addPropertyChangeListener(event -> {
            assertEquals(
                    MovingAverageViewModel.STATE_PROPERTY,
                    event.getPropertyName());
            assertSame(viewModel.getState(), event.getNewValue());
            notifications.incrementAndGet();
        });

        final MovingAveragePresenter presenter =
                new MovingAveragePresenter(viewModel);

        final MovingAverageConfiguration configuration =
                new MovingAverageConfiguration(10, 50);

        presenter.prepareSuccessView(
                new ConfigureMovingAverageOutputData(configuration));

        final MovingAverageState state = viewModel.getState();

        assertEquals("10", state.getShortWindow());
        assertEquals("50", state.getLongWindow());
        assertEquals(
                Integer.valueOf(10),
                state.getConfiguredShortWindow());
        assertEquals(
                Integer.valueOf(50),
                state.getConfiguredLongWindow());
        assertEquals(
                "Moving Average configuration updated successfully.",
                state.getStatusMessage());
        assertTrue(state.isConfigurationSuccessful());
        assertEquals(1, notifications.get());
    }

    @Test
    void failurePreservesInputClearsResultAndNotifiesView() {
        final MovingAverageViewModel viewModel =
                new MovingAverageViewModel();

        final MovingAverageState state = viewModel.getState();
        state.setShortWindow("abc");
        state.setLongWindow("50");
        state.setConfiguredWindows(10, 50);
        state.setConfigurationSuccessful(true);

        final AtomicInteger notifications = new AtomicInteger();

        viewModel.addPropertyChangeListener(
                event -> notifications.incrementAndGet());

        final MovingAveragePresenter presenter =
                new MovingAveragePresenter(viewModel);

        presenter.prepareFailView(
                "Window values must be whole numbers.");

        assertEquals("abc", state.getShortWindow());
        assertEquals("50", state.getLongWindow());
        assertNull(state.getConfiguredShortWindow());
        assertNull(state.getConfiguredLongWindow());
        assertEquals(
                "Window values must be whole numbers.",
                state.getStatusMessage());
        assertFalse(state.isConfigurationSuccessful());
        assertEquals(1, notifications.get());
    }

    @Test
    void constructorRejectsNullViewModel() {
        assertThrows(
                NullPointerException.class,
                () -> new MovingAveragePresenter(null));
    }

    @Test
    void presentationMethodsRejectNullArguments() {
        final MovingAveragePresenter presenter =
                new MovingAveragePresenter(
                        new MovingAverageViewModel());

        assertThrows(
                NullPointerException.class,
                () -> presenter.prepareSuccessView(null));

        assertThrows(
                NullPointerException.class,
                () -> presenter.prepareFailView(null));
    }
}