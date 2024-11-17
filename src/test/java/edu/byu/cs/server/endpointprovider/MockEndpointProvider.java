package edu.byu.cs.server.endpointprovider;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * A mock implementation of EndpointProvider designed for use with
 * Mockito.mock(). Because individual endpoint methods only run once at
 * Server startup, they aren't useful for verifying endpoint calls.
 * MockEndpointProvider provides a runHandler() method that can be used for
 * that purpose.
 */
public class MockEndpointProvider implements EndpointProvider {

    /**
     * An empty function that is run by every endpoint, designed for use in
     * Mockito.verify() to verify endpoint calls. The individual endpoint
     * methods only run once on Server startup, so they can't be used
     * for that purpose.
     *
     * @param name the name of the Route that was called
     * @param req  the Request object passed into the Route
     * @param res  the Response object passed into the Route
     * @return null
     */
    public Object runHandler(String name, Request req, Response res) {
        return "{}";
    }

    @Override
    public Filter beforeAll() {
        return (req, res) -> runHandler("beforeAll", req, res);
    }

    @Override
    public Filter afterAll() {
        return (req, res) -> runHandler("afterAll", req, res);
    }

    @Override
    public Route defaultGet() {
        return (req, res) -> runHandler("defaultGet", req, res);
    }

    @Override
    public Route usersGet() {
        return (req, res) -> runHandler("usersGet", req, res);
    }

    @Override
    public Route testModeGet() {
        return (req, res) -> runHandler("testModeGet", req, res);
    }

    @Override
    public Route commitAnalyticsGet() {
        return (req, res) -> runHandler("commitAnalyticsGet", req, res);
    }

    @Override
    public Route honorCheckerZipGet() {
        return (req, res) -> runHandler("honorCheckerZipGet", req, res);
    }

    @Override
    public Route sectionsGet() {
        return (req, res) -> runHandler("sectionsGet", req, res);
    }

    @Override
    public Filter verifyAuthenticatedMiddleware() {
        return (req, res) -> runHandler("verifyAuthenticatedMiddleware", req, res);
    }

    @Override
    public Filter verifyAdminMiddleware() {
        return (req, res) -> runHandler("verifyAdminMiddleware", req, res);
    }

    @Override
    public Route meGet() {
        return (req, res) -> runHandler("meGet", req, res);
    }

    @Override
    public Route callbackGet() {
        return (req, res) -> runHandler("callbackGet", req, res);
    }

    @Override
    public Route loginGet() {
        return (req, res) -> runHandler("loginGet", req, res);
    }

    @Override
    public Route logoutPost() {
        return (req, res) -> runHandler("logoutPost", req, res);
    }

    @Override
    public Route getConfigAdmin() {
        return (req, res) -> runHandler("getConfigAdmin", req, res);
    }

    @Override
    public Route getConfigStudent() {
        return (req, res) -> runHandler("getConfigStudent", req, res);
    }

    @Override
    public Route updateLivePhases() {
        return (req, res) -> runHandler("updateLivePhases", req, res);
    }

    @Override
    public Route updateBannerMessage() {
        return (req, res) -> runHandler("updateBannerMessage", req, res);
    }

    @Override
    public Route updateCourseIdsPost() {
        return (req, res) -> runHandler("updateCourseIdsPost", req, res);
    }

    @Override
    public Route updateCourseIdsUsingCanvasGet() {
        return (req, res) -> runHandler("updateCourseIdsUsingCanvasGet", req, res);
    }

    @Override
    public Route submitPost() {
        return (req, res) -> runHandler("submitPost", req, res);
    }

    @Override
    public Route adminRepoSubmitPost() {
        return (req, res) -> runHandler("adminRepoSubmitPost", req, res);
    }

    @Override
    public Route submitGet() {
        return (req, res) -> runHandler("submitGet", req, res);
    }

    @Override
    public Route latestSubmissionForMeGet() {
        return (req, res) -> runHandler("latestSubmissionForMeGet", req, res);
    }

    @Override
    public Route submissionXGet() {
        return (req, res) -> runHandler("submissionXGet", req, res);
    }

    @Override
    public Route latestSubmissionsGet() {
        return (req, res) -> runHandler("latestSubmissionsGet", req, res);
    }

    @Override
    public Route submissionsActiveGet() {
        return (req, res) -> runHandler("submissionsActiveGet", req, res);
    }

    @Override
    public Route studentSubmissionsGet() {
        return (req, res) -> runHandler("studentSubmissionsGet", req, res);
    }

    @Override
    public Route approveSubmissionPost() {
        return (req, res) -> runHandler("approveSubmissionPost", req, res);
    }

    @Override
    public Route submissionsReRunPost() {
        return (req, res) -> runHandler("submissionsReRunPost", req, res);
    }

    @Override
    public Route repoPatch() {
        return (req, res) -> runHandler("repoPatch", req, res);
    }

    @Override
    public Route repoPatchAdmin() {
        return (req, res) -> runHandler("repoPatchAdmin", req, res);
    }

    @Override
    public Route repoHistoryAdminGet() {
        return (req, res) -> runHandler("repoHistoryAdminGet", req, res);
    }
}
