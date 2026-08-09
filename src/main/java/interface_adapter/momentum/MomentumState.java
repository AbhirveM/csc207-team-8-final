package interface_adapter.momentum;

import entity.MomentumConfiguration;

/**
 * Stores the current state of the Momentum configuration view.
 */
public final class MomentumState {

    private MomentumConfiguration configuration;
    private String errorMessage;

    public MomentumConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MomentumConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
