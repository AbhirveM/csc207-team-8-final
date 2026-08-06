package data_access;

import entity.Stock;
import use_case.watchlist.StockRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link StockRepository} backed by a map held for the lifetime of the application.
 *
 * <p>Lookups are case-insensitive, so a symbol normalized by
 * {@link use_case.watchlist.TickerSymbolValidator} and a symbol typed in any case
 * resolve to the same entry.
 */
public class InMemoryStockRepository implements StockRepository {

    private final Map<String, Stock> stocksBySymbol = new LinkedHashMap<>();

    @Override
    public void save(Stock stock) {
        if (stock == null) {
            throw new NullPointerException("Stock cannot be null");
        }
        stocksBySymbol.put(key(stock.getSymbol()), stock);
    }

    @Override
    public Optional<Stock> findBySymbol(String normalizedSymbol) {
        if (normalizedSymbol == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stocksBySymbol.get(key(normalizedSymbol)));
    }

    @Override
    public void remove(String normalizedSymbol) {
        if (normalizedSymbol != null) {
            stocksBySymbol.remove(key(normalizedSymbol));
        }
    }

    @Override
    public List<Stock> findAll() {
        final List<Stock> all = new ArrayList<>(stocksBySymbol.values());
        all.sort((left, right) -> left.getSymbol().compareToIgnoreCase(right.getSymbol()));
        return Collections.unmodifiableList(all);
    }

    private static String key(String symbol) {
        return symbol.toUpperCase(Locale.ROOT);
    }
}
