package edu.byu.cs.autograder.git;

public record CommitVerificationResult(
        boolean verified,
        int numCommits,
        int numDays,
        String failureMessage
) { }
