package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

class GitHelperTest {

    private GradingContext defaultGradingContext;

    private static final String COMMIT_AUTHOR_EMAIL = "cosmo@cs.byu.edu";


    @BeforeAll
    static void initialize() throws Exception {
    }

    @BeforeEach
    void beforeEach() {
        Grader.Observer mockObserver = Mockito.mock(Grader.Observer.class);
        defaultGradingContext = new GradingContext(
                null, null, null, null, null, null,
                10, 3, 10, 1,
                mockObserver, false);
    }

    @Test
    void arbitraryRepoFileTest() {
        String repoPath;
        repoPath = "/Users/frozenfrank/Documents/College/Spring_2024/CS_240_TA/student_repos/dant329";
        repoPath = "/Users/frozenfrank/Documents/College/Spring_2024/CS_240_TA/student_repos/temp-failing-repo-michael";
        File repo = new File(repoPath);
        var result = withTestRepo(repo, evaluateRepo());
        System.out.println(result);
    }

    @Test
    void multiPhaseSuccessfullPassoff() {
        evaluateTest("multi-phase-successful-passoff", List.of(
                new VerificationCheckpoint(
                        repoContext -> {
                            makeCommit(repoContext, "Change 1", 24, 39, 20);
                            makeCommit(repoContext, "Change 2", 24, 38, 10);
                            makeCommit(repoContext, "Change 3", 24, 37, 10);
                            makeCommit(repoContext, "Change 4", 24, 36, 10);
                            makeCommit(repoContext, "Change 5", 23, 35, 10);
                            makeCommit(repoContext, "Change 6", 22, 34, 10);
                            makeCommit(repoContext, "Change 7", 22, 33, 10);
                            makeCommit(repoContext, "Change 8", 22, 32, 10);
                            makeCommit(repoContext, "Change 9", 21, 31, 10);
                            makeCommit(repoContext, "Change 10", 20, 30, 10);
                        },
                        generalCommitVerificationResult(true, 10, 5)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            makeCommit(repoContext, "Change 11", 14, 29, 10);
                            makeCommit(repoContext, "Change 12", 14, 28, 10);
                            makeCommit(repoContext, "Change 13", 14, 27, 10);
                            makeCommit(repoContext, "Change 14", 14, 26, 10);
                            makeCommit(repoContext, "Change 15", 13, 25, 10);
                            makeCommit(repoContext, "Change 16", 12, 24, 10);
                            makeCommit(repoContext, "Change 17", 12, 23, 10);
                            makeCommit(repoContext, "Change 18", 12, 22, 10);
                            makeCommit(repoContext, "Change 19", 11, 21, 10);
                            makeCommit(repoContext, "Change 20", 10, 20, 10);
                        },
                        generalCommitVerificationResult(true, 10, 5)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            makeCommit(repoContext, "Change 31", 4, 19, 10);
                            makeCommit(repoContext, "Change 32", 4, 18, 10);
                            makeCommit(repoContext, "Change 33", 4, 17, 10);
                            makeCommit(repoContext, "Change 34", 4, 16, 10);
                            makeCommit(repoContext, "Change 35", 3, 15, 10);
                            makeCommit(repoContext, "Change 36", 2, 14, 10);
                            makeCommit(repoContext, "Change 37", 2, 13, 10);
                            makeCommit(repoContext, "Change 38", 2, 12, 10);
                            makeCommit(repoContext, "Change 39", 1, 11, 10);
                            makeCommit(repoContext, "Change 40", 0, 10, 10);
                        },
                        generalCommitVerificationResult(true, 10, 5)
                )
        ));
    }

    @Test
    void verifyCommitRequirements() {
        // Verify status preservation on repeat submissions
        // Works properly on Phase 0 (no previous submissions)
        // Fails when submitting new phase with same head hash
        // Works when a non-graded phase has already been submitted
    }

    @Nested
    class VerifyRegularCommits {

        @Test
        void insufficentCommitsSufficientDays() throws ProcessUtils.ProcessException {
            Assertions.assertFalse(false);
        }
        @Test
        void sufficientCommitsInsufficientDays() {
            Assertions.assertTrue(true);
            Assertions.assertFalse(false);
        }

    }




    @Test
    void verifyRegularCommits() {
        // Insufficient commits on sufficient days fails
        // Sufficient commits on insufficient days fails
        // Sufficient commits on sufficient days succeeds

        // Cherry-picking an older commit generates a failure message
        // Counts commits from merges properly
        // Low change-content commits do not count towards total
        // Commits authored after the head timestamp trigger failure
        // Commits authored before the tail timestamp trigger failure

    }

    // Testing Helpers

    private <T> T withTestRepo(File file, GitEvaluator<T> gitEvaluator) {
        try (var git = Git.open(file)) {
            return gitEvaluator.eval(git);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simplifying overload that evaluates a test requiring only a single checkpoint.
     * <br>
     * @see GitHelperTest#evaluateTest(String, List) Implementing method for more details.
     *
     * @param checkpoint The verification checkpoint to evaluate
     */
    private void evaluateTest(String testName, VerificationCheckpoint checkpoint) {
        evaluateTest(testName, List.of(checkpoint));
    }
    /**
     * Evaluates all checkpoints sequentially.
     * <br>
     * Automatically remembers the verification from previous checkpoints, and uses those as
     * the minimum threshold for subsequent verification.
     * <br>
     * Bash scrips have a few aliases made available to them including:
     * <ul>
     *     <li><code>commit "Commit message" [DATE_VALUE [NUM_LINES]]</code></li>
     *     <li><code>generate START_CHANGE_NUM END_CHANGE_NUM [DAYS_AGO]</code></li>
     * </ul>
     * @param checkpoints A list of checkpoints to evaluate in the same directory, sequentially
     */
    private void evaluateTest(String testName, List<VerificationCheckpoint> checkpoints) {
        CommitVerificationResult prevVerification = null;

        try {
            RepoContext repoContext = initializeTest(testName, "file.txt");

            CommitVerificationResult verificationResult;
            for (var checkpoint : checkpoints) {
                checkpoint.setupCommands().setup(repoContext);

                // Evaluate repo
                // TODO: Inject previously captured data for tail hash
                verificationResult = withTestRepo(repoContext.directory(), evaluateRepo());
                Assertions.assertEquals(checkpoint.expectedVerification(), verificationResult);

                prevVerification = verificationResult;
            }

        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
    private RepoContext initializeTest(String testName, String changeFileName) throws GitAPIException {
        File testDirectory = new File("src/test/resources/gitTestRepos", testName);
        File changeFile = new File(testDirectory, changeFileName);

        FileUtils.removeDirectory(testDirectory);
        Git git = Git.init().setDirectory(testDirectory).call();

        return new RepoContext(
                git,
                testName,
                testDirectory,
                changeFile,
                changeFileName
        );
    }

    private CommitVerificationResult generalCommitVerificationResult(boolean verified, int allCommitsSignificant, int numDays) {
        return generalCommitVerificationResult(verified, allCommitsSignificant, allCommitsSignificant, numDays);
    }
    private CommitVerificationResult generalCommitVerificationResult(
            boolean verified, int significantCommits, int totalCommits, int numDays) {
        // Note: Unfortunately, we were not able to configure Mockito to properly accept these `any` object times
        // to also accept null. We've moved a different direction now, but we're preserving the `Mockito.nullable/any()`
        // for clarity. They still work, and
        return new CommitVerificationResult(
                verified, false, totalCommits, significantCommits, numDays, 0,
                Mockito.nullable(String.class), Mockito.nullable(Instant.class), Mockito.nullable(Instant.class),
                Mockito.anyString(), Mockito.nullable(String.class));
    }


    private void makeCommit(RepoContext repoContext, String content) {
        makeCommit(repoContext, content, Instant.now());
    }
    private void makeCommit(RepoContext repoContext, String content, int daysAgo, int minsAgo, int numLines) {
        Instant time = Instant.now()
                .minus(Duration.ofDays(daysAgo))
                .minus(Duration.ofMinutes(minsAgo));
        makeCommit(repoContext, content, time, numLines);
    }
    private void makeCommit(RepoContext repoContext, String content, Instant dateValue) {
        makeCommit(repoContext, content, dateValue, 20);
    }
    private void makeCommit(RepoContext repoContext, String content, Instant commitTimestamp, int numLines) {
        try {
            // Write the file
            String fileContents = (content + "\n").repeat(numLines);
            FileUtils.writeStringToFile(fileContents, repoContext.changeFile());

            // Add the file to index
            Git git = repoContext.git();
            git.add().addFilepattern(repoContext.changeFilename()).call();

            // Commit with particular timestamp
            PersonIdent authorIdent = new PersonIdent(
                    "TESTING " + repoContext.testName(),
                    COMMIT_AUTHOR_EMAIL,
                    commitTimestamp,
                    ZoneId.systemDefault());
            git.commit().setMessage(content).setAuthor(authorIdent).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }


    private GitEvaluator<CommitVerificationResult> evaluateRepo() {
        return evaluateRepo(defaultGradingContext);
    }
    private GitEvaluator<CommitVerificationResult> evaluateRepo(GradingContext gradingContext) {
        return evaluateRepo(new GitHelper(gradingContext));
    }
    private GitEvaluator<CommitVerificationResult> evaluateRepo(GitHelper gitHelper) {
        return git -> {
            String phase0HeadHash;
//            phase0HeadHash = "d57567de79755e5ef8293c2cdba07c84c4d289ce";
//            phase0HeadHash = "5d4d714c522a254fc84006b73a7fb5d660b77bef";
            phase0HeadHash = GitHelper.getHeadHash(git);
            CommitThreshold maxThreshold = new CommitThreshold(Instant.now(), phase0HeadHash);
            return gitHelper.verifyRegularCommits(git, GitHelper.MIN_COMMIT_THRESHOLD, maxThreshold);
        };
    }

    @FunctionalInterface
    private interface GitEvaluator <T> {
        T eval(Git git) throws Exception;
    }
}
