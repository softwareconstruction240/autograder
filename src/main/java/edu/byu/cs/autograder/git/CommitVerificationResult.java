package edu.byu.cs.autograder.git;

import org.eclipse.jgit.annotations.NonNull;

import java.time.Instant;

public record CommitVerificationResult(
        @NonNull boolean verified,
        boolean isCachedResponse,
        int numCommits,
        int numDays,
        @NonNull String failureMessage,

        // Debug info
        Instant minAllowedThreshold,
        Instant maxAllowedThreshold,
        @NonNull String headHash,
        String tailHash
) { }
