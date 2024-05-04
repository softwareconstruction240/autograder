package edu.byu.cs.autograder.git;

import org.eclipse.jgit.annotations.NonNull;

import java.time.Instant;

/**
 * An in-memory model that reports the decisions, a few key details,
 * and some debug information from the Commit Verification System.
 *
 * @see edu.byu.cs.model.Submission.ScoreVerification#verifiedStatus() for more information about `penaltyPct`.
 *
 * @param verified Officially reports the verified status of the submission.
 * @param isCachedResponse Whether this was freshly generated, or passed through from a previous decision.
 * @param significantCommits Total number of commits above the minimum line threshold.
 * @param numDays The number of unique days with commits. Insignificant commits contribute to this number.
 * @param penaltyPct A reduction to the phase, if any, when a TA approved an unapproved score. [0-100]
 * @param failureMessage A string that will be presented to the use when this the result is not verified.
 * @param minAllowedThreshold Debug purposes. The min timestamp considered.
 * @param maxAllowedThreshold Debug. The maximum timestamp considered.
 * @param headHash Debug. The head hash evaluated. Not null.
 * @param tailHash Debug. The tail hash evaluated. Sometimes null.
 */
public record CommitVerificationResult(
        @NonNull boolean verified,
        boolean isCachedResponse,
        int significantCommits,
        int numDays,
        int penaltyPct,
        @NonNull String failureMessage,

        // Debug info
        Instant minAllowedThreshold,
        Instant maxAllowedThreshold,
        @NonNull String headHash,
        String tailHash
) { }
