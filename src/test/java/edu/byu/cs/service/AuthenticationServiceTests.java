package edu.byu.cs.service;

import com.google.gson.Gson;
import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.JwtUtils;
import edu.byu.cs.util.NetworkUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private UserDao mockUserDao;
    private User testUser;


    @BeforeEach
    void setUp() {
        mockUserDao = mock(UserDao.class);
        testUser = new User("test_netid",
                0,
                "FirstName",
                "LastName",
                null,
                User.Role.ADMIN);
    }

    // ==================== callback() Tests ====================

    //tests valid url and valid config
    @Test
    @DisplayName("callback should return user from database if user exists")
    void callback_returnUserFromDatabase_whenUserExists() throws Exception {
        try (MockedStatic<DaoService> daoServiceMock = mockStatic(DaoService.class);
             MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class);
             MockedStatic<NetworkUtils> networkUtilsMock = mockStatic(NetworkUtils.class)) {

            daoServiceMock.when(DaoService::getUserDao).thenReturn(mockUserDao);
            when(mockUserDao.getUser("test_netid")).thenReturn(testUser);
            jwtUtilsMock.when(() -> JwtUtils.validateTokenAgainstKeys("valid_token")).thenReturn("test_netid");

            setupMockedValidOpenIDConfig(networkUtilsMock);

            User result = AuthenticationService.callback("valid_token");

            assertEquals(testUser, result);
            verify(mockUserDao).getUser("test_netid");
            verify(mockUserDao, never()).insertUser(any());
        }
    }

    @Test
    @DisplayName("callback should fetch user from Canvas and store in database if user doesn't exist")
    void callback_fetchFromCanvasAndStore_whenUserNotInDatabase() throws Exception {
        try (MockedStatic<DaoService> daoServiceMock = mockStatic(DaoService.class);
             MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class);
             MockedStatic<NetworkUtils> networkUtilsMock = mockStatic(NetworkUtils.class);
             MockedStatic<ApplicationProperties> appPropsMock = mockStatic(ApplicationProperties.class)) {

            daoServiceMock.when(DaoService::getUserDao).thenReturn(mockUserDao);
            when(mockUserDao.getUser("test_netid")).thenReturn(null);
            jwtUtilsMock.when(() -> JwtUtils.validateTokenAgainstKeys("valid_token")).thenReturn("test_netid");

            appPropsMock.when(ApplicationProperties::useCanvas).thenReturn(false);

            setupMockedValidOpenIDConfig(networkUtilsMock);

            User result = AuthenticationService.callback("valid_token");

            assertEquals(testUser, result);
            verify(mockUserDao).insertUser(testUser);
        }
    }

    @Test
    @DisplayName("callback should throw BadRequestException when token validation returns null")
    void callback_throwBadRequestException_whenTokenValidationFails() throws Exception {
        try (MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class);
             MockedStatic<NetworkUtils> networkUtilsMock = mockStatic(NetworkUtils.class)) {

            jwtUtilsMock.when(() -> JwtUtils.validateTokenAgainstKeys("invalid_token")).thenReturn(null);
            setupMockedValidOpenIDConfig(networkUtilsMock);

            assertThrows(BadRequestException.class, () -> AuthenticationService.callback("invalid_token"));
        }
    }

    @Test
    @DisplayName("callback should throw InternalErrorException when OpenID returned is suspicious")
    void callback_throwInternalErrorException_whenOpenIDCacheLooksSuspicious() throws Exception {
        try (MockedStatic<DaoService> daoServiceMock = mockStatic(DaoService.class);
             MockedStatic<JwtUtils> jwtUtilsMock = mockStatic(JwtUtils.class);
             MockedStatic<NetworkUtils> networkUtilsMock = mockStatic(NetworkUtils.class);
             MockedStatic<ApplicationProperties> appPropsMock = mockStatic(ApplicationProperties.class)) {

            daoServiceMock.when(DaoService::getUserDao).thenReturn(mockUserDao);
            when(mockUserDao.getUser("test_netid")).thenReturn(null);
            jwtUtilsMock.when(() -> JwtUtils.validateTokenAgainstKeys("valid_token")).thenReturn("test_netid");

            appPropsMock.when(ApplicationProperties::useCanvas).thenReturn(false);

            setupSuspiciousValidOpenIDConfig(networkUtilsMock);

            Assertions.assertThrows(InternalServerException.class,
                    ()-> AuthenticationService.callback("valid_token"));

            verify(mockUserDao, times(0)).insertUser(testUser);
        }
    }

    // ==================== isSecure() Tests ====================

    @Test
    @DisplayName("isSecure should return true when frontend URL starts with https")
    void isSecure_returnTrue_whenFrontendUrlIsHttps() {
        try (MockedStatic<ApplicationProperties> appPropsMock = mockStatic(ApplicationProperties.class)) {
            appPropsMock.when(ApplicationProperties::frontendUrl).thenReturn("https://example.com");

            assertTrue(AuthenticationService.isSecure());
        }
    }

    @Test
    @DisplayName("isSecure should return false when frontend URL does not start with https")
    void isSecure_returnFalse_whenFrontendUrlIsNotHttps() {
        try (MockedStatic<ApplicationProperties> appPropsMock = mockStatic(ApplicationProperties.class)) {
            appPropsMock.when(ApplicationProperties::frontendUrl).thenReturn("http://example.com");

            assertFalse(AuthenticationService.isSecure());
        }
    }

    // ==================== Authorization Url Test ============

    @Test
    @DisplayName("getAuthorizationUrl returns a filled out authorization request")
    void getAuthUrl() throws Exception{
        try (MockedStatic<NetworkUtils> networkUtilsMock = mockStatic(NetworkUtils.class);
             MockedStatic<ApplicationProperties> appPropsMock = mockStatic(ApplicationProperties.class)) {


            appPropsMock.when(ApplicationProperties::casCallbackUrl).thenReturn("https://cs240.click/auth/callback");
            appPropsMock.when(ApplicationProperties::clientId).thenReturn("cs240");
            setupMockedValidOpenIDConfig(networkUtilsMock);

            Assertions.assertEquals("https://api-sandbox.byu.edu/auth?response_type=code" +
                            "&client_id=cs240" +
                            "&redirect_uri=" +
                            URLEncoder.encode("https://cs240.click/auth/callback", StandardCharsets.UTF_8) +
                            "&scope=openid",
                    AuthenticationService.getAuthorizationUrl());

        }
    }

    // ==================== Helper Methods ====================

    private void setupMockedValidOpenIDConfig(MockedStatic<NetworkUtils> networkUtilsMock) throws IOException, InterruptedException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(createValidOpenIDConfigJson());
        networkUtilsMock.when(() -> NetworkUtils.makeJsonGetRequest(anyString()))
                .thenReturn(mockResponse);
        networkUtilsMock.when(() -> NetworkUtils.getCacheTime(any()))
                .thenReturn(Instant.now().plusSeconds(3600));
    }

    private void setupSuspiciousValidOpenIDConfig(MockedStatic<NetworkUtils> networkUtilsMock){
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(createBadIssuerOpenIDConfigJson());
        networkUtilsMock.when(() -> NetworkUtils.makeJsonGetRequest(anyString()))
                .thenReturn(mockResponse);
        networkUtilsMock.when(() -> NetworkUtils.getCacheTime(any()))
                .thenReturn(Instant.now().plusSeconds(0));
    }

    private String createValidOpenIDConfigJson() {
        AuthenticationService.OpenIDConfig config = new AuthenticationService.OpenIDConfig(
                AuthenticationService.BYU_API_URL,
                "https://api-sandbox.byu.edu/auth",
                "https://api-sandbox.byu.edu/token",
                "https://api-sandbox.byu.edu/jwks",
                List.of("openid"),
                List.of("RS256")
        );
        return new Gson().toJson(config);
    }

    private String createBadIssuerOpenIDConfigJson(){
        AuthenticationService.OpenIDConfig config = new AuthenticationService.OpenIDConfig(
                "https://badactor.com",
                "https://api-sandbox.byu.edu/auth",
                "https://api-sandbox.byu.edu/token",
                "https://api-sandbox.byu.edu/jwks",
                List.of("openid"),
                List.of("RS256")
        );
        return new Gson().toJson(config);
    }
}
