package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingObserver;
import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationConfig;
import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationStrategy;
import edu.byu.cs.autograder.git.CommitValidation.DefaultGitVerificationStrategy;
import edu.byu.cs.autograder.score.LateDayCalculator;
import edu.byu.cs.autograder.score.MockLateDayCalculator;
import edu.byu.cs.model.Phase;
import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GitHelperUtils {

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

    private GradingContext gradingContext;
    /** This is used as the tail hash when evaluating the next checkpoint; it lasts for only a single evaluation. */
    private String prevSubmissionHeadHash;
    /** This is used as the minThreshold, but it only lasts for a single evaluation. */
    private Instant prevSubmissionTimestamp;
    private int submitDaysEarly = 0;

    public GitHelperUtils() {
        gradingContext = generateGradingContext(10, 3, 10, 1);
    }

    public void setGradingContext(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    public void setPrevSubmissionHeadHash(String newHeadHash) {
        prevSubmissionHeadHash = newHeadHash;
    }
    public void setSubmitDaysEarly(int submitDaysEarly) {
        this.submitDaysEarly = submitDaysEarly;
    }
    public void setPrevSubmissionTimestamp(Instant minThreshold) {
        this.prevSubmissionTimestamp = minThreshold;
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
        prevSubmissionHeadHash = null;

        try (RepoContext repoContext = initializeTest(testName, "file.txt")) {
            CommitVerificationResult verificationResult;
            CommitThreshold minThreshold;
            for (var checkpoint : checkpoints) {
                prevSubmissionTimestamp = Instant.MIN;
                checkpoint.setupCommands().setup(repoContext);

                // Evaluate repo
                minThreshold = new CommitThreshold(prevSubmissionTimestamp, prevSubmissionHeadHash);
                verificationResult = withTestRepo(repoContext.directory(), evaluateRepo(minThreshold));
                assertCommitVerification(checkpoint.expectedVerification(), verificationResult);

                prevSubmissionHeadHash = verificationResult.headHash();
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
        return generalCommitVerificationResult(verified, allCommitsSignificant, allCommitsSignificant, numDays, false);
    }
    CommitVerificationResult generalCommitVerificationResult(boolean verified, int allCommitsSignificant, int numDays, int numWarnings) {
        var warnings = Collections.nCopies(numWarnings, "SOME WARNING");
        return generalCommitVerificationResult(verified, allCommitsSignificant, allCommitsSignificant, numDays, false, warnings);
    }
    CommitVerificationResult generalCommitVerificationResult(boolean verified, int allCommitsSignificant, int numDays, boolean missingTail) {
        return generalCommitVerificationResult(verified, allCommitsSignificant, allCommitsSignificant, numDays, missingTail);
    }
    CommitVerificationResult generalCommitVerificationResult(boolean verified, int significantCommits, int totalCommits, int numDays, boolean missingTail) {
        return generalCommitVerificationResult(verified, significantCommits, totalCommits, numDays, missingTail, null);
    }
    CommitVerificationResult generalCommitVerificationResult(
            boolean verified, int significantCommits, int totalCommits, int numDays, boolean missingTail, Collection<String> warnings) {
        // Note: Unfortunately, we were not able to configure Mockito to properly accept these `any` object times
        // to also accept null. We've moved a different direction now, but we're preserving the `Mockito.nullable/any()`
        // for clarity. They still work, and hopefully we can return to them.
        return new CommitVerificationResult(
                verified, false, totalCommits, significantCommits, numDays,
                missingTail, 0, null, warnings, null, null,
                "ANY_HEAD_HASH", null);
    }

    void makeCommit(RepoContext repoContext, String content, int daysAgo, int minsAgo, int numLines) {
        makeCommit(repoContext, content, daysAgo, minsAgo, numLines, true);
    }
    void makeCommit(RepoContext repoContext, String content, int daysAgo, int minsAgo, int numLines, boolean setCommitter) {
        Instant time = Instant.now()
                .minus(Duration.ofDays(daysAgo))
                .minus(Duration.ofMinutes(minsAgo));
        makeCommit(repoContext, content, time, numLines, setCommitter);
    }
    void makeCommit(RepoContext repoContext, String content, Instant commitTimestamp, int numLines, boolean setCommitter) {
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
            var commitCommand = git.commit();
            commitCommand.setMessage(content);
            commitCommand.setAuthor(authorIdent);
            if (setCommitter) {
                commitCommand.setCommitter(authorIdent);
            }
            commitCommand.call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }


    GitEvaluator<CommitVerificationResult> evaluateRepo() {
        return evaluateRepo(gradingContext, GitHelper.MIN_COMMIT_THRESHOLD);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(@Nullable CommitThreshold minThreshold) {
        return evaluateRepo(gradingContext, minThreshold);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(GradingContext gradingContext, @Nullable CommitThreshold minThreshold) {
        LateDayCalculator lateDayCalculator = new MockLateDayCalculator(submitDaysEarly, 0);
        CommitVerificationStrategy verificationStrategy = new DefaultGitVerificationStrategy(lateDayCalculator);
        var gitHelper = new GitHelper(gradingContext, verificationStrategy);
        return evaluateRepo(gitHelper, minThreshold);
    }
    GitEvaluator<CommitVerificationResult> evaluateRepo(GitHelper gitHelper, @Nullable CommitThreshold minThreshold) {
        return git -> {
            String currentHeadHash = GitHelper.getHeadHash(git);
            var maxTimeThreshold = Instant.now().plusSeconds(gradingContext.verificationConfig().forgivenessMinutesHead() * 60L);
            CommitThreshold maxThreshold = new CommitThreshold(maxTimeThreshold, currentHeadHash);
            var report = gitHelper.verifyRegularCommits(git, minThreshold, maxThreshold);
            return report.result();
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
        return generateGradingContext(requiredCommits, requiredDaysWithCommits,
                commitVerificationPenaltyPct, minimumLinesChangedPerCommit, 3);
    }
    GradingContext generateGradingContext(int requiredCommits, int requiredDaysWithCommits,
                                          int commitVerificationPenaltyPct, int minimumLinesChangedPerCommit, int forgivenessMinutes) {
        GradingObserver mockObserver = Mockito.mock(GradingObserver.class);
        var cvConfig = new CommitVerificationConfig(requiredCommits, requiredDaysWithCommits, minimumLinesChangedPerCommit, commitVerificationPenaltyPct, forgivenessMinutes);
        return new GradingContext(
                null, Phase.Phase0, null, null, null, null,
                cvConfig, mockObserver, false);
    }

    void assertCommitVerification(CommitVerificationResult expected, CommitVerificationResult actual) {
        Assertions.assertEquals(expected.verified(), actual.verified());
        Assertions.assertEquals(expected.isCachedResponse(), actual.isCachedResponse());
        Assertions.assertEquals(expected.totalCommits(), actual.totalCommits());
        Assertions.assertEquals(expected.significantCommits(), actual.significantCommits());
        Assertions.assertEquals(expected.numDays(), actual.numDays());
        Assertions.assertEquals(expected.missingTail(), actual.missingTail());
        Assertions.assertEquals(expected.penaltyPct(), actual.penaltyPct());

        var expectWarnings = expected.warningMessages() == null ? 0 : expected.warningMessages().size();
        var actualWarnings = actual.warningMessages() == null ? 0 : actual.warningMessages().size();
        Assertions.assertEquals(expectWarnings, actualWarnings);
    }
}
