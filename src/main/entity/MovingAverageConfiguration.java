package entity;


/*
The overall class is responsible for storing the user's Strategy settings.


* shortWindow: average of a smaller number of recent closing prices, such as 10 trading days.
  longWindow: average of a larger number, such as 50 trading days.
*
* */
public class MovingAverageConfiguration {

    private final int shortWindow;
    private final int longWindow;

    public MovingAverageConfiguration(int shortWindow, int longWindow) {
        if (shortWindow <= 0) {
            throw new IllegalArgumentException("Short window value must be positive");
        }

        if (longWindow <= 0) {
            throw new IllegalArgumentException("Long window value must be positive");
        }

        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("Long window must be greater than short window");
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
