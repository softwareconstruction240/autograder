package edu.byu.cs.dataAccess.daoInterface;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import org.eclipse.jgit.annotations.NonNull;

import java.util.Collection;

public interface SubmissionDao {

    /**
     * Inserts a new submission into the database
     *
     * @param submission the submission to insert
     */
    void insertSubmission(Submission submission) throws DataAccessException;

    /**
     * Gets all submissions for the given netId and phase
     *
     * @param netId the netId to get submissions for
     * @param phase the phase to get submissions for
     * @return all submissions for the given netId and phase
     */
    Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) throws DataAccessException;

    /**
     * Gets all submissions for the given netId
     *
     * @param netId the netId to get submissions for
     * @return all submissions for the given netId
     */
    Collection<Submission> getSubmissionsForUser(String netId) throws DataAccessException;

    Submission getLastSubmissionForUser(String netId) throws DataAccessException;

    /**
     * Gets all latest submissions
     *
     * @return all latest submissions
     */
    Collection<Submission> getAllLatestSubmissions() throws DataAccessException;

    /**
     * Gets the X most recent latest submissions
     *
     * @param batchSize defines how many submissions to return. Set batchSize to a negative int to get All submissions
     * @return the most recent X submissions
     */
    Collection<Submission> getAllLatestSubmissions(int batchSize) throws DataAccessException;

    /**
     * Removes all submissions for the given netId
     * <br/><strong>Note: this will likely only be used for the test student and admins</strong>
     *
     * @param netId     the netId to remove submissions for
     * @param daysOld   how many days the submission needs to exist to be removed
     */
    void removeSubmissionsByNetId(String netId, int daysOld) throws DataAccessException;

    /**
     * Gets the first passing submission chronologically for the given phase
     *
     * @param netId the student's netId
     * @param phase the phase
     * @return the submission object, or null
     */
    Submission getFirstPassingSubmission(String netId, Phase phase) throws DataAccessException;

    /**
     * Retrieves the highest scoring submission of a student's submissions for a phase.
     * <br>
     *
     * @param netId Representing a student.
     * @param phase Representing a phase to submit
     * @return The submission with the highest score for the phase, or null if no submissions exist.
     */
    Submission getBestSubmissionForPhase(String netId, Phase phase) throws DataAccessException;

    /**
     * Gets all submissions that `passed` the grading for any phase.
     * This includes submissions that were not approved for meeting
     * certain thresholds. Therefore, not all the submissions in this
     * result set are necessarily in Canvas.
     *
     * @param netId The netId of the student to filter by
     * @return A collection of Submission objects, or an empty collection if none.
     */
    Collection<Submission> getAllPassingSubmissions(String netId) throws DataAccessException;

    /**
     * Modifies an existing submission in the collection to mark it manually approved.
     * <br>
     * Does all the following to the submission:
     * <ul>
     *     <li>Sets the `verifiedStatus` to {@link Submission.VerifiedStatus#ApprovedManually}</li>
     *     <li>Sets the `verification` field to the provided object</li>
     *     <li>Updates the score to the precalculated value</li>
     * </ul>
     *
     * <br>
     * A submission is considered equal if all the following are equivalent:
     * <ul>
     *     <li>netId</li>
     *     <li>phase</li>
     *     <li>headHash</li>
     * </ul>
     *
     * @param submission The submission to modify in the data structure
     * @param scoreVerification The personalized `ScoreVerification` to update in the `Submission`
     * @throws ItemNotFoundException When the `Submission` cannot be located in the collection.
     */
    void manuallyApproveSubmission(
            @NonNull Submission submission,
            @NonNull Float newScore,
            @NonNull Submission.ScoreVerification scoreVerification
    ) throws ItemNotFoundException, DataAccessException;
}
