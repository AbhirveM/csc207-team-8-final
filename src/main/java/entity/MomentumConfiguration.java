package entity;

import java.io.Serializable;

/**
 * Stores the configuration values used by the momentum RSI strategy.
 */
public class MomentumConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int rsiPeriod;
    private final double buyThreshold;
    private final double sellThreshold;

    public MomentumConfiguration(int rsiPeriod,
                                 double buyThreshold,
                                 double sellThreshold) {
        if (rsiPeriod <= 1) {
            throw new IllegalArgumentException(
                    "RSI period must be greater than 1");
        }

        if (buyThreshold < 0 || buyThreshold > 100) {
            throw new IllegalArgumentException(
                    "Buy threshold must be between 0 and 100");
        }

        if (sellThreshold < 0 || sellThreshold > 100) {
            throw new IllegalArgumentException(
                    "Sell threshold must be between 0 and 100");
        }

        if (buyThreshold >= sellThreshold) {
            throw new IllegalArgumentException(
                    "Sell threshold must be greater than buy threshold");
        }

        this.rsiPeriod = rsiPeriod;
        this.buyThreshold = buyThreshold;
        this.sellThreshold = sellThreshold;
    }

    public int getRsiPeriod() {
        return rsiPeriod;
    }

    public double getBuyThreshold() {
        return buyThreshold;
    }

    public double getSellThreshold() {
        return sellThreshold;
    }
}
