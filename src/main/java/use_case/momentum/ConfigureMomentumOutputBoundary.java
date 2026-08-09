package use_case.momentum;

/**
 * Describes how the configuration use case reports its result.
 */
public interface ConfigureMomentumOutputBoundary {

    void prepareSuccessView(
            ConfigureMomentumOutputData outputData);

    void prepareFailView(String errorMessage);
}
