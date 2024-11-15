package edu.byu.cs.server.endpointprovider;

import edu.byu.cs.controller.*;
import spark.Filter;
import spark.Route;

public class EndpointProviderImpl implements EndpointProvider {

    // AdminController

    @Override
    public Route usersGet() {
        return AdminController.usersGet;
    }

    @Override
    public Route testModeGet() {
        return AdminController.testModeGet;
    }

    @Override
    public Route commitAnalyticsGet() {
        return AdminController.commitAnalyticsGet;
    }

    @Override
    public Route honorCheckerZipGet() {
        return AdminController.honorCheckerZipGet;
    }

    @Override
    public Route sectionsGet() {
        return AdminController.sectionsGet;
    }

    // AuthController

    @Override
    public Filter verifyAuthenticatedMiddleware() {
        return AuthController.verifyAuthenticatedMiddleware;
    }

    @Override
    public Filter verifyAdminMiddleware() {
        return AuthController.verifyAdminMiddleware;
    }

    @Override
    public Route meGet() {
        return AuthController.meGet;
    }

    // CasController

    @Override
    public Route callbackGet() {
        return CasController.callbackGet;
    }

    @Override
    public Route loginGet() {
        return CasController.loginGet;
    }

    @Override
    public Route logoutPost() {
        return CasController.logoutPost;
    }

    // ConfigController

    @Override
    public Route getConfigAdmin() {
        return ConfigController.getConfigAdmin;
    }

    @Override
    public Route getConfigStudent() {
        return ConfigController.getConfigStudent;
    }

    @Override
    public Route updateLivePhases() {
        return ConfigController.updateLivePhases;
    }

    @Override
    public Route updateBannerMessage() {
        return ConfigController.updateBannerMessage;
    }

    @Override
    public Route updateCourseIdsPost() {
        return ConfigController.updateCourseIdsPost;
    }

    @Override
    public Route updateCourseIdsUsingCanvasGet() {
        return ConfigController.updateCourseIdsUsingCanvasGet;
    }

    // SubmissionController

    @Override
    public Route submitPost() {
        return SubmissionController.submitPost;
    }

    @Override
    public Route adminRepoSubmitPost() {
        return SubmissionController.adminRepoSubmitPost;
    }

    @Override
    public Route submitGet() {
        return SubmissionController.submitGet;
    }

    @Override
    public Route latestSubmissionForMeGet() {
        return SubmissionController.latestSubmissionForMeGet;
    }

    @Override
    public Route submissionXGet() {
        return SubmissionController.submissionXGet;
    }

    @Override
    public Route latestSubmissionsGet() {
        return SubmissionController.latestSubmissionsGet;
    }

    @Override
    public Route submissionsActiveGet() {
        return SubmissionController.submissionsActiveGet;
    }

    @Override
    public Route studentSubmissionsGet() {
        return SubmissionController.studentSubmissionsGet;
    }

    @Override
    public Route approveSubmissionPost() {
        return SubmissionController.approveSubmissionPost;
    }

    @Override
    public Route submissionsReRunPost() {
        return SubmissionController.submissionsReRunPost;
    }

    // UserController

    @Override
    public Route repoPatch() {
        return UserController.repoPatch;
    }

    @Override
    public Route repoPatchAdmin() {
        return UserController.repoPatchAdmin;
    }

    @Override
    public Route repoHistoryAdminGet() {
        return UserController.repoHistoryAdminGet;
    }
}
