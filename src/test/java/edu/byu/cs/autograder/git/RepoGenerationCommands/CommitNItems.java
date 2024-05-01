package edu.byu.cs.autograder.git;

/**
 * Generates N commits, all at the same point N days since the previous commit.
 *
 * @param numCommits The number of commits to generate.
 * @param daysAfterPreviousCommit Days since the previous commit.
 */
public record CommitNItems(
        int numCommits,
        int daysAfterPreviousCommit
) implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return daysAfterPreviousCommit;
    }
}
