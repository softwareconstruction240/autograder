package edu.byu.cs.model;

import edu.byu.cs.autograder.TestAnalyzer;

public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Float score,
        TestAnalyzer.TestNode testResults
) {
}
