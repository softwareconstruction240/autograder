package edu.byu.cs.model;

import java.util.EnumMap;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param items
 * @param passed
 * @param notes
 */
public record Rubric(
        EnumMap<RubricType, RubricItem> items,
        boolean passed,
        String notes
) {

    /**
     * Calculates the total number of points in all items
     *
     * @return total number of points contained by this rubric
     */
    public float getTotalPoints() {
        float total = 0f;
        for(RubricItem item : items.values()) {
            if(item != null) {
                total += item.results().score();
            }
        }
        return total;
    }

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
            TestAnalysis testResults,
            String textResults
    ) {
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
        GRADING_ISSUE
    }
}