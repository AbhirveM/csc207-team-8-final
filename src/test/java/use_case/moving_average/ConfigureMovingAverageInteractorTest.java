package use_case.moving_average;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigureMovingAverageInteractorTest {

    @Test
    void createsConfigurationForValidInput() {
        final TestPresenter presenter = new TestPresenter();

        final ConfigureMovingAverageInputBoundary interactor =
                new ConfigureMovingAverageInteractor(presenter);

        final ConfigureMovingAverageInputData inputData =
                new ConfigureMovingAverageInputData("10", "50");

        interactor.execute(inputData);

        assertTrue(presenter.successCalled);
        assertFalse(presenter.failureCalled);
        assertNotNull(presenter.outputData);
        assertNull(presenter.errorMessage);

        assertEquals(
                10,
                presenter.outputData
                        .getConfiguration()
                        .getShortWindow());

        assertEquals(
                50,
                presenter.outputData
                        .getConfiguration()
                        .getLongWindow());
    }

    @Test
    void acceptsSurroundingWhitespace() {
        final TestPresenter presenter = new TestPresenter();

        final ConfigureMovingAverageInputBoundary interactor =
                new ConfigureMovingAverageInteractor(presenter);

        interactor.execute(
                new ConfigureMovingAverageInputData(
                        " 10 ",
                        " 50 "));

        assertTrue(presenter.successCalled);
        assertFalse(presenter.failureCalled);

        assertEquals(
                10,
                presenter.outputData
                        .getConfiguration()
                        .getShortWindow());

        assertEquals(
                50,
                presenter.outputData
                        .getConfiguration()
                        .getLongWindow());
    }

    @Test
    void reportsFailureForNonIntegerShortWindow() {
        assertFailure("ten", "50");
    }

    @Test
    void reportsFailureForNonIntegerLongWindow() {
        assertFailure("10", "fifty");
    }

    @Test
    void reportsFailureForBlankWindow() {
        assertFailure("", "50");
    }

    @Test
    void reportsFailureForZeroShortWindow() {
        assertFailure("0", "50");
    }

    @Test
    void reportsFailureForNegativeLongWindow() {
        assertFailure("10", "-50");
    }

    @Test
    void reportsFailureForEqualWindows() {
        assertFailure("10", "10");
    }

    @Test
    void reportsFailureWhenShortWindowIsLarger() {
        assertFailure("50", "10");
    }

    @Test
    void reportsFailureForNullInputData() {
        final TestPresenter presenter = new TestPresenter();

        final ConfigureMovingAverageInputBoundary interactor =
                new ConfigureMovingAverageInteractor(presenter);

        interactor.execute(null);

        assertFalse(presenter.successCalled);
        assertTrue(presenter.failureCalled);
        assertNull(presenter.outputData);
        assertNotNull(presenter.errorMessage);
        assertFalse(presenter.errorMessage.isBlank());
    }

    /**
     * Runs an invalid input and verifies that the interactor reports
     * failure without producing successful output.
     */
    private void assertFailure(
            String shortWindow,
            String longWindow) {

        final TestPresenter presenter = new TestPresenter();

        final ConfigureMovingAverageInputBoundary interactor =
                new ConfigureMovingAverageInteractor(presenter);

        interactor.execute(
                new ConfigureMovingAverageInputData(
                        shortWindow,
                        longWindow));

        assertFalse(presenter.successCalled);
        assertTrue(presenter.failureCalled);
        assertNull(presenter.outputData);
        assertNotNull(presenter.errorMessage);
        assertFalse(presenter.errorMessage.isBlank());
    }

    /**
     * A test implementation of the output boundary.
     *
     * It records what the interactor attempted to present so that
     * each test can examine the result.
     */
    private static final class TestPresenter
            implements ConfigureMovingAverageOutputBoundary {

        private boolean successCalled;
        private boolean failureCalled;
        private ConfigureMovingAverageOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(
                ConfigureMovingAverageOutputData outputData) {
            successCalled = true;
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failureCalled = true;
            this.errorMessage = errorMessage;
        }
    }
}