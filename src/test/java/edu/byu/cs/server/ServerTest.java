package edu.byu.cs.server;

import edu.byu.cs.server.endpointprovider.MockEndpointProvider;
import edu.byu.cs.server.exception.ResponseParseException;
import edu.byu.cs.server.exception.ServerConnectionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

class ServerTest {
    private static TestServerFacade serverFacade;
    private static Server server;

    private static final MockEndpointProvider mockedMockProvider = spy(new MockEndpointProvider());

    public static Stream<Arguments> getPathParamEndpoints() {
        return Stream.of(
                Arguments.of("GET", "commitAnalyticsGet", "/api/admin/analytics/commit", ":option"),
                Arguments.of("GET", "honorCheckerZipGet", "/api/admin/honorChecker/zip", ":section"),
                Arguments.of("GET", "submissionXGet", "/api/submission", ":phase"),
                Arguments.of("GET", "latestSubmissionsGet", "/api/admin/submissions/latest", ":count"),
                Arguments.of("GET", "studentSubmissionsGet", "/api/admin/submissions/student", ":netid")
        );
    }

    public static Stream<Arguments> getEndpoints() {
        return Stream.of(
                Arguments.of("GET", "callbackGet", "/auth/callback"),
                Arguments.of("GET", "loginGet", "/auth/login"),
                Arguments.of("GET", "usersGet", "/api/admin/users"),
                Arguments.of("GET", "testModeGet", "/api/admin/test_mode"),
                Arguments.of("GET", "commitAnalyticsGet", "/api/admin/analytics/commit"),
                Arguments.of("GET", "sectionsGet", "/api/admin/sections"),
                Arguments.of("GET", "meGet", "/api/me"),
                Arguments.of("GET", "getConfigAdmin", "/api/admin/config"),
                Arguments.of("GET", "getConfigStudent", "/api/config"),
                Arguments.of("GET", "updateCourseIdsUsingCanvasGet", "/api/admin/config/courseIds"),
                Arguments.of("GET", "submitGet", "/api/submit"),
                Arguments.of("GET", "latestSubmissionForMeGet", "/api/latest"),
                Arguments.of("GET", "submissionXGet", "/api/submission"),
                Arguments.of("GET", "latestSubmissionsGet", "/api/admin/submissions/latest"),
                Arguments.of("GET", "submissionsActiveGet", "/api/admin/submissions/active"),
                Arguments.of("GET", "repoHistoryAdminGet", "/api/admin/repo/history"),
                Arguments.of("POST", "logoutPost", "/auth/logout"),
                Arguments.of("POST", "updateLivePhases", "/api/admin/config/phases"),
                Arguments.of("POST", "scheduleShutdown", "/api/admin/config/phases/shutdown"),
                Arguments.of("POST", "updateBannerMessage", "/api/admin/config/banner"),
                Arguments.of("POST", "updateCourseIdsPost", "/api/admin/config/courseIds"),
                Arguments.of("POST", "submitPost", "/api/submit"),
                Arguments.of("POST", "adminRepoSubmitPost", "/api/admin/submit"),
                Arguments.of("POST", "approveSubmissionPost", "/api/admin/submissions/approve"),
                Arguments.of("POST", "submissionsReRunPost", "/api/admin/submissions/rerun")
        );
    }

    // TODO figure out how to test PATCH calls... HttpURLConnection thinks it's an invalid method

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeAll
    public static void init() {
        server = new Server(mockedMockProvider);
        int port = server.start(8080);
        System.out.println("Started test HTTP server on " + port);

        serverFacade = new TestServerFacade("localhost", port);
    }

    @AfterEach
    public void tearDown() {
        reset(mockedMockProvider);
    }

    @ParameterizedTest
    @MethodSource("getEndpoints")
    public void verifyEndpointCallsItsHandlersExactlyOnceInOrder(String method, String endpointName, String path)
            throws ServerConnectionException, ResponseParseException, IOException {
        // When
        serverFacade.makeRequest(method, path);

        InOrder inOrder = inOrder(mockedMockProvider);

        // Then
        inOrder.verify(mockedMockProvider, times(1)).runHandler("beforeAll");
        this.verifyInOrder_authenticationMiddleware(path, inOrder);
        inOrder.verify(mockedMockProvider, times(1)).runHandler(endpointName);
        inOrder.verify(mockedMockProvider, times(1)).runHandler("afterAll");
    }

    @ParameterizedTest
    @MethodSource("getPathParamEndpoints")
    public void verifyPathParameterHasAValueWhenGivenOne(String method, String endpointName, String path, String pathParamName)
            throws ServerConnectionException, ResponseParseException, IOException {
        // Given
        String fullPath = path + "/testParamValue";

        // When
        serverFacade.makeRequest(method, fullPath);

        // Then
        verify(mockedMockProvider, times(1)).hasPathParam(endpointName, pathParamName, "testParamValue");
    }

    private void verifyInOrder_authenticationMiddleware(String path, InOrder inOrder) {
        List<String> pathNodes = Arrays.stream(path.split("/")).toList();

        if (!pathNodes.contains("api")) {
            return;
        }

        if (!pathNodes.contains("auth")) {
            // Requires authentication
            inOrder.verify(mockedMockProvider, times(1)).runHandler("verifyAuthenticatedMiddleware");
        }

        if (pathNodes.contains("admin")) {
            // Requires admin
            inOrder.verify(mockedMockProvider, times(1)).runHandler("verifyAdminMiddleware");
        }
    }

    @Test
    void nonexistent_GET_endpoint_calls_beforeAll_then_defaultGet_then_afterAll_exactly_once_in_order()
            throws IOException, ServerConnectionException, ResponseParseException {
        serverFacade.makeRequest("GET", "/iDoNotExist");

        // Verify they ran in order
        InOrder inOrder = inOrder(mockedMockProvider);
        inOrder.verify(mockedMockProvider).runHandler("beforeAll");
        inOrder.verify(mockedMockProvider).runHandler("defaultGet");
        inOrder.verify(mockedMockProvider).runHandler("afterAll");

        // Verify they only ran once
        verify(mockedMockProvider, times(1)).runHandler("beforeAll");
        verify(mockedMockProvider, times(1)).runHandler("defaultGet");
        verify(mockedMockProvider, times(1)).runHandler("afterAll");
    }
}