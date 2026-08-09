package interface_adapter.momentum;

import java.util.Objects;

import use_case.momentum.ConfigureMomentumInputBoundary;
import use_case.momentum.ConfigureMomentumInputData;

/**
 * Controller for configuring the Momentum strategy.
 */
public final class MomentumController {

    private final ConfigureMomentumInputBoundary interactor;

    public MomentumController(
            ConfigureMomentumInputBoundary interactor) {
        this.interactor = Objects.requireNonNull(
                interactor,
                "Interactor cannot be null");
    }

    /**
     * Configures the Momentum strategy using user-supplied values.
     *
     * @param period the RSI period
     * @param oversoldThreshold the oversold threshold
     * @param overboughtThreshold the overbought threshold
     */
    public void execute(
            String period,
            String oversoldThreshold,
            String overboughtThreshold) {

        final ConfigureMomentumInputData inputData =
                new ConfigureMomentumInputData(
                        period,
                        oversoldThreshold,
                        overboughtThreshold);

        interactor.execute(inputData);
    }
}
