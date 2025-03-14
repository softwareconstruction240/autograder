package edu.byu.cs.model;

/**
 * Represents all the output from running the tests.
 *
 * @param root a {@link TestNode} representing the results of the tests
 * @param extraCredit a {@link TestNode} representing the results of the extra credit tests
 * @param coverage a {@link CoverageAnalysis} representing the results for code coverage from the tests
 * @param error the error output from running the process of running the tests. This is what shows
 *              when you click on the 'see details' button if the tests produced errors.
 */
public record TestOutput(TestNode root, TestNode extraCredit, CoverageAnalysis coverage, String error) {
}
