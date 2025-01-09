package passoff.chess.piece;

import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

public class KnightMoveTests {

    @Test
    public void knightMiddleOfBoardWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | |N| | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 5),
                new int[][]{
                        {7, 6}, {6, 7}, {4, 7}, {3, 6}, {3, 4}, {4, 3}, {6, 3}, {7, 4},
                }
        );
    }

    @Test
    public void knightMiddleOfBoardBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | |n| | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 5),
                new int[][]{
                        {7, 6}, {6, 7}, {4, 7}, {3, 6}, {3, 4}, {4, 3}, {6, 3}, {7, 4},
                }
        );
    }


    @Test
    public void knightEdgeOfBoardLeft() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        |n| | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 1),
                new int[][]{{6, 2}, {5, 3}, {3, 3}, {2, 2}}
        );
    }

    @Test
    public void knightEdgeOfBoardRight() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | |n|
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(3, 8),
                new int[][]{{1, 7}, {2, 6}, {4, 6}, {5, 7}}
        );
    }

    @Test
    public void knightEdgeOfBoardBottom() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | |N| | |
                        """,
                new ChessPosition(1, 6),
                new int[][]{{2, 4}, {3, 5}, {3, 7}, {2, 8}}
        );
    }

    @Test
    public void knightEdgeOfBoardTop() {
        TestUtilities.validateMoves("""
                        | | |N| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(8, 3),
                new int[][]{{7, 5}, {6, 4}, {6, 2}, {7, 1}}
        );
    }


    @Test
    public void knightCornerOfBoardBottomRight() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | |N|
                        """,
                new ChessPosition(1, 8),
                new int[][]{{2, 6}, {3, 7}}
        );
    }

    @Test
    public void knightCornerOfBoardTopRight() {
        TestUtilities.validateMoves("""
                        | | | | | | | |N|
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(8, 8),
                new int[][]{{6, 7}, {7, 6}}
        );
    }

    @Test
    public void knightCornerOfBoardTopLeft() {
        TestUtilities.validateMoves("""
                        |n| | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(8, 1),
                new int[][]{{7, 3}, {6, 2}}
        );
    }

    @Test
    public void knightCornerOfBoardBottomLeft() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        |n| | | | | | | |
                        """,
                new ChessPosition(1, 1),
                new int[][]{{2, 3}, {3, 2}}
        );
    }

    @Test
    public void knightSurroundedButNotBlocked() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |R|R|R| | |
                        | | | |R|N|R| | |
                        | | | |R|R|R| | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 5),
                new int[][]{
                        {7, 6}, {6, 7}, {4, 7}, {3, 6}, {3, 4}, {4, 3}, {6, 3}, {7, 4},
                }
        );
    }

    @Test
    public void knightBlocked() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | |R| | | | |
                        | | | | | | |P| |
                        | | | | |N| | | |
                        | | |N| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 5),
                new int[][]{{3, 4}, {3, 6}, {4, 7}, {7, 6}, {6, 3}}
        );
    }


    @Test
    public void knightCaptureEnemy() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | |n| | | |
                        | | |N| | | | | |
                        | | | |P| |R| | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 5),
                new int[][]{{7, 6}, {6, 7}, {4, 7}, {3, 6}, {3, 4}, {4, 3}, {6, 3}, {7, 4}}
        );
    }
}
