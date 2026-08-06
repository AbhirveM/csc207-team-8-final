package data_access;

import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStockRepositoryTest {

    private InMemoryStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository();
    }

    private static Stock stock(String symbol, int priceCount) {
        final List<DailyPrice> prices = new ArrayList<>();
        final LocalDate date = LocalDate.of(2026, 1, 1);
        for (int index = 0; index < priceCount; index++) {
            prices.add(new DailyPrice(date.plusDays(index), 10, 10, 10, 10, 1L));
        }
        return new Stock(new Ticker(symbol, symbol + " Inc."), prices);
    }

    @Test
    void savedStockCanBeFoundBySymbol() {
        repository.save(stock("AAPL", 3));

        assertEquals(3, repository.findBySymbol("AAPL").orElseThrow().getPriceCount());
    }

    @Test
    void lookupIsCaseInsensitive() {
        repository.save(stock("AAPL", 1));

        assertTrue(repository.findBySymbol("aapl").isPresent());
        assertTrue(repository.findBySymbol("AaPl").isPresent());
    }

    @Test
    void saveReplacesAnExistingEntryForTheSameSymbol() {
        repository.save(stock("AAPL", 1));
        repository.save(stock("AAPL", 5));

        assertEquals(1, repository.findAll().size());
        assertEquals(5, repository.findBySymbol("AAPL").orElseThrow().getPriceCount());
    }

    @Test
    void findBySymbolIsEmptyForUnknownOrNullSymbols() {
        assertTrue(repository.findBySymbol("MSFT").isEmpty());
        assertTrue(repository.findBySymbol(null).isEmpty());
    }

    @Test
    void removeDeletesTheEntry() {
        repository.save(stock("AAPL", 1));

        repository.remove("aapl");

        assertTrue(repository.findBySymbol("AAPL").isEmpty());
    }

    @Test
    void removeIsANoOpForUnknownAndNullSymbols() {
        repository.save(stock("AAPL", 1));

        repository.remove("MSFT");
        repository.remove(null);

        assertEquals(1, repository.findAll().size());
    }

    @Test
    void findAllIsSortedBySymbolAndUnmodifiable() {
        repository.save(stock("TSLA", 1));
        repository.save(stock("AAPL", 1));
        repository.save(stock("MSFT", 1));

        final List<Stock> all = repository.findAll();

        assertEquals(List.of("AAPL", "MSFT", "TSLA"),
                all.stream().map(Stock::getSymbol).toList());
        assertThrows(UnsupportedOperationException.class, () -> all.add(stock("NVDA", 1)));
    }

    /**
     * Nothing in Member 1's watchlist slice calls findAll - it is declared by
     * StockRepository as the hand-off surface for Members 2 and 3's backtesting, so it
     * is kept and tested rather than deleted as dead code.
     */
    @Test
    void findAllIsEmptyForAFreshRepository() {
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void findAllReflectsRemovalsAndIsCaseInsensitiveAboutThem() {
        repository.save(stock("AAPL", 1));
        repository.save(stock("MSFT", 1));

        repository.remove("aapl");

        assertEquals(List.of("MSFT"), repository.findAll().stream().map(Stock::getSymbol).toList());
    }

    /** The returned list is a snapshot: later saves must not appear in an earlier one. */
    @Test
    void findAllReturnsASnapshotRatherThanALiveView() {
        repository.save(stock("AAPL", 1));
        final List<Stock> snapshot = repository.findAll();

        repository.save(stock("MSFT", 1));

        assertEquals(1, snapshot.size());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void saveRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}
