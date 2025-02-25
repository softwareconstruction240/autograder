package passoff.chess.piece;

import chess.ChessPosition;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

public class KingMoveTests {

    @Test
    public void kingMiddleOfBoard() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | |K| | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(3, 6),
                new int[][]{{4, 6}, {4, 7}, {3, 7}, {2, 7}, {2, 6}, {2, 5}, {3, 5}, {4, 5}}
        );
    }


    @Test
    public void kingCaptureEnemy() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |N|n| | | |
                        | | | |k| | | | |
                        | | |P|b|p| | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(3, 4),
                new int[][]{{4, 4}, {3, 5}, {2, 3}, {3, 3}, {4, 3}}
        );
    }


    @Test
    public void kingBlocked() {
        TestUtilities.validateMoves("""
                        | | | | | | |r|k|
                        | | | | | | |p|p|
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(8, 8),
                new int[][]{}
        );
    }

}
