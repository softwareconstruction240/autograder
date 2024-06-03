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

import java.util.EnumMap;
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
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = new EnumMap<>(Rubric.RubricType.class);

        for(Rubric.RubricType type : Rubric.RubricType.values()) {
            Rubric.RubricItem rubricItem = rubric.items().get(type);
            if (rubricItem != null) {
                RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(type);
                if (configItem == null) {
                    throw new GradingException(String.format("Rubric not configured for %s %s", phase, type));
                }

                rubricItems.put(type, new Rubric.RubricItem(rubricItem.category(),
                        convertPoints(rubricItem.results(), configItem.points()), rubricItem.criteria()));
            }
        }

        return new Rubric(rubricItems,
                null, null, null, //TODO: Remove these between semesters
                rubric.passed(), rubric.notes());
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

        for(Rubric.RubricType type : Rubric.RubricType.values()) {
            items.putAll(convertToCanvasFormat(rubric.items().get(type), lateAdjustment, phase,
                    config.items().get(type), type).items());
        }

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
