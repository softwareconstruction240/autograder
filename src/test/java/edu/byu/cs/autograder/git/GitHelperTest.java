package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.git.RepoGenerationCommands.*;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.ProcessUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

class GitHelperTest {

    private GradingContext defaultGradingContext;

    private static final String COMMIT_AUTHOR_EMAIL = "cosmo@cs.byu.edu";


    @BeforeAll
    static void initialize() throws Exception {
        initRepoFiles();
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
                            makeCommit(repoContext, "Change 1", 24, 20);
                            makeCommit(repoContext, "Change 2", 24, 10);
                            makeCommit(repoContext, "Change 3", 24, 10);
                            makeCommit(repoContext, "Change 4", 24, 10);
                            makeCommit(repoContext, "Change 5", 23, 10);
                            makeCommit(repoContext, "Change 6", 22, 10);
                            makeCommit(repoContext, "Change 7", 22, 10);
                            makeCommit(repoContext, "Change 8", 22, 10);
                            makeCommit(repoContext, "Change 9", 21, 10);
                            makeCommit(repoContext, "Change 10", 20, 10);
                        },
                        generalCommitVerificationResult(true, 10, 5)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            makeCommit(repoContext, "Change 11", 14, 10);
                            makeCommit(repoContext, "Change 12", 14, 10);
                            makeCommit(repoContext, "Change 13", 14, 10);
                            makeCommit(repoContext, "Change 14", 14, 10);
                            makeCommit(repoContext, "Change 15", 13, 10);
                            makeCommit(repoContext, "Change 16", 12, 10);
                            makeCommit(repoContext, "Change 17", 12, 10);
                            makeCommit(repoContext, "Change 18", 12, 10);
                            makeCommit(repoContext, "Change 19", 11, 10);
                            makeCommit(repoContext, "Change 20", 10, 10);
                        },
                        generalCommitVerificationResult(true, 10, 5)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            makeCommit(repoContext, "Change 31", 4, 10);
                            makeCommit(repoContext, "Change 32", 4, 10);
                            makeCommit(repoContext, "Change 33", 4, 10);
                            makeCommit(repoContext, "Change 34", 4, 10);
                            makeCommit(repoContext, "Change 35", 3, 10);
                            makeCommit(repoContext, "Change 36", 2, 10);
                            makeCommit(repoContext, "Change 37", 2, 10);
                            makeCommit(repoContext, "Change 38", 2, 10);
                            makeCommit(repoContext, "Change 39", 1, 10);
                            makeCommit(repoContext, "Change 40", 0, 10);
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
            generateCommits(TestRepo.hasCherryPickedCommits.getFile(), List.of(
                    new CommitTiming(0, 0, 0),
                    new CommitNItems(15, 1),
                    new Commit("PHASE 0", 0)
            ));
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
        CommitVerificationResult result = withTestRepo(TestRepo.passesRequirements, evaluateRepo());
        System.out.println(result);
        Assertions.assertTrue(result.verified());
        Assertions.assertEquals(12, result.significantCommits());
        Assertions.assertEquals(3, result.numDays());
        // Sufficient commits on sufficient days succeeds

        // Cherry-picking an older commit generates a failure message
        // Counts commits from merges properly
        // Low change-content commits do not count towards total
        // Commits authored after the head timestamp trigger failure
        // Commits authored before the tail timestamp trigger failure

//        new ProcessBuilder()
//        ProcessUtils.

    }

    private static void initRepoFiles() throws URISyntaxException {
        String resourcesBase = "src/test/resources/gitTestRepos/";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (var testRepo : TestRepo.values()) {
            String resourcePath = resourcesBase + testRepo.getFilePath();
//            URL url = classLoader.getResource(resourcePath);
//            if (url == null) {
////                return null;
//                throw new RuntimeException("Count not locate resource: " + testRepo.getFilePath());
//            }
//            File file = new File(url.toURI());
            File file = new File(resourcePath);
            testRepo.setFile(file);
        }
    }

    private <T> T withTestRepo(TestRepo repo, GitEvaluator<T> gitEvaluator) {
        return withTestRepo(repo.getFile(), gitEvaluator);
    }
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
            RepoContext repoContext = initializeTest(testName, "file-2.txt");

            CommitVerificationResult verificationResult;
            for (var checkpoint : checkpoints) {
                checkpoint.setupCommands.setup(repoContext);

                // Evaluate repo
                // TODO: Inject previously captured data for tail hash
                verificationResult = withTestRepo(repoContext.directory, evaluateRepo());
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
        return new CommitVerificationResult(
                verified, false, totalCommits, significantCommits, numDays, 0,
                Mockito.anyString(), Mockito.any(Instant.class), Mockito.any(Instant.class), Mockito.anyString(), Mockito.anyString());
    }

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
    private record VerificationCheckpoint(
            SetupCommands setupCommands,
            CommitVerificationResult expectedVerification
    ) { }

    @FunctionalInterface
    private interface SetupCommands {
        void setup(RepoContext repoContext);
    }
    private record RepoContext (
            Git git,
            String testName,
            File directory,
            File changeFile,
            String changeFilename
    ) { }

    private void makeCommit(RepoContext repoContext, String content) {
        makeCommit(repoContext, content, Instant.now());
    }
    private void makeCommit(RepoContext repoContext, String content, int daysAgo, int numLines) {
        makeCommit(repoContext, content, Instant.now().minus(Duration.ofDays(daysAgo)), numLines);
    }
    private void makeCommit(RepoContext repoContext, String content, Instant dateValue) {
        makeCommit(repoContext, content, dateValue, 20);
    }
    private void makeCommit(RepoContext repoContext, String content, Instant commitTimestamp, int numLines) {
        try {
            // Write the file
            String fileContents = (content + "\n").repeat(numLines);
            FileUtils.writeStringToFile(fileContents, repoContext.changeFile);

            // Add the file to index
            Git git = repoContext.git;
            git.add().addFilepattern(repoContext.changeFilename).call();

            // Commit with particular timestamp
            PersonIdent authorIdent = new PersonIdent(
                    "TESTING " + repoContext.testName,
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


    private void generateCommits(File repoTargetDirectory, List<GitGenerationCommand> commands) throws ProcessUtils.ProcessException {
        // Count the totals days spaced among all commits
        final int totalDaysChanged = getTotalDaysChanged(commands);
        final GitRepoState repoState = new GitRepoState();

        commands.addFirst(new InitRepo());
        runCommands(commands, totalDaysChanged, repoState);
    }
    private int getTotalDaysChanged(List<GitGenerationCommand> commands) {
        return commands.stream().mapToInt(GitGenerationCommand::getDaysChanged).sum();
    }
    private void runCommands(List<GitGenerationCommand> commands, int totalDaysChanged, GitRepoState repoState) throws ProcessUtils.ProcessException {
        int currentDay = totalDaysChanged;
        ProcessBuilder processBuilder;
        for (var command : commands) {
            processBuilder = command.run(currentDay, repoState);
            ProcessUtils.runProcess(processBuilder);
            command.evaluateResults(repoState);

            currentDay += command.getDaysChanged();
        }
    }

    private enum TestRepo {
        hasCherryPickedCommits,
        hasMergeCommits,
        lowCommitDays,
        lowCommits,
        passesRequirements;


        private final String filePath;
        private File file;

        TestRepo() {
            this(null);
        }
        TestRepo(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            if (filePath != null) return filePath;
            return name();
        }
        public void setFile(File file) {
            this.file = file;
        }
        public File getFile() {
            return file;
        }
    }

    @FunctionalInterface
    private interface GitEvaluator <T> {
        T eval(Git git) throws Exception;
    }
}
