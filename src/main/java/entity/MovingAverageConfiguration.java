package entity;

import java.io.Serializable;

public class MovingAverageConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int shortWindow;
    private final int longWindow;

    public MovingAverageConfiguration(int shortWindow, int longWindow) {
        if (shortWindow <= 0) {
            throw new IllegalArgumentException(
                    "Short window value must be positive");
        }

        if (longWindow <= 0) {
            throw new IllegalArgumentException(
                    "Long window value must be positive");
        }

        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException(
                    "Long window must be greater than short window");
        }

        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
    }

    public int getShortWindow() {
        return shortWindow;
    }

    public int getLongWindow() {
        return longWindow;
    }
}