package edu.byu.cs.autograder.git;

import java.time.Instant;

public record CommitVerificationResult(
        boolean verified,
        int numCommits,
        int numDays,
        String failureMessage,

        // Debug info
        Instant minAllowedThreshold,
        Instant maxAllowedThreshold,
        String headHash,
        String tailHash
) { }
