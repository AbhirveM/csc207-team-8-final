package use_case.moving_average;

/**
 * Describes how the configuration use case reports its result.
 */
public interface ConfigureMovingAverageOutputBoundary {

    void prepareSuccessView(
            ConfigureMovingAverageOutputData outputData);

    void prepareFailView(String errorMessage);
}