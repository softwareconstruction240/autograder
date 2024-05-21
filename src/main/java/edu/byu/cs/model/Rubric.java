package edu.byu.cs.model;

import edu.byu.cs.autograder.test.TestAnalyzer;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param passoffTests
 * @param unitTests
 * @param quality
 * @param gitCommits
 * @param passed
 * @param notes
 */
public record Rubric(
        RubricItem passoffTests,
        RubricItem unitTests,
        RubricItem quality,
        RubricItem gitCommits,
        boolean passed,
        String notes
) {

    /**
     * Represents a single rubric item
     *
     * @param category
     * @param results  The results of the rubric item
     * @param criteria The criteria of the rubric item
     */
    public record RubricItem(
            String category,
            Results results,
            String criteria
    ) { }

    /**
     * Represents the results of a rubric item. textResults or testResults may be null, but not both
     *
     * @param notes
     * @param score
     * @param testResults
     * @param textResults
     * @param possiblePoints
     */
    public record Results(
            String notes,
            Float score,
            Integer possiblePoints,
            TestAnalyzer.TestAnalysis testResults,
            String textResults
    ) { }

    public enum RubricType {
        PASSOFF_TESTS,
        UNIT_TESTS,
        QUALITY,
        GIT_COMMITS
    }

    public RubricItem[] allRubricItems() {
        return new RubricItem[] {
                passoffTests,
                unitTests,
                quality,
                gitCommits
        };
    }
}
