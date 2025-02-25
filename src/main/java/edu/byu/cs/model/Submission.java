package edu.byu.cs.model;

import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationContext;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.util.Serializer;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents and stores a student submission.
 *
 * @param netId Identifies the student who submitted the code.
 * @param repoUrl Points to the GitHub repo where the code is stored.
 * @param headHash The commit hash at which we graded the submission.
 * @param timestamp The time the submission was handed in.
 * @param phase The phase being graded in this submission.
 * @param passed Signifies that the code passed all the grading tests.
 *               <b>Does NOT signify</b> that the score was approved.
 * @param score <p>The final score assigned to this submission (as a percentage [0-1]) that will
 *              be sent to the grade-book, including penalties and extra credit.</p>
 *              <p>This field will be updated if the score changes because of
 *              additional penalties or manual corrections; however, this is
 *              <b>not the canonical source of truth</b>.</p>
 *              <p>These system deals with scores as values between 0 and 1.
 *              These scores are only converted into <b>points</b> as they are sent
 *              to an external source.</p>
 *              <p>While the AutoGrader is storing scores and updating them,
 *              the real source of truth is the grade-book.</p>
 * @param notes Additional notes displayed to the user.
 *              These usually represent the status of their score, or
 *              provide remarks about why a passing score was not given.
 *              These sometimes contain directions to meet with a TA, etc...
 * @param rubric Contains detailed breakdowns of the score.
 * @param admin Flag identifies when the submission was started by an
 *              admin or developer for testing purposes.
 *              Among other behaviors that may be different, these
 *              submissions will never be stored in the grade-book
 * @param verifiedStatus <p>The status of the verification.</p>
 *                       <p>Old submissions will have a `null` value;
 *                       in this case, verification is assumed to equal
 *                       the {@link Submission#passed} field.</p>
 * @param commitContext Debug. Holds the raw information provided to the
 *                      <pre>GitVerificationStrategy</pre> for approval or denial.
 *                      This notably contains the <pre>CommitsByDay</pre> which lists
 *                      the exact hash codes of commits grouped by the warnings they generated.
 * @param commitResult Debug. Holds the raw commit verification results including computed values.
 * @param verification Represents the approval of the submission.
 *                     Added only after the submission is approved manually.
 */
public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Instant timestamp,
        Phase phase,
        Boolean passed,
        Float score,
        Float rawScore,
        String notes,
        Rubric rubric,
        Boolean admin,
        @Nullable VerifiedStatus verifiedStatus,
        @Nullable CommitVerificationContext commitContext,
        @Nullable CommitVerificationResult commitResult,
        @Nullable ScoreVerification verification
) {

    /**
     * Represents the manual approval of a score after it was withheld.
     * <br>
     * When the submission is approved, this entire record should be added to the `Submission`.
     *
     * @param originalScore The score originally calculated by the server.
     * @param approvingNetId The NetId of the individual who approves the score manually.
     * @param approvedTimestamp The timestamp of the approver approving the score.
     * @param penaltyPct <p>The percentage reduction from the original score.</p>
     *                   <p>This percentage will be reduced from all future submissions
     *                   on this phase as well.</p>
     *                   <p>This should be an int between 0-100</p>
     */
    public record ScoreVerification(
             @NonNull Float originalScore,
             @NonNull String approvingNetId,
             @NonNull Instant approvedTimestamp,
             @NonNull Integer penaltyPct
    ) {
        public ScoreVerification setOriginalScore(float score) {
            return new ScoreVerification(score, approvingNetId, approvedTimestamp, penaltyPct);
        }
    }

    public enum VerifiedStatus {
        Unapproved,
        ApprovedAutomatically,
        ApprovedManually,
        PreviouslyApproved;

        public boolean isApproved() {
            return this != VerifiedStatus.Unapproved;
        }
    }

    public boolean isApproved() {
        if (verifiedStatus == null) {
            return true; // Old submissions without this field are assumed to be approved
        }
        return verifiedStatus.isApproved();
    }

    /**
     * Generates a new {@link Submission} object with certain fields updated to reflect a change in score verification.
     * @param newScore The new score to overwrite.
     * @param newStatus The new {@link VerifiedStatus} to overwrite.
     * @param newVerification The new {@link ScoreVerification} to overwrite.
     * @return {@link Submission} A new object
     */
    public Submission updateApproval(Float newScore, VerifiedStatus newStatus, ScoreVerification newVerification) {
        return new Submission(
                this.netId(),
                this.repoUrl(),
                this.headHash(),
                this.timestamp(),
                this.phase(),
                this.passed(),
                newScore,
                this.rawScore(),
                this.notes(),
                this.rubric(),
                this.admin(),
                newStatus,
                this.commitContext(),
                this.commitResult(),
                newVerification
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return Objects.equals(netId, that.netId) && Objects.equals(headHash, that.headHash) && phase == that.phase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(netId, headHash, phase);
    }

    /**
     * Returns the penalty percentage within a submission, or 0 if it doesn't exist.
     * <br>
     * The existence of the penalty pct doesn't necessarily mean it should be applied.
     * Make sure to respect other related status flags. When needed, this will give a value.
     *
     * @return An int representing the penalty pct.
     */
    public int getPenaltyPct() {
        if (verification == null || verification.penaltyPct() == null) {
            return 0;
        }
        return verification.penaltyPct();
    }

    public static String serializeScoreVerification(@NonNull Submission submission) {
        return serializeScoreVerification(submission.verification);
    }
    public static String serializeScoreVerification(@Nullable ScoreVerification scoreVerification) {
        return serializeObject(scoreVerification);
    }

    public static String serializeCommitContext(@NonNull Submission submission) {
        return serializeObject(submission.commitContext);
    }
    public static String serializeCommitResult(@NonNull Submission submission) {
        return  serializeObject(submission.commitResult);
    }
    private static String serializeObject(@Nullable Object obj) {
        if (obj == null) return null;
        return Serializer.serialize(obj);
    }

    public static String serializeVerifiedStatus(@NonNull Submission submission) {
        return serializeVerifiedStatus(submission.verifiedStatus);
    }
    public static String serializeVerifiedStatus(@Nullable VerifiedStatus verifiedStatus) {
        return verifiedStatus == null ? null : verifiedStatus.name();
    }

}
