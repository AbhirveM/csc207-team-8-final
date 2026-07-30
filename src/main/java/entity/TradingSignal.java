package entity;

import java.io.Serializable;
import java.time.LocalDate;

public class TradingSignal implements Serializable {
    public enum Action { BUY, SELL }

    private final LocalDate date;
    private final Action action;
    private final Ticker ticker;

    public TradingSignal(LocalDate date, Action action, Ticker ticker) {
        this.date = date;
        this.action = action;
        this.ticker = ticker;
    }

    public LocalDate getDate() { return date; }
    public Action getAction() { return action; }
    public Ticker getTicker() { return ticker; }
}
