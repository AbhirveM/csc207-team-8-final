package use_case.moving_average;

/**
 * Input supplied when configuring the Moving Average strategy.
 */
public final class ConfigureMovingAverageInputData {

    private final String shortWindow;
    private final String longWindow;

    public ConfigureMovingAverageInputData(
            String shortWindow,
            String longWindow) {
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
    }

    public String getShortWindow() {
        return shortWindow;
    }

    public String getLongWindow() {
        return longWindow;
    }
}
