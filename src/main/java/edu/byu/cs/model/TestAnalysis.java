package edu.byu.cs.model;

/**
 *
 *
 * @param root a {@link TestNode} representing the results of the tests for an item
 * @param extraCredit a {@link TestNode} representing the results of the extra credit tests for an item
 * @param error
 */
public record TestAnalysis(TestNode root, TestNode extraCredit, String error) {
}
