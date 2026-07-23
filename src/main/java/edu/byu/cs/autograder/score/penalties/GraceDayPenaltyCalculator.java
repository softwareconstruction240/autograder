package edu.byu.cs.autograder.score.penalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class GraceDayPenaltyCalculator implements PenaltyCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraceDayPenaltyCalculator.class);

    private final Integer graceDaysAssignmentId;
    private final int canvasUserId;

    public GraceDayPenaltyCalculator (int canvasUserId) throws GradingException {
        this.canvasUserId = canvasUserId;
        try {
            graceDaysAssignmentId = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.GRACE_DAYS_ASSIGNMENT_NUMBER, Integer.class);
        } catch (DataAccessException e) {
            throw new GradingException(e);
        }
    }

    @Override
    public Rubric applyPenalty(Rubric rubric, int daysLate, GradingContext gradingContext)
            throws DataAccessException {
        Submission bestSubmission = DaoService.getSubmissionDao().getBestSubmissionForPhase(gradingContext.netId(), gradingContext.phase());
        Integer initialGraceDays = getGraceDays();
        int graceDaysEarned = 0;

        if (bestSubmission != null){
            if (totalRubricScore(rubric) <= totalRubricScore(bestSubmission.rubric())) {
                return rubric;
            }
            graceDaysEarned = bestSubmission.graceDaysEarned();
        }

        int graceDayDifference = initialGraceDays - graceDaysEarned - daysLate;
        if (graceDayDifference < 0){
            return zeroScore(rubric, daysLate, initialGraceDays);
        }
        Integer finalGraceDays = sendGraceDaysToCanvas(graceDayDifference);
        return new Rubric(
                rubric.items(),
                rubric.passed(),
                makePenaltyNotes(daysLate, finalGraceDays, rubric.notes())
        );
    }

    @Override
    public String makePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes) {
        return origNotes;
    }

    public Integer getGraceDays() throws GradingException {
        try {
            CanvasSubmission submission = CanvasService.getCanvasIntegration().getSubmission(canvasUserId, graceDaysAssignmentId);
            return submission.score().intValue();
        } catch (CanvasException e) {
            throw new GradingException("Unable to retrieve Grace Days from Canvas", e);
        }
    }

    private Integer sendGraceDaysToCanvas(int days) throws GradingException{
        Integer totalGraceDays = getGraceDays();
        if (totalGraceDays-days < 0){
            return 0;
        }
        if (days == 0){
            return totalGraceDays;
        }
        totalGraceDays -= days;
        try {
            CanvasService.getCanvasIntegration().submitGrade(canvasUserId, graceDaysAssignmentId, totalGraceDays.floatValue(), null);
            LOGGER.info("Subtracted {} grace days from Canvas for UserId {}", days, canvasUserId);
            return totalGraceDays;
        } catch (CanvasException e) {
            throw new GradingException("Could not update Grace Days from Canvas", e);
        }
    }

    private float totalRubricScore(Rubric rubric){
        float score = 0;
        for (Rubric.RubricItem item : rubric.items().values()){
            score += item.results().rawScore();
        }
        return score;
    }

    private Rubric zeroScore(Rubric rubric, int daysLate, int initialGraceDays){
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = new EnumMap<>(Rubric.RubricType.class);
        for (Map.Entry<Rubric.RubricType, Rubric.RubricItem> entry : rubric.items().entrySet()) {
            Rubric.RubricType rubricType = entry.getKey();
            Rubric.RubricItem rubricItem = entry.getValue();
            Rubric.Results results = rubricItem.results();
            results = new Rubric.Results(
                    results.notes(),
                    0f,
                    results.score(),
                    results.possiblePoints(),
                    results.testResults(),
                    results.textResults()
            );
            rubricItem = new Rubric.RubricItem(rubricItem.category(), results, rubricItem.criteria());
            items.put(rubricType, rubricItem);
        }
        return new Rubric(items, rubric.passed(), rubric.notes());
    }
}
