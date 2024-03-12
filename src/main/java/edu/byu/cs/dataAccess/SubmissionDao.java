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
     * Gets all submissions for the given phase
     *
     * @return all submissions for the given phase
     */
    Collection<Submission> getAllLatestSubmissions();

    /**
     * Gets the most recent batch of submissions
     * @param batchSize number of submissions to return
     * @return
     */
    Collection<Submission> getLatestSubmissionBatch(int batchSize);

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
}
