package interface_adapter.momentum;

/**
 * Stores the current state of the Momentum configuration view.
 *
 * <p>The saved parameters are held as numbers rather than as a {@code MomentumConfiguration}.
 * A view model is what the Frameworks &amp; Drivers layer reads, so anything it holds is
 * reachable from a screen; keeping the entity out of it is what lets the backtest screen
 * read the user's saved settings without importing {@code entity}. The entity is unpacked
 * one layer earlier, by {@link MomentumPresenter}.
 *
 * <p>Every parameter is boxed and starts null, so "not configured yet" is distinguishable
 * from a configured zero - the backtest screen falls back to its default only in the first
 * case. This mirrors {@code MovingAverageState}.
 */
public final class MomentumState {

    private Integer configuredPeriod;
    private Double configuredOversoldThreshold;
    private Double configuredOverboughtThreshold;
    private String errorMessage;

    public Integer getConfiguredPeriod() {
        return configuredPeriod;
    }

    public Double getConfiguredOversoldThreshold() {
        return configuredOversoldThreshold;
    }

    public Double getConfiguredOverboughtThreshold() {
        return configuredOverboughtThreshold;
    }

    /**
     * Records the parameters of a configuration the user successfully saved.
     *
     * @param period              the RSI period
     * @param oversoldThreshold   the RSI value at or below which the strategy buys
     * @param overboughtThreshold the RSI value at or above which the strategy sells
     */
    public void setConfiguration(int period,
                                 double oversoldThreshold,
                                 double overboughtThreshold) {
        this.configuredPeriod = period;
        this.configuredOversoldThreshold = oversoldThreshold;
        this.configuredOverboughtThreshold = overboughtThreshold;
    }

    /** Forgets any saved configuration, returning the screen to its unconfigured state. */
    public void clearConfiguration() {
        this.configuredPeriod = null;
        this.configuredOversoldThreshold = null;
        this.configuredOverboughtThreshold = null;
    }

    /**
     * Whether the user has successfully saved a configuration.
     *
     * @return true once all three parameters have been recorded
     */
    public boolean isConfigured() {
        return configuredPeriod != null
                && configuredOversoldThreshold != null
                && configuredOverboughtThreshold != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
