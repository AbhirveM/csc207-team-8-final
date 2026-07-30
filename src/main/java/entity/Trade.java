package entity;

import java.io.Serializable;
import java.time.LocalDate;

public class Trade implements Serializable {
    private final Ticker ticker;
    private final LocalDate entryDate;
    private final double entryPrice;
    private final LocalDate exitDate;
    private final double exitPrice;

    public Trade(Ticker ticker, LocalDate entryDate, double entryPrice, LocalDate exitDate, double exitPrice) {
        this.ticker = ticker;
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
    }

    public Ticker getTicker() { return ticker; }
    public LocalDate getEntryDate() { return entryDate; }
    public double getEntryPrice() { return entryPrice; }
    public LocalDate getExitDate() { return exitDate; }
    public double getExitPrice() { return exitPrice; }

    public double getReturnPercent() {
        return ((exitPrice - entryPrice) / entryPrice) * 100.0;
    }
}
