package edu.byu.cs.autograder.git.CommitValidation;

import edu.byu.cs.analytics.CommitsByDay;
import edu.byu.cs.autograder.git.CommitVerificationConfig;

/**
 * Represents the context used to evaluate git commits.
 *
 * @param config
 * @param commitsByDay
 * @param numCommits
 * @param daysWithCommits
 * @param significantCommits
 */
public record CommitVerificationContext(
        CommitVerificationConfig config,
        CommitsByDay commitsByDay,
        int numCommits,
        int daysWithCommits,
        long significantCommits
) {
    public CommitVerificationContext(CommitVerificationConfig config) {
        this(config, null, 0, 0, 0);
    }
}
