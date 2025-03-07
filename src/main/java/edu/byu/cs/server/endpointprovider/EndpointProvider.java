package edu.byu.cs.server.endpointprovider;

import io.javalin.http.Handler;

public interface EndpointProvider {

    // Wildcard endpoints
    Handler beforeAll();
    Handler afterAll();
    Handler notFound();
    Handler defaultOptions();

    // AdminController
    Handler usersGet();
    Handler testModeGet();
    Handler commitAnalyticsGet();
    Handler honorCheckerZipGet();
    Handler sectionsGet();

    // AuthController
    Handler verifyAuthenticatedMiddleware();
    Handler verifyAdminMiddleware();
    Handler meGet();

    // CasController
    Handler callbackGet();
    Handler loginGet();
    Handler logoutPost();

    // ConfigController
    Handler getConfigAdmin();
    Handler getConfigStudent();
    Handler updateLivePhases();
    Handler scheduleShutdown();
    Handler updateBannerMessage();
    Handler updateCourseIdPost();
    Handler reloadCourseAssignmentIds();
    Handler updatePenalties();
    Handler updateHolidays();

    // SubmissionController
    Handler submitPost();
    Handler adminRepoSubmitPost();
    Handler submitGet();
    Handler latestSubmissionForMeGet();
    Handler submissionXGet();
    Handler latestSubmissionsGet();
    Handler submissionsActiveGet();
    Handler studentSubmissionsGet();
    Handler approveSubmissionPost();
    Handler submissionsReRunPost();

    // UserController
    Handler setRepoUrl();
    Handler setRepoUrlAdmin();
    Handler repoHistoryAdminGet();
}
