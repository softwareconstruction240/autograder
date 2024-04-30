package passoff.server;

import chess.ChessGame;
import org.junit.jupiter.api.*;
import passoff.model.*;
import server.Server;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseTests {

    private static TestServerFacade serverFacade;
    private static Server server;


    @BeforeAll
    public static void startServer() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        serverFacade = new TestServerFacade("localhost", Integer.toString(port));
    }

    @BeforeEach
    public void setUp() {
        serverFacade.clear();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }



    @Test
    @DisplayName("Persistence Test")
    @Order(1)
    public void persistenceTest() {
        int initialRowCount = getDatabaseRows();

        TestUser user = new TestUser("ExistingUser", "existingUserPassword", "eu@mail.com");

        TestAuthResult regResult = serverFacade.register(user);
        String auth = regResult.getAuthToken();

        //create a game
        String gameName = "Test Game";
        TestCreateResult createResult = serverFacade.createGame(new TestCreateRequest(gameName), auth);

        //join the game
        serverFacade.joinPlayer(new TestJoinRequest(ChessGame.TeamColor.WHITE, createResult.getGameID()), auth);

        Assertions.assertTrue(initialRowCount < getDatabaseRows(), "No new data added to database");

        // Test that we can read the data after a restart
        stopServer();
        startServer();

        //list games using the auth
        TestListResult listResult = serverFacade.listGames(auth);
        Assertions.assertEquals(200, serverFacade.getStatusCode(), "Server response code was not 200 OK");
        Assertions.assertEquals(1, listResult.getGames().length, "Missing game(s) in database after restart");

        TestListEntry game1 = listResult.getGames()[0];
        Assertions.assertEquals(game1.getGameID(), createResult.getGameID());
        Assertions.assertEquals(gameName, game1.getGameName(), "Game name changed after restart");
        Assertions.assertEquals(user.getUsername(), game1.getWhiteUsername(),
                "White player username changed after restart");

        //test that we can still log in
        serverFacade.login(user);
        Assertions.assertEquals(200, serverFacade.getStatusCode(), "Unable to login");
    }

    @Test
    @DisplayName("Bcrypt")
    @Order(2)
    public void bcrypt() {
        String clearTextPassword = "existingUserPassword";
        TestUser user = new TestUser("ExistingUser", clearTextPassword, "eu@mail.com");
        serverFacade.register(user);

        try (Connection conn = getConnection();) {
            try (Statement statement = conn.createStatement()) {
                for (String table : getTables(conn)) {
                    String sql = "SELECT * FROM " + table;
                    try (ResultSet rs = statement.executeQuery(sql)) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs.next()) {
                            for(int i = 1; i <= columnsNumber; i++) {
                                String value = rs.getString(i);
                                Assertions.assertFalse(value.contains(clearTextPassword),
                                        "Found clear text password in database");
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Assertions.fail("Unable to load database. Are you using dataAccess.DatabaseManager to set your credentials?", ex);
        }
    }

    private Connection getConnection() throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("dataaccess.DatabaseManager");
        Method getConnectionMethod = clazz.getDeclaredMethod("getConnection");
        getConnectionMethod.setAccessible(true);

        Object obj = clazz.getDeclaredConstructor().newInstance();
        return (Connection) getConnectionMethod.invoke(obj);
    }


    private int getDatabaseRows() {
        int rows = 0;
        try (Connection conn = getConnection();) {
            try (var statement = conn.createStatement()) {
                for (String table : getTables(conn)) {
                    var sql = "SELECT count(*) FROM " + table;
                    try (var resultSet = statement.executeQuery(sql)) {
                        if (resultSet.next()) {
                            rows += resultSet.getInt(1);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Assertions.fail("Unable to load database in order to verify persistence. Are you using dataAccess.DatabaseManager to set your credentials?", ex);
        }

        return rows;
    }

    private List<String> getTables(Connection conn) throws SQLException {
        String sql = """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE();
                """;

        List<String> tableNames = new ArrayList<>();
        try (var preparedStatement = conn.prepareStatement(sql)) {
            try (var resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString(1));
                }
            }
        }

        return tableNames;
    }

}