package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class TradingSignal implements Serializable {

    public enum Action { BUY, SELL }

    private final LocalDate date;
//    private final Action action;
//    private final Ticker ticker;
    private final SignalType signalType;

    public TradingSignal(LocalDate date, SignalType signalType) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.signalType = Objects.requireNonNull(
                signalType, "Signal type cannot be null");
    }

    public LocalDate getDate() { return date; }
//    public Action getAction() { return action; }
//    public Ticker getTicker() { return ticker; }
    public SignalType getSignalType() { return signalType; }
}
