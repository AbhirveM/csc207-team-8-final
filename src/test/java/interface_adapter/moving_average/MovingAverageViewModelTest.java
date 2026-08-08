package interface_adapter.moving_average;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class MovingAverageViewModelTest {

    @Test
    void initialStateContainsExpectedDefaults() {
        final MovingAverageViewModel viewModel =
                new MovingAverageViewModel();

        final MovingAverageState state = viewModel.getState();

        assertEquals("10", state.getShortWindow());
        assertEquals("50", state.getLongWindow());
        assertNull(state.getConfiguredShortWindow());
        assertNull(state.getConfiguredLongWindow());
        assertEquals("", state.getStatusMessage());
        assertFalse(state.isConfigurationSuccessful());
    }

    @Test
    void firePropertyChangedNotifiesListener() {
        final MovingAverageViewModel viewModel =
                new MovingAverageViewModel();

        final AtomicReference<PropertyChangeEvent> receivedEvent =
                new AtomicReference<>();

        viewModel.addPropertyChangeListener(receivedEvent::set);
        viewModel.firePropertyChanged();

        assertEquals(
                MovingAverageViewModel.STATE_PROPERTY,
                receivedEvent.get().getPropertyName());
        assertNull(receivedEvent.get().getOldValue());
        assertSame(
                viewModel.getState(),
                receivedEvent.get().getNewValue());
    }

    @Test
    void removedListenerIsNotNotified() {
        final MovingAverageViewModel viewModel =
                new MovingAverageViewModel();

        final AtomicInteger notifications = new AtomicInteger();

        final PropertyChangeListener listener =
                event -> notifications.incrementAndGet();

        viewModel.addPropertyChangeListener(listener);
        viewModel.removePropertyChangeListener(listener);
        viewModel.firePropertyChanged();

        assertEquals(0, notifications.get());
    }
}