package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A ticker together with its available daily price history.
 *
 * <p>This is the hand-off object described in the team blueprint: "a Stock
 * containing a documented oldest-to-newest {@code List<DailyPrice>} that
 * Members 2 and 3 can use without knowing anything about Alpha Vantage JSON."
 *
 * <p><strong>Ordering guarantee.</strong> {@link #getDailyPrices()} always
 * returns prices ordered from oldest to newest, with no null elements and no
 * duplicate dates. This is enforced as a constructor invariant rather than left
 * as a convention, so a strategy such as
 * {@link MovingAverageCrossoverStrategy#generateSignals(List)} can never be
 * handed reversed, sparse, or null-containing data.
 *
 * <p><strong>Why this is separate from {@link Ticker}.</strong> {@code Ticker}
 * is the persisted identity - it is what gets serialized inside a
 * {@link Watchlist}. {@code Stock} is a transient, in-memory aggregate that adds
 * market data to that identity, and is deliberately <em>not</em>
 * {@link java.io.Serializable}: price history is re-fetched from the market-data
 * provider rather than saved to disk, so it never enters the watchlist file
 * format.
 */
public class Stock {

    private final Ticker ticker;
    private final List<DailyPrice> dailyPrices;

    /**
     * Creates a stock with the given price history.
     *
     * @param ticker      the ticker identity; must be non-null with a non-blank symbol
     * @param dailyPrices the price history, oldest to newest; may be empty but not
     *                    null, and may not contain nulls or out-of-order dates.
     *                    The list is copied defensively.
     * @throws NullPointerException     if {@code ticker} or {@code dailyPrices} is null,
     *                                  or any price is null
     * @throws IllegalArgumentException if the symbol is blank, or the dates are not
     *                                  strictly increasing
     */
    public Stock(Ticker ticker, List<DailyPrice> dailyPrices) {
        this.ticker = Objects.requireNonNull(ticker, "Ticker cannot be null");
        Objects.requireNonNull(dailyPrices, "Daily prices cannot be null");

        if (ticker.getSymbol() == null || ticker.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Ticker symbol cannot be blank");
        }

        final List<DailyPrice> copy = new ArrayList<>(dailyPrices);
        for (DailyPrice price : copy) {
            Objects.requireNonNull(price, "Daily prices cannot contain null values");
        }
        requireStrictlyIncreasingDates(copy);

        this.dailyPrices = Collections.unmodifiableList(copy);
    }

    /**
     * Verifies the oldest-to-newest invariant.
     *
     * <p>Strictly increasing rather than merely non-decreasing: a provider that
     * returns the same trading day twice indicates a malformed response, and
     * silently keeping both would distort every moving average computed from it.
     */
    private static void requireStrictlyIncreasingDates(List<DailyPrice> prices) {
        for (int index = 1; index < prices.size(); index++) {
            final LocalDate previous = prices.get(index - 1).getDate();
            final LocalDate current = prices.get(index).getDate();

            if (previous == null || current == null) {
                throw new IllegalArgumentException("Daily prices cannot have a null date");
            }
            if (!previous.isBefore(current)) {
                throw new IllegalArgumentException(
                        "Daily prices must be ordered oldest to newest with no duplicate dates, "
                                + "but " + previous + " is followed by " + current);
            }
        }
    }

    public Ticker getTicker() {
        return ticker;
    }

    public String getSymbol() {
        return ticker.getSymbol();
    }

    /** @return the company name, or null when the provider did not supply one. */
    public String getCompanyName() {
        return ticker.getCompanyName();
    }

    /** @return an unmodifiable price history, guaranteed oldest to newest. */
    public List<DailyPrice> getDailyPrices() {
        return dailyPrices;
    }

    public int getPriceCount() {
        return dailyPrices.size();
    }

    /** @return the most recent day of prices, or empty when there is no history yet. */
    public Optional<DailyPrice> getLatestPrice() {
        if (dailyPrices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dailyPrices.get(dailyPrices.size() - 1));
    }

    /** @return the oldest day of prices, or empty when there is no history yet. */
    public Optional<DailyPrice> getEarliestPrice() {
        if (dailyPrices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dailyPrices.get(0));
    }

    /** @return a copy of this stock carrying a refreshed price history. */
    public Stock withDailyPrices(List<DailyPrice> newDailyPrices) {
        return new Stock(ticker, newDailyPrices);
    }

    /** @return a copy of this stock carrying a newly discovered company name. */
    public Stock withCompanyName(String companyName) {
        return new Stock(new Ticker(ticker.getSymbol(), companyName), dailyPrices);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Stock)) {
            return false;
        }
        final Stock that = (Stock) other;
        return ticker.equals(that.ticker) && dailyPrices.equals(that.dailyPrices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, dailyPrices);
    }

    @Override
    public String toString() {
        if (dailyPrices.isEmpty()) {
            return ticker + " - no price history";
        }
        return ticker + " - " + dailyPrices.size() + " daily prices "
                + dailyPrices.get(0).getDate() + ".." + dailyPrices.get(dailyPrices.size() - 1).getDate();
    }
}
