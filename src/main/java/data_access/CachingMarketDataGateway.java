package data_access;

import entity.DailyPrice;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps any {@link MarketDataGateway} with a short-lived cache.
 *
 * <p>This exists because the free Alpha Vantage plan allows only a small number of
 * requests per day, which a user clicking around a watchlist would exhaust quickly.
 *
 * <p>It is a decorator rather than logic inside the data access object for three
 * reasons: the data access object keeps a single responsibility, so its parsing tests
 * stay readable; the cache can be tested deterministically with an injected
 * {@link Clock} and a counting fake, with no HTTP involved and no sleeping; and it
 * wraps the offline fake just as happily as the real provider, so caching becomes a
 * wiring decision rather than behaviour baked into every implementation.
 *
 * <p>Failures are deliberately not cached, so a transient network problem does not
 * lock the user out until the entry expires.
 */
public class CachingMarketDataGateway implements MarketDataGateway {

    /** Long enough to absorb repeated clicks, short enough that data stays current. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final MarketDataGateway delegate;
    private final Duration timeToLive;
    private final Clock clock;

    private final Map<String, CacheEntry> priceCache = new HashMap<>();
    /** Company names are cached without expiry: they effectively never change. */
    private final Map<String, Optional<String>> companyNameCache = new HashMap<>();

    private record CacheEntry(List<DailyPrice> prices, Instant storedAt) {
    }

    public CachingMarketDataGateway(MarketDataGateway delegate) {
        this(delegate, DEFAULT_TTL, Clock.systemUTC());
    }

    public CachingMarketDataGateway(MarketDataGateway delegate, Duration timeToLive, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.timeToLive = Objects.requireNonNull(timeToLive, "Time to live cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        final String key = key(normalizedSymbol);
        final CacheEntry cached = priceCache.get(key);

        if (cached != null && !isExpired(cached)) {
            return cached.prices();
        }

        return storeAndReturn(key, delegate.fetchDailyPrices(normalizedSymbol));
    }

    @Override
    public List<DailyPrice> fetchDailyPricesFresh(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        // Always goes to the delegate; the refreshed value replaces any cached copy.
        return storeAndReturn(key(normalizedSymbol), delegate.fetchDailyPricesFresh(normalizedSymbol));
    }

    @Override
    public Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        final String key = key(normalizedSymbol);
        final Optional<String> cached = companyNameCache.get(key);

        if (cached != null) {
            return cached;
        }

        final Optional<String> fetched = delegate.fetchCompanyName(normalizedSymbol);
        companyNameCache.put(key, fetched);
        return fetched;
    }

    /** Empties both caches, forcing the next read to hit the provider. */
    public void clear() {
        priceCache.clear();
        companyNameCache.clear();
    }

    public int getCachedSymbolCount() {
        return priceCache.size();
    }

    /**
     * Caches and returns one immutable copy of a delegate's result.
     *
     * <p>The copy is taken here as well as in the delegate because this decorator wraps
     * any {@link MarketDataGateway}, not only ones that already return immutable lists.
     * The stored and returned references are the same object on purpose - it just cannot
     * be mutated, so sharing it is safe.
     *
     * @param key    the normalized cache key
     * @param prices the freshly fetched prices
     * @return the cached, unmodifiable list
     */
    private List<DailyPrice> storeAndReturn(String key, List<DailyPrice> prices) {
        final List<DailyPrice> immutable = List.copyOf(prices);
        priceCache.put(key, new CacheEntry(immutable, clock.instant()));
        return immutable;
    }

    private boolean isExpired(CacheEntry entry) {
        return Duration.between(entry.storedAt(), clock.instant()).compareTo(timeToLive) >= 0;
    }

    /**
     * Enforces the {@link MarketDataGateway} symbol contract before the cache is touched.
     *
     * <p>Validating ahead of {@link #key(String)} is the point: the previous code mapped
     * null and blank alike onto the key {@code ""}, so an invalid symbol could occupy a
     * cache slot and be served back on a later call.
     *
     * @param normalizedSymbol the symbol to check
     * @throws NullPointerException if {@code normalizedSymbol} is null
     * @throws MarketDataException of kind {@code INVALID_SYMBOL} if it is blank
     */
    private static void requireUsableSymbol(String normalizedSymbol) throws MarketDataException {
        Objects.requireNonNull(normalizedSymbol, "Symbol cannot be null");
        if (normalizedSymbol.isBlank()) {
            throw new MarketDataException(MarketDataException.Kind.INVALID_SYMBOL,
                    normalizedSymbol, "A blank symbol is never cached or delegated");
        }
    }

    private static String key(String symbol) {
        return symbol.toUpperCase(Locale.ROOT);
    }
}
