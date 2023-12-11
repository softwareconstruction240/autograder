package edu.byu.cs.autograder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestAnalyzerTest {

    @Test
    @DisplayName("All tests pass")
    void parse__all_tests_pass() {
        String testsPassingInput =
                """
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                """;

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"));

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
        }
    }

    @Test
    @DisplayName("All tests fail")
    void parse__all_tests_fail() {
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsFailingInput.split("\n"));

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertFalse(child.passed);
            assertNotNull(child.errorMessage);
        }
    }

    @Test
    @DisplayName("Some tests pass, some fail")
    void parse__some_pass_some_fail() {
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"));

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
    void parse__starts_with_non_test_content() {
        String testsPassingInput =
                """
                Thanks for using JUnit! Support its development at https://junit.org/sponsoring
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: STARTED
                JUnit Jupiter > PawnMoveTests > pawnMiddleOfBoardWhite() :: SUCCESSFUL
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: STARTED
                JUnit Jupiter > PawnMoveTests > edgePromotionBlack() :: SUCCESSFUL
                """;

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"));

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
        }
    }

    @Test
    @DisplayName("Test input ends with non-test content")
    void parse__ends_with_non_test_content() {
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

        TestAnalyzer.TestNode root = new TestAnalyzer().parse(testsPassingInput.split("\n"));

        assertEquals("JUnit Jupiter", root.testName);
        assertEquals(2, root.children.get("PawnMoveTests").children.size());

        for (TestAnalyzer.TestNode child : root.children.get("PawnMoveTests").children.values()) {
            assertTrue(child.passed);
            assertNull(child.errorMessage);
        }
    }
}