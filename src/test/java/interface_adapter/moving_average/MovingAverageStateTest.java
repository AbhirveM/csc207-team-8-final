package interface_adapter.moving_average;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MovingAverageStateTest {

    @Test
    void initialStateContainsExpectedDefaults() {
        final MovingAverageState state = new MovingAverageState();

        assertEquals("10", state.getShortWindow());
        assertEquals("50", state.getLongWindow());
        assertNull(state.getConfiguredShortWindow());
        assertNull(state.getConfiguredLongWindow());
        assertEquals("", state.getStatusMessage());
        assertFalse(state.isConfigurationSuccessful());
    }

    @Test
    void clearsValidatedResultWithoutClearingEditableInput() {
        final MovingAverageState state = new MovingAverageState();

        state.setShortWindow("abc");
        state.setLongWindow("50");
        state.setConfiguredWindows(10, 50);
        state.setStatusMessage("Configured");
        state.setConfigurationSuccessful(true);

        state.clearConfiguredWindows();

        assertEquals("abc", state.getShortWindow());
        assertEquals("50", state.getLongWindow());
        assertNull(state.getConfiguredShortWindow());
        assertNull(state.getConfiguredLongWindow());
        assertEquals("Configured", state.getStatusMessage());
        assertTrue(state.isConfigurationSuccessful());
    }
}