package edu.byu.cs.autograder.git.RepoGenerationCommands;

import edu.byu.cs.autograder.git.GitRepoState;

public record InitRepo() implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return 0;
    }

    @Override
    public ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState) {
        return null;
    }
}
