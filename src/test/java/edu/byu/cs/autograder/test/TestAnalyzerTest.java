package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.TestAnalysis;
import edu.byu.cs.model.TestNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestAnalyzerTest {

    private Set<String> extraCreditTests;

    @BeforeEach
    void setup() {
        extraCreditTests = new HashSet<>();
    }

    @Test
    @DisplayName("All tests pass")
    void parse__all_tests_pass() throws GradingException {
        String testsPassingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(2, root.getChildren().get("PawnMoveTests").getChildren().size());

        for (TestNode child : root.getChildren().get("PawnMoveTests").getChildren().values()) {
            assertTrue(child.getPassed());
            assertNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("All tests fail")
    void parse__all_tests_fail() throws GradingException {
        String testsFailingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnCaptureBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnCaptureBlack() :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                        at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureBlack(PawnMoveTests.java:227)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                JUnit Jupiter > PawnMoveTests > pawnCaptureWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnCaptureWhite() :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                        at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureWhite(PawnMoveTests.java:210)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                """;

        TestNode root = new TestAnalyzer().parse(testsFailingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(2, root.getChildren().get("PawnMoveTests").getChildren().size());

        for (TestNode child : root.getChildren().get("PawnMoveTests").getChildren().values()) {
            assertFalse(child.getPassed());
            assertNotNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Some tests pass, some fail")
    void parse__some_pass_some_fail() throws GradingException {
        String testsPassingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > pawnCaptureBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnCaptureBlack() :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                        at passoffTests.chessTests.chessPieceTests.PawnMoveTests.pawnCaptureBlack(PawnMoveTests.java:227)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                JUnit Jupiter > RookMoveTests > rookBlocked() :: STARTED
                JUnit Jupiter > RookMoveTests > rookBlocked() :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                        at passoffTests.chessTests.chessPieceTests.RookMoveTests.rookBlocked(RookMoveTests.java:57)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: STARTED
                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.chessTests.chessExtraCredit.CastlingTests.castlingBlockedByEnemy(CastlingTests.java:306)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(3, root.getChildren().get("PawnMoveTests").getChildren().size());
        assertEquals(1, root.getChildren().get("RookMoveTests").getChildren().size());
        assertEquals(1, root.getChildren().get("CastlingTests").getChildren().size());

        for (TestNode child : root.getChildren().get("PawnMoveTests").getChildren().values()) {
            switch (child.getTestName()) {
                case "pawnMiddleOfBoardWhite()", "edgePromotionBlack()" -> {
                    assertTrue(child.getPassed());
                    assertNull(child.getErrorMessage());
                }
                case "pawnCaptureBlack()" -> {
                    assertFalse(child.getPassed());
                    assertNotNull(child.getErrorMessage());
                }
                default -> fail("Unexpected test name: " + child.getTestName());
            }
        }
    }

    @Test
    @DisplayName("Test input starts with non-test content")
    void parse__starts_with_non_test_content() throws GradingException {
        String testsPassingInput =
                """
                Thanks for using JUnit! Support its development at https://junit.org/sponsoring
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(2, root.getChildren().get("PawnMoveTests").getChildren().size());

        for (TestNode child : root.getChildren().get("PawnMoveTests").getChildren().values()) {
            assertTrue(child.getPassed());
            assertNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Test input ends with non-test content")
    void parse__ends_with_non_test_content() throws GradingException {
        String testsPassingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                
                Test run finished after 117 ms
                [        11 containers found      ]
                [         0 containers skipped    ]
                [        11 containers started    ]
                [         0 containers aborted    ]
                [        11 containers successful ]
                [         0 containers failed     ]
                [        47 tests found           ]
                [         0 tests skipped         ]
                [        47 tests started         ]
                [         0 tests aborted         ]
                [         0 tests successful      ]
                [        47 tests failed          ]
                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(2, root.getChildren().get("PawnMoveTests").getChildren().size());

        for (TestNode child : root.getChildren().get("PawnMoveTests").getChildren().values()) {
            assertTrue(child.getPassed());
            assertNull(child.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Counts are correct")
    void TestNode__counts_are_correct() throws GradingException {
        String testsPassingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                JUnit Jupiter > RookMoveTests > rookBlocked() :: STARTED
                JUnit Jupiter > RookMoveTests > rookBlocked() :: FAILED
                    java.lang.RuntimeException: Not implemented
                        at chess.ChessBoard.addPiece(ChessBoard.java:22)
                        at passoffTests.TestFactory.loadBoard(TestFactory.java:104)
                        at passoffTests.TestFactory.validateMoves(TestFactory.java:70)
                        at passoffTests.chessTests.chessPieceTests.RookMoveTests.rookBlocked(RookMoveTests.java:57)
                        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals(2, root.getNumTestsPassed());
        assertEquals(1, root.getNumTestsFailed());

        assertEquals(2, root.getChildren().get("PawnMoveTests").getNumTestsPassed());
        assertEquals(0, root.getChildren().get("PawnMoveTests").getNumTestsFailed());

        assertEquals(0, root.getChildren().get("RookMoveTests").getNumTestsPassed());
        assertEquals(1, root.getChildren().get("RookMoveTests").getNumTestsFailed());
    }

    @Test
    @DisplayName("Escape code are ignored")
    void parse__escape_codes_are_ignored() throws GradingException {
        String testsPassingInput =
                """
                Unit Jupiter > QueenMoveTests > queenMoveUntilEdge() :: STARTED
                [32mJUnit Jupiter > QueenMoveTests > queenMoveUntilEdge() :: SUCCESSFUL[0m
                JUnit Jupiter > QueenMoveTests > queenCaptureEnemy() :: STARTED
                [32mJUnit Jupiter > QueenMoveTests > queenCaptureEnemy() :: SUCCESSFUL[0m
                """;

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();

        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(2, root.getChildren().get("QueenMoveTests").getChildren().size());
    }

    @Test
    @DisplayName("Extra credit all successful")
    void parse__ec_all_success() throws GradingException {
        String testsPassingInput =
                """
                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: STARTED
                JUnit Jupiter > CastlingTests > Cannot Castle in Check :: SUCCESSFUL
                JUnit Jupiter > CastlingTests > Black Team Castle :: STARTED
                JUnit Jupiter > CastlingTests > Black Team Castle :: SUCCESSFUL
                JUnit Jupiter > CastlingTests > White Team Castle :: STARTED
                JUnit Jupiter > CastlingTests > White Team Castle :: SUCCESSFUL
                JUnit Jupiter > EnPassantTests > Black En Passant Left :: STARTED
                JUnit Jupiter > EnPassantTests > Black En Passant Left :: SUCCESSFUL
                JUnit Jupiter > EnPassantTests > White En Passant Right :: STARTED
                JUnit Jupiter > EnPassantTests > White En Passant Right :: SUCCESSFUL
                JUnit Jupiter > EnPassantTests > White En Passant Left :: STARTED
                JUnit Jupiter > EnPassantTests > White En Passant Left :: SUCCESSFUL
                JUnit Jupiter > ChessGameTests > Black in Check :: STARTED
                JUnit Jupiter > ChessGameTests > Black in Check :: SUCCESSFUL
                JUnit Jupiter > ChessGameTests > White in Checkmate :: STARTED
                JUnit Jupiter > ChessGameTests > White in Checkmate :: SUCCESSFUL
                JUnit Jupiter > ChessGameTests > Normal Make Move :: STARTED
                JUnit Jupiter > ChessGameTests > Normal Make Move :: SUCCESSFUL
                JUnit Jupiter > ChessGameTests > White in Check :: STARTED
                JUnit Jupiter > ChessGameTests > White in Check :: SUCCESSFUL
                """;
        extraCreditTests.add("CastlingTests");
        extraCreditTests.add("EnPassantTests");

        TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null).root();
        assertEquals("JUnit Jupiter", root.getTestName());
        assertEquals(4, root.getNumTestsPassed());
        assertEquals(6, root.getNumExtraCreditPassed());
        assertEquals("CastlingTests", root.getChildren().get("CastlingTests").getEcCategory());
        assertEquals("EnPassantTests", root.getChildren().get("EnPassantTests").getEcCategory());
        assertNull(root.getChildren().get("ChessGameTests").getEcCategory());
    }

    @Test
    @DisplayName("No parseable output")
    void parse__nothing() throws GradingException {
        String testOutput =
                """
                
                Thanks for using JUnit! Support its development at https://junit.org/sponsoring
                
                """;

        String errorOutput =
                """
                        java.io.IOException: Failed to bind to /0.0.0.0:8080
                               at org.eclipse.jetty.server.ServerConnector.openAcceptChannel(ServerConnector.java:349)
                               at org.eclipse.jetty.server.ServerConnector.open(ServerConnector.java:310)
                               at org.eclipse.jetty.server.AbstractNetworkConnector.doStart(AbstractNetworkConnector.java:80)
                               at org.eclipse.jetty.server.ServerConnector.doStart(ServerConnector.java:234)
                               at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:72)
                               at org.eclipse.jetty.server.Server.doStart(Server.java:386)
                               at org.eclipse.jetty.util.component.AbstractLifeCycle.start(AbstractLifeCycle.java:72)
                               at spark.embeddedserver.jetty.EmbeddedJettyServer.ignite(EmbeddedJettyServer.java:149)
                               at spark.Service.lambda$init$2(Service.java:632)
                               at java.base/java.lang.Thread.run(Thread.java:1583)
                        Caused by: java.net.BindException: Address already in use
                               at java.base/sun.nio.ch.Net.bind0(Native Method)
                               at java.base/sun.nio.ch.Net.bind(Net.java:565)
                               at java.base/sun.nio.ch.ServerSocketChannelImpl.netBind(ServerSocketChannelImpl.java:344)
                               at java.base/sun.nio.ch.ServerSocketChannelImpl.bind(ServerSocketChannelImpl.java:301)
                               at java.base/sun.nio.ch.ServerSocketAdaptor.bind(ServerSocketAdaptor.java:89)
                               at org.eclipse.jetty.server.ServerConnector.openAcceptChannel(ServerConnector.java:345)
                               ... 9 more
                """;

        TestAnalysis analysis = new TestAnalyzer().parse(testOutput.split("\n"), extraCreditTests, errorOutput);
        assertNull(analysis.root());
        assertEquals(errorOutput, analysis.error());
    }
}