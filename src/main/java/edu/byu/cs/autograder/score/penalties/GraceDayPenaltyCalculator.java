package edu.byu.cs.autograder.score.penalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationReport;
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
    public Submission applyPenalty(Rubric rubric, int daysLate, GradingContext gradingContext,
                               CommitVerificationReport commitReport) throws DataAccessException, GradingException {
        Submission bestSubmission = DaoService.getSubmissionDao().getBestSubmissionForPhase(gradingContext.netId(), gradingContext.phase());
        Integer initialGraceDays = getGraceDays();
        int graceDaysEarned = 0;

        if (bestSubmission != null){
            if (totalRubricScore(rubric) <= totalRubricScore(bestSubmission.rubric())) {
                return generateSubmissionObject(rubric,commitReport, daysLate, rubric.getScores(gradingContext.phase()),
                        "Submission not sent to Canvas due to worse score. Grace days unaffected.", gradingContext);
            }
            graceDaysEarned = bestSubmission.graceDaysEarned();
        }

        int graceDayDifference = initialGraceDays - graceDaysEarned - daysLate;
        if (graceDayDifference < 0){
            Rubric zero = zeroScore(rubric, daysLate, initialGraceDays);
            return generateSubmissionObject(zero, commitReport, daysLate, zero.getScores(gradingContext.phase()),
                    "Score is zero due to not enough Grace Days available. Grace days unaffected.", gradingContext);
        }
        Integer finalGraceDays = sendGraceDaysToCanvas(graceDayDifference);

        // Compute effective days late: accounts for previous early submission
        int effectiveDaysLate = daysLate;
        boolean isRelativeToPrevious = false;
        if (bestSubmission != null && graceDaysEarned > 0) {
            effectiveDaysLate = graceDaysEarned + daysLate;
            isRelativeToPrevious = true;
        }

        rubric = new Rubric(
                rubric.items(),
                rubric.passed(),
                makePenaltyNotes(effectiveDaysLate, finalGraceDays, rubric.notes(), isRelativeToPrevious)
        );
        return generateSubmissionObject(rubric, commitReport, effectiveDaysLate, rubric.getScores(gradingContext.phase()),
                "", gradingContext);
    }

    @Override
    public String makePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes) {
        return makePenaltyNotes(numDaysLate, maxLateDays, origNotes, false);
    }

    public String makePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes, boolean isRelativeToPreviousSubmission) {
        String lateNotes;
        if (numDaysLate == 0){
            lateNotes =  "Assignment turned in on time. Grace days unaffected.";
        } else if (numDaysLate < 0){
            lateNotes =  String.format("Assignment turned in %d day%s early. New total grace days: %d.", -numDaysLate, numDaysLate == -1 ? "" : "s", maxLateDays);
        } else {
            String lateContext = isRelativeToPreviousSubmission ? " (relative to a previous early submission)" : "";
            lateNotes = String.format("Assignment turned in %d day%s late%s. New total grace days: %d.", numDaysLate, numDaysLate == 1 ? "" : "s", lateContext, maxLateDays);
        }

        return String.format("%s\n%s", origNotes, lateNotes);
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
        // FIXME: I believe checks in this method are unnecessary. We have already validated that we have enough grace days to cover the assignment by this point, and the name of the method makes it sound like it should just send the grace days to canvas.
        // It's also less efficient to use getGraceDays multiple times.
      //if (totalGraceDays-days < 0){
      //    return 0;
      //}
      //if (days == 0){
      //    return totalGraceDays;
      //}
      //totalGraceDays -= days;
        try {
            CanvasService.getCanvasIntegration().submitGrade(canvasUserId, graceDaysAssignmentId, ((Integer) days).floatValue(), null);
            if (totalGraceDays > days){
                LOGGER.info("Subtracted {} grace days (total {}) from Canvas for UserId {}", totalGraceDays - days, days, canvasUserId);
            } else if (totalGraceDays == days){
                LOGGER.info("Grace days unaffected for UserId {}", canvasUserId);
            } else {
                LOGGER.info("Added {} grace days (total {}) to Canvas for UserId {}", days - totalGraceDays, days, canvasUserId);
            }
            return days;
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
        return new Rubric(items, rubric.passed(), "Score is zero due to not enough Grace Days available. Grace days unaffected.");
    }
}
