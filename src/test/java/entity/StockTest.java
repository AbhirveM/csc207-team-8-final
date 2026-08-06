package entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockTest {

    private static final Ticker AAPL = new Ticker("AAPL", "Apple Inc.");

    private static DailyPrice priceOn(String date, double close) {
        return new DailyPrice(LocalDate.parse(date), close, close, close, close, 1_000L);
    }

    private static List<DailyPrice> threeAscendingDays() {
        return new ArrayList<>(List.of(
                priceOn("2026-08-03", 100.0),
                priceOn("2026-08-04", 101.0),
                priceOn("2026-08-05", 102.0)));
    }

    @Test
    void constructorKeepsPricesOldestToNewest() {
        Stock stock = new Stock(AAPL, threeAscendingDays());

        assertEquals(3, stock.getPriceCount());
        assertEquals(LocalDate.parse("2026-08-03"), stock.getDailyPrices().get(0).getDate());
        assertEquals(LocalDate.parse("2026-08-05"), stock.getDailyPrices().get(2).getDate());
    }

    @Test
    void constructorCopiesTheListDefensively() {
        List<DailyPrice> mutable = threeAscendingDays();
        Stock stock = new Stock(AAPL, mutable);

        mutable.clear();

        assertEquals(3, stock.getPriceCount(), "Mutating the caller's list must not affect the Stock");
    }

    @Test
    void getDailyPricesIsUnmodifiable() {
        Stock stock = new Stock(AAPL, threeAscendingDays());

        assertThrows(UnsupportedOperationException.class,
                () -> stock.getDailyPrices().add(priceOn("2026-08-06", 103.0)));
    }

    @Test
    void constructorRejectsNullTicker() {
        assertThrows(NullPointerException.class, () -> new Stock(null, threeAscendingDays()));
    }

    @Test
    void constructorRejectsNullPriceList() {
        assertThrows(NullPointerException.class, () -> new Stock(AAPL, null));
    }

    @Test
    void constructorRejectsNullPriceElement() {
        assertThrows(NullPointerException.class,
                () -> new Stock(AAPL, Arrays.asList(priceOn("2026-08-03", 100.0), null)));
    }

    @Test
    void constructorRejectsBlankSymbol() {
        assertThrows(IllegalArgumentException.class,
                () -> new Stock(new Ticker("   ", "Blank"), threeAscendingDays()));
    }

    @Test
    void constructorRejectsNewestToOldestOrder() {
        List<DailyPrice> descending = List.of(
                priceOn("2026-08-05", 102.0),
                priceOn("2026-08-04", 101.0));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new Stock(AAPL, descending));
        assertTrue(thrown.getMessage().contains("oldest to newest"));
    }

    @Test
    void constructorRejectsDuplicateDates() {
        List<DailyPrice> duplicated = List.of(
                priceOn("2026-08-04", 101.0),
                priceOn("2026-08-04", 101.5));

        assertThrows(IllegalArgumentException.class, () -> new Stock(AAPL, duplicated));
    }

    @Test
    void constructorRejectsNullDate() {
        List<DailyPrice> withNullDate = List.of(
                priceOn("2026-08-04", 101.0),
                new DailyPrice(null, 1, 1, 1, 1, 1L));

        assertThrows(IllegalArgumentException.class, () -> new Stock(AAPL, withNullDate));
    }

    @Test
    void emptyHistoryIsAllowed() {
        Stock stock = new Stock(AAPL, List.of());

        assertEquals(0, stock.getPriceCount());
        assertTrue(stock.getLatestPrice().isEmpty());
        assertTrue(stock.getEarliestPrice().isEmpty());
    }

    @Test
    void latestAndEarliestPriceReflectTheOrdering() {
        Stock stock = new Stock(AAPL, threeAscendingDays());

        assertEquals(LocalDate.parse("2026-08-05"), stock.getLatestPrice().orElseThrow().getDate());
        assertEquals(LocalDate.parse("2026-08-03"), stock.getEarliestPrice().orElseThrow().getDate());
    }

    @Test
    void delegatesSymbolAndCompanyNameToTheTicker() {
        Stock stock = new Stock(AAPL, List.of());

        assertEquals("AAPL", stock.getSymbol());
        assertEquals("Apple Inc.", stock.getCompanyName());
    }

    @Test
    void withDailyPricesReplacesHistoryAndKeepsTheTicker() {
        Stock original = new Stock(AAPL, threeAscendingDays());

        Stock refreshed = original.withDailyPrices(List.of(priceOn("2026-08-06", 200.0)));

        assertEquals(1, refreshed.getPriceCount());
        assertEquals("Apple Inc.", refreshed.getCompanyName());
        assertEquals(3, original.getPriceCount(), "The original must be untouched");
    }

    @Test
    void withCompanyNameReplacesTheNameAndKeepsHistory() {
        Stock original = new Stock(new Ticker("AAPL", null), threeAscendingDays());

        Stock named = original.withCompanyName("Apple Inc.");

        assertEquals("Apple Inc.", named.getCompanyName());
        assertEquals(3, named.getPriceCount());
    }

    @Test
    void equalsComparesTickerAndHistory() {
        Stock left = new Stock(AAPL, threeAscendingDays());
        Stock right = new Stock(AAPL, threeAscendingDays());
        Stock shorter = new Stock(AAPL, List.of(priceOn("2026-08-03", 100.0)));

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, shorter);
        assertFalse(left.equals((Object) "not a stock"));
    }

    @Test
    void toStringDescribesTheHistoryRange() {
        assertTrue(new Stock(AAPL, threeAscendingDays()).toString().contains("2026-08-03..2026-08-05"));
        assertTrue(new Stock(AAPL, List.of()).toString().contains("no price history"));
    }
}
