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
}
