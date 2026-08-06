package use_case.watchlist;

/** Input for the Remove Ticker use case. */
public final class RemoveTickerInputData {

    private final String rawSymbol;

    public RemoveTickerInputData(String rawSymbol) {
        this.rawSymbol = rawSymbol;
    }

    public String getRawSymbol() {
        return rawSymbol;
    }
}
