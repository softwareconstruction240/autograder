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
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Handler submitPost = ctx -> {
        User user = ctx.sessionAttribute("user");

        GradeRequest request = validateAndUnpackRequest(ctx);
        if (request != null) {
            SubmissionService.submit(user, request);
        }
    };

    public static final Handler adminRepoSubmitPost = ctx -> {
        User user = ctx.sessionAttribute("user");

        GradeRequest request = validateAndUnpackRequest(ctx);
        if (request == null) {
            return;
        }

        SubmissionService.adminRepoSubmit(user.netId(), request);
    };

    private static GradeRequest validateAndUnpackRequest(Context ctx) throws DataAccessException, BadRequestException {
        User user = ctx.sessionAttribute("user");
        String netId = user.netId();

        if (DaoService.getQueueDao().isAlreadyInQueue(netId)) {
            throw new BadRequestException("You are already in the queue");
        }

        GradeRequest request;
        try {
            request = ctx.bodyAsClass(GradeRequest.class);
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

    public static final Handler submitGet = ctx -> {
        User user = ctx.sessionAttribute("user");
        String netId = user.netId();

        boolean inQueue = SubmissionService.isAlreadyInQueue(netId);

        ctx.json(Map.of("inQueue", inQueue));
    };

    public static final Handler latestSubmissionForMeGet = ctx -> {
        User user = ctx.sessionAttribute("user");
        Submission submission = SubmissionService.getLastSubmissionForUser(user.netId());
        ctx.json(submission);
    };

    public static final Handler submissionXGet = ctx -> {
        String phaseString = ctx.pathParam(":phase"); // TODO pathParam() or formParam()?
        Phase phase = null;

        if (phaseString != null) {
            try {
                phase = Phase.valueOf(phaseString);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid phase", e);
                throw new BadRequestException("Invalid phase", e);
            }
        }

        User user = ctx.sessionAttribute("user");
        Collection<Submission> submissions = SubmissionService.getXSubmissionsForUser(user.netId(), phase);

        ctx.json(submissions);
    };

    public static final Handler latestSubmissionsGet = ctx -> {
        String countString = ctx.pathParam(":count"); // TODO pathParam() or formParam()?
        // TODO Move Integer parsing to service...?
        int count = countString == null ? -1 : Integer.parseInt(countString); // if they don't give a count, set it to -1, which gets all latest submissions

        Collection<Submission> submissions = SubmissionService.getLatestSubmissions(count);

        ctx.json(submissions);
    };

    public static final Handler submissionsActiveGet = ctx -> {
        List<String> inQueue = SubmissionService.getActiveInQueue();
        List<String> currentlyGrading = SubmissionService.getCurrentlyGrading();
        ctx.json(Map.of("currentlyGrading", currentlyGrading, "inQueue", inQueue));
    };

    public static final Handler studentSubmissionsGet = ctx -> {
        String netId = ctx.pathParam(":netId"); // TODO pathParam() or formParam()?
        Collection<Submission> submissions = SubmissionService.getSubmissionsForUser(netId);
        ctx.json(submissions);
    };

    public static final Handler approveSubmissionPost = ctx -> {
        User adminUser = ctx.sessionAttribute("user");
        ApprovalRequest request = Serializer.deserialize(ctx.body(), ApprovalRequest.class);
        SubmissionService.approveSubmission(adminUser.netId(), request);
    };

    public static final Handler submissionsReRunPost = ctx -> {
        SubmissionService.reRunSubmissionsInQueue();
        ctx.json(Map.of("message", "re-running submissions in queue"));
    };

}
