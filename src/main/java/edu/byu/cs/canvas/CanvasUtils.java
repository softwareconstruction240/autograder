package edu.byu.cs.canvas;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasRubricItem;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;

import java.util.HashMap;
import java.util.Map;

public class CanvasUtils {

    /**
     * Converts the score to points for each rubric item. E.g. if the rubric item is worth 10 points and the score is
     * 0.5,
     * the points will be updated to 5
     *
     * @param phase  the phase of the rubric
     * @param rubric the rubric to convert
     * @return the rubric with the score converted to points
     */
    public static Rubric decimalScoreToPoints(Phase phase, Rubric rubric) throws GradingException, DataAccessException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);

        Rubric.RubricItem convertedPassoffTests = null;
        Rubric.RubricItem convertedUnitTests = null;
        Rubric.RubricItem convertedQuality = null;

        if (rubric.passoffTests() != null) {
            if (rubricConfig.passoffTests() == null)
                throw new GradingException("Rubric not configured for " + phase.toString() + " passoff tests");

            convertedPassoffTests = new Rubric.RubricItem(
                    rubric.passoffTests().category(),
                    convertPoints(rubric.passoffTests().results(), rubricConfig.passoffTests().points()),
                    rubric.passoffTests().criteria());
        }

        if (rubric.unitTests() != null) {
            if (rubricConfig.unitTests() == null)
                throw new GradingException("Rubric not configured for " + phase.toString() + " unit tests");

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
                convertedQuality,
                null,
                rubric.passed(),
                rubric.notes());
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

    public static CanvasRubricAssessment convertToAssessment(Rubric rubric, RubricConfig config,
                                                             float lateAdjustment, Phase phase)
            throws GradingException {
        Map<String, CanvasRubricItem> items = new HashMap<>();
        items.putAll(convertToCanvasFormat(rubric.passoffTests(), lateAdjustment, phase, config.passoffTests(),
                Rubric.RubricType.PASSOFF_TESTS).items());
        items.putAll(convertToCanvasFormat(rubric.unitTests(), lateAdjustment, phase, config.unitTests(),
                Rubric.RubricType.UNIT_TESTS).items());
        items.putAll(convertToCanvasFormat(rubric.quality(), lateAdjustment, phase, config.quality(),
                Rubric.RubricType.QUALITY).items());
        items.putAll(convertToCanvasFormat(rubric.gitCommits(), lateAdjustment, phase, config.gitCommits(),
                Rubric.RubricType.GIT_COMMITS).items());
        return new CanvasRubricAssessment(items);
    }

    private static CanvasRubricAssessment convertToCanvasFormat(Rubric.RubricItem rubricItem,
                                                                            float lateAdjustment, Phase phase, RubricConfig.RubricConfigItem rubricConfigItem,
                                                                            Rubric.RubricType rubricType) throws GradingException {
        Map<String, CanvasRubricItem> items = new HashMap<>();
        if (rubricConfigItem != null && rubricConfigItem.points() > 0) {
            Rubric.Results results = rubricItem.results();
            items.put(PhaseUtils.getCanvasRubricId(rubricType, phase),
                    new CanvasRubricItem(results.notes(), results.score() * (1 - lateAdjustment)));
        }
        return new CanvasRubricAssessment(items);
    }
}
