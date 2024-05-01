package edu.byu.cs.autograder.git.RepoGenerationCommands;

import edu.byu.cs.autograder.git.GitRepoState;
import edu.byu.cs.util.ProcessUtils;

/**
 * Generates a commit and saves the hash and timestamp
 * for comparison later.
 *
 * @param title The commit title. This will also be used as the lookup key for future reference.
 * @param daysSincePreviousCommit Default: 0. Days since the previous commit was authored.
 */
public record Commit(
        String title,
        Integer daysSincePreviousCommit
) implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return daysSincePreviousCommit;
    }

    @Override
    public ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState) {
        return new ProcessBuilder(repoState.scriptRoot + "commit.sh", title, "" + absoluteDaysAgo);
    }

    @Override
    public void evaluateResults(GitRepoState repoState) throws ProcessUtils.ProcessException {
        ProcessBuilder process = new ProcessBuilder("git", "rev-parse", "HEAD");
        ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(process);
        String headHash = output.stdOut();

        repoState.namedCommits.put(title, new GitRepoState.NamedCommit(title, headHash));
    }
}
