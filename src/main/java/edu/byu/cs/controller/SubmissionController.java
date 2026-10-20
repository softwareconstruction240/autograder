package edu.byu.cs.controller;

import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.UnauthorizedException;
import edu.byu.cs.controller.netmodel.ApprovalRequest;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.DataAccessException;
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

/**
 * Handles submission-related HTTP endpoints
 */
public class SubmissionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    public Handler submitPost() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            GradeRequest request = validateAndUnpackRequest(ctx);
            submissionService.submit(user, request);
        };
    }

    public Handler adminRepoSubmitPost() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }

            GradeRequest request = validateAndUnpackRequest(ctx);

            submissionService.adminRepoSubmit(user.netId(), request);
        };
    }

    private GradeRequest validateAndUnpackRequest(Context ctx) throws DataAccessException, BadRequestException, UnauthorizedException {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            throw new UnauthorizedException("No user credentials found");
        }
        String netId = user.netId();

        if (submissionService.isAlreadyInQueue(netId)) {
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

    public Handler submitGet() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }
            boolean inQueue = submissionService.isAlreadyInQueue(user.netId());
            ctx.json(Map.of("inQueue", inQueue));
        };
    }

    public Handler latestSubmissionForMeGet() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }
            Submission submission = submissionService.getLastSubmissionForUser(user.netId());
            ctx.json(submission);
        };
    }

    public Handler submissionXGet() {
        return ctx -> {
            Phase phase;
            if(ctx.pathParamMap().containsKey("phase")) {
                try {
                    phase = Phase.valueOf(ctx.pathParam("phase"));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid phase", e);
                    throw new BadRequestException("Invalid phase", e);
                }
            } else {
                phase = null;
            }

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }
            Collection<Submission> submissions = submissionService.getXSubmissionsForUser(user.netId(), phase);
            ctx.json(submissions);
        };
    }

    public Handler latestSubmissionsGet() {
        return ctx -> {
            int count = -1;
            if (ctx.pathParamMap().containsKey("count")) {
                String countString = ctx.pathParam("count");
                count = Integer.parseInt(countString);
            }

            Collection<Submission> submissions = submissionService.getLatestSubmissions(count);
            ctx.json(submissions);
        };
    }

    public Handler submissionsActiveGet() {
        return ctx -> {
            List<String> inQueue = submissionService.getActiveInQueue();
            List<String> currentlyGrading = submissionService.getCurrentlyGrading();
            ctx.json(Map.of("currentlyGrading", currentlyGrading, "inQueue", inQueue));
        };
    }

    public Handler studentSubmissionsGet() {
        return ctx -> {
            String netId = ctx.pathParam("netId");
            Collection<Submission> submissions = submissionService.getSubmissionsForUser(netId);
            ctx.json(submissions);
        };
    }

    public Handler approveSubmissionPost() {
        return ctx -> {
            User adminUser = ctx.sessionAttribute("user");
            if (adminUser == null) {
                throw new UnauthorizedException("No user credentials found");
            }
            ApprovalRequest request = ctx.bodyAsClass(ApprovalRequest.class);
            submissionService.approveSubmission(adminUser.netId(), request);
        };
    }

    public Handler submissionsReRunPost() {
        return ctx -> {
            submissionService.reRunSubmissionsInQueue();
            ctx.json(Map.of("message", "re-running submissions in queue"));
        };
    }

}
