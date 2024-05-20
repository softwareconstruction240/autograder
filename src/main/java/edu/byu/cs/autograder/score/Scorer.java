package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasRubricItem;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.model.*;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.HashMap;

import static edu.byu.cs.model.Submission.VerifiedStatus;

public class Scorer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scorer.class);

    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private static final float PER_DAY_LATE_PENALTY = 0.1F;
    private final GradingContext gradingContext;

    public Scorer(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    /**
     * Main entry point for the {@link Scorer} class.
     * This method takes in a rubric and a commit verification result
     * and scores them together.
     * <br>
     * When appropriate, it will save the score the grade-book,
     * but it always returns a {@link Submission} that can be
     * @param rubric A freshly generated {@link Rubric} from the grading system.
     * @param commitVerificationResult The associated {@link CommitVerificationResult} from the verification system.
     * @return A {@link Submission} ready to save in the database.
     * @throws GradingException When pre-conditions are not met.
     * @throws DataAccessException When the database cannot be accessed.
     */
    public Submission score(Rubric rubric, CommitVerificationResult commitVerificationResult) throws GradingException, DataAccessException {
        gradingContext.observer().update("Grading...");

        rubric = transformRubric(rubric);

        // Exit early when the score isn't important
        if (gradingContext.admin() || !PhaseUtils.isPhaseGraded(gradingContext.phase())) {
            return generateSubmissionObject(rubric, commitVerificationResult, 0, getScore(rubric), "");
        }

        int daysLate = new LateDayCalculator().calculateLateDays(gradingContext.phase(), gradingContext.netId());
        float thisScore = calculateScoreWithLatePenalty(rubric, daysLate);

        // Validate several conditions before submitting to the grade-book
        if (!rubric.passed()) {
            return generateSubmissionObject(rubric, commitVerificationResult, daysLate, thisScore, "");
        } else if (!commitVerificationResult.verified()) {
            return generateSubmissionObject(rubric, commitVerificationResult, daysLate, thisScore, commitVerificationResult.failureMessage());
        } else {
            // The student (may) receive a score in canvas!
            return attemptSendToCanvas(rubric, commitVerificationResult, daysLate, thisScore, false);
        }
    }

    /**
     * Transforms the values in the rubric from percentage grading results to point valued scores.
     * The resulting {@link Rubric} is ready to save in the database and give to the grade-book system.
     *
     * @param rubric A freshly generated Rubric still containing decimal scores from the grading process.
     * @return A new Rubric with values ready for the grade-book.
     * @throws GradingException When pre-conditions are not met.
     * @throws DataAccessException When the database cannot be accessed.
     */
    private Rubric transformRubric(Rubric rubric) throws GradingException, DataAccessException {
        rubric = CanvasUtils.decimalScoreToPoints(gradingContext.phase(), rubric);
        return annotateRubric(rubric);
    }

    /**
     * Uses several values stored in the {@link GradingContext} to send the provided Rubric to Canvas.
     * <br>
     * Note that this operation will be performed carefully so that existing RubricItem's in Canvas
     * will not be overwritten by the operation.
     *
     * @param rubric A {@link Rubric} containing values to set in Canvas.
     *               Any items not set will be populated with their value from Canvas.
     * @param forceSendScore Forces the grade to be submitted, even if it would lower the student's score.
     * @throws GradingException When preconditions are not met.
     * @throws DataAccessException When the database cannot be reached.
     */
    public void attemptSendToCanvas(Rubric rubric, boolean forceSendScore) throws GradingException, DataAccessException {
        attemptSendToCanvas(rubric, null, 0, 0f, forceSendScore);
    }

    /**
     * Carefully submits the score to Canvas when it helps the student's grade.
     * Returns the submission, now with all missing rubric items populated with their previous values from Canvas.
     * <br>
     * Calling this method constitutes a successful, verified submission that will be submitted to canvas.
     *
     * @param rubric Required.
     * @param commitVerificationResult Required when originally creating a submission.
     *                                 Can be null when sending scores to Canvas; this will disable
     *                                 any automatic point deductions for verification, and also result in
     *                                 <code>null</code> being returned instead of a {@link Submission}.
     * @param daysLate Required. Used to add a note to the resulting submission object.
     * @param thisScore Required. Used to place a value in the {@link Submission} object.
     *                  The Canvas grade is based entirely on the provided {@link Rubric}.
     * @param forceSendScore Forces the grade to be submitted, even if it would lower the student's score.
     * @return A construction Submission for continued processing
     * @throws DataAccessException When the database can't be reached.
     * @throws GradingException When other conditions fail.
     */
    private Submission attemptSendToCanvas(
            Rubric rubric, CommitVerificationResult commitVerificationResult,
            int daysLate, float thisScore, boolean forceSendScore
    ) throws DataAccessException, GradingException {

        if (!ApplicationProperties.useCanvas()) {
            return generateSubmissionObject(rubric, commitVerificationResult, daysLate, thisScore,
                    "Would have attempted grade-book submission, but skipped due to application properties.");
        }

        int canvasUserId = getCanvasUserId();
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(gradingContext.phase());

        CanvasRubricAssessment existingAssessment = getExistingAssessment(canvasUserId, assignmentNum);
        CanvasRubricAssessment newAssessment =
                addExistingPoints(constructCanvasRubricAssessment(rubric, daysLate), existingAssessment);
        setCommitVerificationPenalty(newAssessment, gradingContext, commitVerificationResult);

        // prevent score from being saved to canvas if it will lower their score
        Submission submission;
        if (!forceSendScore && totalPoints(newAssessment) <= totalPoints(existingAssessment)) {
            String notes = "Submission did not improve current score. Score not saved to Canvas.\n";
            submission = generateSubmissionObject(rubric, commitVerificationResult, daysLate, thisScore, notes);
        } else {
            submission = generateSubmissionObject(rubric, commitVerificationResult, daysLate, thisScore, "");
            sendToCanvas(canvasUserId, assignmentNum, newAssessment, rubric.notes());
        }

        return submission;
    }

    /**
     * Simplifying overload that modifies the {@link CanvasRubricAssessment} by setting the GIT_COMMITS penalty properly.
     *
     * @see Scorer#setCommitVerificationPenalty(CanvasRubricAssessment, Phase, int, String) for more details.
     *
     * @param assessment The assessment to modify.
     * @param gradingContext Represents the current grading.
     * @param verification The evaluated CommitVerificationResult. If null, then no effects will take place.
     * @throws GradingException When grading errors occur such as the phase not having a GIT_COMMITS rubric item.
     */
    private static void setCommitVerificationPenalty(CanvasRubricAssessment assessment, GradingContext gradingContext,
                                                    CommitVerificationResult verification) throws GradingException {
        if (verification == null) return;
        setCommitVerificationPenalty(assessment, gradingContext.phase(), verification.penaltyPct(), verification.failureMessage());
    }

    /**
     * Modifies a received CanvasRubricAssessment by setting the GitCommit rubric item for particular phase to the proper value.
     * <br>
     * Calculates the penalty value based on the total score of the assessment excluding any existing commit penalty.
     * Any existing penalty will be cleared out as part of the calculation process.
     * <br>
     * Expects a CanvasRubricAssessment to be passed in with existing values already loaded from Canvas.
     *
     * @param assessment The assessment to modify.
     * @param phase The current phase being graded.
     * @param penaltyPct The penalty percentage to reduce.
     * @param commitComment A comment to attach with the score.
     * @throws GradingException When certain conditions are not met.
     */
    public static void setCommitVerificationPenalty(CanvasRubricAssessment assessment, Phase phase, int penaltyPct,
                                                    String commitComment) throws GradingException {
        String commitRubricId = PhaseUtils.getCanvasRubricId(Rubric.RubricType.GIT_COMMITS, phase);

        // Prepare conditions and then calculate penalty
        CanvasRubricItem emptyRubricItem = new CanvasRubricItem("", 0);
        assessment.insertItem(commitRubricId, emptyRubricItem);
        float commitPenalty = getCommitPenaltyValue(penaltyPct, assessment);

        assessment.insertItem(commitRubricId, new CanvasRubricItem(commitComment, commitPenalty));
    }

    /**
     * Determines the penalty value that should be placed in the GIT_COMMITS rubric item
     * so that the overall score calculates properly.
     * <br>
     * This method expects any existing GIT_COMMITS penalty to already be cleared out.
     *
     * @param penaltyPct The percentage by which to reduce the total score
     * @param assessment The canvas rubric item that will be scored. Not modified.
     * @return A float representing the proper score.
     */
    private static float getCommitPenaltyValue(int penaltyPct, CanvasRubricAssessment assessment) {
        if (penaltyPct <= 0) {
            return 0f;
        }

        // Calculate the penalty
        float rawScore = totalPoints(assessment);
        float approvedScore = SubmissionHelper.prepareModifiedScore(rawScore, penaltyPct);
        return approvedScore - rawScore;
    }

    private int getCanvasUserId() throws DataAccessException {
        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(gradingContext.netId());
        return user.canvasUserId();
    }

    private CanvasRubricAssessment constructCanvasRubricAssessment(Rubric rubric, int daysLate)
            throws DataAccessException, GradingException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
        float lateAdjustment = daysLate * PER_DAY_LATE_PENALTY;
        return CanvasUtils.convertToAssessment(rubric, rubricConfig, lateAdjustment, gradingContext.phase());
    }

    /**
     * Annotates the rubric with notes and passed status
     *
     * @param rubric the rubric to annotate
     * @return the annotated rubric
     */
    private Rubric annotateRubric(Rubric rubric) {
        return new Rubric(
                rubric.passoffTests(),
                rubric.unitTests(),
                rubric.quality(),
                null,
                passed(rubric),
                rubric.notes()
        );
    }

    private boolean passed(Rubric rubric) {
        boolean passed = true;

        boolean isPassoffRequired = PhaseUtils.isPassoffRequired(gradingContext.phase());
        if (isPassoffRequired && rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < rubric.passoffTests().results().possiblePoints())
                passed = false;

        return passed;
    }

    /**
     * Returns a new CanvasRubricAssessment that represents the result of merging `assessment` into `existing`.
     * <br>
     * This is used to take as defaults any scores from `existing` that were not modified by `assessment` to produce
     * a complete CanvasRubricAssessment.
     *
     * @param assessment Represents the new scores that will override scores in `existing`.
     * @param existing These scores will be used if not defined in `assessment`. Should usually come from Canvas.
     * @return A new CanvasRubricAssessment.
     */
    private CanvasRubricAssessment addExistingPoints(CanvasRubricAssessment assessment, CanvasRubricAssessment existing) {
        if(existing == null) return assessment.clone();

        HashMap<String, CanvasRubricItem> compareItems = new HashMap<>();
        compareItems.putAll(existing.items());
        compareItems.putAll(assessment.items());
        return new CanvasRubricAssessment(compareItems);
    }

    private CanvasRubricAssessment getExistingAssessment(int userId, int assignmentNum) throws GradingException {
        try {
            CanvasSubmission submission = CanvasService.getCanvasIntegration().getSubmission(userId, assignmentNum);
            return submission.rubric_assessment();
        } catch (CanvasException e) {
            LOGGER.error("Exception from canvas", e);
            throw new GradingException("Could not contact canvas", e);
        }
    }

    private static float totalPoints(CanvasRubricAssessment assessment) {
        float points = 0;
        if(assessment == null) return points;
        for(CanvasRubricItem item : assessment.items().values()) {
            points += item.points();
        }
        return points;
    }

    private float calculateScoreWithLatePenalty(Rubric rubric, int numDaysLate) throws GradingException, DataAccessException {
        float score = getScore(rubric);
        score *= 1 - (numDaysLate * PER_DAY_LATE_PENALTY);
        if (score < 0) score = 0;
        return score;
    }

    /**
     * Gets the score for the phase
     *
     * @return the score
     */
    private float getScore(Rubric rubric) throws GradingException, DataAccessException {
        int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(gradingContext.phase());

        if (totalPossiblePoints == 0)
            throw new GradingException("Total possible points for phase " + gradingContext.phase() + " is 0");

        float score = 0;
        if (rubric.passoffTests() != null)
            score += rubric.passoffTests().results().score();

        if (rubric.unitTests() != null)
            score += rubric.unitTests().results().score();

        if (rubric.quality() != null)
            score += rubric.quality().results().score();

        // TODO: Also account for other RubricItems like GIT_COMMITS?

        return score / totalPossiblePoints;
    }

    /**
     * Prepares the necessary data pieces to construct a {@link Submission}.
     * This can be saved in the database, and has information which is
     * displayed to the user.
     * <br>
     * Note that this object is not sent directly to any grade-book.
     * Other objects are constructed independently for that purpose.
     *
     * @param rubric A fully transformed and populated Rubric.
     * @param commitVerificationResult Results from the commit verification system.
     *                                 If this value is null, the function will return null.
     * @param numDaysLate The number of days late this submission was handed-in.
     *                    For note generating purposes only; this is not used to
     *                    calculate any penalties.
     * @param score The final approved score on the submission represented in points.
     * @param notes Any notes that are associated with the submission.
     *              More comments may be added to this string while preparing the Submission.
     */
    private Submission generateSubmissionObject(Rubric rubric, CommitVerificationResult commitVerificationResult,
                                                int numDaysLate, float score, String notes)
            throws GradingException, DataAccessException {
        if (commitVerificationResult == null) {
            return null; // This is allowed.
        }

        String headHash = commitVerificationResult.headHash();
        String netId = gradingContext.netId();

        if (numDaysLate > 0)
            notes += numDaysLate + " days late. -" + (int)(numDaysLate * PER_DAY_LATE_PENALTY * 100) + "%";

        ZonedDateTime handInDate = ScorerHelper.getHandInDateZoned(netId);
        Submission.VerifiedStatus verifiedStatus;
        if (commitVerificationResult.verified()) {
            verifiedStatus = commitVerificationResult.isCachedResponse() ?
                    VerifiedStatus.PreviouslyApproved : VerifiedStatus.ApprovedAutomatically;
        } else {
            verifiedStatus = VerifiedStatus.Unapproved;
        }
        if (commitVerificationResult.penaltyPct() > 0) {
            score = SubmissionHelper.prepareModifiedScore(score, commitVerificationResult.penaltyPct());
            notes += "Commit history approved with a penalty of %d%%".formatted(commitVerificationResult.penaltyPct());
        }

        return new Submission(
                netId,
                gradingContext.repoUrl(),
                headHash,
                handInDate.toInstant(),
                gradingContext.phase(),
                rubric.passed(),
                score,
                notes,
                rubric,
                gradingContext.admin(),
                verifiedStatus,
                null
        );
    }

    private void sendToCanvas(int userId, int assignmentNum, CanvasRubricAssessment assessment, String notes)
            throws GradingException {
        sendToCanvas(userId, assignmentNum, assessment, notes, gradingContext.netId());
    }

    private static void sendToCanvas(int userId, int assignmentNum, CanvasRubricAssessment assessment, String notes, String netId) throws GradingException {
        try {
            CanvasService.getCanvasIntegration().submitGrade(userId, assignmentNum, assessment, notes);
        } catch (CanvasException e) {
            LOGGER.error("Error submitting to canvas for user {}", netId, e);
            throw new GradingException("Error contacting canvas to record scores");
        }
    }
}
