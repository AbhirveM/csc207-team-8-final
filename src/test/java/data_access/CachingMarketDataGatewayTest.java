package data_access;

import entity.DailyPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingMarketDataGatewayTest {

    /** A clock the test advances by hand, so time-to-live is tested without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-08-06T12:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private InMemoryMarketDataGateway delegate;
    private CachingMarketDataGateway gateway;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        delegate = InMemoryMarketDataGateway.withSampleData();
        gateway = new CachingMarketDataGateway(delegate, Duration.ofMinutes(15), clock);
    }

    @Test
    void aSecondReadWithinTheTimeToLiveDoesNotReachTheProvider() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("AAPL");

        assertEquals(1, delegate.getPriceCallCount("AAPL"));
    }

    @Test
    void readsAfterTheTimeToLiveExpiresReachTheProviderAgain() throws Exception {
        gateway.fetchDailyPrices("AAPL");

        clock.advance(Duration.ofMinutes(16));
        gateway.fetchDailyPrices("AAPL");

        assertEquals(2, delegate.getPriceCallCount("AAPL"));
    }

    @Test
    void aReadExactlyAtTheExpiryBoundaryRefetches() throws Exception {
        gateway.fetchDailyPrices("AAPL");

        clock.advance(Duration.ofMinutes(15));
        gateway.fetchDailyPrices("AAPL");

        assertEquals(2, delegate.getPriceCallCount("AAPL"));
    }

    @Test
    void cachingIsCaseInsensitive() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("aapl");

        assertEquals(1, delegate.getPriceCallCount("AAPL"));
    }

    @Test
    void symbolsAreCachedIndependently() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("MSFT");
        gateway.fetchDailyPrices("AAPL");

        assertEquals(1, delegate.getPriceCallCount("AAPL"));
        assertEquals(1, delegate.getPriceCallCount("MSFT"));
        assertEquals(2, gateway.getCachedSymbolCount());
    }

    @Test
    void freshReadsAlwaysReachTheProvider() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPricesFresh("AAPL");
        gateway.fetchDailyPricesFresh("AAPL");

        assertEquals(3, delegate.getPriceCallCount("AAPL"));
    }

    @Test
    void aFreshReadReplacesTheCachedValue() throws Exception {
        final List<DailyPrice> shortSeries = List.of(
                new DailyPrice(LocalDate.of(2026, 8, 5), 1, 1, 1, 1, 1L));
        delegate.putPrices("AAPL", shortSeries);

        final List<DailyPrice> refreshed = gateway.fetchDailyPricesFresh("AAPL");
        final List<DailyPrice> cached = gateway.fetchDailyPrices("AAPL");

        assertEquals(1, refreshed.size());
        assertEquals(refreshed, cached, "The cached copy should hold the refreshed contents");
        assertEquals(1, delegate.getPriceCallCount("AAPL"), "The follow-up read should be cached");
    }

    /**
     * Every hit hands out the same reference, so a caller that could sort or clear it
     * would corrupt the cache for everyone after them. The list must be unmodifiable.
     */
    @Test
    void cachedPriceListsAreUnmodifiable() throws Exception {
        final DailyPrice extra = new DailyPrice(LocalDate.of(2026, 8, 6), 1, 1, 1, 1, 1L);

        final List<DailyPrice> fromCacheMiss = gateway.fetchDailyPrices("AAPL");
        final List<DailyPrice> fromCacheHit = gateway.fetchDailyPrices("AAPL");
        final List<DailyPrice> fromFreshRead = gateway.fetchDailyPricesFresh("AAPL");

        for (final List<DailyPrice> result : List.of(fromCacheMiss, fromCacheHit, fromFreshRead)) {
            assertThrows(UnsupportedOperationException.class, () -> result.add(extra));
            assertThrows(UnsupportedOperationException.class, result::clear);
        }
    }

    /**
     * The delegate here returns a live ArrayList, as any third-party gateway might. The
     * decorator must copy on the way in rather than aliasing whatever it was handed.
     */
    @Test
    void aMutableDelegateResultIsCopiedBeforeItIsCached() throws Exception {
        final List<DailyPrice> mutableSource = new ArrayList<>(List.of(
                new DailyPrice(LocalDate.of(2026, 8, 4), 1, 1, 1, 1, 1L),
                new DailyPrice(LocalDate.of(2026, 8, 5), 2, 2, 2, 2, 2L)));

        final MarketDataGateway mutableDelegate = new MarketDataGateway() {
            @Override
            public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) {
                return mutableSource;
            }

            @Override
            public Optional<String> fetchCompanyName(String normalizedSymbol) {
                return Optional.empty();
            }
        };
        final CachingMarketDataGateway wrapper =
                new CachingMarketDataGateway(mutableDelegate, Duration.ofMinutes(15), clock);

        final List<DailyPrice> first = wrapper.fetchDailyPrices("AAPL");
        mutableSource.clear();

        assertEquals(2, first.size(), "The returned list must not alias the delegate's");
        assertEquals(2, wrapper.fetchDailyPrices("AAPL").size(),
                "The cached list must not alias the delegate's");
    }

    /** A transient failure must not lock the user out until an entry expires. */
    @Test
    void failuresArePropagatedAndNotCached() {
        delegate.failPricesWith("AAPL", new MarketDataException(
                MarketDataException.Kind.NETWORK, "AAPL", "down"));

        assertThrows(MarketDataException.class, () -> gateway.fetchDailyPrices("AAPL"));
        assertThrows(MarketDataException.class, () -> gateway.fetchDailyPrices("AAPL"));

        assertEquals(2, delegate.getPriceCallCount("AAPL"));
        assertEquals(0, gateway.getCachedSymbolCount());
    }

    @Test
    void companyNamesAreCachedForTheProcessLifetime() throws Exception {
        gateway.fetchCompanyName("AAPL");
        clock.advance(Duration.ofDays(3));
        gateway.fetchCompanyName("AAPL");

        assertEquals(1, delegate.getCompanyNameCallCount("AAPL"));
    }

    @Test
    void anAbsentCompanyNameIsAlsoCached() throws Exception {
        delegate.putPrices("VOO", InMemoryMarketDataGateway.syntheticSeries(
                "VOO", LocalDate.of(2026, 8, 5), 5));

        assertTrue(gateway.fetchCompanyName("VOO").isEmpty());
        assertTrue(gateway.fetchCompanyName("VOO").isEmpty());

        assertEquals(1, delegate.getCompanyNameCallCount("VOO"));
    }

    @Test
    void clearForcesTheNextReadToReachTheProvider() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchCompanyName("AAPL");

        gateway.clear();
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchCompanyName("AAPL");

        assertEquals(2, delegate.getPriceCallCount("AAPL"));
        assertEquals(2, delegate.getCompanyNameCallCount("AAPL"));
        assertEquals(1, gateway.getCachedSymbolCount());
    }

    // --- Eviction and bounds ---------------------------------------------------

    /**
     * The method name promises live entries. Before eviction on access it counted dead
     * ones too, so a caller could not tell a warm cache from an expired one.
     */
    @Test
    void expiredPriceEntriesAreEvictedRatherThanCounted() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        gateway.fetchDailyPrices("MSFT");
        assertEquals(2, gateway.getCachedSymbolCount());

        clock.advance(Duration.ofMinutes(16));

        assertEquals(0, gateway.getCachedSymbolCount(),
                "Expired entries must not be counted as cached symbols");
    }

    @Test
    void anExpiredEntryIsEvictedWhenItIsRead() throws Exception {
        gateway.fetchDailyPrices("AAPL");
        clock.advance(Duration.ofMinutes(16));

        gateway.fetchDailyPrices("AAPL");

        assertEquals(2, delegate.getPriceCallCount("AAPL"));
        assertEquals(1, gateway.getCachedSymbolCount(), "The refetched entry replaces the dead one");
    }

    /**
     * Company names never expire, so without a bound the map would grow for the whole
     * life of the process.
     */
    @Test
    void theCompanyNameCacheIsBounded() throws Exception {
        final InMemoryMarketDataGateway many = new InMemoryMarketDataGateway();
        final int overflowBy = 3;
        final int total = CachingMarketDataGateway.MAX_NAME_ENTRIES + overflowBy;
        for (int index = 0; index < total; index++) {
            many.putCompanyName("SYM" + index, "Company " + index);
        }
        final CachingMarketDataGateway bounded =
                new CachingMarketDataGateway(many, Duration.ofMinutes(15), clock);

        for (int index = 0; index < total; index++) {
            bounded.fetchCompanyName("SYM" + index);
        }

        assertTrue(bounded.getCachedNameCount() <= CachingMarketDataGateway.MAX_NAME_ENTRIES,
                "Name cache grew to " + bounded.getCachedNameCount());
        assertEquals(overflowBy, bounded.getCachedNameCount(),
                "Clear-on-overflow should leave only the entries added since the reset");
    }

    /**
     * The Swing refresh runs on a background thread, so the maps are hit concurrently.
     * With plain HashMaps this loops forever or throws; the assertion is that it
     * finishes and that every reader saw correct data.
     */
    @Test
    void concurrentReadersDoNotCorruptTheCache() throws Exception {
        final int threadCount = 8;
        final int readsPerThread = 200;
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger failures = new AtomicInteger();
        // Any throwable a worker fails to catch - notably an unchecked
        // ConcurrentModificationException from a plain-HashMap merge - would otherwise just
        // kill that thread and leave the test green. Record them all and assert none occurred.
        final List<Throwable> uncaught = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> threads = new ArrayList<>(threadCount);

        for (int index = 0; index < threadCount; index++) {
            final String symbol = index % 2 == 0 ? "AAPL" : "MSFT";
            final Thread thread = new Thread(() -> {
                try {
                    start.await();
                    for (int read = 0; read < readsPerThread; read++) {
                        if (gateway.fetchDailyPrices(symbol).size()
                                != InMemoryMarketDataGateway.SAMPLE_PRICE_COUNT) {
                            failures.incrementAndGet();
                        }
                        gateway.fetchCompanyName(symbol);
                        gateway.getCachedSymbolCount();
                    }
                }
                catch (InterruptedException | MarketDataException exception) {
                    failures.incrementAndGet();
                }
            });
            thread.setUncaughtExceptionHandler((failed, error) -> uncaught.add(error));
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        for (final Thread thread : threads) {
            thread.join(30_000);
            assertFalse(thread.isAlive(), "A reader thread did not finish");
        }

        assertTrue(uncaught.isEmpty(), "A reader thread threw an uncaught exception: " + uncaught);
        assertEquals(0, failures.get());
        assertEquals(2, gateway.getCachedSymbolCount());
    }

    // --- Symbol contract (MarketDataGateway, orchestrator 5.1) -----------------

    @Test
    void aNullSymbolIsRejectedWithANullPointerException() {
        assertThrows(NullPointerException.class, () -> gateway.fetchDailyPrices(null));
        assertThrows(NullPointerException.class, () -> gateway.fetchDailyPricesFresh(null));
        assertThrows(NullPointerException.class, () -> gateway.fetchCompanyName(null));
    }

    @Test
    void aBlankSymbolIsReportedAsInvalidWithoutReachingTheDelegate() {
        for (final String blank : List.of("", "   ", "\t")) {
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchDailyPrices(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchDailyPricesFresh(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> gateway.fetchCompanyName(blank)).getKind(), blank);
        }

        assertEquals(0, delegate.getPriceCallCount(""));
        assertEquals(0, delegate.getCompanyNameCallCount(""));
    }

    /**
     * The old key function folded null and blank alike onto {@code ""}, so a rejected
     * symbol could still occupy a cache slot. Nothing may be stored under a blank key.
     */
    @Test
    void aBlankSymbolIsNeverCached() {
        assertThrows(MarketDataException.class, () -> gateway.fetchDailyPrices(""));
        assertThrows(MarketDataException.class, () -> gateway.fetchCompanyName(""));

        assertEquals(0, gateway.getCachedSymbolCount());
    }

    // --- Construction ----------------------------------------------------------

    @Test
    void constructorRejectsNullCollaborators() {
        assertThrows(NullPointerException.class, () -> new CachingMarketDataGateway(null));
        assertThrows(NullPointerException.class,
                () -> new CachingMarketDataGateway(delegate, null, clock));
        assertThrows(NullPointerException.class,
                () -> new CachingMarketDataGateway(delegate, Duration.ofMinutes(1), null));
    }

    /**
     * The one-argument constructor is what Phase 4's composition root wires up, so its
     * defaults are a contract rather than an implementation detail.
     */
    @Test
    void theOneArgumentConstructorUsesTheDefaultTimeToLive() throws Exception {
        final CachingMarketDataGateway defaults = new CachingMarketDataGateway(delegate);

        defaults.fetchDailyPrices("AAPL");
        defaults.fetchDailyPrices("AAPL");

        assertEquals(Duration.ofMinutes(15), CachingMarketDataGateway.DEFAULT_TTL);
        assertEquals(1, delegate.getPriceCallCount("AAPL"), "The second read should be cached");
        assertEquals(1, defaults.getCachedSymbolCount());
    }

    /** A zero or negative time-to-live silently turns the cache into a no-op. */
    @Test
    void constructorRejectsANonPositiveTimeToLive() {
        for (final Duration invalid : List.of(Duration.ZERO, Duration.ofMinutes(-1))) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CachingMarketDataGateway(delegate, invalid, clock),
                    String.valueOf(invalid));
        }
    }
}
