package entity;

// This class informs the class user what every strategy must be able to do.

import java.util.List;

public interface TradingStrategy {
    String getName();

    List<TradingSignal> generateSignals(List<DailyPrice> prices);

}
