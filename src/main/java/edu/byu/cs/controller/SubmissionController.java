package edu.byu.cs.controller;

import edu.byu.cs.controller.httpexception.BadRequestException;
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

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Route submitPost = (req, res) -> {
        User user = req.session().attribute("user");

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) {
            return null;
        }

        SubmissionService.submit(user, request);

        res.status(200);
        return "";
    };

    public static final Route adminRepoSubmitPost = (req, res) -> {
        User user = req.session().attribute("user");

        GradeRequest request = validateAndUnpackRequest(req);
        if (request == null) {
            return null;
        }

        SubmissionService.adminRepoSubmit(user.netId(), request);

        res.status(200);
        return "";
    };

    private static GradeRequest validateAndUnpackRequest(Request req) throws DataAccessException, BadRequestException {
        User user = req.session().attribute("user");
        String netId = user.netId();

        if (DaoService.getQueueDao().isAlreadyInQueue(netId)) {
            throw new BadRequestException("You are already in the queue");
        }

        GradeRequest request;
        try {
            request = Serializer.deserialize(req.body(), GradeRequest.class);
        } catch (Serializer.SerializationException e) {
            throw new BadRequestException("Request must be valid json", e);
        }

        if (request == null || request.phase() == null) {
            throw new BadRequestException("Request is invalid");
        }

        if (user.repoUrl() == null && user.role() == User.Role.STUDENT) {
            throw new BadRequestException("Student has not provided repo url");
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

        Submission submission = SubmissionService.getLastSubmissionForUser(user.netId());

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
                throw new BadRequestException("Invalid phase", e);
            }
        }

        User user = req.session().attribute("user");
        Collection<Submission> submissions = SubmissionService.getXSubmissionsForUser(user.netId(), phase);

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route latestSubmissionsGet = (req, res) -> {
        String countString = req.params(":count");
        // TODO Move Integer parsing to service...?
        int count = countString == null ? -1 : Integer.parseInt(countString); // if they don't give a count, set it to -1, which gets all latest submissions

        Collection<Submission> submissions = SubmissionService.getLatestSubmissions(count);

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(submissions);
    };

    public static final Route submissionsActiveGet = (req, res) -> {
        List<String> inQueue = SubmissionService.getActiveInQueue();
        List<String> currentlyGrading = SubmissionService.getCurrentlyGrading();

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(Map.of("currentlyGrading", currentlyGrading, "inQueue", inQueue));
    };

    public static final Route studentSubmissionsGet = (req, res) -> {
        String netId = req.params(":netId");

        Collection<Submission> submissions = SubmissionService.getSubmissionsForUser(netId);

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
