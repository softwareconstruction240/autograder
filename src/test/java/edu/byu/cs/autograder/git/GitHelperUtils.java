package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class GitHelperUtils {

    private GradingContext gradingContext;
    static final String TEST_REPO_DIR_ROOT = "src/test/resources/gitTestRepos";
    /**
     * Much more destructive by nature;
     * Deletes the entire directory including other tests that may not have run
     */
    private static final boolean CLEANUP_TEST_REPOS_AT_END = false;
    /**
     * A sensible option to have turned on.
     * These are only cleaned up automatically when successfully pass.
     */
    private static final boolean CLEANUP_TEST_REPOS_AFTER_EACH = true;
    private static final String COMMIT_AUTHOR_EMAIL = "cosmo@cs.byu.edu";

    public GitHelperUtils() {
        gradingContext = generateGradingContext(10, 3, 10, 1);
    }

    public void setGradingContext(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }


    // ### Testing Helpers
    /**
     * Simplifying overload that evaluates a test requiring only a single checkpoint.
     * <br>
     * @see GitHelperUtils#evaluateTest(String, List) Implementing method for more details.
     *
     * @param checkpoint The verification checkpoint to evaluate
     */
    void evaluateTest(String testName, VerificationCheckpoint checkpoint) {
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
     * <br>
     * This will clean up the test when it succeeds, otherwise, the test is left behind for inspection
     * by a developer.
     *
     * @param checkpoints A list of checkpoints to evaluate in the same directory, sequentially
     */
    void evaluateTest(String testName, List<VerificationCheckpoint> checkpoints) {
        CommitVerificationResult prevVerification = null;

        try (RepoContext repoContext = initializeTest(testName, "file.txt")) {
            CommitVerificationResult verificationResult;
            CommitThreshold minThreshold;
            for (var checkpoint : checkpoints) {
                checkpoint.setupCommands().setup(repoContext);

                // Evaluate repo
                minThreshold = prevVerification == null ?
                        GitHelper.MIN_COMMIT_THRESHOLD :
                        new CommitThreshold(Instant.MIN, prevVerification.headHash());
                verificationResult = withTestRepo(repoContext.directory(), evaluateRepo(minThreshold));
                assertCommitVerification(checkpoint.expectedVerification(), verificationResult);

                prevVerification = verificationResult;
            }

            // Only cleanup if it succeeded
            cleanUpTest(repoContext);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    RepoContext initializeTest(String testName, String changeFileName) throws GitAPIException {
        File testDirectory = new File(TEST_REPO_DIR_ROOT, testName);
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

    void cleanUpTest(RepoContext repoContext) {
        cleanUpTest(repoContext, CLEANUP_TEST_REPOS_AFTER_EACH);
    }
    void cleanUpTest(RepoContext repoContext, boolean force) {
        if (!force) return;
        FileUtils.removeDirectory(repoContext.directory());
    }

    /**
     * Call this method in the @AfterAll of each test suite.
     * It respects certain configuration and will clean up after all the tests.
     */
    static void cleanUpTests() {
        cleanUpTests(CLEANUP_TEST_REPOS_AT_END);
    }
    static void cleanUpTests(boolean force) {
        if (!force) return;
        FileUtils.removeDirectory(new File(TEST_REPO_DIR_ROOT));
    }

    CommitVerificationResult generalCommitVerificationResult(boolean verified, int allCommitsSignificant, int numDays) {
        return generalCommitVerificationResult(verified, allCommitsSignificant, allCommitsSignificant, numDays);
    }
    CommitVerificationResult generalCommitVerificationResult(
            boolean verified, int significantCommits, int totalCommits, int numDays) {
        // Note: Unfortunately, we were not able to configure Mockito to properly accept these `any` object times
        // to also accept null. We've moved a different direction now, but we're preserving the `Mockito.nullable/any()`
        // for clarity. They still work, and hopefully we can return to them.
        return new CommitVerificationResult(
                verified, false, totalCommits, significantCommits, numDays, 0,
                null, null, null,
                "ANY_HEAD_HASH", null);
    }


    void makeCommit(RepoContext repoContext, String content) {
        makeCommit(repoContext, content, Instant.now());
    }
    void makeCommit(RepoContext repoContext, String content, int daysAgo, int minsAgo, int numLines) {
        Instant time = Instant.now()
                .minus(Duration.ofDays(daysAgo))
                .minus(Duration.ofMinutes(minsAgo));
        makeCommit(repoContext, content, time, numLines);
    }
    void makeCommit(RepoContext repoContext, String content, Instant dateValue) {
        makeCommit(repoContext, content, dateValue, 20);
    }
    void makeCommit(RepoContext repoContext, String content, Instant commitTimestamp, int numLines) {
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


    GitEvaluator<CommitVerificationResult> evaluateRepo() {
        return evaluateRepo(gradingContext, GitHelper.MIN_COMMIT_THRESHOLD);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(CommitThreshold minThreshold) {
        return evaluateRepo(gradingContext, minThreshold);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(GradingContext gradingContext, CommitThreshold minThreshold) {
        return evaluateRepo(new GitHelper(gradingContext), minThreshold);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(GitHelper gitHelper, CommitThreshold minThreshold) {
        return git -> {
            String phase0HeadHash = GitHelper.getHeadHash(git);
            CommitThreshold maxThreshold = new CommitThreshold(Instant.now(), phase0HeadHash);
            return gitHelper.verifyRegularCommits(git, minThreshold, maxThreshold);
        };
    }


    <T> T withTestRepo(File file, GitEvaluator<T> gitEvaluator) {
        try (var git = Git.open(file)) {
            return gitEvaluator.eval(git);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface GitEvaluator <T> {
        T eval(Git git) throws Exception;
    }



    // ### Assertion Helpers

    GradingContext generateGradingContext(int requiredCommits, int requiredDaysWithCommits,
                                                  int commitVerificationPenaltyPct, int minimumLinesChangedPerCommit) {
        Grader.Observer mockObserver = Mockito.mock(Grader.Observer.class);
        return new GradingContext(
                null, null, null, null, null, null,
                requiredCommits, requiredDaysWithCommits, commitVerificationPenaltyPct, minimumLinesChangedPerCommit,
                mockObserver, false);
    }

    void assertCommitVerification(CommitVerificationResult expected, CommitVerificationResult actual) {
        Assertions.assertEquals(expected.verified(), actual.verified());
        Assertions.assertEquals(expected.isCachedResponse(), actual.isCachedResponse());
        Assertions.assertEquals(expected.totalCommits(), actual.totalCommits());
        Assertions.assertEquals(expected.significantCommits(), actual.significantCommits());
        Assertions.assertEquals(expected.numDays(), actual.numDays());
        Assertions.assertEquals(expected.penaltyPct(), actual.penaltyPct());
    }
}
