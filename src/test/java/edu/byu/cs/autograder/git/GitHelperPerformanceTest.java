package edu.byu.cs.autograder.git;

import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class GitHelperPerformanceTest {
    private GitHelperUtils utils;
    private static final boolean RUN_PERFORMANCE_SUITE = false;

    @AfterAll
    static void cleanUp() {
        GitHelperUtils.cleanUpTests();
    }

    @BeforeEach
    void preparation() {
        utils = new GitHelperUtils();
    }

    @Test
    void reasonablePerformanceTest() throws GitAPIException {
        executePerformanceTest(200, 100, 1, true);
    }

    /**
     * This test executes many consecutive performance tests of diverse sizes.
     * This is primarily focused on gathering data for different test sizes,
     * and can take 10 minutes to run.
     * <br>
     * It prints out the reports while they run, and
     * saves a .csv file with the results.
     * <br>
     * Pay attention to the constants in the loops that define the constraints of the tests.
     *
     * @throws Exception When any kind of error occurs.
     */
    @Test
    void entirePerformanceTest() throws Exception {
        if (!RUN_PERFORMANCE_SUITE) return;
        System.out.println("Executing performance tests\n");

        var results = new LinkedList<PerformanceResults>();
        try {
            // Execute lots of tests
            var performanceStart = Instant.now();
            for (int commits = 100; commits <= 1000; commits += 100) {
                for (int patches = 0; patches <= 1000; patches += 200) {
                    results.add(executePerformanceTest(commits, patches, 6000, false));
                }
            }
            var performanceEnd = Instant.now();
            printTimeElapsed("performance testing", performanceStart, performanceEnd);
        } finally {
            // Print out the results
            var outputStart = Instant.now();
            File outputFile = File.createTempFile("git-performance-test", ".csv");
            String csv = toCsvString(results);
            FileUtils.writeStringToFile(csv, outputFile);
            var outputEnd = Instant.now();
            printTimeElapsed("output generation", outputStart, outputEnd);
            System.out.printf("Saved results to file: %s", outputFile.getAbsolutePath());
        }
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

        try (var repoContext = utils.initializeTest("performance-test", "performance.txt")){
            var generationStart = Instant.now();
            testDuration = printTimeElapsed("test initialization", testStart, generationStart);

            // Make the first commit 1 day ago so that the commits always end up being on two days.
            // This avoids issues where the test would fail when run in the early parts of the morning
            // when the commits would accidentally spill into the previous day.
            utils.makeCommit(
                    repoContext,
                    "Initial Commit" + "\nEmpty line",
                    1,
                    0,
                    commitLines
            );
            for (int i = 2; i <= totalCommits; ++i) {
                utils.makeCommit(
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
                CommitVerificationResult result = utils.evaluateRepo().eval(repoContext.git());
                var evaluationEnd = Instant.now();
                evaluationDuration.set(printTimeElapsed("evaluating history", evaluationStart, evaluationEnd));

                CommitVerificationResult expected = utils.generalCommitVerificationResult(false, totalCommits, 2);
                if (assertResults) utils.assertCommitVerification(expected, result);
            });
            utils.cleanUpTest(repoContext);
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


    /**
     * Prints out the result, and also returns the duration in milliseconds.
     *
     * @param name The name of the test. Usually lower case, space separated brief description.
     * @param start The {@link Instant} captured when the phase started
     * @param end The {@link Instant} captured when the test ended
     * @return A long representing the duration of the operation in millis.
     */
    private long printTimeElapsed(String name, Instant start, Instant end) {
        long millis = end.minusMillis(start.toEpochMilli()).toEpochMilli();
        long seconds = end.minusSeconds(start.getEpochSecond()).getEpochSecond();
        System.out.printf("Finished %s in %d millis (%d) seconds\n", name, millis, seconds);
        return millis;
    }
}
