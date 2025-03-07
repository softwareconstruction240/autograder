package edu.byu.cs.model;

/**
 * Represents the results for code coverage from running tests for a given class.
 * Note that the requirement for code coverage can change between lines and branches.
 *
 * @param className the name of the class tested for code coverage
 * @param packageName the name of the package to which the class belongs
 * @param covered the number of lines/branches covered
 * @param missed the number of lines/branches missed
 */
public record ClassCoverageAnalysis(String className, String packageName, int covered, int missed) {
}
