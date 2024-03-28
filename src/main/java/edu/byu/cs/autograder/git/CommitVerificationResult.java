package edu.byu.cs.autograder.git;

import org.eclipse.jgit.annotations.NonNull;

import java.time.Instant;

public record CommitVerificationResult(
        boolean verified,
        int numCommits,
        int numDays,
        String failureMessage,

        // Debug info
        Instant minAllowedThreshold,
        Instant maxAllowedThreshold,
        @NonNull String headHash,
        String tailHash
) { }
