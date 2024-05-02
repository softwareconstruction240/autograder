package edu.byu.cs.autograder.git;

import org.eclipse.jgit.annotations.NonNull;

import java.time.Instant;

/**
 * An in-memory model that carries information
 *
 * @see edu.byu.cs.model.Submission.ScoreVerification#verifiedStatus() for more information about `penaltyPct`.
 *
 * @param verified
 * @param isCachedResponse
 * @param numCommits
 * @param numDays
 * @param penaltyPct A reduction to the phase, if any, when a TA approved an unapproved score. [0-100]
 * @param failureMessage
 * @param minAllowedThreshold
 * @param maxAllowedThreshold
 * @param headHash
 * @param tailHash
 */
public record CommitVerificationResult(
        @NonNull boolean verified,
        boolean isCachedResponse,
        int numCommits,
        int numDays,
        int penaltyPct,
        @NonNull String failureMessage,

        // Debug info
        Instant minAllowedThreshold,
        Instant maxAllowedThreshold,
        @NonNull String headHash,
        String tailHash
) { }
