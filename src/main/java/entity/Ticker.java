package entity;

import java.io.Serializable;

/**
 * A stock ticker symbol, e.g. "AAPL".
 */
public class Ticker implements Serializable {

    // Pinned to the value the JVM computed before it was declared, so save files written
    // by an earlier build still load. Declaring a fresh 1L here would have changed the UID
    // and made every existing watchlist.dat an InvalidClassException - which the DAO reads
    // as corruption and recovers from by resetting, the exact data loss this prevents.
    private static final long serialVersionUID = -2803816898474223090L;

    private final String symbol;
    private final String companyName;

    public Ticker(String symbol, String companyName) {
        this.symbol = symbol;
        this.companyName = companyName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticker)) {
            return false;
        }
        return symbol.equalsIgnoreCase(((Ticker) o).symbol);
    }

    @Override
    public int hashCode() {
        return symbol.toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return symbol + (companyName != null && !companyName.isEmpty() ? " (" + companyName + ")" : "");
    }
}
