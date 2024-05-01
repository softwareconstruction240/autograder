package edu.byu.cs.autograder.git.RepoGenerationCommands;

import edu.byu.cs.autograder.git.GitRepoState;

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

    @Override
    public ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState) {
        int start = repoState.makeChange();
        int end = start + numCommits;
        return new ProcessBuilder(repoState.scriptRoot + "generator.sh", "" + start, "" + end, "" + absoluteDaysAgo);
    }
}
