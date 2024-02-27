package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import edu.byu.cs.autograder.*;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.QueueDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.*;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static spark.Spark.halt;

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static Route submitPost = (req, res) -> {

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

        if (request == null) {
            halt(400, "Request is invalid");
            return null;
        }

        // FIXME: improve git url validation
//        if (!request.repoUrl().matches("^https://[\\w.]+.\\w+/[\\w\\D]+/[\\w-/]+.git$")) {
//            halt(400, "That doesn't look like a valid git url");
//            return;
//        }
        if (!Arrays.asList(0, 1, 3, 4, 6).contains(request.phase())) {
            halt(400, "Valid phases are 0, 1, 3, 4, or 6");
            return null;
        }

        if (user.repoUrl() == null) {
            halt(400, "You must provide a repo url");
            return null;
        }

        // check for updated repoUrl
        String newRepoUrl = CanvasIntegration.getGitRepo(user.canvasUserId());
        if (!newRepoUrl.equals(user.repoUrl())) {
            user = new User(user.netId(), user.canvasUserId(), user.firstName(), user.lastName(), newRepoUrl, user.role());
            DaoService.getUserDao().setRepoUrl(user.netId(), newRepoUrl);
            req.session().attribute("user", user);
        }

        String headHash;
        try {
            headHash = getRemoteHeadHash(user.repoUrl());
        } catch (Exception e) {
            LOGGER.error("Error getting remote head hash", e);
            halt(400, "Invalid repo url");
            return null;
        }
        if (mostRecentHasMaxScore(netId, request.getPhase())) {
            halt(400, "You have already earned the highest possible score on this phase");
            return null;
        }
        Submission submission = getMostRecentSubmission(netId, request.getPhase());
        if (submission != null && submission.headHash().equals(headHash)) {
            halt(400, "You have already submitted this version of your code for this phase. Make a new commit before submitting again");
            return null;
        }

        LOGGER.info("User " + user.netId() + " submitted phase " + request.phase() + " for grading");

        DaoService.getQueueDao().add(
                new edu.byu.cs.model.QueueItem(
                        netId,
                        Phase.valueOf("Phase" + request.phase()),
                        Instant.now(),
                        false
                )
        );

        TrafficController.sessions.put(netId, new ArrayList<>());

        try {
            Grader grader = getGrader(netId, Phase.valueOf("Phase" + request.phase()), user.repoUrl());

            TrafficController.getInstance().addGrader(grader);

        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid phase", e);
            halt(400, "Invalid phase");
        } catch (Exception e) {
            LOGGER.error("Something went wrong submitting", e);
            halt(500, "Something went wrong");
        }

        res.status(200);
        return "";
    };

    /**
     * checks to see if the specified student achieved the highest possible grade on the specified phase on their most recent submission
     *
     * @param netId netId of the student to check
     * @param phase phase of the project to check
     * @return true if the student's latest submission has the max score.
     * False if the student did not score the max on their latest submission, or if they haven't submitted before at all for this phase
     */
    private static boolean mostRecentHasMaxScore(String netId, Phase phase) {
//        Submission mostRecent = getMostRecentSubmission(netId, phase);
//        if (mostRecent == null) {
//            return false;
//        }
//
//        // If they passed the required tests, and there are no extra credit tests they haven't passed,
//        // then by definition they can't get a higher score
//        return mostRecent.passed() && mostRecent.testResults().getNumExtraCreditFailed() == 0;

        //FIXME: this needs to be reworked to use the Rubric format
        return false;
    }

    /**
     * gets the most recent submission for the specified user in the specified phase
     *
     * @param netId the netID of the student to get a submission for
     * @param phase the phase of the project to get
     * @return the most recent submission, or null if there are no submissions for this student in this phase
     */
    private static Submission getMostRecentSubmission(String netId, Phase phase) {
        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(netId, phase);
        Submission mostRecent = null;

        for (Submission submission : submissions) {
            if (mostRecent == null || mostRecent.timestamp().isBefore(submission.timestamp())) {
                mostRecent = submission;
            }
        }
        return mostRecent;
    }

    public static Route submitGet = (req, res) -> {
        User user = req.session().attribute("user");
        String netId = user.netId();

        boolean inQueue = DaoService.getQueueDao().isAlreadyInQueue(netId);

        res.status(200);

        return new Gson().toJson(Map.of(
                "inQueue", inQueue
        ));
    };

    public static Route submissionXGet = (req, res) -> {
        String phase = req.params(":phase");
        Phase phaseEnum = PhaseUtils.getPhaseByString(phase);

        if (phaseEnum == null) {
            res.status(400);
            return "Invalid phase";
        }

        User user = req.session().attribute("user");

        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(user.netId(), phaseEnum);

        res.status(200);
        res.type("application/json");

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
    };

    public static Route latestSubmissionsGet = (req, res) -> {
        Collection<Submission> submissions = DaoService.getSubmissionDao().getAllLatestSubmissions();

        res.status(200);
        res.type("application/json");

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
    };

    public static Route submissionsActiveGet = (req, res) -> {
        List<String> inQueue = DaoService.getQueueDao().getAll().stream().filter((queueItem) -> !queueItem.started()).map(QueueItem::netId).toList();

        List<String> currentlyGrading = DaoService.getQueueDao().getAll().stream().filter(QueueItem::started).map(QueueItem::netId).toList();


        res.status(200);
        res.type("application/json");

        return new Gson().toJson(Map.of(
                "currentlyGrading", currentlyGrading,
                "inQueue", inQueue
        ));
    };

    /**
     * Creates a grader for the given request with an observer that sends messages to the subscribed sessions
     *
     * @param netId the netId of the user
     * @param phase the phase to grade
     * @return the grader
     * @throws IOException if there is an error creating the grader
     */
    private static Grader getGrader(String netId, Phase phase, String repoUrl) throws IOException {
        Grader.Observer observer = new Grader.Observer() {
            @Override
            public void notifyStarted() {
                DaoService.getQueueDao().markStarted(netId);

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
                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "error",
                        "message", message
                ));

                TrafficController.sessions.remove(netId);
                DaoService.getQueueDao().remove(netId);
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
                DaoService.getQueueDao().remove(netId);
            }
        };

        return switch (phase) {
            case Phase0 -> new PhaseZeroGrader(netId, repoUrl, observer);
            case Phase1 -> new PhaseOneGrader(netId, repoUrl, observer);
            case Phase3 -> new PhaseThreeGrader(netId, repoUrl, observer);
            case Phase4 -> null;
            case Phase6 -> null;
        };
    }

    public static String getRemoteHeadHash(String repoUrl) {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "ls-remote", repoUrl, "HEAD");
        try {
            Process process = processBuilder.start();
            if (process.waitFor() != 0) {
                throw new RuntimeException("exited with non-zero exit code");
            }
            String output = new String(process.getInputStream().readAllBytes());
            return output.split("\\s+")[0];
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Route submissionsReRunPost = (req, res) -> {
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
    public static void reRunSubmissionsInQueue() throws IOException {
        QueueDao queueDao = DaoService.getQueueDao();
        UserDao userDao = DaoService.getUserDao();
        Collection<QueueItem> inQueue = queueDao.getAll();

        for (QueueItem queueItem : inQueue) {
            User currentUser = userDao.getUser(queueItem.netId());
            queueDao.markNotStarted(queueItem.netId());

            TrafficController.getInstance().addGrader(
                    getGrader(queueItem.netId(),
                            queueItem.phase(),
                            currentUser.repoUrl() ));
        }
    }
}
