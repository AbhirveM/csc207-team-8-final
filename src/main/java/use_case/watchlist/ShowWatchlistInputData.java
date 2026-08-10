package use_case.watchlist;

/** The symbol the user selected in the watchlist table, and how much of its history to plot. */
public final class ShowWatchlistInputData {

    private final String selectedSymbol;
    private final ChartPeriod chartPeriod;

    /**
     * @param selectedSymbol the symbol to select. A null value is normalized to
     *                       {@code ""}, which means "nothing selected". A symbol that
     *                       is not on the watchlist is not an error — it degrades
     *                       silently to no selection.
     * @param chartPeriod    how much of the price history the chart should plot. A null value
     *                       is normalized to {@link ChartPeriod#ALL}, which is what every
     *                       caller that has no opinion wants.
     */
    public ShowWatchlistInputData(String selectedSymbol, ChartPeriod chartPeriod) {
        this.selectedSymbol = selectedSymbol == null ? "" : selectedSymbol;
        this.chartPeriod = chartPeriod == null ? ChartPeriod.ALL : chartPeriod;
    }

    /** @return the selected symbol, or "" when nothing is selected. Never null. */
    public String getSelectedSymbol() {
        return selectedSymbol;
    }

    /** @return the window of history to plot. Never null. */
    public ChartPeriod getChartPeriod() {
        return chartPeriod;
    }
}
