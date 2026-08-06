package use_case.watchlist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import entity.DailyPrice;

/**
 * Small deterministic price series for the watchlist interactor tests.
 *
 * <p>Hand-built rather than borrowed from {@code InMemoryMarketDataGateway}'s sample
 * data, so a test can state the exact dates and closes it asserts on. Everything here
 * is offline: no test in this package touches the network.
 */
final class WatchlistTestData {

    /** The newest date in every ascending series built here. */
    static final LocalDate LAST_DAY = LocalDate.of(2026, 3, 10);

    private WatchlistTestData() {
    }

    /**
     * Builds {@code count} consecutive days of prices ending on {@link #LAST_DAY}.
     *
     * @param count how many days to build; must be at least one
     * @return prices ordered oldest to newest, as {@code Stock} requires
     */
    static List<DailyPrice> ascendingPrices(int count) {
        final List<DailyPrice> prices = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            final LocalDate date = LAST_DAY.minusDays((long) count - 1 - index);
            final double close = 100.0 + index;
            prices.add(new DailyPrice(date, close - 1.0, close + 2.0, close - 2.0, close,
                    1_000L + index));
        }
        return prices;
    }

    /**
     * Builds a series whose dates run newest to oldest.
     *
     * <p>{@code Stock}'s constructor rejects this with an
     * {@code IllegalArgumentException}, which is how these tests reach the
     * {@code MALFORMED_RESPONSE} path without a real malformed HTTP body.
     *
     * @return prices in the wrong order
     */
    static List<DailyPrice> descendingPrices() {
        final List<DailyPrice> ascending = ascendingPrices(3);
        final List<DailyPrice> descending = new ArrayList<>(ascending);
        Collections.reverse(descending);
        return descending;
    }

    /**
     * Builds a series that repeats a trading day.
     *
     * @return prices containing the same date twice
     */
    static List<DailyPrice> duplicateDatePrices() {
        return List.of(
                new DailyPrice(LAST_DAY, 1.0, 2.0, 0.5, 1.5, 100L),
                new DailyPrice(LAST_DAY, 1.0, 2.0, 0.5, 1.5, 100L));
    }
}
