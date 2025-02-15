package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitAnalytics;
import edu.byu.cs.analytics.CommitThreshold;
import edu.byu.cs.analytics.CommitsByDay;
import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitValidation.*;
import edu.byu.cs.autograder.score.ScorerHelper;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Submission;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * In this class, <pre>static</pre> methods are used to distinguish those that run are threadsafe and preserve no state.
 */
public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);
    private final GradingContext gradingContext;
    private final CommitVerificationStrategy commitVerificationStrategy;
    private String headHash;

    public static final CommitThreshold MIN_COMMIT_THRESHOLD = new CommitThreshold(Instant.MIN, null);

    public GitHelper(GradingContext gradingContext, CommitVerificationStrategy commitVerificationStrategy) {
        this.gradingContext = gradingContext;
        this.commitVerificationStrategy = commitVerificationStrategy;
    }

    // ## Entry Point ##
    public CommitVerificationReport setUpAndVerifyHistory() throws GradingException {
        setUp();
        return verifyCommitHistory();
    }

    public void setUp() throws GradingException {
        File stageRepo = gradingContext.stageRepo();
        fetchRepo(stageRepo);
        headHash = getHeadHash(stageRepo);
    }

    public CommitVerificationReport verifyCommitHistory() {
        if (headHash == null) {
            throw new RuntimeException("Cannot verifyCommitHistory before headHash has been populated. Call setUp() first.");
        }

        gradingContext.observer().update("Verifying commits...");

        try {
            CommitVerificationReport report = shouldVerifyCommits() ?
                    verifyCommitRequirements(gradingContext.stageRepo()) :
                    skipCommitVerification(true, headHash, null);

            CommitVerificationResult result = report.result();
            if (result.warningMessages() != null) {
                var observer = gradingContext.observer();
                result.warningMessages().forEach(observer::notifyWarning);
            }

            return report;
        } catch (GradingException e) {
            // Grading can continue, we'll just alert them of the error.
            String errorStr = "Internally failed to evaluate commit history: " + e.getMessage();
            gradingContext.observer().notifyWarning(errorStr);
            LOGGER.error("Failed to evaluate commit history", e);
            return skipCommitVerification(false, headHash, errorStr);
        }
    }

    /**
     * Determines if the current grading context requires verifying the commit history.
     *
     * @return True if the commits should be verified; otherwise, false.
     */
    private boolean shouldVerifyCommits() {
        return !gradingContext.admin() && PhaseUtils.shouldVerifyCommits(gradingContext.phase());
    }

    /**
     * Fetches the student repo and puts it in the given local path
     */
    private void fetchRepo(File intoDirectory) throws GradingException {
        gradingContext.observer().update("Fetching repo...");

        fetchRepoFromUrl(gradingContext.repoUrl(), intoDirectory);
    }

    /**
     * Clones a repo URL into a temporary directory, and returns the directory where it was saved.
     * <br>
     * If any problems arise, the temporary directory is cleaned up and a {@link GradingException} is thrown.
     *
     * @param repoUrl The string URL to clone.
     * @return A {@link File} to the newly cloned repo root.
     * @throws GradingException When any problem occurs while downloading the repo. No temporary directory is preserved.
     */
    public static File fetchRepoFromUrl(String repoUrl) throws GradingException {
        File cloningDir = new File("./tmp" + UUID.randomUUID());
        try {
            fetchRepoFromUrl(repoUrl, cloningDir);
        } catch (GradingException exception) {
            FileUtils.removeDirectory(cloningDir);
            throw exception;
        }
        return cloningDir;
    }

    /**
     * Clones a repo URL into the specified directory on the local machine.
     *
     * @param repoUrl A string URL to clone
     * @param intoDirectory A {@link File} representing the target location.
     * @throws GradingException When the repo cannot be cloned. This is usually due to an invalid or non-existent repository.
     *                          When this occurs, the temporary directory will have any remnants of the partially complete clone command.
     */
    public static void fetchRepoFromUrl(String repoUrl, File intoDirectory) throws GradingException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(intoDirectory);

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Cloned repo to {}", git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            throw new GradingException("Failed to clone repo: " + e.getMessage(), e);
        }
    }

    // Early decisions
    private CommitVerificationReport skipCommitVerification(boolean verified, File stageRepo) throws GradingException {
        String headHash = getHeadHash(stageRepo);
        return skipCommitVerification(verified, headHash, null);
    }
    private CommitVerificationReport skipCommitVerification(boolean verified, @NonNull String headHash, String failureMessage) {
        if (headHash == null) {
            throw new IllegalArgumentException("Head hash cannot be null");
        }
        LOGGER.debug("Skipping commit verification. Verified: {}", verified);
        return new CommitVerificationResult(
                verified, false,
                0, 0, 0, false, 0, failureMessage, null,
                Instant.MIN, Instant.MAX,
                headHash, null
        ).toReport(null);
    }

    // Decision Logic

    /**
     * Decides whether a git repo passes the requirements and returns the evaluation results.
     *
     * @param stageRepo A {@link File} pointing to a directory on the local machine with the repo to evaluate.
     * @return A {@link CommitVerificationReport} with the results.
     * @throws GradingException When certain assumptions are not met.
     */
    CommitVerificationReport verifyCommitRequirements(File stageRepo) throws GradingException {
        try {
            var potentialResult = preserveOriginalVerification();
            if (potentialResult != null) {
                return potentialResult;
            }

            // This could be the first passing submission. We have to calculate it from scratch.
            Collection<Submission> passingSubmissions = getPassingSubmissions();

            try (Git git = Git.open(stageRepo)) {
                CommitThreshold lowerThreshold = getMostRecentPassingSubmission(git, passingSubmissions);
                CommitThreshold upperThreshold = constructCurrentThreshold(git);

                return verifyRegularCommits(git, lowerThreshold, upperThreshold);
            }
        } catch (Exception e) {
            throw new GradingException("Failed to verify commits: " + e.getMessage(), e);
        }
    }

    /**
     * Performs the explicit remembering of the authorization of previous phases.
     *
     * @return Null if no decision is made. If a previous submission exists for the given phase,
     * returns a special {@link CommitVerificationReport} that represents the state.
     */
    private CommitVerificationReport preserveOriginalVerification() throws DataAccessException {
        Submission firstPassingSubmission = getFirstPassingSubmission();
        if (firstPassingSubmission == null || firstPassingSubmission.verifiedStatus() == null) {
            return null;
        }

        // We have a previous result to defer to:
        int originalPenaltyPct = firstPassingSubmission.getPenaltyPct();
        boolean verified = firstPassingSubmission.isApproved();
        String failureMessage = generateFailureMessage(verified, firstPassingSubmission);
        return new CommitVerificationResult(
                verified, true,
                0, 0, 0, false, originalPenaltyPct, failureMessage, null,
                null, null, headHash, null
        ).toReport(null);
    }

    private static String generateFailureMessage(boolean verified, Submission firstPassingSubmission) {
        String message;
        if (!verified) {
            message = "You have previously failed commit verification.\n"+
                    "You still need to meet with a TA or a professor to gain credit for this phase.";
        } else {
            var verification = firstPassingSubmission.verification();
            if (verification == null || verification.penaltyPct() <= 0) {
                message = "You passed the commit verification on your first passing submission! You're good to go!";
            } else {
                message = "Your commit verification was previously approved with a penalty. That penalty is being applied to this submission as well.";
            }
        }
        return message;
    }

    /**
     * Analyzes the commits in the given directory within the bounds provided.
     *
     * @return {@link CommitVerificationReport} Representing the verification results and context used to generate it
     */
    CommitVerificationReport verifyRegularCommits(
            Git git, CommitThreshold lowerThreshold, CommitThreshold upperThreshold)
            throws GitAPIException, IOException, DataAccessException, GradingException {

        Set<String> excludeCommits = new HashSet<>();
        int minimumLinesChangedPerCommit = gradingContext.verificationConfig().minimumChangedLinesPerCommit();

        do {

            CommitsByDay commitsByDay = CommitAnalytics.countCommitsByDay(git, lowerThreshold, upperThreshold, excludeCommits);

            int numCommits = commitsByDay.totalCommits();
            int daysWithCommits = commitsByDay.dayMap().size();
            long significantCommits = commitsByDay.lineChangesPerCommit().values()
                    .stream().filter(i -> i >= minimumLinesChangedPerCommit).count();

            CommitVerificationContext context = new CommitVerificationContext(
                    gradingContext.verificationConfig(),
                    commitsByDay,
                    numCommits,
                    daysWithCommits,
                    significantCommits
            );

            commitVerificationStrategy.evaluate(context, gradingContext);
            var excludeSet = commitVerificationStrategy.extendExcludeSet();
            if (excludeSet != null && !excludeSet.isEmpty()) {
                // Restart, but exclude the requested commits
                excludeCommits.addAll(excludeSet);
                continue;
            }

            Result warningResults = commitVerificationStrategy.getWarnings();
            Result errorResults = commitVerificationStrategy.getErrors();
            String errorMessage = errorResults == null ? "" : String.join("\n", errorResults.messages());
            boolean hasErrors = errorResults != null && !errorResults.isEmpty();

            var result = new CommitVerificationResult(
                    !hasErrors,
                    false,
                    numCommits,
                    (int) significantCommits,
                    daysWithCommits,
                    commitsByDay.missingTailHash(),
                    0, // Penalties are applied by TA's upon approval of unapproved submissions
                    errorMessage,
                    warningResults.messages(),
                    commitsByDay.lowerThreshold().timestamp(),
                    commitsByDay.upperThreshold().timestamp(),
                    commitsByDay.upperThreshold().commitHash(),
                    commitsByDay.lowerThreshold().commitHash()
            );
            return new CommitVerificationReport(context, result);
        } while (true);

    }

    // Evaluation Helpers

    /**
     * Returns a timestamp corresponding to the most recent commit that was giving a passing grade,
     * and the commit at that point.
     * <br>
     * In any case, if the commit cannot be located, then submission timestamp will be used in its place.
     * If there are no previous passing submissions, this returns the minimum Instant instead.
     * <br>
     * Submissions on non-graded phases do not count towards the `CommitThreshold`.
     *
     * @return An {@link CommitThreshold}. Returns an empty object rather than null when no result exists.
     * @throws GradingException When certain preconditions are not met, or when this would have returned null.
     */
    @NonNull
    private CommitThreshold getMostRecentPassingSubmission(Git git, Collection<Submission> passingSubmissions)
            throws IOException, GradingException {
        if (passingSubmissions == null) {
            throw new GradingException("Cannot extract previous submission date before passingSubmissions are loaded.");
        }
        if (passingSubmissions.isEmpty()) {
            return MIN_COMMIT_THRESHOLD;
        }

        Instant latestTimestamp = null;
        String latestCommitHash = null;
        Repository repo = git.getRepository();
        boolean hasCandidateSubmission = false;

        Instant effectiveSubmissionTimestamp;
        try (RevWalk revWalk = new RevWalk(repo)) {
            for (Submission submission : passingSubmissions) {
                if (!PhaseUtils.isPhaseGraded(submission.phase())) continue;

                hasCandidateSubmission = true;
                effectiveSubmissionTimestamp = getEffectiveTimestampOfSubmission(revWalk, submission);
                revWalk.reset(); // Resetting a `revWalk` is more effective than creating a new one
                if (latestTimestamp == null || effectiveSubmissionTimestamp.isAfter(latestTimestamp)) {
                    latestTimestamp = effectiveSubmissionTimestamp;
                    latestCommitHash = submission.headHash();
                }
            }
        }

        if (!hasCandidateSubmission) {
            return MIN_COMMIT_THRESHOLD;
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
     * Constructs the current threshold for the grading.
     * This will represent the upper bound on the grading process.
     * <br>
     * Specifically guarantees that not only is the result NonNull,
     * but also that the `timestamp` and `headHash` fields are NonNull as well.
     *
     * @param git The `git` object of the repo to grade
     * @return A NonNull, non-null field {@link CommitThreshold}
     * @throws IOException When a file reading error occurs
     * @throws GradingException When one field would be null, or when another error occurs.
     */
    @NonNull
    private CommitThreshold constructCurrentThreshold(Git git) throws IOException, GradingException, DataAccessException {
        var handInTimestamp = ScorerHelper.getHandInDateInstant(gradingContext.netId());
        var forgivenessMinutesHead = gradingContext.verificationConfig().forgivenessMinutesHead();
        if (handInTimestamp != null) {
            handInTimestamp = handInTimestamp.plusSeconds(forgivenessMinutesHead * 60L);
        }
        CommitThreshold currentThreshold = new CommitThreshold(
                handInTimestamp,
                getHeadHash(git)
        );
        if (currentThreshold.timestamp() == null) {
            throw new GradingException("Current threshold cannot have a null timestamp");
        }
        if (currentThreshold.commitHash() == null) {
            throw new GradingException("Current threshold cannot have a null commit hash");
        }

        return currentThreshold;
    }

    // Helpers

    private Collection<Submission> getPassingSubmissions() throws DataAccessException {
        return DaoService.getSubmissionDao().getAllPassingSubmissions(gradingContext.netId());
    }
    private Submission getFirstPassingSubmission() throws DataAccessException {
        // CONSIDER: Rather than resolving this as a second database call,
        // read out the data from our `passingSubmissions` data that
        // we've already loaded locally.
        // This would require performing `getPassingSubmissions()` before this method.
        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        return submissionDao.getFirstPassingSubmission(gradingContext.netId(), gradingContext.phase());
    }
    private String getHeadHash(File stageRepo) throws GradingException {
        try (Git git = Git.open(stageRepo)) {
            return getHeadHash(git);
        } catch (IOException e) {
            throw new GradingException("Failed to get head hash: " + e.getMessage());
        }
    }
    static String getHeadHash(Git git) throws IOException {
        return git.getRepository().findRef("HEAD").getObjectId().getName();
    }

}
