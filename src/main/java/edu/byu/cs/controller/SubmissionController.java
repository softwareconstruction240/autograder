package edu.byu.cs.controller;

import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.netmodel.ApprovalRequest;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.service.SubmissionService;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Route;

import java.util.*;

import static spark.Spark.halt;

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Route submitPost = (req, res) -> {
        User user = req.session().attribute("user");

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) {
            return null;
        }

        try {
            SubmissionService.submit(user, request);
        } catch (BadRequestException e) {
            halt(400, e.getMessage());
            return null;
        } catch (DataAccessException e) {
            halt(500, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

    public static final Route adminRepoSubmitPost = (req, res) -> {
        User user = req.session().attribute("user");

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) {
            return null;
        }

        try {
            SubmissionService.adminRepoSubmit(user.netId(), request);
        } catch (BadRequestException e) {
            halt(400, e.getMessage());
            return null;
        } catch (DataAccessException e) {
            halt(500, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

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

    public static final Route submitGet = (req, res) -> {
        User user = req.session().attribute("user");
        String netId = user.netId();

        boolean inQueue = SubmissionService.isAlreadyInQueue(netId);

        res.status(200);

        return Serializer.serialize(Map.of("inQueue", inQueue));
    };

    public static final Route latestSubmissionForMeGet = (req, res) -> {
        User user = req.session().attribute("user");

        Submission submission;
        try {
            submission = SubmissionService.getLastSubmissionForUser(user.netId());
        } catch (DataAccessException e) {
            halt(500);
            return null;
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submission);
    };

    public static final Route submissionXGet = (req, res) -> {
        String phaseString = req.params(":phase");
        Phase phase = null;

        if (phaseString != null) {
            try {
                phase = Phase.valueOf(phaseString);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid phase", e);
                halt(400, "Invalid phase");
            }
        }

        User user = req.session().attribute("user");
        Collection<Submission> submissions;
        try {
            submissions = SubmissionService.getXSubmissionsForUser(user.netId(), phase);
        } catch (DataAccessException e) {
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
            submissions = SubmissionService.getLatestSubmissions(count);
        } catch (DataAccessException e) {
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
            inQueue = SubmissionService.getActiveInQueue();
            currentlyGrading = SubmissionService.getCurrentlyGrading();
        } catch (DataAccessException e) {
            halt(500);
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(Map.of("currentlyGrading", currentlyGrading, "inQueue", inQueue));
    };

    public static final Route studentSubmissionsGet = (req, res) -> {
        String netId = req.params(":netId");

        Collection<Submission> submissions = null;
        try {
            submissions = SubmissionService.getSubmissionsForUser(netId);
        } catch (DataAccessException e) {
            halt(500);
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route approveSubmissionPost = (req, res) -> {
        User adminUser = req.session().attribute("user");
        ApprovalRequest request = Serializer.deserialize(req.body(), ApprovalRequest.class);
        SubmissionService.approveSubmission(adminUser.netId(), request);
        return "{}";
    };

    public static final Route submissionsReRunPost = (req, res) -> {
        SubmissionService.reRunSubmissionsInQueue();

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(Map.of("message", "re-running submissions in queue"));
    };

}
