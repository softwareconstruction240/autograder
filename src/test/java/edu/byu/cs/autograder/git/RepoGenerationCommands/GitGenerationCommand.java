package edu.byu.cs.autograder.git.RepoGenerationCommands;

import edu.byu.cs.autograder.git.GitRepoState;
import edu.byu.cs.util.ProcessUtils;

public interface GitGenerationCommand {
    int getDaysChanged();
    ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState);
    default void evaluateResults(GitRepoState repoState) throws ProcessUtils.ProcessException {
        // Do nothing. Override me!
    }
}
