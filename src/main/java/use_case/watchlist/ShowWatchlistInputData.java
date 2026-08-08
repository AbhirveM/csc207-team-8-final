package use_case.watchlist;

/** The symbol the user selected in the watchlist table. */
public final class ShowWatchlistInputData {

    private final String selectedSymbol;

    /**
     * @param selectedSymbol the symbol to select. A null value is normalized to
     *                       {@code ""}, which means "nothing selected". A symbol that
     *                       is not on the watchlist is not an error — it degrades
     *                       silently to no selection.
     */
    public ShowWatchlistInputData(String selectedSymbol) {
        this.selectedSymbol = selectedSymbol == null ? "" : selectedSymbol;
    }

    /** @return the selected symbol, or "" when nothing is selected. Never null. */
    public String getSelectedSymbol() {
        return selectedSymbol;
    }
}
