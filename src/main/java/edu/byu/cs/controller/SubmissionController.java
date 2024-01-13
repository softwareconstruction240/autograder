package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import edu.byu.cs.autograder.*;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static spark.Spark.halt;

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static Route submitPost = (req, res) -> {

        User user = req.session().attribute("user");
        String netId = user.netId();

        if (TrafficController.queue.stream().anyMatch(netId::equals)) {
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

        String headHash = getRemoteHeadHash(request.repoUrl());
        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = submissionDao.getSubmissionsForPhase(user.netId(), Phase.Phase0).stream()
                .filter(s -> s.headHash().equals(headHash))
                .findFirst()
                .orElse(null);
        if (submission != null) {
            halt(400, "You have already submitted this version of your code for this phase. Make a new commit before submitting again");
            return null;
        }

        LOGGER.info("User " + user.netId() + " submitted phase " + request.phase() + " for grading");

        TrafficController.queue.add(netId);
        TrafficController.sessions.put(netId, new ArrayList<>());

        try {
            Grader grader = getGrader(netId, request);

            TrafficController.getInstance().addGrader(grader);

        } catch (Exception e) {
            LOGGER.error("Something went wrong submitting", e);
            halt(500, "Something went wrong");
        }

        res.status(200);
        return "";
    };

    public static Route submissionXGet = (req, res) -> {
        String phase = req.params(":phase");
        Phase phaseEnum = switch (phase) {
            case "0" -> Phase.Phase0;
            case "1" -> Phase.Phase1;
            case "3" -> Phase.Phase3;
            case "4" -> Phase.Phase4;
            case "6" -> Phase.Phase6;
            default -> null;
        };

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

    /**
     * Creates a grader for the given request with an observer that sends messages to the subscribed sessions
     *
     * @param netId   the netId of the user
     * @param request the request to create a grader for
     * @return the grader
     * @throws IOException if there is an error creating the grader
     */
    private static Grader getGrader(String netId, GradeRequest request) throws IOException {
        Grader.Observer observer = new Grader.Observer() {
            @Override
            public void notifyStarted() {
                TrafficController.queue.removeIf(queueNetId -> queueNetId.equals(netId));

                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "started"
                ));

                broadcastQueueStatus();
            }

            @Override
            public void update(String message) {
                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "update",
                        "message", message
                ));
            }

            @Override
            public void notifyError(String message) {
                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "error",
                        "message", message
                ));

                TrafficController.sessions.remove(netId);
            }

            @Override
            public void notifyDone(TestAnalyzer.TestNode results) {
                TrafficController.getInstance().notifySubscribers(netId, Map.of(
                        "type", "results",
                        "results", new Gson().toJson(results)
                ));

                TrafficController.sessions.remove(netId);
            }
        };

        return switch (request.phase()) {
            case 0 -> new PhaseZeroGrader(netId, request.repoUrl(), observer);
            case 1 -> new PhaseOneGrader(netId, request.repoUrl(), observer);
            case 3 -> null;
            case 4 -> null;
            case 6 -> null;
            default -> throw new IllegalStateException("Unexpected value: " + request.phase());
        };
    }

    /**
     * Broadcasts the current queue status to all connected clients.
     * Each client will be notified of their specific position in the queue.
     */
    private static void broadcastQueueStatus() {
        int i = 1;
        for (String netId : TrafficController.queue) {
            TrafficController.getInstance().notifySubscribers(netId, Map.of(
                    "type", "queueStatus",
                    "position", i,
                    "total", TrafficController.queue.size()
            ));
            i++;
        }
    }

    private static String getRemoteHeadHash(String repoUrl) {
        //git ls-remote <repoUrl>> HEAD
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
}
