package interface_adapter.momentum;

import java.util.Objects;

import use_case.momentum.ConfigureMomentumOutputBoundary;
import use_case.momentum.ConfigureMomentumOutputData;

/**
 * Presenter for the Momentum strategy configuration.
 */
public final class MomentumPresenter
        implements ConfigureMomentumOutputBoundary {

    private final MomentumViewModel viewModel;

    public MomentumPresenter(MomentumViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(
                viewModel,
                "View model cannot be null");
    }

    @Override
    public void prepareSuccessView(
            ConfigureMomentumOutputData outputData) {

        final MomentumState state = viewModel.getState();

        state.setConfiguration(outputData.getConfiguration());
        state.setErrorMessage(null);

        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {

        final MomentumState state = viewModel.getState();

        state.setConfiguration(null);
        state.setErrorMessage(errorMessage);

        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}