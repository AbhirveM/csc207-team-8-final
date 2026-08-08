package use_case.momentum;

import java.util.Objects;

import entity.MomentumConfiguration;

/**
 * Creates a Momentum configuration from user-supplied values.
 */
public final class ConfigureMomentumInteractor
        implements ConfigureMomentumInputBoundary {

    private final ConfigureMomentumOutputBoundary presenter;

    public ConfigureMomentumInteractor(
            ConfigureMomentumOutputBoundary presenter) {
        this.presenter = Objects.requireNonNull(
                presenter,
                "Presenter cannot be null");
    }

    @Override
    public void execute(ConfigureMomentumInputData inputData) {

        if (inputData == null) {
            presenter.prepareFailView(
                    "Configuration input cannot be null");
            return;
        }

        final MomentumConfiguration configuration;

        try {
            final int period = parsePeriod(
                    inputData.getPeriod());

            final double oversoldThreshold = parseThreshold(
                    inputData.getOversoldThreshold(),
                    "Oversold");

            final double overboughtThreshold = parseThreshold(
                    inputData.getOverboughtThreshold(),
                    "Overbought");

            configuration = new MomentumConfiguration(
                    period,
                    oversoldThreshold,
                    overboughtThreshold);
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView(exception.getMessage());
            return;
        }

        presenter.prepareSuccessView(
                new ConfigureMomentumOutputData(configuration));
    }

    /**
     * Converts the supplied RSI period into an integer.
     *
     * @param value the supplied text
     * @return the parsed integer
     */
    private int parsePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "RSI period is required");
        }

        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "RSI period must be a whole number");
        }
    }

    /**
     * Converts a supplied RSI threshold into a double.
     *
     * @param value the supplied text
     * @param thresholdName the name used in an error message
     * @return the parsed threshold
     */
    private double parseThreshold(
            String value,
            String thresholdName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    thresholdName + " threshold is required");
        }

        try {
            return Double.parseDouble(value.trim());
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    thresholdName + " threshold must be a number");
        }
    }
}