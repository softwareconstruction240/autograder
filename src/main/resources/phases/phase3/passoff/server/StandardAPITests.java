package passoff.server;

import chess.ChessGame;
import org.junit.jupiter.api.*;
import passoff.model.*;
import server.Server;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StandardAPITests {

    private static TestUser existingUser;

    private static TestUser newUser;

    private static TestCreateRequest createRequest;

    private static TestServerFacade serverFacade;
    private static Server server;

    private String existingAuth;

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        serverFacade = new TestServerFacade("localhost", Integer.toString(port));

        existingUser = new TestUser("ExistingUser", "existingUserPassword", "eu@mail.com");

        newUser = new TestUser("NewUser", "newUserPassword", "nu@mail.com");

        createRequest = new TestCreateRequest("testGame");
    }

    @BeforeEach
    public void setup() {
        serverFacade.clear();

        //one user already logged in
        TestAuthResult regResult = serverFacade.register(existingUser);
        existingAuth = regResult.getAuthToken();
    }

    @Test
    @Order(1)
    @DisplayName("Static Files")
    public void staticFiles() throws Exception {
        String htmlFromServer = serverFacade.file("/").replaceAll("\r", "");
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, serverFacade.getStatusCode(),
                "Server response code was not 200 OK");
        Assertions.assertNotNull(htmlFromServer, "Server returned an empty file");
        Assertions.assertTrue(htmlFromServer.contains("CS 240 Chess Server Web API"));
    }

    @Test
    @Order(2)
    @DisplayName("Normal User Login")
    public void successLogin() {
        TestAuthResult loginResult = serverFacade.login(existingUser);

        assertHttpOk(loginResult);
        Assertions.assertEquals(existingUser.getUsername(), loginResult.getUsername(),
                "Response did not give the same username as user");
        Assertions.assertNotNull(loginResult.getAuthToken(), "Response did not return authentication String");
    }

    @Test
    @Order(3)
    @DisplayName("Login Invalid User")
    public void loginInvalidUser() {
        TestAuthResult loginResult = serverFacade.login(newUser);

        assertHttpUnauthorized(loginResult);
        assertAuthFieldsMissing(loginResult);
    }

    @Test
    @Order(3)
    @DisplayName("Login Wrong Password")
    public void loginWrongPassword() {
        TestUser loginRequest = new TestUser(existingUser.getUsername(), newUser.getPassword());

        TestAuthResult loginResult = serverFacade.login(loginRequest);

        assertHttpUnauthorized(loginResult);
        assertAuthFieldsMissing(loginResult);
    }

    @Test
    @Order(4)
    @DisplayName("Normal User Registration")
    public void successRegister() {
        //submit register request
        TestAuthResult registerResult = serverFacade.register(newUser);

        assertHttpOk(registerResult);
        Assertions.assertEquals(newUser.getUsername(), registerResult.getUsername(),
                "Response did not have the same username as was registered");
        Assertions.assertNotNull(registerResult.getAuthToken(), "Response did not contain an authentication string");
    }

    @Test
    @Order(5)
    @DisplayName("Re-Register User")
    public void registerTwice() {
        //submit register request trying to register existing user
        TestAuthResult registerResult = serverFacade.register(existingUser);

        assertHttpForbidden(registerResult);
        assertAuthFieldsMissing(registerResult);
    }

    @Test
    @Order(5)
    @DisplayName("Register Bad Request")
    public void failRegister() {
        //attempt to register a user without a password
        TestUser registerRequest = new TestUser(newUser.getUsername(), null, newUser.getEmail());

        TestAuthResult registerResult = serverFacade.register(registerRequest);

        assertHttpBadRequest(registerResult);
        assertAuthFieldsMissing(registerResult);
    }

    @Test
    @Order(6)
    @DisplayName("Normal Logout")
    public void successLogout() {
        //log out existing user
        TestResult result = serverFacade.logout(existingAuth);

        assertHttpOk(result);
    }

    @Test
    @Order(7)
    @DisplayName("Invalid Auth Logout")
    public void failLogout() {
        //log out user twice
        //second logout should fail
        serverFacade.logout(existingAuth);
        TestResult result = serverFacade.logout(existingAuth);

        assertHttpUnauthorized(result);
    }

    @Test
    @Order(8)
    @DisplayName("Valid Creation")
    public void goodCreate() {
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        assertHttpOk(createResult);
        Assertions.assertNotNull(createResult.getGameID(), "Result did not return a game ID");
        Assertions.assertTrue(createResult.getGameID() > 0, "Result returned invalid game ID");
    }

    @Test
    @Order(9)
    @DisplayName("Create with Bad Authentication")
    public void badAuthCreate() {
        //log out user so auth is invalid
        serverFacade.logout(existingAuth);

        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        assertHttpUnauthorized(createResult);
        Assertions.assertNull(createResult.getGameID(), "Bad result returned a game ID");
    }

    @Test
    @Order(10)
    @DisplayName("Join Created Game")
    public void goodJoin() {
        //create game
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        //join as white
        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.WHITE, createResult.getGameID());

        //try join
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, existingAuth);

        //check
        assertHttpOk(joinResult);

        TestListResult listResult = serverFacade.listGames(existingAuth);

        Assertions.assertEquals(1, listResult.getGames().length);
        Assertions.assertEquals(existingUser.getUsername(), listResult.getGames()[0].getWhiteUsername());
        Assertions.assertNull(listResult.getGames()[0].getBlackUsername());
    }

    @Test
    @Order(11)
    @DisplayName("Join Bad Authentication")
    public void badAuthJoin() {
        //create game
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        //try join as white
        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.WHITE, createResult.getGameID());
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, existingAuth + "bad stuff");

        //check
        assertHttpUnauthorized(joinResult);
    }

    @Test
    @Order(11)
    @DisplayName("Join Bad Team Color")
    public void badColorJoin() {
        //create game
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        //try join as white
        TestJoinRequest joinRequest = new TestJoinRequest(null, createResult.getGameID());
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, existingAuth);

        //check
        assertHttpBadRequest(joinResult);
    }

    @Test
    @Order(11)
    @DisplayName("Join Steal Team Color")
    public void stealColorJoin() {
        //create game
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        //add existing user as black
        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.BLACK, createResult.getGameID());
        serverFacade.joinPlayer(joinRequest, existingAuth);

        //register second user
        TestAuthResult registerResult = serverFacade.register(newUser);

        //join request trying to also join  as black
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, registerResult.getAuthToken());

        //check failed
        assertHttpForbidden(joinResult);
    }

    @Test
    @Order(11)
    @DisplayName("Join Bad Game ID")
    public void badGameIDJoin() {
        //create game
        createRequest = new TestCreateRequest("Bad Join");
        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);

        //try join as white
        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.WHITE, null);
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, existingAuth);

        //check
        assertHttpBadRequest(joinResult);
    }

    @Test
    @Order(12)
    @DisplayName("List No Games")
    public void noGamesList() {
        TestListResult result = serverFacade.listGames(existingAuth);

        assertHttpOk(result);
        Assertions.assertTrue(result.getGames() == null || result.getGames().length == 0,
                "Found games when none should be there");
    }

    @Test
    @Order(12)
    @DisplayName("List Multiple Games")
    public void gamesList() {
        //register a few users to create games
        TestUser userA = new TestUser("a", "A", "a.A");
        TestUser userB = new TestUser("b", "B", "b.B");
        TestUser userC = new TestUser("c", "C", "c.C");
        
        TestAuthResult authA = serverFacade.register(userA);
        TestAuthResult authB = serverFacade.register(userB);
        TestAuthResult authC = serverFacade.register(userC);

        //create games
        Collection<TestListEntry> expectedList = new HashSet<>();

        //1 as black from A
        String game1Name = "I'm numbah one!";
        TestCreateResult game1 = serverFacade.createGame(new TestCreateRequest(game1Name), authA.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.BLACK, game1.getGameID()), authA.getAuthToken());
        expectedList.add(new TestListEntry(game1.getGameID(), game1Name, null, authA.getUsername()));


        //1 as white from B
        String game2Name = "Lonely";
        TestCreateResult game2 = serverFacade.createGame(new TestCreateRequest(game2Name), authB.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.WHITE, game2.getGameID()), authB.getAuthToken());
        expectedList.add(new TestListEntry(game2.getGameID(), game2Name, authB.getUsername(), null));


        //1 of each from C
        String game3Name = "GG";
        TestCreateResult game3 = serverFacade.createGame(new TestCreateRequest(game3Name), authC.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.WHITE, game3.getGameID()), authC.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.BLACK, game3.getGameID()), authA.getAuthToken());
        expectedList.add(new TestListEntry(game3.getGameID(), game3Name, authC.getUsername(), authA.getUsername()));


        //C play self
        String game4Name = "All by myself";
        TestCreateResult game4 = serverFacade.createGame(new TestCreateRequest(game4Name), authC.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.WHITE, game4.getGameID()), authC.getAuthToken());
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.BLACK, game4.getGameID()), authC.getAuthToken());
        expectedList.add(new TestListEntry(game4.getGameID(), game4Name, authC.getUsername(), authC.getUsername()));


        //list games
        TestListResult listResult = serverFacade.listGames(existingAuth);
        assertHttpOk(listResult);
        Collection<TestListEntry> returnedList = new HashSet<>(Arrays.asList(listResult.getGames()));

        //check
        Assertions.assertEquals(expectedList, returnedList, "Returned Games list was incorrect");
    }

    @Test
    @Order(13)
    @DisplayName("Unique Authtoken Each Login")
    public void uniqueAuthorizationTokens() {
        TestAuthResult loginOne = serverFacade.login(existingUser);
        assertHttpOk(loginOne);
        Assertions.assertNotNull(loginOne.getAuthToken(), "Login result did not contain an authToken");

        TestAuthResult loginTwo = serverFacade.login(existingUser);
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, serverFacade.getStatusCode(),
                "Server response code was not 200 OK");
        Assertions.assertNotNull(loginTwo.getAuthToken(), "Login result did not contain an authToken");

        Assertions.assertNotEquals(existingAuth, loginOne.getAuthToken(),
                "Authtoken returned by login matched authtoken from prior register");
        Assertions.assertNotEquals(existingAuth, loginTwo.getAuthToken(),
                "Authtoken returned by login matched authtoken from prior register");
        Assertions.assertNotEquals(loginOne.getAuthToken(), loginTwo.getAuthToken(),
                "Authtoken returned by login matched authtoken from prior login");


        TestCreateResult createResult = serverFacade.createGame(createRequest, existingAuth);
        assertHttpOk(createResult);


        TestResult logoutResult = serverFacade.logout(existingAuth);
        assertHttpOk(logoutResult);


        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.WHITE, createResult.getGameID());
        TestResult joinResult = serverFacade.joinPlayer(joinRequest, loginOne.getAuthToken());
        assertHttpOk(joinResult);


        TestListResult listResult = serverFacade.listGames(loginTwo.getAuthToken());
        assertHttpOk(listResult);
        Assertions.assertEquals(1, listResult.getGames().length);
        Assertions.assertEquals(existingUser.getUsername(), listResult.getGames()[0].getWhiteUsername());
    }

    @Test
    @Order(14)
    @DisplayName("Clear Test")
    public void clearData() {
        //create filler games
        serverFacade.createGame(new TestCreateRequest("Mediocre game"), existingAuth);
        serverFacade.createGame(new TestCreateRequest("Awesome game"), existingAuth);

        //log in new user
        TestUser user = new TestUser("ClearMe", "cleared", "clear@mail.com");
        TestAuthResult registerResult = serverFacade.register(user);

        //create and join game for new user
        TestCreateResult createResult = serverFacade.createGame(new TestCreateRequest("Clear game"),
                registerResult.getAuthToken());

        TestJoinRequest joinRequest = new TestJoinRequest(ChessGame.TeamColor.WHITE, createResult.getGameID());
        serverFacade.joinPlayer(joinRequest, registerResult.getAuthToken());

        //do clear
        TestResult clearResult = serverFacade.clear();

        //test clear successful
        assertHttpOk(clearResult);

        //make sure neither user can log in
        //first user
        TestAuthResult loginResult = serverFacade.login(existingUser);
        assertHttpUnauthorized(loginResult);

        //second user
        loginResult = serverFacade.login(user);
        assertHttpUnauthorized(loginResult);

        //try to use old auth token to list games
        TestListResult listResult = serverFacade.listGames(existingAuth);
        assertHttpUnauthorized(listResult);

        //log in new user and check that list is empty
        registerResult = serverFacade.register(user);
        assertHttpOk(registerResult);
        listResult = serverFacade.listGames(registerResult.getAuthToken());
        assertHttpOk(listResult);

        //check listResult
        Assertions.assertEquals(0, listResult.getGames().length, "list result did not return 0 games after clear");
    }

    @Test
    @Order(14)
    @DisplayName("Multiple Clears")
    public void multipleClear() {

        //clear multiple times
        serverFacade.clear();
        serverFacade.clear();
        TestResult result = serverFacade.clear();

        //make sure returned good
        assertHttpOk(result);
    }

    private void assertHttpOk(TestResult result) {
        Assertions.assertEquals(HttpURLConnection.HTTP_OK, serverFacade.getStatusCode(),
                "Server response code was not 200 OK (message: %s)".formatted(result.getMessage()));
        Assertions.assertFalse(result.getMessage() != null &&
                        result.getMessage().toLowerCase(Locale.ROOT).contains("error"),
                "Result returned an error message");
    }

    private void assertHttpBadRequest(TestResult result) {
        assertHttpError(result, HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request");
    }

    private void assertHttpUnauthorized(TestResult result) {
        assertHttpError(result, HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized");
    }

    private void assertHttpForbidden(TestResult result) {
        assertHttpError(result, HttpURLConnection.HTTP_FORBIDDEN, "Forbidden");
    }

    private void assertHttpError(TestResult result, int statusCode, String message) {
        Assertions.assertEquals(statusCode, serverFacade.getStatusCode(),
                "Server response code was not %d %s (message: %s)".formatted(statusCode, message, result.getMessage()));
        Assertions.assertTrue(result.getMessage() != null &&
                        result.getMessage().toLowerCase(Locale.ROOT).contains("error"),
                "Invalid Request didn't return an error message");
    }

    private void assertAuthFieldsMissing(TestAuthResult result) {
        Assertions.assertNull(result.getUsername(), "Response incorrectly returned username");
        Assertions.assertNull(result.getAuthToken(), "Response incorrectly return authentication String");
    }

}
