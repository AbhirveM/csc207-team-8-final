package interface_adapter.momentum;

import java.util.Objects;

import entity.MomentumConfiguration;
import use_case.momentum.ConfigureMomentumOutputBoundary;
import use_case.momentum.ConfigureMomentumOutputData;

/**
 * Presenter for the Momentum strategy configuration.
 *
 * <p>This is where the configuration entity stops. {@link MomentumState} holds the three
 * numbers unpacked here, so nothing further out - including the backtest screen, which
 * reads the saved parameters - has to know {@code MomentumConfiguration} exists.
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
        final MomentumConfiguration configuration = outputData.getConfiguration();

        state.setConfiguration(
                configuration.getPeriod(),
                configuration.getOversoldThreshold(),
                configuration.getOverboughtThreshold());
        state.setErrorMessage(null);

        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {

        final MomentumState state = viewModel.getState();

        state.clearConfiguration();
        state.setErrorMessage(errorMessage);

        viewModel.setState(state);
        viewModel.firePropertyChanged();
    }
}
