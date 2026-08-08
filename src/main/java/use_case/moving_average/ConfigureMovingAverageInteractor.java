package use_case.moving_average;

import java.util.Objects;

import entity.MovingAverageConfiguration;

/**
 * Creates a Moving Average configuration from user-supplied values.
 */
public final class ConfigureMovingAverageInteractor
        implements ConfigureMovingAverageInputBoundary {

    private final ConfigureMovingAverageOutputBoundary presenter;

    public ConfigureMovingAverageInteractor(
            ConfigureMovingAverageOutputBoundary presenter) {
        this.presenter = Objects.requireNonNull(
                presenter,
                "Presenter cannot be null");
    }

    @Override
    public void execute(
            ConfigureMovingAverageInputData inputData) {

        if (inputData == null) {
            presenter.prepareFailView(
                    "Configuration input cannot be null");
            return;
        }

        final MovingAverageConfiguration configuration;

        try {
            final int shortWindow = parseWindow(
                    inputData.getShortWindow(),
                    "Short");

            final int longWindow = parseWindow(
                    inputData.getLongWindow(),
                    "Long");

            configuration = new MovingAverageConfiguration(
                    shortWindow,
                    longWindow);
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        presenter.prepareSuccessView(
                new ConfigureMovingAverageOutputData(configuration));
    }

    /**
     * Converts a supplied window value into an integer.
     *
     * @param value the supplied text
     * @param windowName the name used in an error message
     * @return the parsed integer
     */
    private int parseWindow(
            String value,
            String windowName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    windowName + " window is required");
        }

        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    windowName + " window must be a whole number");
        }
    }
}
