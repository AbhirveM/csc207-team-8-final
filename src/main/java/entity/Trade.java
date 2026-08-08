package entity;

import java.io.Serializable;
import java.time.LocalDate;

public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Ticker ticker;
    private final LocalDate entryDate;
    private final double entryPrice;
    private final LocalDate exitDate;
    private final double exitPrice;
    private final int quantity;

    /**
     * Existing constructor kept for backwards compatibility.
     */
    public Trade(
            Ticker ticker,
            LocalDate entryDate,
            double entryPrice,
            LocalDate exitDate,
            double exitPrice) {

        this(
                ticker,
                entryDate,
                entryPrice,
                exitDate,
                exitPrice,
                0);
    }

    /**
     * Creates a completed trade including the number of shares traded.
     */
    public Trade(
            Ticker ticker,
            LocalDate entryDate,
            double entryPrice,
            LocalDate exitDate,
            double exitPrice,
            int quantity) {

        this.ticker = ticker;
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public double getExitPrice() {
        return exitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getReturnPercent() {
        return ((exitPrice - entryPrice) / entryPrice) * 100.0;
    }
}