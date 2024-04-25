package edu.byu.cs.dataAccess;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

public class SubmissionHelper {

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

        var phaseSubmissions = submissionDao.getSubmissionsForPhase(studentNetId, phase);
        Float modifiedScore;
        Submission.ScoreVerification individualVerification;
        for (var submission : phaseSubmissions) {
            if (!submission.passed()) continue;

            try {
                individualVerification = SubmissionHelper.prepareScoreVerification(scoreVerification, submission);
                modifiedScore = SubmissionHelper.prepareModifiedScore(scoreVerification);
                submissionDao.manuallyApproveSubmission(
                        submission, modifiedScore, individualVerification);
            } catch (ItemNotFoundException e) {
                throw new RuntimeException(e);
            }
            affected++;
        }

        return affected;
    }

    private static Submission.ScoreVerification prepareScoreVerification(
            Submission.ScoreVerification ogVerified,
            Submission submission
    ) {
        return new Submission.ScoreVerification(
                submission.score(),
                ogVerified.approvingNetId(),
                ogVerified.approvedTimestamp(),
                ogVerified.penaltyPct()
        );
    }

    public static Float prepareModifiedScore(float originalScore, Submission.ScoreVerification scoreVerification) {
        return originalScore * (100 - scoreVerification.penaltyPct()) / 100f;
    }
    private static Float prepareModifiedScore(Submission.ScoreVerification scoreVerification) {
        return prepareModifiedScore(scoreVerification.originalScore(), scoreVerification);
    }
}
