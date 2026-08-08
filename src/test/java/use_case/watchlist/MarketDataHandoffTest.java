package use_case.watchlist;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryMarketDataGateway;
import data_access.InMemoryStockRepository;
import entity.DailyPrice;
import entity.MovingAverageConfiguration;
import entity.MovingAverageCrossoverStrategy;
import entity.SignalType;
import entity.Stock;
import entity.TradingSignal;
import entity.Watchlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The executable proof of the hand-off from this vertical to Members 2 and 3.
 *
 * <p>The team blueprint promises that the market-data vertical produces "a {@code Stock}
 * containing a documented oldest-to-newest {@code List<DailyPrice>} that Members 2 and 3
 * can use without knowing anything about Alpha Vantage JSON". Every other test in this
 * suite checks one side of that promise. This one checks the seam itself: it drives the
 * real pipeline and feeds the result straight into a real
 * {@link MovingAverageCrossoverStrategy}, with nothing in between.
 *
 * <p><strong>Why the price list is not hand-built.</strong> The point is to exercise the
 * pipeline, so the {@link Stock} comes from
 * {@link InMemoryMarketDataGateway#withSampleData()} through {@link AddTickerInteractor}
 * and out of the {@link StockRepository} - gateway, interactor, repository and entity in
 * one shot. Hand-constructing the prices would prove only that the strategy works, which
 * {@code MovingAverageCrossoverStrategyTest} already covers.
 *
 * <p><strong>Why this test imports {@code data_access}.</strong> Deliberately, and unlike
 * the surrounding interactor tests where the same import is a coupling smell. A fake
 * gateway here would defeat the test's entire purpose: the offline sample data is what
 * the application itself runs on when no API key is configured, so "does the hand-off
 * work?" and "does the offline fake work?" are the same question.
 */
class MarketDataHandoffTest {

    /** The symbol the walkthrough and the demo both use. */
    private static final String SYMBOL = "AAPL";

    /** Deliberately lower-case: normalization is part of the path being proved. */
    private static final String RAW_SYMBOL = "aapl";

    /** A conventional pair of windows, comfortably inside the compact-response ceiling. */
    private static final int SHORT_WINDOW = 5;
    private static final int LONG_WINDOW = 20;

    /**
     * How many trading days Alpha Vantage's free {@code outputsize=compact} response
     * carries. Roughly, not exactly - which is why the hand-off notes recommend keeping
     * long windows near 90 rather than at the arithmetic limit this constant implies.
     */
    private static final int COMPACT_RESPONSE_DAYS = 100;

    private InMemoryMarketDataGateway gateway;
    private InMemoryStockRepository stocks;
    private Watchlist watchlist;
    private RecordingWatchlistPresenter presenter;

    @BeforeEach
    void setUp() {
        gateway = InMemoryMarketDataGateway.withSampleData();
        stocks = new InMemoryStockRepository();
        watchlist = new Watchlist();
        presenter = new RecordingWatchlistPresenter();
    }

    /**
     * Runs the real add path and returns the stock it stored.
     *
     * @return the stock the pipeline produced for {@link #SYMBOL}
     */
    private Stock addThroughTheRealPipeline() {
        final AddTickerInteractor interactor = new AddTickerInteractor(
                watchlist, gateway, stocks, new RecordingSaveWatchlist(), presenter);
        interactor.execute(new AddTickerInputData(RAW_SYMBOL));

        assertEquals(1, presenter.getAddSuccessCount(),
                "the pipeline must succeed before its output can be handed off");
        assertEquals(0, presenter.getFailureCount());

        return stocks.findBySymbol(SYMBOL)
                .orElseThrow(() -> new AssertionError("the add path stored no stock"));
    }

    @Test
    void marketDataFromTheRealPipelineProducesBothBuyAndSellSignals() {
        final Stock stock = addThroughTheRealPipeline();

        final List<TradingSignal> signals =
                new MovingAverageCrossoverStrategy(
                        new MovingAverageConfiguration(SHORT_WINDOW, LONG_WINDOW))
                        .generateSignals(stock.getDailyPrices());

        /*
         * The assertion that matters. A flat or monotonic series returns nothing but
         * HOLD, which would make this test and the offline demo equally vacuous. If this
         * ever fails, the fix is making InMemoryMarketDataGateway.syntheticSeries
         * oscillate harder - never weakening the assertion.
         */
        assertTrue(countOf(signals, SignalType.BUY) > 0,
                "the sample series must cross upwards at least once, but produced no BUY");
        assertTrue(countOf(signals, SignalType.SELL) > 0,
                "the sample series must cross downwards at least once, but produced no SELL");

        assertEquals(stock.getPriceCount(), signals.size(),
                "the strategy emits one signal per trading day");
    }

    @Test
    void theHandedOffPricesSatisfyEveryPreconditionTheStrategyDocuments() {
        final List<DailyPrice> prices = addThroughTheRealPipeline().getDailyPrices();

        assertFalse(prices.isEmpty());
        assertTrue(prices.size() >= LONG_WINDOW + 1,
                "generateSignals requires longWindow + 1 records");

        LocalDate previous = null;
        for (final DailyPrice price : prices) {
            assertNotNull(price, "the strategy rejects a list containing nulls");
            assertNotNull(price.getDate());
            if (previous != null) {
                assertTrue(previous.isBefore(price.getDate()),
                        "prices must run oldest to newest with no repeated trading day, but "
                                + previous + " is followed by " + price.getDate());
            }
            previous = price.getDate();
        }
    }

    @Test
    void theSelectedSymbolIsNormalizedBeforeItReachesTheHandOff() {
        final Stock stock = addThroughTheRealPipeline();

        assertEquals(SYMBOL, stock.getSymbol(),
                "Members 2 and 3 receive the normalized symbol, not what the user typed");
        assertEquals("Apple Inc.", stock.getCompanyName());
    }

    /**
     * Pins hazard H8 - the compact-response ceiling - as a failing build rather than
     * as a paragraph someone has to read.
     *
     * <p>{@code generateSignals} needs {@code longWindow + 1} records because a crossover
     * is defined against the previous day's averages. With a compact response of roughly
     * {@value #COMPACT_RESPONSE_DAYS} trading days, a long window at that number throws
     * while one below it does not. The hand-off notes recommend staying near 90 because
     * "roughly" is doing real work in that sentence: holidays and newly listed symbols
     * both return fewer rows.
     */
    @Test
    void aLongWindowAtTheCompactResponseCeilingBreaksTheStrategy() {
        final List<DailyPrice> compactSeries = InMemoryMarketDataGateway.syntheticSeries(
                SYMBOL, InMemoryMarketDataGateway.SAMPLE_LAST_TRADING_DAY,
                COMPACT_RESPONSE_DAYS);
        final Stock stock = handOff(compactSeries);

        assertEquals(COMPACT_RESPONSE_DAYS, stock.getPriceCount());

        /*
         * The configuration and the strategy are built outside the lambda deliberately.
         * MovingAverageConfiguration's constructor also throws IllegalArgumentException,
         * so leaving it inside would give the lambda two throw sites and make the test
         * depend on the message assertion to tell them apart.
         */
        final MovingAverageCrossoverStrategy atTheCeiling = new MovingAverageCrossoverStrategy(
                new MovingAverageConfiguration(SHORT_WINDOW, COMPACT_RESPONSE_DAYS));

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> atTheCeiling.generateSignals(stock.getDailyPrices()));

        assertEquals("Not enough price history to calculate a crossover", exception.getMessage());

        // One day below the ceiling the same data is fine, so the cliff is exactly here
        // and not somewhere vaguer.
        final MovingAverageCrossoverStrategy belowTheCeiling = new MovingAverageCrossoverStrategy(
                new MovingAverageConfiguration(SHORT_WINDOW, COMPACT_RESPONSE_DAYS - 1));

        assertEquals(COMPACT_RESPONSE_DAYS,
                belowTheCeiling.generateSignals(stock.getDailyPrices()).size());
    }

    /**
     * Runs the real add path against a gateway carrying the given series.
     *
     * @param prices the series the provider should return
     * @return the stock the pipeline produced
     */
    private Stock handOff(List<DailyPrice> prices) {
        gateway = new InMemoryMarketDataGateway()
                .putPrices(SYMBOL, prices)
                .putCompanyName(SYMBOL, "Apple Inc.");
        return addThroughTheRealPipeline();
    }

    /**
     * Counts signals of one type.
     *
     * @param signals    the signals to scan
     * @param signalType the type to count
     * @return how many signals carry that type
     */
    private static int countOf(List<TradingSignal> signals, SignalType signalType) {
        int count = 0;
        for (final TradingSignal signal : signals) {
            if (signal.getSignalType() == signalType) {
                count++;
            }
        }
        return count;
    }
}
