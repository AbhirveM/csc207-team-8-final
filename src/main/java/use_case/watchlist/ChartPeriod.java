package use_case.watchlist;

/**
 * How much of a ticker's price history the close-price chart should plot.
 *
 * <p><strong>These are trading days, not calendar days.</strong> A month is twenty-one rows in the
 * price history, not thirty-one dates on a calendar, because the history holds one row per day the
 * market opened. Converting to calendar days and filtering by date would quietly drop a week
 * whenever a holiday fell inside the window.
 *
 * <p>A period longer than the history clamps to the whole history, and that is a correct answer
 * rather than an error: the bundled offline series is 120 trading days, so {@link #SIX_MONTHS} and
 * {@link #ONE_YEAR} both show everything there is until real data is loaded.
 *
 * <p>The period is user intent about what to show, so it travels the ordinary route - view to
 * controller to interactor to snapshot to presenter - rather than being filtered inside the view.
 * A view that sliced the series itself would be deciding what the user is looking at.
 */
public enum ChartPeriod {

    /** Roughly one month of trading. */
    ONE_MONTH(21, "1M"),

    /** Roughly one quarter of trading. */
    THREE_MONTHS(63, "3M"),

    /** Roughly half a year of trading. */
    SIX_MONTHS(126, "6M"),

    /** Roughly one year of trading. */
    ONE_YEAR(252, "1Y"),

    /** Everything the history holds, however long that is. */
    ALL(0, "ALL");

    private final int tradingDays;
    private final String label;

    ChartPeriod(int tradingDays, String label) {
        this.tradingDays = tradingDays;
        this.label = label;
    }

    /**
     * @return how many trading days this period keeps; meaningless for {@link #ALL}, which keeps
     *         everything - test {@link #isAll()} rather than this value
     */
    public int tradingDays() {
        return tradingDays;
    }

    /**
     * @return whether this period is the unbounded one
     */
    public boolean isAll() {
        return this == ALL;
    }

    /**
     * The text a control shows for this period.
     *
     * <p>A fixed label rather than composed text, in the same way a button's caption is fixed: the
     * view puts it on screen without building it, which is what keeps number formatting out of
     * {@code view}.
     *
     * @return the short label, such as "3M"
     */
    @Override
    public String toString() {
        return label;
    }
}
