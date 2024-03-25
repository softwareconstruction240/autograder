package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Submission;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);
    private final GradingContext gradingContext;

    public GitHelper(GradingContext gradingContext) {this.gradingContext = gradingContext;}

    public int setUp() throws GradingException {
        fetchRepo();
        return verifyRegularCommits();
    }

    /**
     * Fetches the student repo and puts it in the given local path
     */
    private void fetchRepo() throws GradingException {
        gradingContext.observer().update("Fetching repo...");

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gradingContext.repoUrl())
                .setDirectory(gradingContext.stageRepo());

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            gradingContext.observer().notifyError("Failed to clone repo: " + e.getMessage());
            LOGGER.error("Failed to clone repo", e);
            throw new GradingException("Failed to clone repo: ",  e.getMessage());
        }

        gradingContext.observer().update("Successfully fetched repo");
    }

    /**
     * Counts the commits since the last passoff and halts progress if there are less than the required amount
     *
     * @return the number of commits since the last passoff
     */
    private int verifyRegularCommits() throws GradingException {
        gradingContext.observer().update("Verifying commits...");

        try (Git git = Git.open(gradingContext.stageRepo())) {
            Iterable<RevCommit> commits = git.log().all().call();
            Submission submission = DaoService.getSubmissionDao().getFirstPassingSubmission(gradingContext.netId(),
                    gradingContext.phase());
            long timestamp = submission == null ? 0L : submission.timestamp().getEpochSecond();
            Map<String, Integer> commitHistory = CommitAnalytics.handleCommits(commits, timestamp, Instant.now().getEpochSecond());
            int numCommits = CommitAnalytics.getTotalCommits(commitHistory);
//            if (numCommits < requiredCommits) {
//                observer.notifyError("Not enough commits to pass off. (" + numCommits + "/" + requiredCommits + ")");
//                LOGGER.error("Insufficient commits to pass off.");
//                throw new GradingException("Not enough commits to pass off");
//            }

            return numCommits;
        } catch (IOException | GitAPIException e) {
            gradingContext.observer().notifyError("Failed to count commits: " + e.getMessage());
            LOGGER.error("Failed to count commits", e);
            throw new GradingException("Failed to count commits: ", e.getMessage());
        }
    }
}
