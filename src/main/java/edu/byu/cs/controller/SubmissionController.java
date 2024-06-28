package edu.byu.cs.controller;

import edu.byu.cs.autograder.*;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.controller.netmodel.ApprovalRequest;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.model.*;
import edu.byu.cs.util.ProcessUtils;
import edu.byu.cs.util.Serializer;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Route;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static spark.Spark.halt;

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Route submitPost = (req, res) -> {

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) { return null; }

        User user = req.session().attribute("user");

        if (!phaseIsEnabled(request.phase())) {
            halt(400, "Student submission is disabled for " + request.phase());
        }

        updateRepoFromCanvas(user, req);

        if (! verifyHasNewCommits(user, request.phase()) ) { return null; }

        LOGGER.info("User {} submitted phase {} for grading", user.netId(), request.phase());

        startGrader(user.netId(), request.phase(), user.repoUrl(), false);

        res.status(200);
        return "";
    };

    private static boolean phaseIsEnabled(Phase phase) {
        boolean phaseEnabled;

        try {
            phaseEnabled = DaoService.getConfigurationDao()
                    .getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class)
                    .contains(phase.toString());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting configuration for live phase", e);
            halt(500);
            return false;
        }

        return phaseEnabled;
    }

    private static Boolean getSubmissionsEnabledConfig() {
        boolean submissionsEnabled;
        try {
            submissionsEnabled = DaoService.getConfigurationDao().getConfiguration(
                    ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED,
                    Boolean.class);
        } catch (Exception e) {
            LOGGER.error("Error getting configuration", e);
            halt(500);
            return null;
        }
        return submissionsEnabled;
    }

    public static final Route adminRepoSubmitPost = (req, res) -> {

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) { return null; }

        User user = req.session().attribute("user");

        LOGGER.info("Admin {} submitted phase {} on repo {} for test grading", user.netId(), request.phase(),
                request.repoUrl());

        DaoService.getSubmissionDao().removeSubmissionsByNetId(user.netId(), 3);

        startGrader(user.netId(), request.phase(), request.repoUrl(), true);

        res.status(200);
        return "";
    };

    private static void startGrader(String netId, Phase phase, String repoUrl, boolean adminSubmission) throws DataAccessException {
        DaoService.getQueueDao().add(
                new edu.byu.cs.model.QueueItem(
                        netId,
                        phase,
                        Instant.now(),
                        false
                )
        );

        TrafficController.sessions.put(netId, new ArrayList<>());

        try {
            Grader grader = getGrader(netId, phase, repoUrl, adminSubmission);

            TrafficController.getInstance().addGrader(grader);

        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid phase", e);
            halt(400, "Invalid phase");
        } catch (Exception e) {
            LOGGER.error("Error starting grader", e);
            halt(500);
        }
    }

    private static void updateRepoFromCanvas(User user, Request req) throws CanvasException, DataAccessException {
        CanvasIntegration canvas = CanvasService.getCanvasIntegration();
        String newRepoUrl = canvas.getGitRepo(user.canvasUserId());
        if (!newRepoUrl.equals(user.repoUrl())) {
            user = new User(user.netId(), user.canvasUserId(), user.firstName(), user.lastName(), newRepoUrl, user.role());
            DaoService.getUserDao().setRepoUrl(user.netId(), newRepoUrl);
            req.session().attribute("user", user);
        }
    }

    private static boolean verifyHasNewCommits(User user, Phase phase) throws DataAccessException {
        String headHash;
        try {
            headHash = getRemoteHeadHash(user.repoUrl());
        } catch (Exception e) {
            LOGGER.error("Error getting remote head hash", e);
            halt(400, "Invalid repo url");
            return false;
        }
        Submission submission = getMostRecentSubmission(user.netId(), phase);
        if (submission != null && submission.headHash().equals(headHash)) {
            halt(400, "You have already submitted this version of your code for this phase. Make a new commit before submitting again");
            return false;
        }
        return true;
    }

    private static GradeRequest validateAndUnpackRequest(Request req) throws DataAccessException {
        User user = req.session().attribute("user");
        String netId = user.netId();

        if (DaoService.getQueueDao().isAlreadyInQueue(netId)) {
            halt(400, "You are already in the queue");
            return null;
        }

        GradeRequest request;
        try {
            request = Serializer.deserialize(req.body(), GradeRequest.class);
        } catch (Serializer.SerializationException e) {
            halt(400, "Request must be valid json");
            return null;
        }

        if (request == null || request.phase() == null) {
            halt(400, "Request is invalid");
            return null;
        }

        if (user.repoUrl() == null && user.role() == User.Role.STUDENT) {
            halt(400, "Student has no provided repo url");
            return null;
        }

        return request;
    }

    /**
     * gets the most recent submission for the specified user in the specified phase
     *
     * @param netId the netID of the student to get a submission for
     * @param phase the phase of the project to get
     * @return the most recent submission, or null if there are no submissions for this student in this phase
     */
    public static Submission getMostRecentSubmission(String netId, Phase phase) throws DataAccessException {
        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(netId, phase);
        Submission mostRecent = null;

        for (Submission submission : submissions) {
            if (mostRecent == null || mostRecent.timestamp().isBefore(submission.timestamp())) {
                mostRecent = submission;
            }
        }
        return mostRecent;
    }

    public static final Route submitGet = (req, res) -> {
        User user = req.session().attribute("user");
        String netId = user.netId();

        boolean inQueue = DaoService.getQueueDao().isAlreadyInQueue(netId);

        res.status(200);

        return Serializer.serialize(Map.of("inQueue", inQueue));
    };

    public static final Route latestSubmissionForMeGet = (req, res) -> {
        User user = req.session().attribute("user");

        Submission submission;
        try {
            submission = DaoService.getSubmissionDao().getLastSubmissionForUser(user.netId());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", user.netId(), e);
            halt(500);
            return null;
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submission);
    };

    public static final Route submissionXGet = (req, res) -> {
        String phase = req.params(":phase");
        Phase phaseEnum = null;

        if (phase != null) {
            try {
                phaseEnum = Phase.valueOf(phase);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid phase", e);
                halt(400, "Invalid phase");
            }
        }

        User user = req.session().attribute("user");
        Collection<Submission> submissions;
        try {
            if (phase == null) {
                submissions = DaoService.getSubmissionDao().getSubmissionsForUser(user.netId());
            } else {
                submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(user.netId(), phaseEnum);
            }
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", user.netId(), e);
            halt(500);
            return null;
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route latestSubmissionsGet = (req, res) -> {
        String countString = req.params(":count");
        int count = countString == null ? -1 : Integer.parseInt(countString); // if they don't give a count, set it to -1, which gets all latest submissions
        Collection<Submission> submissions = null;
        try {
            submissions = DaoService.getSubmissionDao().getAllLatestSubmissions(count);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting latest submissions", e);
            halt(500);
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route submissionsActiveGet = (req, res) -> {
        List<String> inQueue = null;
        List<String> currentlyGrading = null;
        try {
            inQueue = DaoService.getQueueDao().getAll().stream().filter((queueItem) -> !queueItem.started()).map(QueueItem::netId).toList();
            currentlyGrading = DaoService.getQueueDao().getAll().stream().filter(QueueItem::started).map(QueueItem::netId).toList();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting active submissions", e);
            halt(500);
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(Map.of("currentlyGrading", currentlyGrading, "inQueue", inQueue));
    };

    public static final Route studentSubmissionsGet = (req, res) -> {
        String netId = req.params(":netId");

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Collection<Submission> submissions = null;
        try {
            submissions = submissionDao.getSubmissionsForUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", netId, e);
            halt(500);
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route approveSubmissionPost = (req, res) -> {
        User adminUser = req.session().attribute("user");

        ApprovalRequest request = Serializer.deserialize(req.body(), ApprovalRequest.class);

        int penalty = 0;
        if (request.penalize()) {
            //TODO: Put somewhere better/more configurable
            penalty = 10;
        }

        approveSubmission(request.netId(), request.phase(), adminUser.netId(), penalty);
        return "{}";
    };

    /**
     * Creates a grader for the given request with an observer that sends messages to the subscribed sessions
     *
     * @param netId the netId of the user
     * @param phase the phase to grade
     * @param adminSubmission if the grader should run in admin mode
     * @return the grader
     * @throws IOException if there is an error creating the grader
     */
    private static Grader getGrader(String netId, Phase phase, String repoUrl, boolean adminSubmission) throws IOException, GradingException {
        GradingObserver observer = new GradingObserver() {
            @Override
            public void notifyStarted() {
                try {
                    DaoService.getQueueDao().markStarted(netId);
                } catch (DataAccessException e) {
                    LOGGER.error("Error marking queue item as started", e);
                    return;
                }

                notifySubscribers(Map.of("type", "started"));

                try {
                    TrafficController.broadcastQueueStatus();
                } catch (Exception e) {
                    LOGGER.error("Error broadcasting queue status", e);
                }
            }

            @Override
            public void update(String message) {
                notifySubscribers(Map.of("type", "update", "message", message));
            }

            @Override
            public void notifyError(String message) {
                notifyError(message, Map.of());
            }

            @Override
            public void notifyError(String message, Submission submission) {
                notifyError(message, Map.of("results", Serializer.serialize(submission)));
            }

            public void notifyError(String message, Map<String, Object> contents) {
                contents = new HashMap<>(contents);
                contents.put( "type", "error");
                contents.put("message", message);
                notifySubscribers(contents);
                removeFromQueue();
            }

            @Override
            public void notifyWarning(String message) {
                notifySubscribers(Map.of("type", "warning", "message", message));
            }

            @Override
            public void notifyDone(Submission submission) {
                notifySubscribers(Map.of("type", "results", "results", Serializer.serialize(submission)));
                removeFromQueue();
            }

            private void notifySubscribers(Map<String, Object> contents) {
                try {
                    TrafficController.getInstance().notifySubscribers(netId, contents);
                } catch (Exception e) {
                    LOGGER.error("Error updating subscribers", e);
                }
            }

            private void removeFromQueue() {
                TrafficController.sessions.remove(netId);
                try {
                    DaoService.getQueueDao().remove(netId);
                } catch (DataAccessException e) {
                    LOGGER.error("Error removing queue item", e);
                }
            }
        };

        return new Grader(repoUrl, netId, observer, phase, adminSubmission);
    }

    public static String getRemoteHeadHash(String repoUrl) {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "ls-remote", repoUrl, "HEAD");
        try {
            ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(processBuilder);
            if (output.statusCode() != 0) {
                LOGGER.error("git ls-remote exited with non-zero exit code\n{}", output.stdErr());
                throw new RuntimeException("exited with non-zero exit code");
            }
            return output.stdOut().split("\\s+")[0];
        } catch (ProcessUtils.ProcessException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Route submissionsReRunPost = (req, res) -> {
        reRunSubmissionsInQueue();

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(Map.of("message", "re-running submissions in queue"));
    };


    /**
     * Takes any submissions currently in the queue and reruns them through the grader.
     * Used if the queue got stuck or if the server crashed while submissions were
     * waiting in the queue.
     */
    public static void reRunSubmissionsInQueue() throws IOException, DataAccessException, GradingException {
        QueueDao queueDao = DaoService.getQueueDao();
        UserDao userDao = DaoService.getUserDao();
        Collection<QueueItem> inQueue = queueDao.getAll();

        for (QueueItem queueItem : inQueue) {
            User currentUser = userDao.getUser(queueItem.netId());
            queueDao.markNotStarted(queueItem.netId());

            TrafficController.getInstance().addGrader(
                    getGrader(queueItem.netId(),
                            queueItem.phase(),
                            currentUser.repoUrl(),
                            currentUser.role() == User.Role.ADMIN));
        }
    }

    /**
     * Approves the highest scoring submissions on the phase so far with a provided penalty percentage.
     * <br>
     * This is a simple overload triggering default behavior in the actual method.
     * @see SubmissionController#approveSubmission(String, Phase, String, Integer, Float, Submission).
     *
     * @param studentNetId The student to approve
     * @param phase The phase to approve
     * @param approverNetId Identifies the TA or professor approving the score
     * @param penaltyPct The penalty applied for the reduction.
     *                   This should already be reflected in the `approvedScore` if present.
     */
    public static void approveSubmission(
            @NonNull String studentNetId, @NonNull Phase phase, @NonNull String approverNetId, @NonNull Integer penaltyPct)
            throws DataAccessException, GradingException {
        approveSubmission(studentNetId, phase, approverNetId, penaltyPct, null, null);
    }
    /**
     * Approves a submission.
     * Modifies all existing submissions in the phase with constructed values,
     * and saves a given value into the grade-book.
     *
     * @param studentNetId The student to approve
     * @param phase The phase to approve
     * @param approverNetId Identifies the TA or professor approving the score
     * @param penaltyPct The penalty applied for the reduction.
     *                   This should already be reflected in the `approvedScore` if present.
     *                   <p>This will be used to reduce all other submissions,
     *                   but has no effect in the grade-book when <code>approvedScore</code> is provided.</p>
     * @param approvedScore <p>The final score that should go in the grade-book.
     *                      Submissions in the database will <b>not</b> be affected by this value.</p>
     *                      <p>If `null`, we'll determine this value by applying the penalty to
     *                      the highest score for any submission in the phase.</p>
     *                      <p>Provided so that a TA can approve an arbitrary (highest score)
     *                      submission with a penalty instead of any other fixed rule.</p>
     * @param submissionToUse Required when `approvedScored` is passed in.
     *                         Provides a submission which will be used to overwrite the existing score in the grade-book.
     *                         If a full {@link Submission} object is not available, the {@link Rubric} is only required field in it.
     */
    public static void approveSubmission(
            @NonNull String studentNetId,
            @NonNull Phase phase,
            @NonNull String approverNetId,
            @NonNull Integer penaltyPct,
            @Nullable Float approvedScore,
            @Nullable Submission submissionToUse
    ) throws DataAccessException, GradingException {
        if (approvedScore != null) {
            throw new IllegalArgumentException("Passing in non-null approvedScore is not yet implemented.");
        }

        // Validate params
        if (studentNetId == null || phase == null || approverNetId == null || penaltyPct == null) {
            throw new IllegalArgumentException("All of studentNetId, approverNetId, and penaltyPct must not be null.");
        }
        if (studentNetId.isBlank() || approverNetId.isBlank()) {
            throw new IllegalArgumentException("Both studentNetId and approverNetId must not be blank");
        }
        if (penaltyPct < 0 || (approvedScore != null && approvedScore < 0)) {
            throw new IllegalArgumentException("Both penaltyPct and approvedScore must be greater or equal than 0");
        }

        // Read in data
        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        assertSubmissionUnapproved(submissionDao, studentNetId, phase);
        Submission withheldSubmission = determineSubmissionForConsideration(submissionDao, submissionToUse, studentNetId, phase);

        // Modify values in our database first
        int submissionsAffected = modifySubmissionEntriesInDatabase(submissionDao, withheldSubmission, approverNetId, penaltyPct);

        // Send score to Grade-book
        if (approvedScore == null) {
            approvedScore = SubmissionHelper.prepareModifiedScore(withheldSubmission.score(), penaltyPct);
        }
        String gitCommitsComment = "Submission initially blocked due to low commits. Submission approved by admin " + approverNetId;
        sendScoreToCanvas(withheldSubmission, penaltyPct, gitCommitsComment);

        // Done
        LOGGER.info("Approved submission for %s on phase %s with score %f. Approval by %s. Affected %d submissions."
                .formatted(studentNetId, phase.name(), approvedScore, approverNetId, submissionsAffected));
    }

    private static void assertSubmissionUnapproved(SubmissionDao submissionDao, String studentNetId, Phase phase) throws DataAccessException {
        Submission withheldSubmission = submissionDao.getFirstPassingSubmission(studentNetId, phase);
        if (withheldSubmission.isApproved()) {
            throw new RuntimeException(studentNetId + " needs no approval for phase " + phase);
        }
    }

    private static Submission determineSubmissionForConsideration(
            SubmissionDao submissionDao, Submission providedValue, String studentNetId, Phase phase)
            throws DataAccessException, GradingException {
        if (providedValue != null) return providedValue;

        Submission submission = submissionDao.getBestSubmissionForPhase(studentNetId, phase);
        if (submission == null) {
            throw new GradingException("No submission was provided nor found for phase " + phase + " with user " + studentNetId);
        }
        return submission;
    }

    private static int modifySubmissionEntriesInDatabase(
            SubmissionDao submissionDao, Submission withheldSubmission, String approvingNetId, int penaltyPct)
            throws DataAccessException {

        Float originalScore = withheldSubmission.score();
        Instant approvedTimestamp = Instant.now();
        Submission.ScoreVerification scoreVerification = new Submission.ScoreVerification(
                originalScore,
                approvingNetId,
                approvedTimestamp,
                penaltyPct);
        int submissionsAffected = SubmissionHelper.approveWithheldSubmissions(
                submissionDao,
                withheldSubmission.netId(),
                withheldSubmission.phase(),
                scoreVerification);

        if (submissionsAffected < 1) {
            LOGGER.warn("Approving submissions did not affect any submissions. Something probably went wrong.");
        }
        return submissionsAffected;
    }

    /**
     * Submits the <code>approvedScore</code> to Canvas by modifying the GIT_COMMITS rubric item.
     * <br>
     * In this context, the <code>withheldSubmission</code> isn't <i>strictly</i> necessary,
     * but it is used to transfer several fields and values that would need to be transferred individually
     * without it. The most important of these is the <score>field</score>, which will be used to calculate the penalty.
     *
     * @param withheldSubmission The baseline submission to approve
     * @param penaltyPct The percentage that should be reduced from the score for GIT_COMMITS.
     * @param commitPenaltyComment The comment that should be associated with the GIT_COMMITS rubric, if any.
     * @throws DataAccessException When the database cannot be accessed.
     * @throws GradingException When pre-conditions are not met.
     */
    private static void sendScoreToCanvas(Submission withheldSubmission, int penaltyPct, String commitPenaltyComment) throws DataAccessException, GradingException {
        // Prepare and assert arguments
        if (withheldSubmission == null) {
            throw new IllegalArgumentException("Withheld submission cannot be null");
        }
        Scorer scorer = getScorer(withheldSubmission);
        scorer.attemptSendToCanvas(withheldSubmission.rubric(), penaltyPct, commitPenaltyComment, true);
    }

    /**
     * Constructs an instance of {@link Scorer} using the limited data available in a {@link Submission}.
     *
     * @param submission A submission containing context to extract.
     * @return A constructed Scorer instance.
     */
    private static Scorer getScorer(Submission submission) {
        String studentNetId = submission.netId();
        Phase phase = submission.phase();
        if (studentNetId == null) {
            throw new IllegalArgumentException("Student net ID cannot be null");
        }
        if (phase == null) {
            throw new IllegalArgumentException("Phase cannot be null");
        }

        return new Scorer(new GradingContext(
                studentNetId, phase, null, null, null, null,
                new CommitVerificationConfig(0, 0, 0, 0, 0),
                null, submission.admin()
        ));
    }

}
