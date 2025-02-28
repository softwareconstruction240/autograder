package edu.byu.cs.model;

import java.util.EnumMap;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param items the map of rubric items for a Canvas assignment
 * @param passed the result of the grading process. Determines whether the rubric items get updated in canvas
 * @param notes the notes resulting from the grading process
 */
public record Rubric(
        EnumMap<RubricType, RubricItem> items,
        boolean passed,
        String notes
) {

    /**
     * Represents a single rubric item
     *
     * @param category The category of the rubric item
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
     * @param notes The notes for the rubric item
     * @param score The score for the rubric item
     * @param rawScore The score before penalties for the rubric item
     * @param possiblePoints The amount of possible points for the rubric item
     * @param testResults The results of the tests for the rubric item
     * @param textResults Any other results for the rubric item
     */
    public record Results(
            String notes,
            Float score,
            Float rawScore,
            Integer possiblePoints,
            TestAnalysis testResults,
            String textResults
    ) {
        public Results(String notes, Float score, Integer possiblePoints, TestAnalysis testResults, String textResults) {
            this(notes, score, score, possiblePoints, testResults, textResults);
        }

        public static Results testError(String notes, TestAnalysis testResults) {
            return new Results(notes, 0f, 0, testResults, null);
        }

        public static Results textError(String notes, String textResults) {
            return new Results(notes, 0f, 0, null, textResults);
        }
    }
    public enum RubricType {
        PASSOFF_TESTS,
        UNIT_TESTS,
        QUALITY,
        GIT_COMMITS,
        GITHUB_REPO,
        GRADING_ISSUE;
    }
}
