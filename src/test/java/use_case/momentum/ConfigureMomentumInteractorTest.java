package use_case.momentum;

import entity.MomentumConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigureMomentumInteractorTest {

    @Test
    void successTest() {
        TestPresenter presenter = new TestPresenter();
        ConfigureMomentumInteractor interactor =
                new ConfigureMomentumInteractor(presenter);

        ConfigureMomentumInputData inputData =
                new ConfigureMomentumInputData(
                        "14",
                        "30",
                        "70");

        interactor.execute(inputData);

        assertNotNull(presenter.outputData);
        assertNull(presenter.errorMessage);

        MomentumConfiguration configuration =
                presenter.outputData.getConfiguration();

        assertEquals(14, configuration.getPeriod());
        assertEquals(30.0,
                configuration.getOversoldThreshold());
        assertEquals(70.0,
                configuration.getOverboughtThreshold());
    }

    @Test
    void invalidPeriodFormatTest() {
        TestPresenter presenter = new TestPresenter();
        ConfigureMomentumInteractor interactor =
                new ConfigureMomentumInteractor(presenter);

        ConfigureMomentumInputData inputData =
                new ConfigureMomentumInputData(
                        "abc",
                        "30",
                        "70");

        interactor.execute(inputData);

        assertNull(presenter.outputData);
        assertEquals(
                "RSI period must be a whole number",
                presenter.errorMessage);
    }

    @Test
    void invalidThresholdOrderTest() {
        TestPresenter presenter = new TestPresenter();
        ConfigureMomentumInteractor interactor =
                new ConfigureMomentumInteractor(presenter);

        ConfigureMomentumInputData inputData =
                new ConfigureMomentumInputData(
                        "14",
                        "80",
                        "30");

        interactor.execute(inputData);

        assertNull(presenter.outputData);
        assertEquals(
                "Oversold threshold must be smaller than overbought threshold",
                presenter.errorMessage);
    }

    @Test
    void nullInputTest() {
        TestPresenter presenter = new TestPresenter();
        ConfigureMomentumInteractor interactor =
                new ConfigureMomentumInteractor(presenter);

        interactor.execute(null);

        assertNull(presenter.outputData);
        assertEquals(
                "Configuration input cannot be null",
                presenter.errorMessage);
    }

    private static final class TestPresenter
            implements ConfigureMomentumOutputBoundary {

        private ConfigureMomentumOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(
                ConfigureMomentumOutputData outputData) {
            this.outputData = outputData;
            this.errorMessage = null;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.outputData = null;
            this.errorMessage = errorMessage;
        }
    }
}