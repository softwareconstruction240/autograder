package edu.byu.cs.dataAccess;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;

public interface SubmissionDao {

    /**
     * Inserts a new submission into the database
     *
     * @param submission the submission to insert
     */
    void insertSubmission(Submission submission);

    /**
     * Gets all submissions for the given netId and phase
     *
     * @param netId the netId to get submissions for
     * @param phase the phase to get submissions for
     * @return all submissions for the given netId and phase
     */
    Collection<Submission> getSubmissionsForPhase(String netId, Phase phase);

    /**
     * Gets all submissions for the given netId
     *
     * @param netId the netId to get submissions for
     * @return all submissions for the given netId
     */
    Collection<Submission> getSubmissionsForUser(String netId);

    /**
     * Gets all latest submissions
     *
     * @return all latest submissions
     */
    Collection<Submission> getAllLatestSubmissions();

    /**
     * Gets the X most recent latest submissions
     *
     * @param batchSize defines how many submissions to return. Set batchSize to a negative int to get All submissions
     * @return the most recent X submissions
     */
    Collection<Submission> getAllLatestSubmissions(int batchSize);

    /**
     * Removes all submissions for the given netId
     * <br/><strong>Note: this will likely only be used for the test student</strong>
     *
     * @param netId the netId to remove submissions for
     */
    void removeSubmissionsByNetId(String netId);

    /**
     * Gets the first passing submission chronologically for the given phase
     *
     * @param netId the student's netId
     * @param phase the phase
     * @return the submission object, or null
     */
    Submission getFirstPassingSubmission(String netId, Phase phase);

    float getBestScoreForPhase(String netId, Phase phase);

    /**
     * Gets all submissions that `passed` the grading for any phase.
     * This includes submissions that were not approved for meeting
     * certain thresholds. Therefore, not all the submissions in this
     * result set are necessarily in Canvas.
     *
     * @param netId The netId of the student to filter by
     * @return A collection of Submission objects, or an empty collection if none.
     */
    Collection<Submission> getAllPassingSubmissions(String netId);

    /**
     * Updates <b>all</b> of the relevant submissions with an appropriate {@link Submission.ScoreVerification}
     * object.
     * <br>
     * Note that while a `ScoreVerification` object is passed in, a different variation of it must be stored
     * on each submission so that each has its appropriate `originalScore` field set.
     * It must also set the `VerifiedStatus` field of each submission to {@link Submission.VerifiedStatus#ApprovedManually}.
     *
     * @param studentNetId Identifies the student to approve
     * @param phase Identifies the phase to approve
     * @param scoreVerification A `ScoreVerification` containing information to set.
     *                          Note that the `originalScore` field will be handled
     *                          for each submission individually.
     * @return An integer representing the number of affected submissions.
     */
    int approveWithheldSubmission(
            String studentNetId, Phase phase,
            Submission.ScoreVerification scoreVerification
    );
}
