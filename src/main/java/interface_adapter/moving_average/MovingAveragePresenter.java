package interface_adapter.moving_average;

import java.util.Objects;

import use_case.moving_average.ConfigureMovingAverageOutputBoundary;
import use_case.moving_average.ConfigureMovingAverageOutputData;

/**
 * Converts Moving Average use-case results into UI-ready state.
 */
public final class MovingAveragePresenter
        implements ConfigureMovingAverageOutputBoundary {

    private final MovingAverageViewModel viewModel;

    public MovingAveragePresenter(
            MovingAverageViewModel viewModel) {

        this.viewModel = Objects.requireNonNull(
                viewModel,
                "View model cannot be null");
    }

    @Override
    public void prepareSuccessView(
            ConfigureMovingAverageOutputData outputData) {

        Objects.requireNonNull(
                outputData,
                "Output data cannot be null");

        final int shortWindow = outputData
                .getConfiguration()
                .getShortWindow();

        final int longWindow = outputData
                .getConfiguration()
                .getLongWindow();

        final MovingAverageState state = viewModel.getState();

        state.setShortWindow(Integer.toString(shortWindow));
        state.setLongWindow(Integer.toString(longWindow));
        state.setConfiguredWindows(shortWindow, longWindow);
        state.setStatusMessage(
                "Moving Average configuration updated successfully.");
        state.setConfigurationSuccessful(true);

        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        Objects.requireNonNull(
                errorMessage,
                "Error message cannot be null");

        final MovingAverageState state = viewModel.getState();

        state.setStatusMessage(errorMessage);
        state.setConfigurationSuccessful(false);

        viewModel.firePropertyChanged();
    }
}
