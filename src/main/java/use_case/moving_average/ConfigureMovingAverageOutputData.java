package use_case.moving_average;

import java.util.Objects;

import entity.MovingAverageConfiguration;

/**
 * Data produced after a valid configuration is created.
 */
public final class ConfigureMovingAverageOutputData {

    private final MovingAverageConfiguration configuration;

    public ConfigureMovingAverageOutputData(
            MovingAverageConfiguration configuration) {
        this.configuration = Objects.requireNonNull(
                configuration,
                "Configuration cannot be null");
    }

    public MovingAverageConfiguration getConfiguration() {
        return configuration;
    }
}