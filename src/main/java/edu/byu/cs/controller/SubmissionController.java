package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.TrafficController;
import edu.byu.cs.autograder.test.TestAnalyzer;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.model.*;
import edu.byu.cs.util.ProcessUtils;
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

        Boolean submissionsEnabled = getSubmissionsEnabledConfig();
        if (submissionsEnabled == null) return null;

        if (!submissionsEnabled) {
            halt(400, "Student submission is disabled");
            return null;
        }

        updateRepoFromCanvas(user, req);

        if (! verifyHasNewCommits(user, request.phase()) ) { return null; }

        LOGGER.info("User {} submitted phase {} for grading", user.netId(), request.phase());

        startGrader(user.netId(), request.phase(), user.repoUrl(), false);

        res.status(200);
        return "";
    };

    private static Boolean getSubmissionsEnabledConfig() {
        boolean submissionsEnabled;
        try {
            submissionsEnabled = DaoService.getConfigurationDao().getConfiguration(
                    ConfigurationDao.Configuration.STUDENT_SUBMISSION_ENABLED,
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
            request = new Gson().fromJson(req.body(), GradeRequest.class);
        } catch (JsonSyntaxException e) {
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

        return new Gson().toJson(Map.of(
                "inQueue", inQueue
        ));
    };

    public static final Route submissionXGet = (req, res) -> {
        String phase = req.params(":phase");
        Phase phaseEnum = null;
        try {
            phaseEnum = Phase.valueOf(phase);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid phase", e);
            halt(400, "Invalid phase");
        }

        User user = req.session().attribute("user");

        Collection<Submission> submissions;
        try {
            submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(user.netId(), phaseEnum);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", user.netId(), e);
            halt(500);
            return null;
        }

        res.status(200);
        res.type("application/json");

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
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

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
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

        return new Gson().toJson(Map.of(
                "currentlyGrading", currentlyGrading,
                "inQueue", inQueue
        ));
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

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
    };

    public static final Route approveSubmissionPost = (req, res) -> {
        // FIXME: These are only provided as reasonable guesses, and as a starting point.
        // This may not be the way we want to actually go with this end-point,
        // the only reflect the data that will need to be transferred.
        String studentNetId = req.params(":studentNetId");
        Phase phase = Phase.valueOf(req.params(":phase"));
        Float approvedScore = Float.valueOf(req.params(":approvedScore"));
        Integer penaltyPct = Integer.valueOf(req.params(":penaltyPct"));
        String approvingNetId = req.params(":approvingNetId");

        // FIXME: Validate that all of the parameters were received as valid, non-empty types.
        // Note that the `approvedScore` field can be optionally `null`.

        approveSubmission(studentNetId, phase, approvingNetId, penaltyPct);
//        // TODO: Lookup a submission by phase, netId, and headHash to pass in as `targetSubmission`
//        approveSubmission(studentNetId, phase, approvingNetId, penaltyPct, approvedScore, null);

        // FIXME: Consider returning more interesting or hepful data.
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
    private static Grader getGrader(String netId, Phase phase, String repoUrl, boolean adminSubmission) throws IOException {
        Grader.Observer observer = new Grader.Observer() {
            @Override
            public void notifyStarted() {
                try {
                    DaoService.getQueueDao().markStarted(netId);
                } catch (DataAccessException e) {
                    LOGGER.error("Error marking queue item as started", e);
                    return;
                }

                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "started"
                ));

                try {
                    TrafficController.broadcastQueueStatus();
                } catch (Exception e) {
                    LOGGER.error("Error broadcasting queue status", e);
                }
            }

            @Override
            public void update(String message) {
                try {
                    TrafficController.getInstance().notifySubscribers(netId, Map.of(
                            "type", "update",
                            "message", message
                    ));
                } catch (Exception e) {
                    LOGGER.error("Error updating subscribers", e);
                }
            }

            @Override
            public void notifyError(String message) {
                notifyError(message, Map.of());
            }

            @Override
            public void notifyError(String message, String details) {
                notifyError(message, Map.of("details", details));
            }

            @Override
            public void notifyError(String message, TestAnalyzer.TestAnalysis analysis) {
                notifyError(message, Map.of("analysis", analysis));
            }

            public void notifyError(String message, Map<String, Object> contents) {
                contents = new HashMap<>(contents);
                contents.put( "type", "error");
                contents.put("message", message);
                TrafficController.getInstance().notifySubscribers(netId, contents);

                TrafficController.sessions.remove(netId);
                try {
                    DaoService.getQueueDao().remove(netId);
                } catch (DataAccessException e) {
                    LOGGER.error("Error removing queue item", e);
                }
            }

            @Override
            public void notifyDone(Submission submission) {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                        .create();
                try {
                    TrafficController.getInstance().notifySubscribers(netId, Map.of(
                            "type", "results",
                            "results", gson.toJson(submission)
                    ));
                } catch (Exception e) {
                    LOGGER.error("Error updating subscribers", e);
                }

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

        return new Gson().toJson(Map.of(
                "message", "re-running submissions in queue"
        ));
    };


    /**
     * Takes any submissions currently in the queue and reruns them through the grader.
     * Used if the queue got stuck or if the server crashed while submissions were
     * waiting in the queue.
     */
    public static void reRunSubmissionsInQueue() throws IOException, DataAccessException {
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
            throws DataAccessException {
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
     * @param approvedScore <p>The final score that should go in the grade-book.</p>
     *                      <p>If `null`, we'll apply the penalty to the highest score for any submission in the phase.</p>
     *                      <p>Provided so that a TA can approve an arbitrary (highest score)
     *                      submission with a penalty instead of any other fixed rule.</p>
     * @param targetSubmission Required when `approvedScored` is passed in.
     *                         Provides a submission which will be used to overwrite the existing score in the grade-book.
     *                         If a full {@link Submission} object is not available, the {@link Rubric} is only required field in it.
     */
    public static void approveSubmission(
            @NonNull String studentNetId,
            @NonNull Phase phase,
            @NonNull String approverNetId,
            @NonNull Integer penaltyPct,
            @Nullable Float approvedScore,
            @Nullable Submission targetSubmission
    ) throws DataAccessException {
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
        Submission withheldSubmission = submissionDao.getFirstPassingSubmission(studentNetId, phase);

        // Update Submissions
        Float originalScore = withheldSubmission.score();
        Instant approvedTimestamp = Instant.now();
        Submission.ScoreVerification scoreVerification = new Submission.ScoreVerification(
                originalScore,
                approverNetId,
                approvedTimestamp,
                penaltyPct);
        int submissionsAffected = SubmissionHelper.approveWithheldSubmissions(submissionDao, studentNetId, phase, scoreVerification);

        if (submissionsAffected < 1) {
            LOGGER.warn("Approving submissions did not affect any submissions. Something probably went wrong.");
        }

        // Determine approvedScore
        if (approvedScore == null) {
            float bestScoreForPhase = submissionDao.getBestScoreForPhase(studentNetId, phase);
            if (bestScoreForPhase < 0.0f) {
                throw new RuntimeException("Cannot determine best score for phase without any submissions in the phase.");
            }
            approvedScore = SubmissionHelper.prepareModifiedScore(bestScoreForPhase, scoreVerification);
        }
        if (approvedScore <= 0.0f) {
            throw new RuntimeException("Cannot set grade without a positive approvedScore!");
        }

        // Update grade-book
        CanvasIntegration canvasIntegration = CanvasService.getCanvasIntegration();
        // FIXME: Store `approvedScore` in the Grade-book
        // canvasIntegration.submitGrade(studentNetId, approvedScore, );
        if (true) {
            throw new RuntimeException("ApproveSubmission not implemented!"); // TODO: Finish implementing method
        }

        // Done
        LOGGER.info("Approved submission for %s on phase %s with score %f. Approval by %s. Affected %d submissions."
                .formatted(studentNetId, phase.name(), approvedScore, approverNetId, submissionsAffected));
    }

}
