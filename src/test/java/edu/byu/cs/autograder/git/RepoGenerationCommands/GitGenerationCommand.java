package edu.byu.cs.autograder.git;

public abstract class GitGenerationCommand {
    abstract int getDaysChanged();
    abstract ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState);
    void evaluateResults(GitRepoState repoState) {
        // Do nothing. Override me!
    }
}
