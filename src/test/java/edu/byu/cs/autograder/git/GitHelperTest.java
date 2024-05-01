package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.dataAccess.DataAccessException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GitHelperTest {

    @BeforeAll
    static void initialize() throws Exception {
        initRepoFiles();
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

    @Test
    void verifyRegularCommits() {
        var gitHelper = new GitHelper(null);
        // Insufficient commits on sufficient days fails
        // Sufficient commits on insufficient days fails
        withTestRepo(TestRepo.passesRequirements, git -> {
            CommitThreshold maxThreshold = new CommitThreshold(Instant.now(), GitHelper.getHeadHash(git));
            return gitHelper.verifyRegularCommits(git, GitHelper.MIN_COMMIT_THRESHOLD, maxThreshold);
        });
        // Sufficient commits on sufficient days succeeds

        // Cherry-picking an older commit generates a failure message
        // Counts commits from merges properly
        // Low change-content commits do not count towards total
        // Commits authored after the head timestamp trigger failure
        // Commits authored before the tail timestamp trigger failure

    }

    private static void initRepoFiles() throws URISyntaxException {
        String resourcesBase = "gitTestRepos/";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (var testRepo : TestRepo.values()) {
            String resourcePath = resourcesBase + testRepo.getFilePath();
            URL url = classLoader.getResource(resourcePath);
            if (url == null) {
                throw new RuntimeException("Count not locate resource: " + testRepo.getFilePath());
            }
            File file = new File(url.toURI());
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