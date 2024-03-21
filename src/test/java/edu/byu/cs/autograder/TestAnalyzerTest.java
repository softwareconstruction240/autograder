package edu.byu.cs.autograder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsFailingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertFalse(child.passed);
            assertNotNull(child.errorMessage);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(3, root.children.get("PawnMoveTests").children.size());
        assertEquals(1, root.children.get("RookMoveTests").children.size());
        assertEquals(1, root.children.get("CastlingTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            switch (child.testName) {
                case "pawnMiddleOfBoardWhite()", "edgePromotionBlack()" -> {
                    assertTrue(child.passed);
                    assertNull(child.errorMessage);
                }
                case "pawnCaptureBlack()" -> {
                    assertFalse(child.passed);
                    assertNotNull(child.errorMessage);
                }
                default -> fail("Unexpected test name: " + child.testName);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals(2, root.numTestsPassed);
        assertEquals(1, root.numTestsFailed);

        assertEquals(2, root.children.get("PawnMoveTests").numTestsPassed);
        assertEquals(0, root.children.get("PawnMoveTests").numTestsFailed);

        assertEquals(0, root.children.get("RookMoveTests").numTestsPassed);
        assertEquals(1, root.children.get("RookMoveTests").numTestsFailed);
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("QueenMoveTests").children.size());
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"), extraCreditTests, null);
        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(4, root.numTestsPassed);
        assertEquals(6, root.numExtraCreditPassed);
        assertEquals("CastlingTests", root.children.get("CastlingTests").ecCategory);
        assertEquals("EnPassantTests", root.children.get("EnPassantTests").ecCategory);
        assertNull(root.children.get("ChessGameTests").ecCategory);
    }
}