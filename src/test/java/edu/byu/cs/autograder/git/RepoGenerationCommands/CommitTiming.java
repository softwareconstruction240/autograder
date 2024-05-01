package edu.byu.cs.autograder.git;

import java.util.Arrays;

/**
 * Generates a sequence of commits, each with incrementing values.
 * They will all by applied so that they are N days separated from each other.
 *
 * @param daysBetweenCommits
 */
public record CommitTiming(
        int... daysBetweenCommits
) implements GitGenerationCommand {
    @Override
    public int getDaysChanged() {
        return Arrays.stream(daysBetweenCommits).sum();
    }
}
