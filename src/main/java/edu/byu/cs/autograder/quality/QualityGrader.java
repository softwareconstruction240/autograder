package edu.byu.cs.autograder.quality;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

public class QualityGrader {
    private final GradingContext gradingContext;

    public QualityGrader(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    /**
     * Runs quality checks on the student's code
     *
     * @return the results of the quality checks as a CanvasIntegration.RubricItem
     */
    public Rubric.Results runQualityChecks() throws GradingException, DataAccessException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
        if(rubricConfig.quality() == null) return null;
        gradingContext.observer().update("Running code quality...");

        QualityAnalyzer analyzer = new QualityAnalyzer();

        QualityAnalyzer.QualityAnalysis quality = analyzer.runQualityChecks(gradingContext.stageRepo());

        return new Rubric.Results(quality.notes(), quality.score(),
                rubricConfig.quality().points(), null, quality.results());
    }
}
