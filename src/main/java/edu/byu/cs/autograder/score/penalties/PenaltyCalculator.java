package edu.byu.cs.autograder.score.penalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationReport;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.autograder.score.ScorerHelper;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Submission;

import java.time.ZonedDateTime;

public interface PenaltyCalculator {
    Submission applyPenalty(Rubric rubric, int daysLate, GradingContext gradingContext,
                            CommitVerificationReport commitReport) throws DataAccessException, GradingException;
    String makePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes);
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
    public default Submission generateSubmissionObject(Rubric rubric, CommitVerificationReport commitVerificationReport,
                                                       int numDaysLate, Rubric.ScorePair scores, String notes, GradingContext gradingContext)
            throws GradingException, DataAccessException {
        if (commitVerificationReport == null) {
            return null; // This is allowed.
        }

        CommitVerificationResult commitVerificationResult = commitVerificationReport.result();

        if (!commitVerificationResult.verified()){
            notes += commitVerificationResult.failureMessage();
        }
        String headHash = commitVerificationResult.headHash();
        String netId = gradingContext.netId();

        Integer maxLateDays = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);

        notes = makePenaltyNotes(numDaysLate, maxLateDays, notes);

        ZonedDateTime handInDate = ScorerHelper.getHandInDateZoned(netId);
        Submission.VerifiedStatus verifiedStatus;
        if (commitVerificationResult.verified()) {
            verifiedStatus = commitVerificationResult.isCachedResponse() ?
                    Submission.VerifiedStatus.PreviouslyApproved : Submission.VerifiedStatus.ApprovedAutomatically;
        } else {
            verifiedStatus = Submission.VerifiedStatus.Unapproved;
        }
        if (commitVerificationResult.penaltyPct() > 0) {
            scores = new Rubric.ScorePair(Scorer.prepareModifiedScore(scores.score(), commitVerificationResult.penaltyPct()), scores.rawScore());
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
                null,
                -numDaysLate
        );
    }
}
