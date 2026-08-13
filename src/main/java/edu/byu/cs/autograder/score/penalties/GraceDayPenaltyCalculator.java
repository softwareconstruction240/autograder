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
    private int totalGraceDays;

    public GraceDayPenaltyCalculator(int canvasUserId) throws GradingException {
        this.canvasUserId = canvasUserId;
        try {
            graceDaysAssignmentId = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.GRACE_DAYS_ASSIGNMENT_NUMBER, Integer.class);
        } catch (DataAccessException e) {
            throw new GradingException(e);
        }
        totalGraceDays = getGraceDays();
    }

    @Override
    public Submission applyPenalty(Rubric rubric, int daysAfterDue, GradingContext gradingContext,
                                   CommitVerificationReport commitReport) throws DataAccessException, GradingException {
        Submission bestSubmission = DaoService.getSubmissionDao().getBestSubmissionForPhase(gradingContext.netId(), gradingContext.phase());
        int graceDaysPreviouslyEarned = 0;

        if (bestSubmission != null) {
            if (totalRubricScore(rubric) <= totalRubricScore(bestSubmission.rubric())) {
                return generateSubmissionObject(rubric, commitReport, daysAfterDue, rubric.getScores(gradingContext.phase()),
                        "Submission not sent to Canvas due to worse score. Grace days unaffected. ", gradingContext);
            }
            graceDaysPreviouslyEarned = bestSubmission.graceDaysEarned();
        }

        int newGraceDayTotal = totalGraceDays - graceDaysPreviouslyEarned - daysAfterDue;
        if (newGraceDayTotal < 0) {
            Rubric zero = zeroScore(rubric);
            return generateSubmissionObject(zero, commitReport, daysAfterDue, zero.getScores(gradingContext.phase()),
                    "Score is zero due to not enough Grace Days available. Grace days unaffected. ", gradingContext);
        }
        int changeInGraceDays = newGraceDayTotal - totalGraceDays;
        Integer finalGraceDays = sendGraceDaysToCanvas(changeInGraceDays, newGraceDayTotal);

        // Compute effective days late: accounts for previous early submission
        int effectiveDaysLate = daysAfterDue;
        boolean isRelativeToPrevious = false;
        if (bestSubmission != null && graceDaysPreviouslyEarned > 0) {
            effectiveDaysLate = graceDaysPreviouslyEarned + daysAfterDue;
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

    @Override
    public int getMaxLateDays() {
        return totalGraceDays;
    }

    public String makePenaltyNotes(int daysAfterDue, int newTotalGraceDays, String origNotes, boolean isRelativeToPreviousSubmission) {
        String lateNotes;
        if (daysAfterDue == 0){
            lateNotes =  "Assignment turned in on time. Grace days unaffected. ";
        } else if (daysAfterDue < 0){
            lateNotes =  String.format("Assignment turned in %d day%s early. New total grace days: %d. ", -daysAfterDue, daysAfterDue == -1 ? "" : "s", newTotalGraceDays);
        } else {
            String lateContext = isRelativeToPreviousSubmission ? " (relative to a previous early submission)" : "";
            lateNotes = String.format("Assignment turned in %d day%s late%s. New total grace days: %d. ", daysAfterDue, daysAfterDue == 1 ? "" : "s", lateContext, newTotalGraceDays);
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

    public void updateGraceDays() throws GradingException {
        this.totalGraceDays = getGraceDays();
    }

    private Integer sendGraceDaysToCanvas(int changeInGraceDays, int newGraceDayTotal) throws GradingException {
        try {
            //if the score will remain the same, short circuit the canvas call
            if (changeInGraceDays != 0){
                CanvasService.getCanvasIntegration().submitGrade(canvasUserId, graceDaysAssignmentId, ((Integer) newGraceDayTotal).floatValue(), null);
            }
            if (totalGraceDays > newGraceDayTotal){
                LOGGER.info("Subtracted {} grace days (total {}) from Canvas for UserId {}", -changeInGraceDays, newGraceDayTotal, canvasUserId);
            } else if (totalGraceDays == newGraceDayTotal) {
                LOGGER.info("Grace days unaffected for UserId {}", canvasUserId);
            } else {
                LOGGER.info("Added {} grace days (total {}) to Canvas for UserId {}", changeInGraceDays, newGraceDayTotal, canvasUserId);
            }
            totalGraceDays = newGraceDayTotal;
            return newGraceDayTotal;
        } catch (CanvasException e) {
            throw new GradingException("Could not update Grace Days from Canvas", e);
        }
    }

    private float totalRubricScore(Rubric rubric) {
        float score = 0;
        for (Rubric.RubricItem item : rubric.items().values()) {
            score += item.results().rawScore();
        }
        return score;
    }

    private Rubric zeroScore(Rubric rubric) {
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
        return new Rubric(items, rubric.passed(), "Score is zero due to not enough Grace Days available. Grace days unaffected. ");
    }
}
