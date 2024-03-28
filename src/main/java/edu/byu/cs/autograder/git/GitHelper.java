package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.analytics.CommitsByDay;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.ScorerHelper;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Submission;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);
    private final GradingContext gradingContext;
    private Collection<Submission> passingSubmissions;

    public GitHelper(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    public CommitVerificationResult setUp() throws GradingException {
        File stageRepo = gradingContext.stageRepo();
        fetchRepo(stageRepo);

        boolean gradedPhase = true; // FIXME: Replace with a conditional call for #271
        return gradedPhase ? verifyCommitRequirements(stageRepo) : skipCommitVerification(stageRepo);
    }

    /**
     * Fetches the student repo and puts it in the given local path
     */
    private void fetchRepo(File intoDirectory) throws GradingException {
        gradingContext.observer().update("Fetching repo...");

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gradingContext.repoUrl())
                .setDirectory(intoDirectory);

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Cloned repo to " + git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            gradingContext.observer().notifyError("Failed to clone repo: " + e.getMessage());
            LOGGER.error("Failed to clone repo", e);
            throw new GradingException("Failed to clone repo: ",  e.getMessage());
        }

        gradingContext.observer().update("Successfully fetched repo");
    }

    private CommitVerificationResult skipCommitVerification(File stageRepo) throws GradingException {
        LOGGER.debug("Skipping commit verification");
        String headHash = getHeadHash(stageRepo);
        return new CommitVerificationResult(
                true,
                0, 0, null,
                Instant.MIN, Instant.MAX,
                headHash, null
        );
    }

    private String getHeadHash(File stageRepo) throws GradingException {
        try (Git git = Git.open(stageRepo)) {
            return getHeadHash(git);
        } catch (IOException e) {
            throw new GradingException("Failed to get head hash: " + e.getMessage());
        }
    }
    private String getHeadHash(Git git) throws IOException {
        return git.getRepository().findRef("HEAD").getObjectId().getName();
    }

    private CommitVerificationResult verifyCommitRequirements(File stageRepo) throws GradingException {
        loadPassingSubmission();

        try (Git git = Git.open(stageRepo)) {
            return verifyRegularCommits(git);
        } catch (IOException | GitAPIException e) {
            var observer = gradingContext.observer();
            observer.notifyError("Failed to verify commits: " + e.getMessage());
            LOGGER.error("Failed to verify commits", e);
            throw new GradingException("Failed to verify commits: " + e.getMessage());
        }
    };
    private void loadPassingSubmission() {
        passingSubmissions = DaoService.getSubmissionDao().getAllPassingSubmissions(gradingContext.netId());
    }
    /**
     * Returns a timestamp corresponding to the most recent commit that was giving a passing grade,
     * and the commit at that point.
     * <br>
     * In any case, if the commit cannot be located, then submission timestamp will be used in its place.
     * If there are no previous passing submissions, this returns the minimum Instant instead.
     *
     * @return An {@link CommitThreshold}.
     * @throws GradingException When certain preconditions are not met, or when this would have returned null.
     */
    @NonNull
    private CommitThreshold getMostRecentPassingSubmission(Git git) throws IOException, GradingException {
        if (passingSubmissions == null) {
            throw new GradingException("Cannot extract previous submission date before passingSubmissions are loaded.");
        }
        if (passingSubmissions.isEmpty()) {
            return new CommitThreshold(Instant.MIN, null);
        }

        Instant latestTimestamp = null;
        String latestCommitHash = null;
        Repository repo = git.getRepository();

        Instant effectiveSubmissionTimestamp;
        try (RevWalk revWalk = new RevWalk(repo)) {
            for (Submission submission : passingSubmissions) {
                effectiveSubmissionTimestamp = getEffectiveTimestampOfSubmission(revWalk, submission);
                revWalk.reset(); // Resetting a `revWalk` is more effective than creating a new one
                if (latestTimestamp == null || effectiveSubmissionTimestamp.isAfter(latestTimestamp)) {
                    latestTimestamp = effectiveSubmissionTimestamp;
                    latestCommitHash = submission.headHash();
                }
            }
        }

        if (latestTimestamp == null) {
            throw new GradingException("After processing a non-empty set of passing submissions, our latestTimestamp timestamp is null.");
        }

        return new CommitThreshold(latestTimestamp, latestCommitHash);
    }
    private Instant getEffectiveTimestampOfSubmission(RevWalk revWalk, Submission submission) throws IOException {
        try {
            ObjectId commitId = ObjectId.fromString(submission.headHash());
            RevCommit commit = revWalk.parseCommit(commitId);
            return Instant.ofEpochSecond(commit.getCommitTime());
        } catch (MissingObjectException | IncorrectObjectTypeException ex) {
            // The commit didn't exist. It may have been garbage collected if they rebased.
            // The hash may not have been valid. This shouldn't happen, but if it does, we'll continue.
            return submission.timestamp();
        }
    }

    /**
     * Counts the commits since the last passoff and halts progress if there are less than the required amount
     *
     * @return the number of commits since the last passoff
     */
    private CommitVerificationResult verifyRegularCommits(Git git) throws GitAPIException, IOException, GradingException {
        CommitThreshold mostRecentSubmission = getMostRecentPassingSubmission(git);
        Instant minValidThreshold = mostRecentSubmission.timestamp;
        Instant maxValidThreshold = ScorerHelper.getHandInDateInstant(gradingContext.netId());

        CommitsByDay commitHistory = analyzeCommitHistoryForSubmission(git, minValidThreshold, maxValidThreshold);
        CommitVerificationResult commitVerificationResult = commitsPassRequirements(git, commitHistory);
        LOGGER.debug("Commit verification result: " + JSON.toString(commitVerificationResult));

        var observer = gradingContext.observer();
        if (commitVerificationResult.verified()) {
            observer.update("Passed commit verification.");
        } else {
            observer.update("Failed commit verification. Continuing with grading anyways.");
        }
        return commitVerificationResult;
    }
    private CommitsByDay analyzeCommitHistoryForSubmission(
            Git git,
            CommitThreshold lowerThreshold,
            CommitThreshold upperThreshold
    ) throws IOException, GitAPIException {
        Iterable<RevCommit> commits = git.log().all().call();
        return CommitAnalytics.countCommitsByDay(
                commits,
                minValidThreshold.getEpochSecond(),
                maxValidThreshold.getEpochSecond());
    }
    private CommitVerificationResult commitsPassRequirements(Git git, CommitsByDay commitsByDay) throws IOException {
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
            errorMessages.add(String.format("Did not commit on enough days to pass off (%d/%d).", daysWithCommits, requiredDaysWithCommits));
        }

        // Add additional explanatory message
        if (!verified) {
            errorMessages.add("Since you did not meet the prerequisites for commit frequency, "
                    + "you will need to talk to a TA to receive a score. ");
            errorMessages.add(String.format("It will come with a %d%% penalty.", commitVerificationPenaltyPct));
        }

        String headHash = getHeadHash(git);
        return new CommitVerificationResult(
                verified,
                numCommits,
                daysWithCommits,
                String.join("\n", errorMessages),
                Instant.ofEpochSecond(commitsByDay.lowerBoundSeconds()),
                Instant.ofEpochSecond(commitsByDay.upperBoundSeconds()),
                headHash,
                null // TODO: populate with the tail hash
        );
    }

}
