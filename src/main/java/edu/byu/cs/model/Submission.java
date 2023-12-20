package edu.byu.cs.model;

import edu.byu.cs.autograder.TestAnalyzer;

public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Double score,
        TestAnalyzer.TestNode testResults
) {
}
