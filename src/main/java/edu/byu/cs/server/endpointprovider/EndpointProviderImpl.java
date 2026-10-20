package edu.byu.cs.server.endpointprovider;

import edu.byu.cs.controller.*;
import edu.byu.cs.properties.ApplicationProperties;

import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

/**
 * The implementation of the {@link EndpointProvider} interface, and provides the
 * handlers/controllers for the endpoints
 */
public class EndpointProviderImpl implements EndpointProvider {
    private final UserController userController;
    private final AdminController adminController;
    private final SubmissionController submissionController;
    private final ConfigController configController;
    private final RedirectController redirectController;
    private final AuthController authController;

    public EndpointProviderImpl(UserController userController, AdminController adminController,
                                SubmissionController submissionController, ConfigController configController,
                                RedirectController redirectController, AuthController authController) {
        this.userController = userController;
        this.adminController = adminController;
        this.submissionController = submissionController;
        this.configController = configController;
        this.redirectController = redirectController;
        this.authController = authController;
    }

    // Wildcard endpoints

    @Override
    public Handler beforeAll() {
        return ctx -> {
            ctx.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
            ctx.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,PATCH,OPTIONS");
            ctx.header("Access-Control-Allow-Credentials", "true");
            ctx.header("Access-Control-Allow-Origin", ApplicationProperties.frontendUrl());
        };
    }

    @Override
    public Handler afterAll() {
        return ctx -> {};
    }

    @Override
    public Handler notFound() {
        return ctx -> {
            if (!ctx.path().equals("/ws")) {
                String urlParams = ctx.queryString();
                urlParams = urlParams == null ? "" : "?" + urlParams;
                ctx.redirect("/" + urlParams, HttpStatus.FOUND);
            }
        };
    }

    @Override
    public Handler defaultOptions() {
        return ctx -> {};
    }

    // AdminController

    @Override
    public Handler usersGet() {
        return adminController.usersGet();
    }

    @Override
    public Handler testModeGet() {
        return adminController.testModeGet();
    }

    @Override
    public Handler commitAnalyticsGet() {
        return adminController.commitAnalyticsGet();
    }

    @Override
    public Handler honorCheckerZipGet() {
        return adminController.honorCheckerZipGet();
    }

    @Override
    public Handler sectionsGet() {
        return adminController.sectionsGet();
    }

    // AuthController

    @Override
    public Handler verifyAuthenticatedMiddleware() {
        return authController.verifyAuthenticatedMiddleware();
    }

    @Override
    public Handler verifyAdminMiddleware() {
        return authController.verifyAdminMiddleware();
    }

    @Override
    public Handler meGet() {
        return authController.meGet();
    }

    // CasController

    @Override
    public Handler callbackGet() {
        return redirectController.callbackGet();
    }

    @Override
    public Handler loginGet() {
        return redirectController.loginGet();
    }

    @Override
    public Handler logoutPost() {
        return redirectController.logoutPost();
    }

    // ConfigController

    @Override
    public Handler getConfigAdmin() {
        return configController.getConfigAdmin();
    }

    @Override
    public Handler getConfigStudent() {
        return configController.getConfigStudent();
    }

    @Override
    public Handler updateLivePhases() {
        return configController.updateLivePhases();
    }

    @Override
    public Handler scheduleShutdown() {
        return configController.scheduleShutdown();
    }

    @Override
    public Handler updateBannerMessage() {
        return configController.updateBannerMessage();
    }

    @Override
    public Handler updateCourseIdPost() {
        return configController.updateCourseIdPost();
    }

    @Override
    public Handler reloadCourseAssignmentIds() {
        return configController.updateCourseIdsUsingCanvasGet();
    }

    @Override
    public Handler updatePenalties() {
        return configController.updatePenalties();
    }

    @Override
    public Handler updateHolidays() {
        return configController.updateHolidays();
    }

    @Override
    public Handler updateSlackLink() {
        return configController.updateSlackLink();
    }

    // SubmissionController

    @Override
    public Handler submitPost() {
        return submissionController.submitPost();
    }

    @Override
    public Handler adminRepoSubmitPost() {
        return submissionController.adminRepoSubmitPost();
    }

    @Override
    public Handler submitGet() {
        return submissionController.submitGet();
    }

    @Override
    public Handler latestSubmissionForMeGet() {
        return submissionController.latestSubmissionForMeGet();
    }

    @Override
    public Handler submissionXGet() {
        return submissionController.submissionXGet();
    }

    @Override
    public Handler latestSubmissionsGet() {
        return submissionController.latestSubmissionsGet();
    }

    @Override
    public Handler submissionsActiveGet() {
        return submissionController.submissionsActiveGet();
    }

    @Override
    public Handler studentSubmissionsGet() {
        return submissionController.studentSubmissionsGet();
    }

    @Override
    public Handler approveSubmissionPost() {
        return submissionController.approveSubmissionPost();
    }

    @Override
    public Handler submissionsReRunPost() {
        return submissionController.submissionsReRunPost();
    }

    // UserController

    @Override
    public Handler setRepoUrl() {
        return userController.setRepoUrl();
    }

    @Override
    public Handler setRepoUrlAdmin() {
        return userController.setRepoUrlAdmin();
    }

    @Override
    public Handler repoHistoryAdminGet() {
        return userController.repoHistoryAdminGet();
    }
}
