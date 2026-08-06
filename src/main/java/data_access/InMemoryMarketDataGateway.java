package data_access;

import entity.DailyPrice;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * An offline {@link MarketDataGateway} holding pre-canned market data.
 *
 * <p>The blueprint requires a fake gateway "so all development and tests can run
 * offline". It lives in {@code src/main} rather than the test sources for three
 * reasons: it is a driver-layer adapter in the same sense as an in-memory database;
 * the blueprint mandates it for development as well as testing; and the application
 * itself falls back to it when no API key is configured, so the program is
 * demonstrable with no network access and no credentials.
 *
 * <p>It also records call counts and can be told to fail for a given symbol, which
 * lets tests verify caching behaviour and every provider error path.
 */
public class InMemoryMarketDataGateway implements MarketDataGateway {

    /** How much history the sample data provides, comfortably above any sane long window. */
    public static final int SAMPLE_PRICE_COUNT = 120;

    /** The last trading day the sample data runs up to; fixed so runs are reproducible. */
    public static final LocalDate SAMPLE_LAST_TRADING_DAY = LocalDate.of(2026, 8, 5);

    private final Map<String, List<DailyPrice>> pricesBySymbol = new LinkedHashMap<>();
    private final Map<String, String> companyNamesBySymbol = new HashMap<>();
    private final Map<String, MarketDataException> priceFailures = new HashMap<>();
    private final Map<String, MarketDataException> companyNameFailures = new HashMap<>();
    private final Map<String, Integer> priceCallCounts = new HashMap<>();
    private final Map<String, Integer> companyNameCallCounts = new HashMap<>();

    /**
     * Builds a gateway carrying deterministic sample data for three well-known symbols.
     *
     * @return a gateway usable for an offline demo or an end-to-end test
     */
    public static InMemoryMarketDataGateway withSampleData() {
        return new InMemoryMarketDataGateway()
                .putPrices("AAPL", syntheticSeries("AAPL", SAMPLE_LAST_TRADING_DAY, SAMPLE_PRICE_COUNT))
                .putCompanyName("AAPL", "Apple Inc.")
                .putPrices("MSFT", syntheticSeries("MSFT", SAMPLE_LAST_TRADING_DAY, SAMPLE_PRICE_COUNT))
                .putCompanyName("MSFT", "Microsoft Corporation")
                .putPrices("TSLA", syntheticSeries("TSLA", SAMPLE_LAST_TRADING_DAY, SAMPLE_PRICE_COUNT))
                .putCompanyName("TSLA", "Tesla, Inc.");
    }

    public InMemoryMarketDataGateway putPrices(String symbol, List<DailyPrice> prices) {
        pricesBySymbol.put(key(symbol), List.copyOf(prices));
        return this;
    }

    public InMemoryMarketDataGateway putCompanyName(String symbol, String companyName) {
        companyNamesBySymbol.put(key(symbol), companyName);
        return this;
    }

    public InMemoryMarketDataGateway failPricesWith(String symbol, MarketDataException failure) {
        priceFailures.put(key(symbol), failure);
        return this;
    }

    public InMemoryMarketDataGateway failCompanyNameWith(String symbol, MarketDataException failure) {
        companyNameFailures.put(key(symbol), failure);
        return this;
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException {
        final String key = key(normalizedSymbol);
        priceCallCounts.merge(key, 1, Integer::sum);

        final MarketDataException failure = priceFailures.get(key);
        if (failure != null) {
            throw failure;
        }

        final List<DailyPrice> prices = pricesBySymbol.get(key);
        if (prices == null) {
            throw new MarketDataException(MarketDataException.Kind.INVALID_SYMBOL,
                    normalizedSymbol, "No sample data is configured for this symbol");
        }

        return prices;
    }

    @Override
    public Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException {
        final String key = key(normalizedSymbol);
        companyNameCallCounts.merge(key, 1, Integer::sum);

        final MarketDataException failure = companyNameFailures.get(key);
        if (failure != null) {
            throw failure;
        }

        return Optional.ofNullable(companyNamesBySymbol.get(key));
    }

    public int getPriceCallCount(String symbol) {
        return priceCallCounts.getOrDefault(key(symbol), 0);
    }

    public int getCompanyNameCallCount(String symbol) {
        return companyNameCallCounts.getOrDefault(key(symbol), 0);
    }

    /**
     * Generates a deterministic price series that genuinely oscillates.
     *
     * <p>The closing price follows a slow sine wave, so a short moving average
     * repeatedly crosses a long one. That matters because a flat or monotonic series
     * would produce nothing but HOLD signals, making both the offline demo and the
     * strategy hand-off test vacuous.
     *
     * <p>Weekends are skipped so the dates look like real trading days. The series is
     * seeded from the symbol, so different symbols differ but every run is identical.
     *
     * @param seedSymbol     the symbol to seed the shape from
     * @param lastTradingDay the newest date in the series
     * @param count          how many trading days to generate
     * @return prices ordered oldest to newest
     */
    public static List<DailyPrice> syntheticSeries(String seedSymbol, LocalDate lastTradingDay, int count) {
        final List<LocalDate> tradingDays = new ArrayList<>(count);
        LocalDate cursor = lastTradingDay;
        while (tradingDays.size() < count) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY
                    && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                tradingDays.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        Collections.reverse(tradingDays);

        final int seed = Math.abs(seedSymbol.toUpperCase(Locale.ROOT).hashCode());
        final double base = 100.0 + seed % 150;
        final double amplitude = 15.0 + seed % 10;
        // A period shorter than the series length guarantees several full cycles,
        // and therefore several crossovers, across the generated history.
        final double period = 50.0;
        final double phase = seed % 7;

        final List<DailyPrice> prices = new ArrayList<>(count);
        for (int index = 0; index < tradingDays.size(); index++) {
            final double wave = Math.sin((index + phase) * 2 * Math.PI / period);
            final double close = base + amplitude * wave + index * 0.05;
            final double open = close - amplitude * 0.02;
            final double high = Math.max(open, close) + amplitude * 0.05;
            final double low = Math.min(open, close) - amplitude * 0.05;
            final long volume = 1_000_000L + (long) (index * 1_000L) + seed % 5_000;

            prices.add(new DailyPrice(tradingDays.get(index),
                    round(open), round(high), round(low), round(close), volume));
        }

        return prices;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String key(String symbol) {
        return symbol == null ? "" : symbol.toUpperCase(Locale.ROOT);
    }
}
