package passoff.chess.piece;

import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

public class QueenMoveTests {
    @Test
    public void queenMoveUntilEdge() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | |q| |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 7),
                new int[][]{
                        {8, 7},
                        {8, 8},
                        {7, 8},
                        {6, 8},
                        {6, 7}, {5, 7}, {4, 7}, {3, 7}, {2, 7}, {1, 7},
                        {6, 6}, {5, 5}, {4, 4}, {3, 3}, {2, 2}, {1, 1},
                        {7, 6}, {7, 5}, {7, 4}, {7, 3}, {7, 2}, {7, 1},
                        {8, 6},
                }
        );
    }


    @Test
    public void queenCaptureEnemy() {
        TestUtilities.validateMoves("""
                        |b| | | | | | | |
                        | | | | | | | | |
                        | | |R| | | | | |
                        | | | | | | | | |
                        |Q| | |p| | | | |
                        | | | | | | | | |
                        |P| |n| | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 1),
                new int[][]{
                        {5, 1}, {6, 1}, {7, 1}, {8, 1},
                        {5, 2},
                        {4, 2}, {4, 3}, {4, 4},
                        {3, 1}, {3, 2},
                        {2, 3},
                }
        );
    }


    @Test
    public void queenBlocked() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        |P|R| | | | | | |
                        |Q|K| | | | | | |
                        """,
                new ChessPosition(1, 1),
                new int[][]{}
        );
    }
}