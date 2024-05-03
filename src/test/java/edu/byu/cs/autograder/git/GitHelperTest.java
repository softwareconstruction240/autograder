package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.git.RepoGenerationCommands.*;
import edu.byu.cs.util.ProcessUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class GitHelperTest {

    private GradingContext defaultGradingContext;


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
    void setUpTest() {
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
        var gitHelper = new GitHelper(defaultGradingContext);
        // Insufficient commits on sufficient days fails
        // Sufficient commits on insufficient days fails
        var result = withTestRepo(TestRepo.passesRequirements, git -> {
            String phase0HeadHash;
//            phase0HeadHash = "d57567de79755e5ef8293c2cdba07c84c4d289ce";
//            phase0HeadHash = "5d4d714c522a254fc84006b73a7fb5d660b77bef";
            phase0HeadHash = GitHelper.getHeadHash(git);
            CommitThreshold maxThreshold = new CommitThreshold(Instant.now(), phase0HeadHash);
            return gitHelper.verifyRegularCommits(git, GitHelper.MIN_COMMIT_THRESHOLD, maxThreshold);
        });
        System.out.println(result);
        Assertions.assertTrue(result.verified());
        Assertions.assertEquals(14, result.numCommits());
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
        try (var git = Git.open(repo.getFile())) {
            return gitEvaluator.eval(git);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
