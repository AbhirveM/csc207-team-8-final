package entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BacktestEngineTest {

    private static final double DELTA = 0.0001;

    @Test
    void buyAndSellExecuteAtNextDayOpen() {
        final BacktestEngine engine = new BacktestEngine();
        final Ticker ticker = createTicker();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 50.0, 50.0),
                createPrice(3, 60.0, 60.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(ticker, strategy, prices);

        assertEquals(12000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(20.0,
                result.getTotalReturn(),
                DELTA);

        assertEquals(1,
                result.getNumberOfTrades());

        assertEquals(100.0,
                result.getWinRate(),
                DELTA);

        final Trade trade =
                result.getTradeLog().get(0);

        assertEquals(
                LocalDate.of(2026, 1, 2),
                trade.getEntryDate());

        assertEquals(
                50.0,
                trade.getEntryPrice(),
                DELTA);

        assertEquals(
                200,
                trade.getQuantity());

        assertEquals(
                LocalDate.of(2026, 1, 3),
                trade.getExitDate());

        assertEquals(
                60.0,
                trade.getExitPrice(),
                DELTA);
    }

    @Test
    void buysOnlyWholeShares() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 60.0, 60.0),
                createPrice(3, 70.0, 70.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        /*
         * $10,000 buys 166 shares at $60.
         *
         * Cost = $9,960
         * Remaining cash = $40
         *
         * 166 shares sold at $70 = $11,620
         *
         * Final capital = $11,660
         */
        assertEquals(
                11660.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                16.6,
                result.getTotalReturn(),
                DELTA);

        assertEquals(
                166,
                result.getTradeLog().get(0).getQuantity());
    }

    @Test
    void ignoresBuyWhenAlreadyHoldingPosition() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 100.0, 100.0),
                createPrice(3, 50.0, 50.0),
                createPrice(4, 110.0, 110.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                11000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                1,
                result.getNumberOfTrades());
    }

    @Test
    void ignoresSellWhenNoPositionIsHeld() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 90.0, 90.0),
                createPrice(3, 100.0, 100.0),
                createPrice(4, 110.0, 110.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.SELL,
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                11000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                1,
                result.getNumberOfTrades());
    }

    @Test
    void openPositionIsClosedAtFinalClosingPrice() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 100.0, 105.0),
                createPrice(3, 110.0, 120.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.HOLD,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                12000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                20.0,
                result.getTotalReturn(),
                DELTA);

        assertEquals(
                1,
                result.getNumberOfTrades());

        final Trade trade =
                result.getTradeLog().get(0);

        assertEquals(
                LocalDate.of(2026, 1, 3),
                trade.getExitDate());

        assertEquals(
                120.0,
                trade.getExitPrice(),
                DELTA);
    }

    @Test
    void noTradesLeavesCapitalUnchanged() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 110.0, 110.0),
                createPrice(3, 120.0, 120.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD,
                        SignalType.HOLD,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                10000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                0.0,
                result.getTotalReturn(),
                DELTA);

        assertEquals(
                0,
                result.getNumberOfTrades());

        assertEquals(
                0.0,
                result.getWinRate(),
                DELTA);

        assertEquals(
                0,
                result.getTradeLog().size());
    }

    @Test
    void losingTradeProducesZeroWinRate() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 100.0, 100.0),
                createPrice(3, 90.0, 90.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                9000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                -10.0,
                result.getTotalReturn(),
                DELTA);

        assertEquals(
                0.0,
                result.getWinRate(),
                DELTA);
    }

    @Test
    void finalDaySignalIsNotExecuted() {
        final BacktestEngine engine = new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 100.0, 100.0),
                createPrice(3, 100.0, 200.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD,
                        SignalType.HOLD,
                        SignalType.BUY);

        final BacktestResult result =
                engine.run(
                        createTicker(),
                        strategy,
                        prices);

        assertEquals(
                10000.0,
                result.getFinalCapital(),
                DELTA);

        assertEquals(
                0,
                result.getNumberOfTrades());
    }

    @Test
    void rejectsEmptyPriceHistory() {
        final BacktestEngine engine =
                new BacktestEngine();

        final TradingStrategy strategy =
                new FixedSignalStrategy();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.run(
                        createTicker(),
                        strategy,
                        List.of()));
    }

    @Test
    void rejectsNullTicker() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD);

        assertThrows(
                NullPointerException.class,
                () -> engine.run(
                        null,
                        strategy,
                        prices));
    }

    @Test
    void rejectsNullStrategy() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0)
        );

        assertThrows(
                NullPointerException.class,
                () -> engine.run(
                        createTicker(),
                        null,
                        prices));
    }

    @Test
    void rejectsNullPriceList() {
        final BacktestEngine engine =
                new BacktestEngine();

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD);

        assertThrows(
                NullPointerException.class,
                () -> engine.run(
                        createTicker(),
                        strategy,
                        null));
    }

    @Test
    void rejectsUnorderedPrices() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(2, 100.0, 100.0),
                createPrice(1, 100.0, 100.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD,
                        SignalType.HOLD);

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.run(
                        createTicker(),
                        strategy,
                        prices));
    }

    @Test
    void equityCurveHasOneEntryPerTradingDayAndStartsAtTheInitialCapital() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 50.0, 50.0),
                createPrice(3, 60.0, 60.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.SELL,
                        SignalType.HOLD);

        final List<Double> curve =
                engine.run(createTicker(), strategy, prices)
                        .getEquityCurve();

        assertEquals(prices.size(), curve.size());

        assertEquals(
                BacktestEngine.INITIAL_CAPITAL,
                curve.get(0),
                DELTA);
    }

    @Test
    void equityCurveEndsExactlyAtTheFinalCapital() {
        // The presenter prints the last point of the curve and the final capital as two
        // figures in the same sentence, so they have to be the same number and not merely
        // close: the last entry is overwritten with the settled cash for this reason.
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(2, 50.0, 50.0),
                createPrice(3, 60.0, 62.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY,
                        SignalType.HOLD,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(createTicker(), strategy, prices);

        final List<Double> curve =
                result.getEquityCurve();

        assertEquals(
                result.getFinalCapital(),
                curve.get(curve.size() - 1),
                1e-9);
    }

    @Test
    void aStrategyThatNeverTradesLeavesTheCurveFlatAtTheInitialCapital() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 110.0),
                createPrice(2, 120.0, 90.0),
                createPrice(3, 80.0, 130.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD,
                        SignalType.HOLD,
                        SignalType.HOLD);

        final List<Double> curve =
                engine.run(createTicker(), strategy, prices)
                        .getEquityCurve();

        for (final Double value : curve) {
            assertEquals(
                    BacktestEngine.INITIAL_CAPITAL,
                    value,
                    DELTA);
        }
    }

    @Test
    void aSingleDayOfPricesStillProducesAOneEntryCurve() {
        // The execution loop never runs here, so the curve is the seed alone. Off by one and
        // this is either empty or two entries long.
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0));

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.BUY);

        final List<Double> curve =
                engine.run(createTicker(), strategy, prices)
                        .getEquityCurve();

        assertEquals(1, curve.size());

        assertEquals(
                BacktestEngine.INITIAL_CAPITAL,
                curve.get(0),
                DELTA);
    }

    @Test
    void theRunCarriesTheDatesItsPricesSpanned() {
        final BacktestEngine engine =
                new BacktestEngine();

        final List<DailyPrice> prices = List.of(
                createPrice(1, 100.0, 100.0),
                createPrice(4, 100.0, 100.0)
        );

        final TradingStrategy strategy =
                new FixedSignalStrategy(
                        SignalType.HOLD,
                        SignalType.HOLD);

        final BacktestResult result =
                engine.run(createTicker(), strategy, prices);

        assertEquals(
                LocalDate.of(2026, 1, 1),
                result.getStartDate());

        assertEquals(
                LocalDate.of(2026, 1, 4),
                result.getEndDate());
    }

    private Ticker createTicker() {
        return new Ticker(
                "TEST",
                "Test Company");
    }

    private DailyPrice createPrice(
            int day,
            double open,
            double close) {

        final double high =
                Math.max(open, close);

        final double low =
                Math.min(open, close);

        return new DailyPrice(
                LocalDate.of(2026, 1, day),
                open,
                high,
                low,
                close,
                1000L);
    }

    /**
     * Simple deterministic strategy used only for engine tests.
     */
    private static class FixedSignalStrategy
            implements TradingStrategy {

        private final List<SignalType> signalTypes;

        FixedSignalStrategy(
                SignalType... signalTypes) {

            this.signalTypes =
                    List.of(signalTypes);
        }

        @Override
        public String getName() {
            return "Fixed Test Strategy";
        }

        @Override
        public List<TradingSignal> generateSignals(
                List<DailyPrice> prices) {

            final List<TradingSignal> signals =
                    new ArrayList<>();

            for (int index = 0;
                 index < signalTypes.size();
                 index++) {

                signals.add(
                        new TradingSignal(
                                prices.get(index).getDate(),
                                signalTypes.get(index)));
            }

            return signals;
        }
    }
}