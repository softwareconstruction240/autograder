package edu.byu.cs.autograder.git;

/**
 * Represents one stage of a potentially multi-stage test.
 * <br>
 * It performs the following:
 * <ol>
 *     <li>Creates a fresh directory</li>
 *     <li>Runs `setupCommands` as a bash script in the directory</li>
 *     <li>If `expectedVerification` is non-null, runs commit verification and asserts the result</li>
 *     <li>Calls the callback after completion</li>
 * </ol>
 *
 * @param setupCommands
 * @param expectedVerification
 */
record VerificationCheckpoint(
        SetupCommands setupCommands,
        CommitVerificationResult expectedVerification
) {
    @FunctionalInterface
    interface SetupCommands {
        void setup(RepoContext repoContext);
    }
}
