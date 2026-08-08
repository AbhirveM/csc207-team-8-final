package entity;

import java.io.Serializable;

/**
 * Stores configuration values for the RSI Momentum strategy.
 */
public class MomentumConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int period;
    private final double oversoldThreshold;
    private final double overboughtThreshold;

    public MomentumConfiguration(int period,
                                 double oversoldThreshold,
                                 double overboughtThreshold) {
        if (period <= 1) {
            throw new IllegalArgumentException(
                    "RSI period must be greater than 1");
        }

        if (Double.isNaN(oversoldThreshold)
                || oversoldThreshold < 0
                || oversoldThreshold > 100) {
            throw new IllegalArgumentException(
                    "Oversold threshold must be between 0 and 100");
        }

        if (Double.isNaN(overboughtThreshold)
                || overboughtThreshold < 0
                || overboughtThreshold > 100) {
            throw new IllegalArgumentException(
                    "Overbought threshold must be between 0 and 100");
        }

        if (oversoldThreshold >= overboughtThreshold) {
            throw new IllegalArgumentException(
                    "Oversold threshold must be smaller than overbought threshold");
        }

        this.period = period;
        this.oversoldThreshold = oversoldThreshold;
        this.overboughtThreshold = overboughtThreshold;
    }

    public int getPeriod() {
        return period;
    }

    public double getOversoldThreshold() {
        return oversoldThreshold;
    }

    public double getOverboughtThreshold() {
        return overboughtThreshold;
    }
}
