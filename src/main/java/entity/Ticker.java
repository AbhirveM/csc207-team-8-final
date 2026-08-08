package entity;

import java.io.Serializable;

/**
 * A stock ticker symbol, e.g. "AAPL".
 */
public class Ticker implements Serializable {
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
