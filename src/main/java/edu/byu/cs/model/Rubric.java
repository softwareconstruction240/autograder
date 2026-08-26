package edu.byu.cs.model;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;

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
            TestOutput testResults,
            String textResults
    ) {
        public Results(String notes, Float score, Integer possiblePoints, TestOutput testResults, String textResults) {
            this(notes, score, score, possiblePoints, testResults, textResults);
        }

        public static Results testError(String notes, TestOutput testResults) {
            return new Results(notes, 0f, 0, testResults, null);
        }

        public static Results textError(String notes, String textResults) {
            return new Results(notes, 0f, 0, null, textResults);
        }
    }
    public enum RubricType {
        PASSOFF_TESTS,
        UNIT_TESTS,
        EXTRA_CREDIT,
        QUALITY,
        GIT_COMMITS,
        GITHUB_REPO,
        GRADING_ISSUE;
    }

    public record ScorePair(float score, float rawScore) {}

    /**
     * Gets the score and rawScore for the rubric and phase
     *
     * @return a ScorePair with both the score and rawScore as a percentage value from [0-1].
     */
    public ScorePair getScores(Phase phase) throws GradingException, DataAccessException{
        int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(phase);

        if (totalPossiblePoints == 0) {
            throw new GradingException("Total possible points for phase " + phase + " is 0");
        }

        if (DaoService.getRubricConfigDao().getRubricConfig(phase) instanceof RubricConfig rubricConfig &&
                rubricConfig.items().get(Rubric.RubricType.EXTRA_CREDIT) instanceof RubricConfig.RubricConfigItem item) {
            totalPossiblePoints -= item.points();
        }

        float score = 0;
        float rawScore = 0;
        for (Rubric.RubricType type : Rubric.RubricType.values()) {
            var rubricItem = this.items().get(type);
            if (rubricItem == null) continue;
            score += rubricItem.results().score();
            rawScore += rubricItem.results().rawScore();
        }
        return new ScorePair(score/totalPossiblePoints, rawScore/totalPossiblePoints);
    }
}
