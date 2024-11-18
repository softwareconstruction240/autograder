package edu.byu.cs.server;

import edu.byu.cs.server.endpointprovider.MockEndpointProvider;
import edu.byu.cs.server.exception.ResponseParseException;
import edu.byu.cs.server.exception.ServerConnectionException;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;

class ServerTest {
    private static TestServerFacade serverFacade;
    private static Server server;

    private static final MockEndpointProvider mockedMockProvider = spy(new MockEndpointProvider());

    // TODO figure out how to test PATCH calls... HttpURLConnection thinks it's an invalid method
    // TODO verify endpoints that take pathParams

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

    @Test
    void method_GET_endpoints_call_their_handlers_exactly_once() {
        String[][] endpoints = {
                {"callbackGet", "/auth/callback"},
                {"loginGet", "/auth/login"},
                {"usersGet", "/api/admin/users"},
                {"testModeGet", "/api/admin/test_mode"},
                {"commitAnalyticsGet", "/api/admin/analytics/commit"}, // TODO: test with {option}
//                {"honorCheckerZipGet", "/api/admin/honorChecker/zip/{section}"}, // TODO: requires {section}
                {"sectionsGet", "/api/admin/sections"},
                {"meGet", "/api/me"},
                {"getConfigAdmin", "/api/admin/config"},
                {"getConfigStudent", "/api/config"},
                {"updateCourseIdsUsingCanvasGet", "/api/admin/config/courseIds"},
                {"submitGet", "/api/submit"},
                {"latestSubmissionForMeGet", "/api/latest"},
                {"submissionXGet", "/api/submission"}, // TODO test with {phase}
                {"latestSubmissionsGet", "/api/admin/submissions/latest"}, // TODO: test with {count}
                {"submissionsActiveGet", "/api/admin/submissions/active"},
//                {"studentSubmissionsGet", "/admin/submissions/student/{netId}"}, // TODO requires {netId}
                {"repoHistoryAdminGet", "/api/admin/repo/history"}
        };

        verifyEndpointsCallTheirHandlersExactlyOnceInOrder("GET", endpoints);
    }

    @Test
    void method_POST_endpoints_call_their_handlers_exactly_once() {
        String[][] endpoints = {
                {"logoutPost", "/auth/logout"},
                {"updateLivePhases", "/api/admin/config/phases"},
                {"updateBannerMessage", "/api/admin/config/banner"},
                {"updateCourseIdsPost", "/api/admin/config/courseIds"},
                {"submitPost", "/api/submit"},
                {"adminRepoSubmitPost", "/api/admin/submit"},
                {"approveSubmissionPost", "/api/admin/submissions/approve"},
                {"submissionsReRunPost", "/api/admin/submissions/rerun"}
        };

        verifyEndpointsCallTheirHandlersExactlyOnceInOrder("POST", endpoints);
    }

    private void verifyEndpointsCallTheirHandlersExactlyOnceInOrder(String method, String[][] endpoints) {
        Set<String> failingEndpoints = new HashSet<>();
        for (String[] endpoint : endpoints) {
            String endpointName = endpoint[0];
            String path = endpoint[1];
            try {
                verifyEndpointCallsItsHandlersExactlyOnceInOrder(endpointName, method, path);
            } catch (Exception e) {
                failingEndpoints.add(endpointName);
            }

            reset(mockedMockProvider);
        }

        Assertions.assertEquals(new HashSet<>(), failingEndpoints,
                "Not all endpoints had exactly 1 function call.");
    }

    private void verifyEndpointCallsItsHandlersExactlyOnceInOrder(String endpointName, String method, String path)
            throws ServerConnectionException, ResponseParseException, IOException {
        // When
        serverFacade.makeRequest(method, path);

        // Then
        InOrder inOrder = inOrder(mockedMockProvider);
        inOrder.verify(mockedMockProvider, times(1)).runHandler(eq("beforeAll"), any(), any());
        this.verifyInOrder_authenticationMiddleware(path, inOrder);
        inOrder.verify(mockedMockProvider, times(1)).runHandler(eq(endpointName), any(), any());
        inOrder.verify(mockedMockProvider, times(1)).runHandler(eq("afterAll"), any(), any());
    }

    private void verifyInOrder_authenticationMiddleware(String path, InOrder inOrder) {
        List<String> pathNodes = Arrays.stream(path.split("/")).toList();

        if (!pathNodes.contains("api")) {
            return;
        }

        if (!pathNodes.contains("auth")) {
            // Requires authentication
            inOrder.verify(mockedMockProvider, times(1)).runHandler(eq("verifyAuthenticatedMiddleware"), any(), any());
        }

        if (pathNodes.contains("admin")) {
            // Requires admin
            inOrder.verify(mockedMockProvider, times(1)).runHandler(eq("verifyAdminMiddleware"), any(), any());
        }
    }

    @Test
    void nonexistent_GET_endpoint_calls_defaultGet_exactly_once()
            throws IOException, ServerConnectionException, ResponseParseException {
        serverFacade.makeRequest("GET", "/iDoNotExist");
        verify(mockedMockProvider, times(1)).runHandler(eq("defaultGet"), any(), any());
    }

    @Test
    void nonexistent_GET_endpoint_calls_beforeAll_then_defaultGet_then_afterAll_exactly_once_in_order()
            throws IOException, ServerConnectionException, ResponseParseException {
        serverFacade.makeRequest("GET", "/iDoNotExist");

        // Verify they ran in order
        InOrder inOrder = inOrder(mockedMockProvider);
        inOrder.verify(mockedMockProvider).runHandler(eq("beforeAll"), any(), any());
        inOrder.verify(mockedMockProvider).runHandler(eq("defaultGet"), any(), any());
        inOrder.verify(mockedMockProvider).runHandler(eq("afterAll"), any(), any());

        // Verify they only ran once
        verify(mockedMockProvider, times(1)).runHandler(eq("beforeAll"), any(), any());
        verify(mockedMockProvider, times(1)).runHandler(eq("defaultGet"), any(), any());
        verify(mockedMockProvider, times(1)).runHandler(eq("afterAll"), any(), any());
    }
}