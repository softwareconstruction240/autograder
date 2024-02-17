package edu.byu.cs.canvas;

import edu.byu.cs.autograder.TestAnalyzer;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param passoffTests
 * @param unitTests
 * @param quality
 */
public record Rubric(
        RubricItem passoffTests,
        RubricItem unitTests,
        RubricItem quality
) {

    /**
     * Represents a single rubric item
     *
     * @param description The description of the rubric item
     * @param results     The results of the rubric item
     */
    public record RubricItem(
            String description,
            Results results

    ) {
    }

    /**
     * Represents the results of a rubric item. textResults or testResults may be null, but not both
     *
     * @param notes
     * @param score
     * @param testResults
     * @param textResults
     */
    public record Results(
            String notes,
            Float score,
            TestAnalyzer.TestNode testResults,
            String textResults
    ) {
    }
}
