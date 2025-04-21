package edu.byu.cs.model;

import java.util.Collection;

/**
 * Represents the results for code coverage from running tests for a particular phase
 *
 * @param classAnalyses the collection of code coverage results for each class tested
 */
public record CoverageAnalysis(Collection<ClassCoverageAnalysis> classAnalyses) {

}
