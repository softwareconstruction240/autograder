package edu.byu.cs.autograder.git;

/**
 * Represents several configurable values about the
 * commit verification system.
 *
 * @param requiredCommits The number of significant commits that must exist
 * @param requiredDaysWithCommits The number of unique days that on which any commit is made
 * @param commitVerificationPenaltyPct The penalty percentage which should be deducted when conditions are not met
 * @param minimumChangedLinesPerCommit The number of line changes (insertions + deletions) that qualify a commit as "significant"
 */
public record CommitVerificationConfig(
        // Variable values
        int requiredCommits,
        int requiredDaysWithCommits,
        int minimumChangedLinesPerCommit,

        // Generally constant values
        int commitVerificationPenaltyPct
) { }
