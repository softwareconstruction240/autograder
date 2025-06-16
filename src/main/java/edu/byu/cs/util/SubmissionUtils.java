package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;

/**
 * A utility class that provides methods for getting the hash of the HEAD of a remote repository
 * and approving submissions
 */
public class SubmissionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionUtils.class);

    /**
     * Gets the hash of the HEAD of a remote repository using the {@code repoUrl}
     *
     * @param repoUrl the repo URL to get the remote repository
     * @return the hash of the HEAD
     * @throws DataAccessException if the git ls-remote process failed to execute or
     * exited with non-zero exit code
     */
    public static String getRemoteHeadHash(String repoUrl) throws DataAccessException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "ls-remote", repoUrl, "HEAD");
        try {
            ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(processBuilder);
            if (output.statusCode() != 0) {
                throw new DataAccessException("git ls-remote exited with non-zero exit code:\n" + output.stdErr());
            }
            return output.stdOut().split("\\s+")[0];
        } catch (ProcessUtils.ProcessException e) {
            throw new DataAccessException("Failed to execute git ls-remote process.", e);
        }
    }

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
        float approvedScore = Scorer.prepareModifiedScore(withheldSubmission.score(), penaltyPct);
        String gitCommitsComment = "Submission initially blocked due to low commits. Submission approved by admin " + approverNetId;
        Scorer.attemptSendToCanvas(withheldSubmission.rubric(), withheldSubmission.phase(), withheldSubmission.netId(),
                penaltyPct, gitCommitsComment);

        // Done
        LOGGER.info("Approved submission for %s on phase %s with score %f. Approval by %s. Affected %d submissions."
                .formatted(studentNetId, phase.name(), approvedScore, approverNetId, submissionsAffected));
    }

    /**
     * Asserts that a submission by a student for a phase has been approved, throws otherwise
     *
     * @param submissionDao the DAO used to get the submission
     * @param studentNetId the student to approve
     * @param phase the phase for the submission
     * @throws DataAccessException if an issue arises getting the submission from the database
     */
    private static void assertSubmissionUnapproved(SubmissionDao submissionDao, String studentNetId, Phase phase) throws DataAccessException {
        Submission withheldSubmission = submissionDao.getFirstPassingSubmission(studentNetId, phase);
        if (withheldSubmission.isApproved()) {
            throw new RuntimeException(studentNetId + " needs no approval for phase " + phase);
        }
    }

    /**
     * Modifies the submission entries in the database and approves passing withheld submissions
     * for a phase using a manual approval from a TA or professor for a withheld submission
     *
     * @param submissionDao the DAO used to get the submission
     * @param withheldSubmission the withheld submission that was approved
     * @param approvingNetId identifies the TA or professor approving the score
     * @param penaltyPct the penalty percentage (as an int between 0-100)
     * @return an integer representing the number of submissions affected and approved
     * @throws DataAccessException if an issue arises accessing submissions from the database
     */
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
                float modifiedScore = Scorer.prepareModifiedScore(submission.score(), scoreVerification.penaltyPct());
                submissionDao.manuallyApproveSubmission(submission, modifiedScore, subVerification);
            } catch (ItemNotFoundException e) {
                throw new RuntimeException(e);
            }
            affected++;
        }

        return affected;
    }


}
