package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationReport;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasRubricItem;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.*;
import edu.byu.cs.model.Rubric.RubricItem;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static edu.byu.cs.model.Submission.VerifiedStatus;

/**
 * Scores a submission, applying appropriate penalties before attempting to send the score to Canvas.
 * The penalties applied are for late days and commit penalties due to insufficient commits. A score
 * will never send to Canvas if it doesn't improve the score already recorded in Canvas.
 */
public class Scorer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scorer.class);

    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private final float PER_DAY_LATE_PENALTY;
    private final GradingContext gradingContext;
    private final LateDayCalculator lateDayCalculator;

    public Scorer(GradingContext gradingContext, LateDayCalculator lateDayCalculator) {
        this.gradingContext = gradingContext;
        this.lateDayCalculator = lateDayCalculator;

        try {
            ConfigurationDao dao = DaoService.getConfigurationDao();
            PER_DAY_LATE_PENALTY = dao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);
        } catch (DataAccessException e) {
            LOGGER.error("Error while getting Per Day Late Penalty for Scorer.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Main entry point for the {@link Scorer} class.
     * This method takes in a rubric and a commit verification result
     * and scores them together.
     * <br>
     * When appropriate, it will save the score in the grade-book,
     * but it always returns a {@link Submission} that will be saved in the database.
     *
     * @param rubric A freshly generated {@link Rubric} from the grading system.
     * @param commitVerificationReport The report from the verification system.
     * @return A {@link Submission} ready to save in the database.
     * @throws GradingException When pre-conditions are not met.
     * @throws DataAccessException When the database cannot be accessed.
     */
    public Submission score(Rubric rubric, CommitVerificationReport commitVerificationReport) throws GradingException, DataAccessException {
        gradingContext.observer().update("Grading...");

        rubric = transformRubric(rubric);

        // Exit early when the score isn't important
        if (gradingContext.admin() || !PhaseUtils.isPhaseGraded(gradingContext.phase())) {
            return generateSubmissionObject(rubric, commitVerificationReport, 0, getScores(rubric), "");
        }

        int daysLate = lateDayCalculator.calculateLateDays(gradingContext.phase(), gradingContext.netId());
        rubric = applyLatePenalty(rubric, daysLate);
        ScorePair scores = getScores(rubric);

        // Validate several conditions before submitting to the grade-book
        if (!rubric.passed()) {
            return generateSubmissionObject(rubric, commitVerificationReport, daysLate, scores, "");
        }

        CommitVerificationResult commitVerificationResult = commitVerificationReport.result();
        if (!commitVerificationResult.verified()) {
            return generateSubmissionObject(rubric, commitVerificationReport, daysLate, scores, commitVerificationResult.failureMessage());
        } else {
            // The student (may) receive a score in canvas!
            return successfullyProcessSubmission(rubric, commitVerificationReport, daysLate, scores);
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
        return new Rubric(rubric.items(), passed(rubric), rubric.notes());
    }


    /**
     * Carefully submits the score to Canvas when it helps the student's grade.
     * Returns the submission, now with all missing rubric items populated with their previous values from Canvas.
     * <br>
     * Calling this method constitutes a successful, verified submission that will be submitted to canvas.
     *
     * @param rubric                   The rubric for the submission
     * @param commitVerificationReport Required when originally creating a submission.
     *                                 Can be null when sending scores to Canvas; this will disable
     *                                 any automatic point deductions for verification, and also result in
     *                                 <code>null</code> being returned instead of a {@link Submission}.
     * @param daysLate                 Required. Used to add a note to the resulting submission object.
     * @param scores                Required. Used to place values in the {@link Submission} object.
     *                                 The Canvas grade is based entirely on the provided {@link Rubric}.
     * @return A construction Submission for continued processing
     * @throws DataAccessException When the database can't be reached.
     * @throws GradingException    When other conditions fail.
     */
    private Submission successfullyProcessSubmission(Rubric rubric, CommitVerificationReport commitVerificationReport,
                                                     int daysLate, ScorePair scores) throws DataAccessException, GradingException {

        if (!ApplicationProperties.useCanvas()) {
            return generateSubmissionObject(rubric, commitVerificationReport, daysLate, scores,
                    "Would have attempted grade-book submission, but skipped due to application properties.");
        }

        CommitVerificationResult commitVerificationResult = commitVerificationReport.result();
        AssessmentSubmittalRemnants submittalRemnants = attemptSendToCanvas(rubric, commitVerificationResult);
        return generateSubmissionObject(rubric, commitVerificationReport, daysLate, scores, submittalRemnants.notes);
    }

    /**
     * Uses several values stored in the {@link GradingContext} to send the provided Rubric to Canvas.
     * <br>
     * Note that this operation will be performed carefully so that existing RubricItem's in Canvas
     * will not be overwritten by the operation.
     *
     * @param rubric A {@link Rubric} containing values to set in Canvas.
     *               Any items not set will be populated with their value from Canvas.
     * @param phase The phase being graded
     * @param netId Net ID of student being
     * @param penaltyPct The approved GIT_COMMITS penalty percentage
     * @throws GradingException When preconditions are not met.
     * @throws DataAccessException When the database cannot be reached.
     */
    public static void attemptSendToCanvas(Rubric rubric, Phase phase, String netId, int penaltyPct, String commitPenaltyMsg) throws GradingException, DataAccessException {
        int canvasUserId = getCanvasUserId(netId);
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);

        CanvasRubricAssessment newAssessment = constructCanvasRubricAssessment(rubric, phase);

        if (PhaseUtils.phaseHasCommitPenalty(phase)) {
            setCommitVerificationPenalty(newAssessment, phase, penaltyPct, commitPenaltyMsg);
        }

        sendToCanvas(canvasUserId, assignmentNum, newAssessment, rubric.notes(), netId);
    }

    /**
     * Sends a rubric to Canvas, respecting configuration for lowering a student's score.
     *
     * @param rubric The Rubric to submit
     * @param commitVerificationResult Will be used to apply the Git Commits penalty
     * @return {@link AssessmentSubmittalRemnants} that contains some information about the results of the operation.
     * @throws DataAccessException When the database cannot be accessed.
     * @throws GradingException When pre-conditions are not met.
     */
    private AssessmentSubmittalRemnants attemptSendToCanvas(Rubric rubric, CommitVerificationResult commitVerificationResult)
            throws DataAccessException, GradingException {

        int canvasUserId = getCanvasUserId(gradingContext.netId());
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(gradingContext.phase());

        CanvasRubricAssessment existingAssessment = getExistingAssessment(canvasUserId, assignmentNum);
        CanvasRubricAssessment newAssessment = constructCanvasRubricAssessment(rubric, gradingContext.phase());
        newAssessment = addExistingPoints(newAssessment, existingAssessment);

        if (PhaseUtils.phaseHasCommitPenalty(gradingContext.phase())) {
            setCommitVerificationPenalty(newAssessment, gradingContext.phase(), commitVerificationResult);
        }

        // prevent score from being saved to canvas if it will lower their score
        String notes = "";
        boolean didSend = false;
        float newPoints = totalPoints(newAssessment);
        if (newPoints <= totalPoints(existingAssessment)) {
            notes = "Submission did not improve current score. Score not saved to Canvas.\n";
        } else {
            didSend = true;
            sendToCanvas(canvasUserId, assignmentNum, newAssessment, rubric.notes());
        }

        return new AssessmentSubmittalRemnants(didSend, newPoints, notes);
    }

    private record AssessmentSubmittalRemnants(
            boolean didSend,
            float pointsSent,
            String notes
    ) { }

    /**
     * Simplifying overload that modifies the {@link CanvasRubricAssessment} by setting the GIT_COMMITS penalty properly.
     *
     * @see Scorer#setCommitVerificationPenalty(CanvasRubricAssessment, Phase, int, String) for more details.
     *
     * @param assessment The assessment to modify.
     * @param phase Represents the phase currently being scored.
     * @param verification The evaluated CommitVerificationResult. If null, then no effects will take place.
     * @throws GradingException When grading errors occur such as the phase not having a GIT_COMMITS rubric item.
     */
    private static void setCommitVerificationPenalty(CanvasRubricAssessment assessment, Phase phase,
                                                    CommitVerificationResult verification) throws GradingException {
        if (verification == null) return;
        setCommitVerificationPenalty(assessment, phase, verification.penaltyPct(), verification.failureMessage());
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
        String commitRubricId;
        try {
            commitRubricId = PhaseUtils.getCanvasRubricId(Rubric.RubricType.GIT_COMMITS, phase);
        } catch (DataAccessException e) {
            throw new GradingException(e);
        }


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
        float approvedScore = prepareModifiedScore(rawScore, penaltyPct);
        return approvedScore - rawScore;
    }

    private static int getCanvasUserId(String netId) throws DataAccessException {
        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(netId);
        return user.canvasUserId();
    }

    private static CanvasRubricAssessment constructCanvasRubricAssessment(Rubric rubric, Phase phase)
            throws DataAccessException, GradingException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(phase);
        return CanvasUtils.convertToAssessment(rubric, rubricConfig, phase);
    }

    private boolean passed(Rubric rubric) {
        Collection<Rubric.RubricType> requiredTypes = PhaseUtils.requiredRubricTypes(gradingContext.phase());
        for(Rubric.RubricType type : requiredTypes) {
            Rubric.RubricItem typedRubricItem = rubric.items().get(type);
            if(typedRubricItem != null && typedRubricItem.results() != null &&
                    typedRubricItem.results().score() < typedRubricItem.results().possiblePoints()) {
                return false;
            }
        }
        return true;
    }

    private Rubric applyLatePenalty(Rubric rubric, int daysLate) throws DataAccessException {
        Collection<Submission> previousSubmissions = DaoService.getSubmissionDao().getSubmissionsForPhase(gradingContext.netId(), gradingContext.phase());
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = new EnumMap<>(Rubric.RubricType.class);
        float lateScoreMultiplier = 1 - (daysLate * PER_DAY_LATE_PENALTY);
        Integer maxLateDays = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        for (Map.Entry<Rubric.RubricType, Rubric.RubricItem> entry : rubric.items().entrySet()) {
            Rubric.RubricType rubricType = entry.getKey();
            Rubric.RubricItem rubricItem = entry.getValue();
            rubricItem = addLateNotesToRubricItem(rubricItem, daysLate, maxLateDays);
            Rubric.Results results = mergeResultsWithPrevious(rubricType, rubricItem, previousSubmissions, lateScoreMultiplier);
            rubricItem = new Rubric.RubricItem(rubricItem.category(), results, rubricItem.criteria());
            items.put(rubricType, rubricItem);
        }
        return new Rubric(items, rubric.passed(), rubric.notes());
    }
    
    private RubricItem addLateNotesToRubricItem(RubricItem rubricItem, int daysLate, int maxLateDays){
        Rubric.Results results = rubricItem.results();
        results = new Rubric.Results(
            makeLatePenaltyNotes(daysLate, maxLateDays, results.notes()),
            results.score(),
            results.rawScore(),
            results.possiblePoints(),
            results.testResults(),
            results.textResults());
        return new RubricItem(rubricItem.category(), results, rubricItem.criteria());
    }

    private Rubric.Results mergeResultsWithPrevious(Rubric.RubricType rubricType, Rubric.RubricItem rubricItem,
                                                    Collection<Submission> previousSubmissions, float scoreMultiplier) {
        Rubric.Results results = rubricItem.results();

        String notes = results.notes();
        float startingScore = results.score() * scoreMultiplier;
        float score = startingScore;

        for (Submission previousSubmission : previousSubmissions) {
            if(previousSubmission.passed()) {
                Rubric.RubricItem previousItem = previousSubmission.rubric().items().get(rubricType);
                if (previousItem != null && previousItem.results().rawScore() <= results.rawScore()) {
                    score = Math.max(score, previousItem.results().score());
                }
            }
        }

        if(score > startingScore) {
            notes = String.format("Deferring to less-penalized prior score of %s/%d\n%s",
                    Math.round(score * 100) / 100.0, rubricItem.results().possiblePoints(), notes);
        }

        return new Rubric.Results(notes,
                score,
                results.score(),
                results.possiblePoints(),
                results.testResults(),
                results.textResults());
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

    /**
     * Gets the score and rawScore for the rubric and phase
     *
     * @return a ScorePair with both the score and rawScore as a percentage value from [0-1].
     */
    private ScorePair getScores(Rubric rubric) throws GradingException, DataAccessException {
        RubricConfigDao rubricConfigDao = DaoService.getRubricConfigDao();
        int totalPossiblePoints = rubricConfigDao.getPhaseTotalPossiblePoints(gradingContext.phase());

        if (totalPossiblePoints == 0) {
            throw new GradingException("Total possible points for phase " + gradingContext.phase() + " is 0");
        }

        if (rubricConfigDao.getRubricConfig(gradingContext.phase()) instanceof RubricConfig rubricConfig &&
                rubricConfig.items().get(Rubric.RubricType.EXTRA_CREDIT) instanceof RubricConfig.RubricConfigItem item) {
            totalPossiblePoints -= item.points();
        }

        float score = 0;
        float rawScore = 0;
        for (Rubric.RubricType type : Rubric.RubricType.values()) {
            var rubricItem = rubric.items().get(type);
            if (rubricItem == null) continue;
            score += rubricItem.results().score();
            rawScore += rubricItem.results().rawScore();
        }

        return new ScorePair(score / totalPossiblePoints, rawScore / totalPossiblePoints);
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
     * @param commitVerificationReport Results from the commit verification system.
     *                                 If this value is null, the function will return null.
     * @param numDaysLate The number of days late this submission was handed-in.
     *                    For note generating purposes only; this is not used to
     *                    calculate any penalties.
     * @param scores The final approved score and rawScore on the submission represented in points.
     * @param notes Any notes that are associated with the submission.
     *              More comments may be added to this string while preparing the Submission.
     */
    public Submission generateSubmissionObject(Rubric rubric, CommitVerificationReport commitVerificationReport,
                                                int numDaysLate, ScorePair scores, String notes)
            throws GradingException, DataAccessException {
        if (commitVerificationReport == null) {
            return null; // This is allowed.
        }

        CommitVerificationResult commitVerificationResult = commitVerificationReport.result();
        String headHash = commitVerificationResult.headHash();
        String netId = gradingContext.netId();

        Integer maxLateDays = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);

        notes = makeLatePenaltyNotes(numDaysLate, maxLateDays, notes);

        ZonedDateTime handInDate = ScorerHelper.getHandInDateZoned(netId);
        Submission.VerifiedStatus verifiedStatus;
        if (commitVerificationResult.verified()) {
            verifiedStatus = commitVerificationResult.isCachedResponse() ?
                    VerifiedStatus.PreviouslyApproved : VerifiedStatus.ApprovedAutomatically;
        } else {
            verifiedStatus = VerifiedStatus.Unapproved;
        }
        if (commitVerificationResult.penaltyPct() > 0) {
            scores = new ScorePair(prepareModifiedScore(scores.score(), commitVerificationResult.penaltyPct()), scores.rawScore());
            notes += "Commit history approved with a penalty of %d%%".formatted(commitVerificationResult.penaltyPct());
        }

        return new Submission(
                netId,
                gradingContext.repoUrl(),
                headHash,
                handInDate.toInstant(),
                gradingContext.phase(),
                rubric.passed(),
                scores.score(),
                scores.rawScore(),
                notes,
                rubric,
                gradingContext.admin(),
                verifiedStatus,
                commitVerificationReport.context(),
                commitVerificationResult,
                null
        );
    }

    private String makeLatePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes) {
        if (numDaysLate <= 0) {
            return origNotes;
        }

        String penaltyPercentage = String.format("-%d%%", (int)(numDaysLate * PER_DAY_LATE_PENALTY * 100));
        String lateNotes;
        if (numDaysLate >= maxLateDays) {
            lateNotes = "Late penalty maxed out: " + penaltyPercentage;
        } else {
            lateNotes = String.format("%d days late: %s", numDaysLate, penaltyPercentage);
        }

        if (origNotes == null || origNotes.isBlank()) {
            return lateNotes;
        }
        return String.format("%s\n%s", origNotes, lateNotes);
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

    public static float prepareModifiedScore(float originalScore, int penaltyPct) {
        return originalScore * (100 - penaltyPct) / 100f;
    }

    public record ScorePair(float score, float rawScore) {}
}
