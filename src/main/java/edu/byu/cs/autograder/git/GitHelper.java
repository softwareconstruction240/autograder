package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.autograder.Grader;
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
import java.util.ArrayList;
import java.util.Map;

public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);
    private final GradingContext gradingContext;

    public GitHelper(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    public CommitVerificationResult setUp() throws GradingException {
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
    private CommitVerificationResult verifyRegularCommits() throws GradingException {
        var observer = gradingContext.observer();
        var stageRepo = gradingContext.stageRepo();
        observer.update("Verifying commits...");

        try (Git git = Git.open(stageRepo)) {
            Iterable<RevCommit> commits = git.log().all().call();
            CommitAnalytics.CommitsByDay commitHistory = analyzeCommitHistoryForSubmission(commits);
            CommitVerificationResult commitVerificationResult = commitsPassRequirements(commitHistory);

            if (commitVerificationResult.verified()) {
                observer.update("Passed commit verification.");
            } else {
                observer.update("Failed commit verification. Continuing with grading anyways.");
            }
            return commitVerificationResult;
        } catch (IOException | GitAPIException e) {
            observer.notifyError("Failed to verify commits: " + e.getMessage());
            LOGGER.error("Failed to verify commits", e);
            throw new GradingException("Failed to verify commits: " + e.getMessage());
        }
    }
    private CommitAnalytics.CommitsByDay analyzeCommitHistoryForSubmission(Iterable<RevCommit> commits) {
        var netId = gradingContext.netId();
        var phase = gradingContext.phase();
        Submission submission = DaoService.getSubmissionDao().getFirstPassingSubmission(netId, phase);
        long timestamp = submission == null ? 0L : submission.timestamp().getEpochSecond();
        return CommitAnalytics.countCommitsByDay(commits, timestamp, Instant.now().getEpochSecond());
    }
    private CommitVerificationResult commitsPassRequirements(CommitAnalytics.CommitsByDay commitsByDay) {
        int requiredCommits = gradingContext.requiredCommits();
        int requiredDaysWithCommits = gradingContext.requiredDaysWithCommits();
        int commitVerificationPenaltyPct = gradingContext.commitVerificationPenaltyPct();

        boolean verified = true;
        int numCommits = commitsByDay.totalCommits();
        int daysWithCommits = commitsByDay.dayMap().size();
        ArrayList<String> errorMessages = new ArrayList<>();

        // Assert conditions
        if (numCommits < requiredCommits) {
            verified = false;
            errorMessages.add(String.format("Not enough commits to pass off (%d/%d).", numCommits, requiredCommits));
        }
        if (daysWithCommits < requiredDaysWithCommits) {
            verified = false;
            errorMessages.add(String.format("Did not commit on enough days to pass off (%d/%d).", numCommits, requiredCommits));
        }

        // Add additional explanatory message
        if (!verified) {
            errorMessages.add("Since you did not meet the prerequisites for commit frequency, "
                    + "you will need to talk to a TA to receive a score. ");
            errorMessages.add(String.format("It will come with a %d%% penalty.", commitVerificationPenaltyPct));
        }

        return new CommitVerificationResult(
                verified,
                numCommits,
                daysWithCommits,
                String.join("\n", errorMessages)
        );
    }

}
