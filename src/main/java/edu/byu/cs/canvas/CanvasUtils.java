package edu.byu.cs.canvas;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

public class CanvasUtils {

    /**
     * Converts the score to points for each rubric item. E.g. if the rubric item is worth 10 points and the score is 0.5,
     * the points will be updated to 5
     *
     * @param phase  the phase of the rubric
     * @param rubric the rubric to convert
     * @return the rubric with the score converted to points
     */
    public static Rubric decimalScoreToPoints(Phase phase, Rubric rubric) {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);

        Rubric.RubricItem convertedPassoffTests = null;
        Rubric.RubricItem convertedUnitTests = null;
        Rubric.RubricItem convertedQuality = null;

        if (rubric.passoffTests() != null) {
            if (rubricConfig.passoffTests() == null)
                throw new RuntimeException("Rubric not configured for " + phase.toString() + " passoff tests");

            convertedPassoffTests = new Rubric.RubricItem(
                    rubric.passoffTests().category(),
                    convertPoints(rubric.passoffTests().results(), rubricConfig.passoffTests().points()),
                    rubric.passoffTests().criteria());
        }

        if (rubric.unitTests() != null) {
            if (rubricConfig.unitTests() == null)
                throw new RuntimeException("Rubric not configured for " + phase.toString() + " unit tests");

            convertedUnitTests = new Rubric.RubricItem(
                    rubric.unitTests().category(),
                    convertPoints(rubric.unitTests().results(), rubricConfig.unitTests().points()),
                    rubric.unitTests().criteria());
        }

        if (rubric.quality() != null) {
            if (rubricConfig.quality() == null)
                throw new RuntimeException("Rubric not configured for " + phase.toString() + " quality");

            convertedQuality = new Rubric.RubricItem(
                    rubric.quality().category(),
                    convertPoints(rubric.quality().results(), rubricConfig.quality().points()),
                    rubric.quality().criteria());
        }

        return new Rubric(
                convertedPassoffTests,
                convertedUnitTests,
                convertedQuality
        );
    }

    private static Rubric.Results convertPoints(Rubric.Results results, int points) {
        return new Rubric.Results(
                results.notes(),
                results.score() * points,
                results.possiblePoints(),
                results.testResults(),
                results.textResults()
        );
    }
}
