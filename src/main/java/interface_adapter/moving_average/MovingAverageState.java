package interface_adapter.moving_average;

public final class MovingAverageState {

    private String shortWindow = "10";
    private String longWindow = "50";

    private Integer configuredShortWindow;
    private Integer configuredLongWindow;

    private String statusMessage = "";
    private boolean configurationSuccessful;

    public String getShortWindow() {
        return shortWindow;
    }

    public void setShortWindow(String shortWindow) {
        this.shortWindow = shortWindow;
    }

    public String getLongWindow() {
        return longWindow;
    }

    public void setLongWindow(String longWindow) {
        this.longWindow = longWindow;
    }

    public Integer getConfiguredShortWindow() {
        return configuredShortWindow;
    }

    public Integer getConfiguredLongWindow() {
        return configuredLongWindow;
    }

    public void setConfiguredWindows(int shortWindow, int longWindow) {
        this.configuredShortWindow = shortWindow;
        this.configuredLongWindow = longWindow;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public boolean isConfigurationSuccessful() {
        return configurationSuccessful;
    }

    public void setConfigurationSuccessful(
            boolean configurationSuccessful) {
        this.configurationSuccessful = configurationSuccessful;
    }

    public void clearConfiguredWindows() {
        this.configuredShortWindow = null;
        this.configuredLongWindow = null;
    }
}