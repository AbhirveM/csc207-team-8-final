package use_case.momentum;

/**
 * Input supplied when configuring the Momentum strategy.
 */
public final class ConfigureMomentumInputData {

    private final String period;
    private final String oversoldThreshold;
    private final String overboughtThreshold;

    public ConfigureMomentumInputData(
            String period,
            String oversoldThreshold,
            String overboughtThreshold) {
        this.period = period;
        this.oversoldThreshold = oversoldThreshold;
        this.overboughtThreshold = overboughtThreshold;
    }

    public String getPeriod() {
        return period;
    }

    public String getOversoldThreshold() {
        return oversoldThreshold;
    }

    public String getOverboughtThreshold() {
        return overboughtThreshold;
    }
}