package entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradingSignalTest {

    @Test
    void storesDateAndBuySignalType() {
        final LocalDate date = LocalDate.of(2026, 12, 4);

        final TradingSignal signal = new TradingSignal(date, SignalType.BUY);

        assertEquals(date, signal.getDate());
        assertEquals(SignalType.BUY, signal.getSignalType());
    }

    @Test
    void storesSellSignalType() {
        final TradingSignal signal = new TradingSignal(LocalDate.of(2026, 12, 4),
                SignalType.SELL);

        assertEquals(SignalType.SELL, signal.getSignalType());


    }
    @Test
    void storesHoldSignalType() {
        final TradingSignal signal = new TradingSignal(
                LocalDate.of(2026, 8, 3),
                SignalType.HOLD);

        assertEquals(SignalType.HOLD, signal.getSignalType());
    }

    @Test
    void rejectsNullDate() {
        assertThrows(NullPointerException.class,
                () -> new TradingSignal(null, SignalType.BUY));
    }

    @Test
    void rejectsNullSignalType() {
        final LocalDate date = LocalDate.of(2026, 8, 3);

        assertThrows(NullPointerException.class,
                () -> new TradingSignal(date, null));
    }
}