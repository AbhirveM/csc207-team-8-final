package interface_adapter.moving_average;

import java.util.Objects;

import use_case.moving_average.ConfigureMovingAverageInputBoundary;
import use_case.moving_average.ConfigureMovingAverageInputData;

/**
 * Controller for configuring the Moving Average strategy.
 */
public class MovingAverageController {
    private final ConfigureMovingAverageInputBoundary interactor;

    public MovingAverageController(ConfigureMovingAverageInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(interactor, "Interactor cannot be null");
    }

    /**
     * Sends the user-entered window values to the configuration use case.
     *
     * @param shortWindow the short-window text
     * @param longWindow the long-Window text
     */
    public void configure(String shortWindow, String longWindow) {
        final ConfigureMovingAverageInputData inputData = new ConfigureMovingAverageInputData(shortWindow, longWindow);

        interactor.execute(inputData);
    }

}
