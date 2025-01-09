package passoff.server;

import chess.*;
import org.junit.jupiter.api.*;
import passoff.model.*;
import passoff.websocket.*;
import server.Server;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static websocket.messages.ServerMessage.ServerMessageType.*;

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
        environment = new WebsocketTestingEnvironment("localhost", port, "/ws", TestFactory.getGsonBuilder());
        waitTime = TestFactory.getMessageTime();
    }

    @BeforeEach
    public void setup() {
        //populate database with HTTP calls
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
    @DisplayName("Connect 1 User")
    public void connectSingleUser() {
        connectToGame(white, gameID, true, Set.of(), Set.of()); //Connects 1 User to the game
    }

    @Test
    @Order(2)
    @DisplayName("Normal Connect")
    public void connectGood() {
        setupNormalGame();    //Connects 3 Users to the game, and notifies others upon connection
    }

    @Test
    @Order(3)
    @DisplayName("Connect Bad GameID")
    public void connectBadGameID() {
        connectToGame(white, gameID + 1, false, Set.of(), Set.of()); //player connect with an incorrect game id
        connectToGame(observer, gameID + 1, false, Set.of(white), Set.of()); //observer incorrect game id
    }

    @Test
    @Order(3)
    @DisplayName("Connect Bad AuthToken")
    public void connectBadAuthToken() {
        connectToGame(new WebsocketUser(black.username(), "badAuth"), gameID, false, Set.of(), Set.of());
        connectToGame(new WebsocketUser(observer.username(), "badAuth"), gameID, false, Set.of(black), Set.of());
    }

    @Test
    @Order(4)
    @DisplayName("Normal Make Move")
    public void validMove() {
        setupNormalGame();

        //make a valid pawn move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(3, 5), null);
        makeMove(white, gameID, move,true, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Make Move Bad Authtoken")
    public void makeMoveBadAuthtoken() {
        setupNormalGame();

        //make valid move command with wrong authtoken
        ChessMove move = new ChessMove(new ChessPosition(2, 6), new ChessPosition(4, 6), null);
        makeMove(new WebsocketUser(white.username(), "badAuth"), gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Make Invalid Move")
    public void invalidMoveBadMove() {
        setupNormalGame();

        //try to move rook through a pawn - invalid move
        ChessMove move = new ChessMove(new ChessPosition(1, 1), new ChessPosition(1, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Make Move Wrong Turn")
    public void invalidMoveWrongTurn() {
        setupNormalGame();

        //try to move pawn out of turn - would be valid if in turn
        ChessMove move = new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null);
        makeMove(black, gameID, move, false, false, Set.of(white, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Make Move for Opponent")
    public void invalidMoveOpponent() {
        setupNormalGame();

        //attempt to have black player move white piece
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(black, gameID, move, false, false, Set.of(white, observer), Set.of());
    }

    @Test
    @Order(5)
    @DisplayName("Make Move Observer")
    public void invalidMoveObserver() {
        setupNormalGame();

        //have observer attempt to make an otherwise valid move
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(observer, gameID, move, false, false, Set.of(white, black), Set.of());
    }

    @Test
    @Order(5)
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
        //checkmate--attempt another move
        move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(6)
    @DisplayName("Normal Resign")
    public void validResign() {
        setupNormalGame();
        resign(white, gameID, true, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(7)
    @DisplayName("Cannot Move After Resign")
    public void moveAfterResign() {
        setupNormalGame();
        resign(black, gameID, true, Set.of(white, observer), Set.of());

        //attempt to make a move after other player resigns
        ChessMove move = new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null);
        makeMove(white, gameID, move, false, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(7)
    @DisplayName("Observer Resign")
    public void invalidResignObserver() {
        setupNormalGame();

        //have observer try to resign - should reject
        resign(observer, gameID, false, Set.of(white, black), Set.of());
    }

    @Test
    @Order(7)
    @DisplayName("Double Resign")
    public void invalidResignGameOver() {
        setupNormalGame();
        resign(black, gameID, true, Set.of(white, observer), Set.of());

        //attempt to resign after other player resigns
        resign(white, gameID, false, Set.of(black, observer), Set.of());
    }

    @Test
    @Order(8)
    @DisplayName("Leave Game")
    public void leaveGame() {
        setupNormalGame();

        //have white player leave--all other players get notified, white player should not be
        leave(white, gameID, Set.of(black, observer), Set.of());

        //observer leaves - only black player should get a notification
        leave(observer, gameID, Set.of(black), Set.of(white));
    }

    @Test
    @Order(9)
    @DisplayName("Join After Leave Game")
    public void joinAfterLeaveGame() {
        setupNormalGame();

        //have white player leave--all other players get notified, white player should not be
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
    @Order(10)
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
        connectToGame(white, gameID, true, Set.of(), Set.of()); //connect white player
        connectToGame(black, gameID, true, Set.of(white), Set.of()); //connect black player
        connectToGame(observer, gameID, true,  Set.of(white, black), Set.of()); //connect observer
    }

    private WebsocketUser registerUser(String name, String password, String email) {
        TestAuthResult authResult = serverFacade.register(new TestUser(name, password, email));
        assertHttpOk(authResult, "registering a new user");
        return new WebsocketUser(authResult.getUsername(), authResult.getAuthToken());
    }

    private int createGame(WebsocketUser user, String name) {
        TestCreateResult createResult = serverFacade.createGame(new TestCreateRequest(name), user.authToken());
        assertHttpOk(createResult, "creating a new game");
        return createResult.getGameID();
    }

    private void joinGame(int gameID, WebsocketUser user, ChessGame.TeamColor color) {
        TestResult result = serverFacade.joinPlayer(new TestJoinRequest(color, gameID), user.authToken());
        assertHttpOk(result, "joining a player to a game");
    }

    private void assertHttpOk(TestResult result, String context) {
        Assertions.assertEquals(200, serverFacade.getStatusCode(),
                String.format("HTTP Status code was not 200 for %s, was %d. Message: %s",
                        context, serverFacade.getStatusCode(), result.getMessage()));
    }

    private void connectToGame(WebsocketUser sender, int gameID, boolean expectSuccess,
                               Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand connectCommand = new TestCommand(UserGameCommand.CommandType.CONNECT, sender.authToken(), gameID);
        Map<String, Integer> numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 1 : 0), otherClients);
        Map<String, List<TestMessage>> actualMessages = environment.exchange(sender.username(), connectCommand, numExpectedMessages, waitTime);

        assertCommandMessages(actualMessages, expectSuccess, sender, types(LOAD_GAME), inGame, types(NOTIFICATION), otherClients);
    }

    private void makeMove(WebsocketUser sender, int gameID, ChessMove move, boolean expectSuccess,
                          boolean extraNotification, Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand moveCommand = new TestCommand(sender.authToken(), gameID, move);
        Map<String, Integer> numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 2 : 0), otherClients);
        Map<String, List<TestMessage>> actualMessages = environment.exchange(sender.username(), moveCommand, numExpectedMessages, waitTime);

        if(extraNotification && actualMessages.get(sender.username()).size() > 1) {
            assertCommandMessages(actualMessages, expectSuccess, sender, types(LOAD_GAME, NOTIFICATION),
                    inGame, types(LOAD_GAME, NOTIFICATION, NOTIFICATION), otherClients);
        }
        else {
            assertCommandMessages(actualMessages, expectSuccess, sender, types(LOAD_GAME),
                    inGame, types(LOAD_GAME, NOTIFICATION), otherClients);
        }
    }

    private void resign(WebsocketUser sender, int gameID, boolean expectSuccess,
                        Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand resignCommand = new TestCommand(UserGameCommand.CommandType.RESIGN, sender.authToken(), gameID);
        Map<String, Integer> numExpectedMessages = expectedMessages(sender, 1, inGame, (expectSuccess ? 1 : 0), otherClients);
        Map<String, List<TestMessage>> actualMessages = environment.exchange(sender.username(), resignCommand, numExpectedMessages, waitTime);

        assertCommandMessages(actualMessages, expectSuccess, sender, types(NOTIFICATION),
                inGame, types(NOTIFICATION), otherClients);
    }

    private void leave(WebsocketUser sender, int gameID, Set<WebsocketUser> inGame, Set<WebsocketUser> otherClients) {
        TestCommand leaveCommand = new TestCommand(UserGameCommand.CommandType.LEAVE, sender.authToken(), gameID);
        Map<String, Integer> numExpectedMessages = expectedMessages(sender, 0, inGame, 1, otherClients);
        Map<String, List<TestMessage>> actualMessages = environment.exchange(sender.username(), leaveCommand, numExpectedMessages, waitTime);

        assertCommandMessages(actualMessages, true, sender, types(), inGame, types(NOTIFICATION), otherClients);
    }
    
    private Map<String, Integer> expectedMessages(WebsocketUser sender, int senderExpected,
                                                  Set<WebsocketUser> inGame, int inGameExpected, Set<WebsocketUser> otherClients) {
        Map<String, Integer> expectedMessages = new HashMap<>();
        expectedMessages.put(sender.username(), senderExpected);
        expectedMessages.putAll(inGame.stream().collect(Collectors.toMap(WebsocketUser::username, s -> inGameExpected)));
        expectedMessages.putAll(otherClients.stream().collect(Collectors.toMap(WebsocketUser::username, s -> 0)));
        return expectedMessages;
    }

    private void assertCommandMessages(Map<String, List<TestMessage>> messages, boolean expectSuccess,
                                            WebsocketUser user, ServerMessage.ServerMessageType[] userExpectedTypes,
                                            Set<WebsocketUser> inGame, ServerMessage.ServerMessageType[] inGameExpectedTypes,
                                            Set<WebsocketUser> otherClients) {
        if(!expectSuccess) {
            userExpectedTypes = new ServerMessage.ServerMessageType[]{ERROR};
            inGameExpectedTypes = new ServerMessage.ServerMessageType[0];
        }
        assertMessages(user.username(), userExpectedTypes, messages.get(user.username()));
        for(WebsocketUser inGameUser : inGame) {
            assertMessages(inGameUser.username(), inGameExpectedTypes, messages.get(inGameUser.username()));
        }
        for(WebsocketUser otherUser : otherClients) {
            assertMessages(otherUser.username(), new ServerMessage.ServerMessageType[0], messages.get(otherUser.username()));
        }
    }

    private void assertMessages(String username, ServerMessage.ServerMessageType[] expectedTypes, List<TestMessage> messages) {
        Assertions.assertEquals(expectedTypes.length, messages.size(), "Expected %d messages for %s, got %d: %s"
                .formatted(expectedTypes.length, username, messages.size(), messages));
        Arrays.sort(expectedTypes);
        messages.sort(Comparator.comparing(TestMessage::getServerMessageType));
        try {
            for(int i = 0; i < expectedTypes.length; i++) {
                switch (expectedTypes[i]) {
                    case LOAD_GAME -> assertLoadGame(username, messages.get(i));
                    case NOTIFICATION -> assertNotification(username, messages.get(i));
                    case ERROR -> assertError(username, messages.get(i));
                }
            }
        } catch(AssertionError e) {
            Assertions.fail("Expected message types matching %s for %s, got %s"
                    .formatted(Arrays.toString(expectedTypes), username, messages.reversed()), e);
        }
    }

    private void assertLoadGame(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.LOAD_GAME, message.getServerMessageType(),
                "Message for %s was not a LOAD_GAME message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getGame(),
                "%s's LOAD_GAME message did not contain a game (Make sure it's specifically called 'game')".formatted(username));
        Assertions.assertNull(message.getMessage(),
                "%s's LOAD_GAME message contained a message: %s".formatted(username, message.getMessage()));
        Assertions.assertNull(message.getErrorMessage(),
                "%s's LOAD_GAME message contained an error message: %s".formatted(username, message.getErrorMessage()));
    }

    private void assertNotification(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.NOTIFICATION, message.getServerMessageType(),
                "Message for %s was not a NOTIFICATION message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getMessage(),
                "%s's NOTIFICATION message did not contain a message (Make sure it's specifically called 'message')".formatted(username));
        Assertions.assertNull(message.getGame(),
                "%s's NOTIFICATION message contained a game: %s".formatted(username, message.getGame()));
        Assertions.assertNull(message.getErrorMessage(),
                "%s's NOTIFICATION message contained an error message: %s".formatted(username, message.getErrorMessage()));
    }

    private void assertError(String username, TestMessage message) {
        Assertions.assertEquals(ServerMessage.ServerMessageType.ERROR, message.getServerMessageType(),
                "Message for %s was not an ERROR message: %s".formatted(username, message));
        Assertions.assertNotNull(message.getErrorMessage(),
                "%s's ERROR message did not contain an error message (Make sure it's specifically called 'errorMessage')".formatted(username));
        Assertions.assertNull(message.getGame(),
                "%s's ERROR message contained a game: %s".formatted(username, message.getGame()));
        Assertions.assertNull(message.getMessage(),
                "%s's ERROR message contained a non-error message: %s".formatted(username, message.getMessage()));
    }

    private ServerMessage.ServerMessageType[] types(ServerMessage.ServerMessageType... types) {
        return types;
    }

    private static record WebsocketUser(String username, String authToken) { }
}
