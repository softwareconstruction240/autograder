package passoff.server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import passoff.model.*;
import passoff.websocket.TestCommand;
import passoff.websocket.TestMessage;
import passoff.websocket.WebsocketTestingEnvironment;
import server.Server;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketTests {

    private static WebsocketTestingEnvironment environment;
    private static TestServerFacade serverFacade;
    private static Server server;
    private static Long waitTime;
    private WebsocketUser white;
    private WebsocketUser black;
    private WebsocketUser observer;
    private Integer gameID;

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeAll
    public static void init() throws URISyntaxException {
        server = new Server();
        var port = Integer.toString(server.run(0));
        System.out.println("Started test HTTP server on " + port);

        serverFacade = new TestServerFacade("localhost", port);
        serverFacade.clear();

        GsonBuilder gsonBuilder = TestFactory.getGsonBuilder();
        environment = new WebsocketTestingEnvironment("localhost", port, "/ws", gsonBuilder);

        waitTime = TestFactory.getMessageTime();
    }

    @BeforeEach
    public void setup() {
        //populate database
        serverFacade.clear();

        white = registerUser("white", "WHITE", "white@chess.com");
        black = registerUser("black", "BLACK", "black@chess.com");
        observer = registerUser("observer", "OBSERVER", "observer@chess.com");

        gameID = createGame(white, "testGame");

        joinGame(gameID, white, ChessGame.TeamColor.WHITE);
        joinGame(gameID, black, ChessGame.TeamColor.BLACK);
    }

    @AfterEach
    public void tearDown() {
        environment.disconnectAll();
    }

    @Test
    @Order(1)
    @DisplayName("Normal Connect")
    public void connectGood() {
        setupNormalGame();
    }

    @Test
    @Order(2)
    @DisplayName("Connect Bad GameID")
    public void connectBadGameID() {
        //player connect with an incorrect game id
        connectToGame(white, gameID + 1, false, Set.of(), Set.of());

        //observer connect with an incorrect game id
        connectToGame(observer, gameID + 1, false, Set.of(white), Set.of());
    }

    @Test
    @Order(2)
    @DisplayName("Connect Bad AuthToken")
    public void connectBadAuthToken() {
        connectToGame(new WebsocketUser(black.username(), "badAuth"), gameID, false, Set.of(), Set.of());

        connectToGame(new WebsocketUser(observer.username(), "badAuth"), gameID, false, Set.of(black), Set.of());
    }

    @Test
    @Order(3)
    @DisplayName("Normal Make Move")
    public void validMove() {
        setupNormalGame();

        //create pawn move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 5), null);

        //make a valid move
        makeMove(white, gameID, move,true, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Move Bad Authtoken")
    public void makeMoveBadAuthtoken() {
        setupNormalGame();

        //set up valid move - pawn move two steps forward
        ChessMove move = new ChessMove(new ChessPosition(2, 6), new ChessPosition(4, 6), null);

        //send command with wrong authtoken
        makeMove(new WebsocketUser(white.username(), "badAuth"), gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Invalid Move")
    public void invalidMoveBadMove() {
        setupNormalGame();

        //try to move rook through a pawn - invalid move
        ChessMove move = new ChessMove(new ChessPosition(1, 1), new ChessPosition(1, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Move Wrong Turn")
    public void invalidMoveWrongTurn() {
        setupNormalGame();

        //try to move pawn out of turn - would be valid if in turn
        ChessMove move = new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null);
        makeMove(black, gameID, move, false, false, Set.of(white, observer), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Move for Opponent")
    public void invalidMoveOpponent() {
        setupNormalGame();

        //setup valid pawn move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);

        //attempt to make the move as the other player
        makeMove(black, gameID, move, false, false, Set.of(white, observer), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Move Observer")
    public void invalidMoveObserver() {
        setupNormalGame();

        //setup valid pawn move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);

        //have observer attempt to make a move
        makeMove(observer, gameID, move, false, false, Set.of(white, black), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Make Move Game Over")
    public void invalidMoveGameOver() {
        setupNormalGame();

        //Fools mate setup
        ChessMove move = new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null);
        makeMove(white, gameID, move, true, false, Set.of(black, observer), Set.of());

        move = new ChessMove(new ChessPosition(7, 5), new ChessPosition(6, 5), null);
        makeMove(black, gameID, move, true, false, Set.of(white, observer), Set.of());

        move = new ChessMove(new ChessPosition(2, 6), new ChessPosition(3, 6), null);
        makeMove(white, gameID, move, true, false, Set.of(black, observer), Set.of());

        move = new ChessMove(new ChessPosition(8, 4), new ChessPosition(4, 8), null);
        makeMove(black, gameID, move, true, true, Set.of(white, observer), Set.of());
        //checkmate

        //attempt another move
        move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Normal Resign")
    public void validResign() {
        setupNormalGame();

        resign(white, gameID, true, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(6)
    @DisplayName("Cannot Move After Resign")
    public void moveAfterResign() {
        setupNormalGame();

        resign(black, gameID, true, Set.of(white, observer), Set.of());

        //attempt to make a move after other player resigns
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(6)
    @DisplayName("Observer Resign")
    public void invalidResignObserver() {
        setupNormalGame();

        //have observer try to resign - should reject
        resign(observer, gameID, false, Set.of(white, black), Set.of());
    }

    @Test
    @Order(6)
    @DisplayName("Double Resign")
    public void invalidResignGameOver() {
        setupNormalGame();

        //normal resign
        resign(black, gameID, true, Set.of(white, observer), Set.of());

        //attempt to resign after other player resigns
        resign(white, gameID, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(7)
    @DisplayName("Leave Game")
    public void leaveGame() {
        setupNormalGame();

        //have white player leave
        //all other players get notified, white player should not be
        leave(white, gameID, Set.of(black, observer), Set.of());

        //observer leaves - only black player should get a notification
        leave(observer, gameID, Set.of(black), Set.of(white));
    }

    @Test
    @Order(8)
    @DisplayName("Join After Leave Game")
    public void joinAfterLeaveGame() {
        setupNormalGame();

        //have white player leave
        //all other players get notified, white player should not be
        leave(white, gameID, Set.of(black, observer), Set.of());

        //replace white player with a different player
        WebsocketUser white2 = registerUser("white2", "WHITE", "white2@chess.com");
        joinGame(gameID, white2, ChessGame.TeamColor.WHITE);
        connectToGame(white2, gameID, true, Set.of(black, observer), Set.of(white));

        //new white player can make move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 5), null);
        makeMove(white2, gameID, move, true, false, Set.of(black, observer), Set.of(white));
    }

    @Test
    @Order(9)
    @DisplayName("Multiple Concurrent Games")
    public void multipleConcurrentGames() {
        setupNormalGame();

        //setup parallel game
        WebsocketUser white2 = registerUser("white2", "WHITE", "white2@chess.com");
        WebsocketUser black2 = registerUser("black2", "BLACK", "black2@chess.com");
        WebsocketUser observer2 = registerUser("observer2", "OBSERVER", "observer2@chess.com");

        int otherGameID = createGame(white, "testGame2");

        joinGame(otherGameID, white2, ChessGame.TeamColor.WHITE);
        joinGame(otherGameID, black2, ChessGame.TeamColor.BLACK);

        //setup second game
        connectToGame(white2, otherGameID, true, Set.of(), Set.of(white, black, observer));
        connectToGame(black2, otherGameID, true, Set.of(white2), Set.of(white, black, observer));
        connectToGame(observer2, otherGameID, true,  Set.of(white2, black2), Set.of(white, black, observer));

        //make move in first game - only users in first game should be notified
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 5), null);
        makeMove(white, gameID, move, true, false, Set.of(black, observer), Set.of(white2, black2, observer2));

        //resign in second game - only users in second game should be notified
        resign(white2, otherGameID, true, Set.of(black2, observer2), Set.of(white, black, observer));

        //player leave in first game - only users remaining in first game should be notified
        leave(white, gameID, Set.of(black, observer), Set.of(white2, black2, observer2));
    }

    private void setupNormalGame() {
        //connect white player
        connectToGame(white, gameID, true, Set.of(), Set.of());

        //connect black player
        connectToGame(black, gameID, true, Set.of(white), Set.of());

        //connect observer
        connectToGame(observer, gameID, true,  Set.of(white, black), Set.of());
    }

    private WebsocketUser registerUser(String name, String password, String email) {
        TestAuthResult authResult = serverFacade.register(new TestUser(name, password, email));
        Assertions.assertEquals(200, serverFacade.getStatusCode(), "HTTP Status code was not 200 for registering a new user, was %d. Message: %s".formatted(serverFacade.getStatusCode(), authResult.getMessage()));
        return new WebsocketUser(authResult.getUsername(), authResult.getAuthToken());
    }

    private int createGame(WebsocketUser user, String name) {
        TestCreateResult createResult = serverFacade.createGame(new TestCreateRequest(name), user.authToken());
        Assertions.assertEquals(200, serverFacade.getStatusCode(), "HTTP Status code was not 200 for creating a new game, was %d. Message: %s".formatted(serverFacade.getStatusCode(), createResult.getMessage()));
        return createResult.getGameID();
    }

    private void joinGame(int gameID, WebsocketUser user, ChessGame.TeamColor color) {
        TestResult result = serverFacade.joinPlayer(new TestJoinRequest(color, gameID), user.authToken());
        Assertions.assertEquals(200, serverFacade.getStatusCode(), "HTTP Status code was not 200 for joining a player to a game, was %d. Message: %s".formatted(serverFacade.getStatusCode(), result.getMessage()));
    }

    private void connectToGame(WebsocketUser sender, int gameID, boolean expectSuccess,
                               Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand connectCommand = new TestCommand(UserGameCommand.CommandType.CONNECT, sender.authToken(), gameID);
        var numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 1 : 0), otherClients);
        var actualMessages = environment.exchange(sender.username(), connectCommand, numExpectedMessages, waitTime);

        if(expectSuccess) assertValidCommandMessages(actualMessages, sender, this::assertLoadGameMessage, inGame, this::assertNotificationMessage, otherClients);
        else assertInvalidCommandMessages(actualMessages, sender, inGame, otherClients);
    }

    private void makeMove(WebsocketUser sender, int gameID, ChessMove move, boolean expectSuccess,
                          boolean extraNotification, Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand moveCommand = new TestCommand(sender.authToken(), gameID, move);
        var numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 2 : 0), otherClients);
        var actualMessages = environment.exchange(sender.username(), moveCommand, numExpectedMessages, waitTime);

        if(expectSuccess) {
            if(extraNotification) assertValidCommandMessages(actualMessages, sender, this::assertLoadGameWithExtra, inGame, this::assertMoveMadeWithExtra, otherClients);
            else assertValidCommandMessages(actualMessages, sender, this::assertLoadGameMessage, inGame, this::assertMoveMadePair, otherClients);
        }
        else assertInvalidCommandMessages(actualMessages, sender, inGame, otherClients);
    }

    private void resign(WebsocketUser sender, int gameID, boolean expectSuccess,
                               Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand resignCommand = new TestCommand(UserGameCommand.CommandType.RESIGN, sender.authToken(), gameID);
        var numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 1 : 0), otherClients);
        var actualMessages = environment.exchange(sender.username(), resignCommand, numExpectedMessages, waitTime);

        if(expectSuccess) assertValidCommandMessages(actualMessages, sender, this::assertNotificationMessage, inGame, this::assertNotificationMessage, otherClients);
        else assertInvalidCommandMessages(actualMessages, sender, inGame, otherClients);
    }

    private void leave(WebsocketUser sender, int gameID, Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand leaveCommand = new TestCommand(UserGameCommand.CommandType.LEAVE, sender.authToken(), gameID);
        var numExpectedMessages = expectedMessages(sender, 0, inGame, 1, otherClients);
        var actualMessages = environment.exchange(sender.username(), leaveCommand, numExpectedMessages, waitTime);
        assertValidCommandMessages(actualMessages, sender, this::assertNoMessagesLeave, inGame, this::assertNotificationMessage, otherClients);
    }

    private Map<String, Integer> expectedMessages(WebsocketUser sender, int senderExpected, Set<WebsocketUser> inGame, int inGameExpected, Set<WebsocketUser> otherClients) {
        Map<String, Integer> expectedMessages = new HashMap<>();
        expectedMessages.put(sender.username(), senderExpected);
        expectedMessages.putAll(inGame.stream().collect(Collectors.toMap(WebsocketUser::username, s -> inGameExpected)));
        expectedMessages.putAll(otherClients.stream().collect(Collectors.toMap(WebsocketUser::username, s -> 0)));
        return expectedMessages;
    }

    private void assertValidCommandMessages(Map<String, List<TestMessage>> messages, WebsocketUser user, MessageAsserter userAsserter, Set<WebsocketUser> inGame, MessageAsserter inGameAsserter, Set<WebsocketUser> otherClients) {
        userAsserter.runAssertions(user.username(), messages.get(user.username()));
        for(WebsocketUser inGameUser : inGame) inGameAsserter.runAssertions(inGameUser.username(), messages.get(inGameUser.username()));
        for(WebsocketUser otherUser : otherClients) assertNoMessagesFromOtherGame(otherUser.username(), messages.get(otherUser.username()));
    }

    private void assertInvalidCommandMessages(Map<String, List<TestMessage>> messages, WebsocketUser user, Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        assertErrorMessage(user.username(), messages.get(user.username()));
        for(WebsocketUser inGameUser : inGame) assertNoMessagesInvalid(inGameUser.username(), messages.get(inGameUser.username()));
        for(WebsocketUser otherUser : otherClients) assertNoMessagesFromOtherGame(otherUser.username(), messages.get(otherUser.username()));
    }

    private void assertLoadGame(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.LOAD_GAME, message.getServerMessageType(), "Message for %s was not a LOAD_GAME message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getGame(), "%s's LOAD_GAME message did not contain a game (Make sure it's specifically called 'game')".formatted(username));
        Assertions.assertNull(message.getMessage(), "%s's LOAD_GAME message contained a message: %s".formatted(username, message.getMessage()));
        Assertions.assertNull(message.getErrorMessage(), "%s's LOAD_GAME message contained an error message: %s".formatted(username, message.getErrorMessage()));
    }

    private void assertNotification(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.NOTIFICATION, message.getServerMessageType(), "Message for %s was not a NOTIFICATION message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getMessage(), "%s's NOTIFICATION message did not contain a message (Make sure it's specifically called 'message')".formatted(username));
        Assertions.assertNull(message.getGame(), "%s's NOTIFICATION message contained a game: %s".formatted(username, message.getGame()));
        Assertions.assertNull(message.getErrorMessage(), "%s's NOTIFICATION message contained an error message: %s".formatted(username, message.getErrorMessage()));
    }

    private void assertError(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.ERROR, message.getServerMessageType(), "Message for %s was not an ERROR message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getErrorMessage(), "%s's ERROR message did not contain an error message (Make sure it's specifically called 'errorMessage')".formatted(username));
        Assertions.assertNull(message.getGame(), "%s's ERROR message contained a game: %s".formatted(username, message.getGame()));
        Assertions.assertNull(message.getMessage(), "%s's ERROR message contained a non-error message: %s".formatted(username, message.getMessage()));
    }

    private void assertLoadGameMessage(String username, List<TestMessage> messages) {
        Assertions.assertEquals(1, messages.size(), "Expected 1 message for %s, got %s: %s".formatted(username, messages.size(), messages));
        assertLoadGame(username, messages.get(0));
    }

    private void assertNotificationMessage(String username, List<TestMessage> messages) {
        Assertions.assertEquals(1, messages.size(), "Expected 1 message for %s, got %s: %s".formatted(username, messages.size(), messages));
        assertNotification(username, messages.get(0));
    }

    private void assertErrorMessage(String username, List<TestMessage> messages) {
        Assertions.assertEquals(1, messages.size(), "Expected 1 message for %s, got %s: %s".formatted(username, messages.size(), messages));
        assertError(username, messages.get(0));
    }

    private void assertMoveMadePair(String username, List<TestMessage> messages) {
        Assertions.assertEquals(2, messages.size(), "Expected 2 messages for %s, got %s".formatted(username, messages.size()));
        messages.sort(Comparator.comparing(TestMessage::getServerMessageType));
        try {
            assertLoadGame(username, messages.get(0));
            assertNotification(username, messages.get(1));
        } catch(AssertionError e) {
            Assertions.fail("Expected a LOAD_GAME and a NOTIFICATION for %s, got %s".formatted(username, messages.reversed()), e);
        }
    }

    private void assertMoveMadeWithExtra(String username, List<TestMessage> messages) {
        Assertions.assertTrue(messages.size() == 2 || messages.size() == 3, "Expected 2 or 3 messages, got " + messages.size());
        messages.sort(Comparator.comparing(TestMessage::getServerMessageType));
        try {
            assertLoadGame(username, messages.get(0));
            assertNotification(username, messages.get(1));
            if (messages.size() == 3) assertNotification(username, messages.get(2));
        } catch(AssertionError e) {
            Assertions.fail("Expected a LOAD_GAME and 1 or 2 NOTIFICATION's for %s, got %s".formatted(username, messages.reversed()), e);
        }
    }

    private void assertLoadGameWithExtra(String username, List<TestMessage> messages) {
        Assertions.assertTrue(messages.size() == 1 || messages.size() == 2, "Expected 1 or 2 messages, got " + messages.size());
        messages.sort(Comparator.comparing(TestMessage::getServerMessageType));
        try {
            assertLoadGame(username, messages.get(0));
            if (messages.size() == 2) assertNotification(username, messages.get(1));
        } catch(AssertionError e) {
            Assertions.fail("Expected a LOAD_GAME and an optional NOTIFICATION for %s, got %s".formatted(username, messages.reversed()), e);
        }
    }

    private void assertNoMessages(String username, List<TestMessage> messages, String description) {
            Assertions.assertTrue(messages.isEmpty(), "%s got a message after %s. messages: %s".formatted(username, description, messages));
    }

    private void assertNoMessagesInvalid(String username, List<TestMessage> messages) {
        assertNoMessages(username, messages, "another user sent an invalid command");
    }

    private void assertNoMessagesLeave(String username, List<TestMessage> messages) {
        assertNoMessages(username, messages, "leaving a game");
    }

    private void assertNoMessagesFromOtherGame(String username, List<TestMessage> messages) {
        assertNoMessages(username, messages, "a user from a different game or a game this user previously left sent a command");
    }

    @FunctionalInterface
    private static interface MessageAsserter {
        void runAssertions(String username, List<TestMessage> messages);
    }

    private static record WebsocketUser(String username, String authToken) { }
}