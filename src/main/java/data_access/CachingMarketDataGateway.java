package data_access;

import entity.DailyPrice;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;
import use_case.watchlist.TickerSymbolValidator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
 *
 * <p><strong>Thread safety.</strong> Both maps are {@link ConcurrentHashMap}s because
 * this cache is read from the Swing background thread the watchlist refresh runs on, as
 * {@code WatchlistSnapshot}'s own javadoc describes. The check-then-put around a cache
 * miss is deliberately left non-atomic: the worst outcome is two concurrent misses for
 * the same symbol both calling the delegate, one extra request rather than a corrupted
 * map, and holding a lock across a network call would be far worse.
 */
public class CachingMarketDataGateway implements MarketDataGateway {

    /** Long enough to absorb repeated clicks, short enough that data stays current. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final MarketDataGateway delegate;
    private final Duration timeToLive;
    private final Clock clock;

    /**
     * How many company names are retained before the name cache is emptied.
     *
     * <p>Names never expire, so without a bound the map would grow for the lifetime of
     * the process. Clear-on-overflow is chosen over an LRU because the cost of a miss is
     * one cheap OVERVIEW call and a watchlist is never anywhere near this size.
     */
    static final int MAX_NAME_ENTRIES = 64;

    private final Map<String, CacheEntry> priceCache = new ConcurrentHashMap<>();
    /** Company names are cached without expiry: they effectively never change. */
    private final Map<String, Optional<String>> companyNameCache = new ConcurrentHashMap<>();

    private record CacheEntry(List<DailyPrice> prices, Instant storedAt) {
    }

    /**
     * Wraps a gateway with the default time-to-live and the system clock.
     *
     * @param delegate the gateway to cache in front of
     */
    public CachingMarketDataGateway(MarketDataGateway delegate) {
        this(delegate, DEFAULT_TTL, Clock.systemUTC());
    }

    /**
     * Wraps a gateway with an explicit time-to-live and clock, as the tests do.
     *
     * @param delegate   the gateway to cache in front of
     * @param timeToLive how long a price entry stays valid; must be strictly positive
     * @param clock      the time source, injected so expiry is testable without sleeping
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code timeToLive} is zero or negative, which
     *                                  would silently turn the cache into a no-op
     */
    public CachingMarketDataGateway(MarketDataGateway delegate, Duration timeToLive, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.timeToLive = Objects.requireNonNull(timeToLive, "Time to live cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");

        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("Time to live must be positive, but was " + timeToLive);
        }
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        final String key = key(normalizedSymbol);
        final CacheEntry cached = priceCache.get(key);

        if (cached != null) {
            if (!isExpired(cached)) {
                return cached.prices();
            }
            // Evicted on access rather than left in place, so getCachedSymbolCount()
            // reports live entries the way its name promises.
            priceCache.remove(key, cached);
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
        if (companyNameCache.size() >= MAX_NAME_ENTRIES) {
            companyNameCache.clear();
        }
        companyNameCache.put(key, fetched);
        return fetched;
    }

    /**
     * Empties both caches, forcing the next read to hit the provider.
     *
     * <p>Package-private: this is a test seam, not application API. The only caller is
     * {@code CachingMarketDataGatewayTest}, which lives in this package.
     */
    void clear() {
        priceCache.clear();
        companyNameCache.clear();
    }

    /**
     * Counts the symbols with a <em>live</em> price entry.
     *
     * <p>Expired entries are purged first, so the count means what its name says.
     * Package-private for the same reason as {@link #clear()}.
     *
     * @return the number of unexpired cached symbols
     */
    int getCachedSymbolCount() {
        priceCache.values().removeIf(this::isExpired);
        return priceCache.size();
    }

    /**
     * Counts the cached company names.
     *
     * @return the number of names currently retained, never above
     *         {@link #MAX_NAME_ENTRIES}
     */
    int getCachedNameCount() {
        return companyNameCache.size();
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

    /**
     * Folds a symbol to its cache key.
     *
     * <p>Delegates to {@link TickerSymbolValidator#normalizeKey(String)} so the cache
     * and the validator cannot drift apart on what makes two symbols the same. Null is
     * unreachable here: every caller runs {@link #requireUsableSymbol(String)} first.
     *
     * @param symbol an already-validated symbol
     * @return the upper-cased cache key
     */
    private static String key(String symbol) {
        return TickerSymbolValidator.normalizeKey(symbol);
    }
}
