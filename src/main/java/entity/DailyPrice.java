package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One day of OHLCV price data for a ticker.
 * (Matches the shared entity name from the team's Member Responsibilities doc.)
 */
public class DailyPrice implements Serializable {
    private final LocalDate date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;

    public DailyPrice(LocalDate date, double open, double high, double low, double close, long volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public LocalDate getDate() { return date; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }

    /*
     * equals/hashCode/toString are additive only - the constructor deliberately
     * still accepts any values. Adding validation here would break existing
     * strategy tests that construct throwaway prices; provider responses are
     * validated in the market-data gateway instead.
     */

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DailyPrice)) {
            return false;
        }
        DailyPrice that = (DailyPrice) other;
        return Double.compare(open, that.open) == 0
                && Double.compare(high, that.high) == 0
                && Double.compare(low, that.low) == 0
                && Double.compare(close, that.close) == 0
                && volume == that.volume
                && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, open, high, low, close, volume);
    }

    @Override
    public String toString() {
        return "DailyPrice{" + date
                + ", open=" + open
                + ", high=" + high
                + ", low=" + low
                + ", close=" + close
                + ", volume=" + volume + '}';
    }
}
