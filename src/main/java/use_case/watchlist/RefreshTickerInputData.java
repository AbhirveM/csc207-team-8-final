package use_case.watchlist;

/** Input for the Refresh Ticker use case. */
public final class RefreshTickerInputData {

    private final String rawSymbol;

    public RefreshTickerInputData(String rawSymbol) {
        this.rawSymbol = rawSymbol;
    }

    public String getRawSymbol() {
        return rawSymbol;
    }
}
