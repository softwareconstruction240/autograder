package edu.byu.cs.autograder.git;

import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationContext;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;

/**
 * An in-memory model that reports the decisions, a few key details,
 * and some debug information from the Commit Verification System.
 *
 * @see edu.byu.cs.model.Submission.ScoreVerification#verifiedStatus() for more information about `penaltyPct`.
 * <br>
 * Many of these fields are clearly and intentionally labelled as {@link Nullable} or {@link NonNull}.
 * All the other fields are primitive types and can never be set to null.
 *
 * @param verified Officially reports the verified status of the submission.
 * @param isCachedResponse Whether this was freshly generated, or passed through from a previous decision.
 * @param totalCommits Total number of relevant commits analyzed. Excludes merges and commits before the tail threshold.
 * @param significantCommits Total number of commits above the minimum line threshold.
 * @param numDays The number of unique days with commits. Insignificant commits contribute to this number.
 * @param missingTail Error flag indicates when a tail hash was expected but not found during evaluation.
 * @param penaltyPct A reduction to the phase, if any, when a TA approved an unapproved score. [0-100]
 * @param failureMessage A string that will be presented to the use when this the result is not verified.
 * @param warningMessages A nullable, potentially empty collection of warning messages that should be presented to the user.
 * @param minAllowedThreshold Debug purposes. The min timestamp considered. Null when the evaluation is not performed, such as when <pre>isCachedResponse</pre> is true.
 * @param maxAllowedThreshold Debug. The maximum timestamp considered. Similar conditions to above.
 * @param headHash Debug. The head hash evaluated. Not null. Even when evaluation is skipped, this still represents the hash at the time of consideration.
 * @param tailHash Debug. The tail hash evaluated. Sometimes null if there is no previous passing submission, or if no evaluation was completed.
 */
public record CommitVerificationResult(
        @NonNull boolean verified,
        boolean isCachedResponse,
        int totalCommits,
        int significantCommits,
        int numDays,
        boolean missingTail,
        int penaltyPct,
        @NonNull String failureMessage,
        @Nullable Collection<String> warningMessages,

        // Debug info
        @Nullable Instant minAllowedThreshold,
        @Nullable Instant maxAllowedThreshold,
        @NonNull String headHash,
        @Nullable String tailHash
) {
    public CommitVerificationReport toReport(CommitVerificationContext context) {
        return new CommitVerificationReport(context, this);
    }
}
