package edu.byu.cs.autograder.git;

public record InitRepo() implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return 0;
    }

    @Override
    public ProcessBuilder run(int absoluteDaysAgo, GitRepoState repoState) {

    }
}
