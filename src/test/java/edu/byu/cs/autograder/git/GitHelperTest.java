package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.util.FileUtils;
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
import java.util.concurrent.atomic.AtomicLong;

class GitHelperTest {

    private GradingContext gradingContext;
    private static final String COMMIT_AUTHOR_EMAIL = "cosmo@cs.byu.edu";


    @BeforeEach
    void beforeEach() {
        gradingContext = generateGradingContext(10, 3, 10, 1);
    }

    /**
     * This test is designed to help with manual debugging.
     * <br>
     * Given a pre-organized git repository, by its full path name,
     * it evaluates the git repo and prints out the results.
     * <br>
     * If the repository doesn't exist, this test does nothing.
     */
    @Test
    void arbitraryRepoFileTest() {
        String repoPath;
        repoPath = "/Users/frozenfrank/Documents/College/Spring_2024/CS_240_TA/student_repos/dant329";
        File repo = new File(repoPath);
        if (!repo.exists()) return;

        System.out.printf("Evaluating repo at path:\n\t%s%n\n", repoPath);
        var result = withTestRepo(repo, evaluateRepo());
        System.out.println("Finished results:");
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
    void insufficientCommits() {
        evaluateTest("insufficient-commits", new VerificationCheckpoint(
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
                    // Notice only nine commits
                },
                generalCommitVerificationResult(false, 9, 4)
        ));
    }

    @Test
    void sufficientCommitsOnInsufficientDays() {
        evaluateTest("sufficient-commits-insufficient-days", new VerificationCheckpoint(
                repoContext -> {
                    // Four commits two days ago
                    makeCommit(repoContext, "Change 1", 2, 39, 20);
                    makeCommit(repoContext, "Change 2", 2, 38, 10);
                    makeCommit(repoContext, "Change 3", 2, 37, 10);
                    makeCommit(repoContext, "Change 4", 2, 36, 10);
                    // Six commits today
                    makeCommit(repoContext, "Change 5", 0, 35, 10);
                    makeCommit(repoContext, "Change 6", 0, 34, 10);
                    makeCommit(repoContext, "Change 7", 0, 33, 10);
                    makeCommit(repoContext, "Change 8", 0, 32, 10);
                    makeCommit(repoContext, "Change 9", 0, 31, 10);
                    makeCommit(repoContext, "Change 10", 0, 30, 10);
                },
                generalCommitVerificationResult(false, 10, 2)
        ));
    }

    @Test
    void commitsOutOfOrder() {
        evaluateTest("commits-out-of-order", new VerificationCheckpoint(
                repoContext -> {
                    makeCommit(repoContext, "Change 1", 2, 39, 20);
                    makeCommit(repoContext, "Change 5", 1, 35, 10); // Cherry-picked!
                    makeCommit(repoContext, "Change 2", 2, 38, 10);
                    makeCommit(repoContext, "Change 3", 2, 37, 10);
                    makeCommit(repoContext, "Change 4", 2, 36, 10);
                    makeCommit(repoContext, "Change 6", 0, 34, 10);
                    makeCommit(repoContext, "Change 7", 0, 33, 10);
                    makeCommit(repoContext, "Change 8", 0, 32, 10);
                    makeCommit(repoContext, "Change 9", 0, 31, 10);
                    makeCommit(repoContext, "Change 10", 0, 30, 10);
                },
                generalCommitVerificationResult(false, 10, 3)
        ));
    }

    @Test
    void insignificantCommits() {
        gradingContext = generateGradingContext(10, 0, 0, 10);
        evaluateTest("insignificant-commits", new VerificationCheckpoint(
                repoContext -> {
                    makeCommit(repoContext, "Change 1", 24, 39, 10); // Significant
                    makeCommit(repoContext, "Change 2", 24, 38, 1); // Also significant
                    makeCommit(repoContext, "Change 3", 24, 37, 4);
                    makeCommit(repoContext, "Change 4", 24, 36, 1);
                    makeCommit(repoContext, "Change 5", 23, 35, 4);
                    makeCommit(repoContext, "Change 6", 22, 34, 6); // Significant
                    makeCommit(repoContext, "Change 7", 22, 33, 1);
                    makeCommit(repoContext, "Change 8", 22, 32, 1);
                    makeCommit(repoContext, "Change 9", 21, 31, 1);
                    makeCommit(repoContext, "Change 10", 20, 30, 900); // Significant
                },
                generalCommitVerificationResult(false, 4, 10, 5)
        ));
    }

    @Test
    void simpleBackdatingCommits() {
        gradingContext = generateGradingContext(3, 0, 10, 0);
        evaluateTest("simple-backdating-commits", new VerificationCheckpoint(
                repoContext -> {
                    makeCommit(repoContext, "Change 1", 2, 20, 10);
                    makeCommit(repoContext, "Change 2", 1, 0, 10); // Simply backdated!
                    makeCommit(repoContext, "Change 3", 0, 10, 10);
                },
                generalCommitVerificationResult(false, 3, 3)
        ));
    }

    @Test
    void verifyingPerformanceTest() throws Exception {
        // Execute lots of tests
        var results = new LinkedList<PerformanceResults>();
        var performanceStart = Instant.now();
        for (int commits = 100; commits <= 1000; commits += 100) {
            for (int patches = 0; patches <= 1000; patches += 200) {
                results.add(executePerformanceTest(commits, patches, 6000, false));
            }
        }
        var performanceEnd = Instant.now();
        printTimeElapsed("performance testing", performanceStart, performanceEnd);

        // Print out the results
        var outputStart = Instant.now();
        File outputFile = File.createTempFile("git-performance-test", ".csv");
        String csv = toCsvString(results);
        FileUtils.writeStringToFile(csv, outputFile);
        var outputEnd = Instant.now();
        printTimeElapsed("output generation", outputStart, outputEnd);
        System.out.printf("Saved results to file: %s", outputFile.getAbsolutePath());
    }

    PerformanceResults executePerformanceTest(int totalCommits, int commitLines, int maxSeconds, boolean assertResults) throws GitAPIException {
        if (totalCommits > 24*60) {
            throw new IllegalArgumentException("totalCommits must be less than 24*60, the number of minutes in one day");
        }

        System.out.printf("\nExecuting performance test for %s commits with %s changed patches within %s seconds...\n", totalCommits, commitLines, maxSeconds);
        var testStart = Instant.now();
        long testDuration;
        long generationDuration;
        AtomicLong evaluationDuration = new AtomicLong();

        try (var repoContext = initializeTest("verifying-performance-test", "performance.txt")){
            var generationStart = Instant.now();
            testDuration = printTimeElapsed("test initialization", testStart, generationStart);

            for (int i = 1; i <= totalCommits; ++i) {
                makeCommit(
                        repoContext,
                        "Change " + i + "\nEmpty line",
                        0,
                        totalCommits - i,
                        commitLines
                );
            }
            var generationEnd = Instant.now();
            generationDuration = printTimeElapsed("generating commits", generationStart, generationEnd);

            Assertions.assertTimeout(Duration.ofSeconds(maxSeconds), () -> {
                var evaluationStart = Instant.now();
                CommitVerificationResult result = evaluateRepo().eval(repoContext.git());
                var evaluationEnd = Instant.now();
                evaluationDuration.set(printTimeElapsed("evaluating history", evaluationStart, evaluationEnd));

                CommitVerificationResult expected = generalCommitVerificationResult(false, totalCommits, 1);
                if (assertResults) assertCommitVerification(expected, result);
            });
        }

        return new PerformanceResults(totalCommits, commitLines, testDuration, generationDuration, evaluationDuration.longValue());
    }

    private String toCsvString(List<PerformanceResults> results) {
        StringBuilder builder = new StringBuilder();
        builder.append(PerformanceResults.getCsvHeader());
        builder.append('\n');
        results.forEach(result -> {
            builder.append(result.toCsvEntry());
            builder.append('\n');
        });
        return builder.toString();
    }
    private record PerformanceResults(
            int numCommits,
            int numPatches,

            long initMillis,
            long generationMillis,
            long evaluationMillis
    ) {
        public static String getCsvHeader() {
            return "Num Commits, Num Patches, Initialization Millis, Generation Millis, Evaluation Millis";
        }
        public String toCsvEntry() {
            return numCommits + "," + numPatches + "," + initMillis + "," + generationMillis + "," + evaluationMillis;
        }
    }

    @Test
    void verifyCommitRequirements() {
        // Verify status preservation on repeat submissions
        // Fails when submitting new phase with same head hash
        // Works when a non-graded phase has already been submitted
    }


    @Test
    void verifyRegularCommits() {
        // Counts commits from merges properly
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
        // for clarity. They still work, and hopefully we can return to them.
        return new CommitVerificationResult(
                verified, false, totalCommits, significantCommits, numDays, 0,
                null, null, null,
                "ANY_HEAD_HASH", null);
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
        return evaluateRepo(gradingContext, GitHelper.MIN_COMMIT_THRESHOLD);
    }
    private GitEvaluator<CommitVerificationResult> evaluateRepo(CommitThreshold minThreshold) {
        return evaluateRepo(gradingContext, minThreshold);
    }
    private GitEvaluator<CommitVerificationResult> evaluateRepo(GradingContext gradingContext, CommitThreshold minThreshold) {
        return evaluateRepo(new GitHelper(gradingContext), minThreshold);
    }
    private GitEvaluator<CommitVerificationResult> evaluateRepo(GitHelper gitHelper, CommitThreshold minThreshold) {
        return git -> {
            String phase0HeadHash;
//            phase0HeadHash = "d57567de79755e5ef8293c2cdba07c84c4d289ce";
//            phase0HeadHash = "5d4d714c522a254fc84006b73a7fb5d660b77bef";
            phase0HeadHash = GitHelper.getHeadHash(git);
            CommitThreshold maxThreshold = new CommitThreshold(Instant.now(), phase0HeadHash);
            return gitHelper.verifyRegularCommits(git, minThreshold, maxThreshold);
        };
    }

    @FunctionalInterface
    private interface GitEvaluator <T> {
        T eval(Git git) throws Exception;
    }

    /**
     * Prints out the result, and also returns the duration in milliseconds.
     *
     * @param name
     * @param start
     * @param end
     * @return A long representing the duration of the operation in millis.
     */
    private long printTimeElapsed(String name, Instant start, Instant end) {
        long millis = end.minusMillis(start.toEpochMilli()).toEpochMilli();
        long seconds = end.minusSeconds(start.getEpochSecond()).getEpochSecond();
        System.out.printf("Finished %s in %d millis (%d) seconds\n", name, millis, seconds);
        return millis;
    }

    // Assertion Helpers


    private GradingContext generateGradingContext(int requiredCommits, int requiredDaysWithCommits,
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
