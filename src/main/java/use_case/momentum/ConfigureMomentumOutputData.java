package use_case.momentum;

import java.util.Objects;

import entity.MomentumConfiguration;

/**
 * Data produced after a valid configuration is created.
 */
public final class ConfigureMomentumOutputData {

    private final MomentumConfiguration configuration;

    public ConfigureMomentumOutputData(
            MomentumConfiguration configuration) {
        this.configuration = Objects.requireNonNull(
                configuration,
                "Configuration cannot be null");
    }

    public MomentumConfiguration getConfiguration() {
        return configuration;
    }
}
