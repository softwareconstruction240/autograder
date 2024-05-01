package edu.byu.cs.autograder.git;

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
}
