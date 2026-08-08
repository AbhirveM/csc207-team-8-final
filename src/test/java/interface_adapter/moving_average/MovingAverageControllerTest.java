package interface_adapter.moving_average;

import org.junit.jupiter.api.Test;
import use_case.moving_average.ConfigureMovingAverageInputBoundary;
import use_case.moving_average.ConfigureMovingAverageInputData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MovingAverageControllerTest {

    @Test
    void configurePassesRawValuesToInteractor() {
        final RecordingInteractor interactor =
                new RecordingInteractor();

        final MovingAverageController controller =
                new MovingAverageController(interactor);

        controller.configure(" 10 ", "50");

        assertNotNull(interactor.receivedInput);
        assertEquals(
                " 10 ",
                interactor.receivedInput.getShortWindow());
        assertEquals(
                "50",
                interactor.receivedInput.getLongWindow());
    }

    @Test
    void constructorRejectsNullInteractor() {
        assertThrows(
                NullPointerException.class,
                () -> new MovingAverageController(null));
    }

    private static final class RecordingInteractor
            implements ConfigureMovingAverageInputBoundary {

        private ConfigureMovingAverageInputData receivedInput;

        @Override
        public void execute(
                ConfigureMovingAverageInputData inputData) {

            receivedInput = inputData;
        }
    }
}