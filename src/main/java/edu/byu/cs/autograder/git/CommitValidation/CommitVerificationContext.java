package edu.byu.cs.autograder.git.CommitValidation;

import edu.byu.cs.analytics.CommitsByDay;
import edu.byu.cs.autograder.git.CommitVerificationConfig;

public record CommitVerificationContext(
        CommitVerificationConfig config,
        CommitsByDay commitsByDay,
        int numCommits,
        int daysWithCommits,
        long significantCommits
) {
}
