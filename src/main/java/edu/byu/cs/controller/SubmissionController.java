package edu.byu.cs.controller;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.GradingObserver;
import edu.byu.cs.autograder.TrafficController;
import edu.byu.cs.controller.netmodel.ApprovalRequest;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.util.Serializer;
import edu.byu.cs.util.SubmissionUtils;
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

    private static boolean verifyHasNewCommits(User user, Phase phase) throws DataAccessException {
        String headHash;
        try {
            headHash = SubmissionUtils.getRemoteHeadHash(user.repoUrl());
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

        SubmissionUtils.approveSubmission(request.netId(), request.phase(), adminUser.netId(), penalty);
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

            private void notifyError(String message, Map<String, Object> contents) {
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
            if(currentUser.repoUrl() != null) {
                queueDao.markNotStarted(queueItem.netId());

                TrafficController.getInstance().addGrader(
                        getGrader(queueItem.netId(),
                                queueItem.phase(),
                                currentUser.repoUrl(),
                                currentUser.role() == User.Role.ADMIN));
            }
            else {
                queueDao.remove(queueItem.netId());
            }
        }
    }

}
