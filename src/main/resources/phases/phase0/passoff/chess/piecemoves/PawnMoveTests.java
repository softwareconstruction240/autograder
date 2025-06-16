package passoff.chess.piecemoves;

import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

import java.util.ArrayList;

public class PawnMoveTests {

    @Test
    public void pawnMiddleOfBoardWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |P| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 4),
                new int[][]{{5, 4}}
        );
    }

    @Test
    public void pawnMiddleOfBoardBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |p| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 4),
                new int[][]{{3, 4}}
        );
    }


    @Test
    public void pawnInitialMoveWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | |P| | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(2, 5),
                new int[][]{{3, 5}, {4, 5}}
        );
    }

    @Test
    public void pawnInitialMoveBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | |p| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 3),
                new int[][]{{6, 3}, {5, 3}}
        );
    }


    @Test
    public void pawnPromotionWhite() {
        validatePromotion("""
                        | | | | | | | | |
                        | | |P| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 3),
                new int[][]{{8, 3}}
        );
    }


    @Test
    public void edgePromotionBlack() {
        validatePromotion("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | |p| | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(2, 3),
                new int[][]{{1, 3}}
        );
    }


    @Test
    public void pawnPromotionCapture() {
        validatePromotion("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | |p| | | | | | |
                        |N| | | | | | | |
                        """,
                new ChessPosition(2, 2),
                new int[][]{{1, 1}, {1, 2}}
        );
    }


    @Test
    public void pawnAdvanceBlockedWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |n| | | | |
                        | | | |P| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 4),
                new int[][]{}
        );
    }

    @Test
    public void pawnAdvanceBlockedBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |p| | | | |
                        | | | |r| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 4),
                new int[][]{}
        );
    }


    @Test
    public void pawnAdvanceBlockedDoubleMoveWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | |p| |
                        | | | | | | | | |
                        | | | | | | |P| |
                        | | | | | | | | |
                        """,
                new ChessPosition(2, 7),
                new int[][]{{3, 7}}
        );
    }

    @Test
    public void pawnAdvanceBlockedDoubleMoveBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | |p| | | | | |
                        | | |p| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 3),
                new int[][]{}
        );
    }


    @Test
    public void pawnCaptureWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | |r| |N| | | |
                        | | | |P| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 4),
                new int[][]{{5, 3}, {5, 4}}
        );
    }

    @Test
    public void pawnCaptureBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |p| | | | |
                        | | | |n|R| | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 4),
                new int[][]{{3, 5}}
        );
    }

    @Test
    public void pawnMoveFromEdgeWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | |P|
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 8),
                new int[][]{{5, 8}}
        );
    }

    @Test
    public void pawnMoveFromEdgeBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | |p|
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 8),
                new int[][]{{4, 8}}
        );
    }

    @Test
    public void pawnCaptureFromEdgeWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | |r| |
                        | | | | | | | |P|
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(4, 8),
                new int[][]{{5, 8}, {5, 7}}
        );
    }

    @Test
    public void pawnCaptureFromEdgeBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | |p|
                        | | | | | | |R| |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(5, 8),
                new int[][]{{4, 8}, {4, 7}}
        );
    }

    @Test
    public void pawnCaptureFromStartWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | |r| | | | | |
                        | | |r| | | | | |
                        | | | |P| | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(2, 4),
                new int[][]{{3, 3}, {3, 4}, {4, 4}}
        );
    }

    @Test
    public void pawnCaptureFromStartBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | |p| | | | |
                        | | |R| | | | | |
                        | | |R| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 4),
                new int[][]{{6, 4}, {5, 4}, {6, 3}}
        );
    }

    @Test
    public void captureAndPromoteWhite() {
        validatePromotion("""
                        | | |r| | | | | |
                        | | | |P| | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(7, 4),
                new int[][]{{8, 3}, {8, 4}}
        );
    }

    @Test
    public void captureAndPromoteBlack() {
        validatePromotion("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | |p| | | | |
                        | | |R| | | | | |
                        """,
                new ChessPosition(2, 4),
                new int[][]{{1, 3}, {1, 4}}
        );
    }

    @Test
    public void pawnCannotCaptureBackwardWhite() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | |P| | | | | |
                        | | |r|r| | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(3, 3),
                new int[][]{{4, 3}}
        );
    }

    @Test
    public void pawnCannotCaptureBackwardBlack() {
        TestUtilities.validateMoves("""
                        | | | | | | | | |
                        | | |R|R| | | | |
                        | | |p| | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        | | | | | | | | |
                        """,
                new ChessPosition(6, 3),
                new int[][]{{5, 3}}
        );
    }


    private static void validatePromotion(String boardText, ChessPosition startingPosition, int[][] endPositions) {
        var board = TestUtilities.loadBoard(boardText);
        var testPiece = board.getPiece(startingPosition);
        Assertions.assertNotNull(testPiece, "Could not find piece on board");
        var validMoves = new ArrayList<ChessMove>();
        for (var endPosition : endPositions) {
            var end = new ChessPosition(endPosition[0], endPosition[1]);
            validMoves.add(new ChessMove(startingPosition, end, ChessPiece.PieceType.QUEEN));
            validMoves.add(new ChessMove(startingPosition, end, ChessPiece.PieceType.BISHOP));
            validMoves.add(new ChessMove(startingPosition, end, ChessPiece.PieceType.ROOK));
            validMoves.add(new ChessMove(startingPosition, end, ChessPiece.PieceType.KNIGHT));
        }

        TestUtilities.validateMoves(board, testPiece, startingPosition, validMoves);
    }

}
