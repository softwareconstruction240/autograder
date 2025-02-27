package edu.byu.cs.model;

/**
 * Represents all the output from running the tests.
 *
 * @param root a {@link TestNode} representing the results of the tests
 * @param extraCredit a {@link TestNode} representing the results of the extra credit tests
 * @param error the error output from running the process of running the tests. This is what shows
 *              when you click on the 'see details' button if the tests produced errors.
 */
public record TestAnalysis(TestNode root, TestNode extraCredit, String error) {
}
