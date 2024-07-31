package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;

public class SubmissionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionUtils.class);


    /**
     * Approves a submission.
     * Modifies all existing submissions in the phase with constructed values,
     * and saves a given value into the grade-book.
     *
     * @param studentNetId The student to approve
     * @param phase The phase to approve
     * @param approverNetId Identifies the TA or professor approving the score
     * @param penaltyPct The penalty applied for the reduction. This will be used to reduce all submissions
     */
    public static void approveSubmission(
            @NonNull String studentNetId,
            @NonNull Phase phase,
            @NonNull String approverNetId,
            @NonNull Integer penaltyPct
    ) throws DataAccessException, GradingException {

        // Validate params
        if (studentNetId == null || phase == null || approverNetId == null || penaltyPct == null) {
            throw new IllegalArgumentException("All of studentNetId, approverNetId, and penaltyPct must not be null.");
        }
        if (studentNetId.isBlank() || approverNetId.isBlank()) {
            throw new IllegalArgumentException("Both studentNetId and approverNetId must not be blank");
        }
        if (penaltyPct < 0) {
            throw new IllegalArgumentException("penaltyPct must be greater or equal than 0");
        }

        // Read in data
        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        assertSubmissionUnapproved(submissionDao, studentNetId, phase);
        Submission withheldSubmission = submissionDao.getBestSubmissionForPhase(studentNetId, phase);
        if (withheldSubmission == null) {
            throw new GradingException("No submission was provided nor found for phase " + phase + " with user " + studentNetId);
        }

        // Modify values in our database first
        int submissionsAffected = modifySubmissionEntriesInDatabase(submissionDao, withheldSubmission, approverNetId, penaltyPct);

        // Send score to Grade-book
        float approvedScore = SubmissionUtils.prepareModifiedScore(withheldSubmission.score(), penaltyPct);
        String gitCommitsComment = "Submission initially blocked due to low commits. Submission approved by admin " + approverNetId;
        sendScoreToCanvas(withheldSubmission, penaltyPct, gitCommitsComment);

        // Done
        LOGGER.info("Approved submission for %s on phase %s with score %f. Approval by %s. Affected %d submissions."
                .formatted(studentNetId, phase.name(), approvedScore, approverNetId, submissionsAffected));
    }

    private static void assertSubmissionUnapproved(SubmissionDao submissionDao, String studentNetId, Phase phase) throws DataAccessException {
        Submission withheldSubmission = submissionDao.getFirstPassingSubmission(studentNetId, phase);
        if (withheldSubmission.isApproved()) {
            throw new RuntimeException(studentNetId + " needs no approval for phase " + phase);
        }
    }

    private static int modifySubmissionEntriesInDatabase(
            SubmissionDao submissionDao, Submission withheldSubmission, String approvingNetId, int penaltyPct)
            throws DataAccessException {

        Float originalScore = withheldSubmission.score();
        Instant approvedTimestamp = Instant.now();
        Submission.ScoreVerification scoreVerification =
                new Submission.ScoreVerification(originalScore, approvingNetId, approvedTimestamp, penaltyPct);
        int submissionsAffected = approveWithheldSubmissions(submissionDao, withheldSubmission.netId(),
                withheldSubmission.phase(), scoreVerification);

        if (submissionsAffected < 1) {
            LOGGER.warn("Approving submissions did not affect any submissions. Something probably went wrong.");
        }
        return submissionsAffected;
    }

    /**
     * Submits the <code>approvedScore</code> to Canvas by modifying the GIT_COMMITS rubric item.
     * <br>
     * In this context, the <code>withheldSubmission</code> isn't <i>strictly</i> necessary,
     * but it is used to transfer several fields and values that would need to be transferred individually
     * without it. The most important of these is the <score>field</score>, which will be used to calculate the penalty.
     *
     * @param withheldSubmission The baseline submission to approve
     * @param penaltyPct The percentage that should be reduced from the score for GIT_COMMITS.
     * @param commitPenaltyComment The comment that should be associated with the GIT_COMMITS rubric, if any.
     * @throws DataAccessException When the database cannot be accessed.
     * @throws GradingException When pre-conditions are not met.
     */
    private static void sendScoreToCanvas(Submission withheldSubmission, int penaltyPct, String commitPenaltyComment) throws DataAccessException, GradingException {
        // Prepare and assert arguments
        if (withheldSubmission == null) {
            throw new IllegalArgumentException("Withheld submission cannot be null");
        }
        Scorer scorer = getScorer(withheldSubmission);
        scorer.attemptSendToCanvas(withheldSubmission.rubric(), penaltyPct, commitPenaltyComment);
    }

    /**
     * Constructs an instance of {@link Scorer} using the limited data available in a {@link Submission}.
     *
     * @param submission A submission containing context to extract.
     * @return A constructed Scorer instance.
     */
    private static Scorer getScorer(Submission submission) {
        String studentNetId = submission.netId();
        Phase phase = submission.phase();
        if (studentNetId == null) {
            throw new IllegalArgumentException("Student net ID cannot be null");
        }
        if (phase == null) {
            throw new IllegalArgumentException("Phase cannot be null");
        }

        return new Scorer(new GradingContext(
                studentNetId, phase, null, null, null, null,
                new CommitVerificationConfig(0, 0, 0, 0, 0),
                null, submission.admin()
        ));
    }

    /**
     * Updates <b>all</b> of the relevant submissions with an appropriate {@link Submission.ScoreVerification}
     * object.
     * <br>
     * Note that while a `ScoreVerification` object is passed in, a different variation of it must be stored
     * on each submission so that each has its appropriate `originalScore` field set.
     * It must also set the `VerifiedStatus` field of each submission to {@link Submission.VerifiedStatus#ApprovedManually}.
     *
     * @param submissionDao A SubmissionDao for convenience.
     * @param studentNetId Identifies the student to approve.
     * @param phase Identifies the phase to approve.
     * @param scoreVerification A `ScoreVerification` containing information to set.
     *                          Note that the `originalScore` field will be handled
     *                          for each submission individually.
     * @return An integer representing the number of affected submissions.
     */
    public static int approveWithheldSubmissions(
            SubmissionDao submissionDao, String studentNetId,
            Phase phase, Submission.ScoreVerification scoreVerification)
            throws DataAccessException {
        int affected = 0;

        var phaseSubmissions = new HashSet<>(submissionDao.getSubmissionsForPhase(studentNetId, phase));
        for (var submission : phaseSubmissions) {
            if (!submission.passed()) continue;

            try {
                Submission.ScoreVerification subVerification = scoreVerification.setOriginalScore(submission.score());
                float modifiedScore = prepareModifiedScore(submission.score(), scoreVerification.penaltyPct());
                submissionDao.manuallyApproveSubmission(submission, modifiedScore, subVerification);
            } catch (ItemNotFoundException e) {
                throw new RuntimeException(e);
            }
            affected++;
        }

        return affected;
    }

    public static float prepareModifiedScore(float originalScore, int penaltyPct) {
        return originalScore * (100 - penaltyPct) / 100f;
    }
}
