package use_case.moving_average;

/**
 * The operation available to a Moving Average configuration controller.
 */
public interface ConfigureMovingAverageInputBoundary {

    void execute(ConfigureMovingAverageInputData inputData);
}