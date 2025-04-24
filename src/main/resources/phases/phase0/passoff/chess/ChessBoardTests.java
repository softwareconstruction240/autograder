package passoff.chess;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChessBoardTests extends EqualsTestingUtility<ChessBoard> {
    public ChessBoardTests() {
        super("ChessBoard", "boards");
    }

    @Test
    @DisplayName("Construct Empty ChessBoard")
    public void constructChessBoard() {
        ChessBoard board = new ChessBoard();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                Assertions.assertNull(
                        board.getPiece(new ChessPosition(row, col)),
                        "Immediately upon construction, a ChessBoard should be empty."
                );
            }
        }

    }

    @Test
    @DisplayName("Add and Get Piece")
    public void getAddPiece() {
        ChessPosition position = new ChessPosition(4, 4);
        ChessPiece piece = new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.BISHOP);

        var board = new ChessBoard();
        board.addPiece(position, piece);

        ChessPiece foundPiece = board.getPiece(position);

        Assertions.assertNotNull(foundPiece, "getPiece returned null for a position just added");
        Assertions.assertEquals(piece.getPieceType(), foundPiece.getPieceType(),
                "ChessPiece returned by getPiece had the wrong piece type");
        Assertions.assertEquals(piece.getTeamColor(), foundPiece.getTeamColor(),
                "ChessPiece returned by getPiece had the wrong team color");
    }

    @Test
    @DisplayName("Reset Board")
    public void defaultGameBoard() {
        var expectedBoard = TestUtilities.defaultBoard();

        var actualBoard = new ChessBoard();
        actualBoard.resetBoard();

        Assertions.assertEquals(expectedBoard, actualBoard, "Reset board did not create the correct board");
    }

    @Override
    protected ChessBoard buildOriginal() {
        var basicBoard = new ChessBoard();
        basicBoard.resetBoard();
        return basicBoard;
    }

    @Override
    protected Collection<ChessBoard> buildAllDifferent() {
        List<ChessBoard> differentBoards = new ArrayList<>();

        differentBoards.add(new ChessBoard()); // An empty board

        ChessPiece.PieceType[] pieceSchedule = {
                ChessPiece.PieceType.ROOK, ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.BISHOP, ChessPiece.PieceType.QUEEN,
                ChessPiece.PieceType.KING, ChessPiece.PieceType.PAWN,
                ChessPiece.PieceType.KING, ChessPiece.PieceType.ROOK,
        };

        // Generate boards each with one piece added from a static list.
        // The color is assigned in a mixed pattern.
        ChessPiece.PieceType type;
        boolean isWhite;
        for (int col = 1; col <= 8; col++) {
            for (int row = 1; row <= 8; row++) {
                type = pieceSchedule[row-1];
                isWhite = (row + col) % 2 == 0;
                differentBoards.add(createBoardWithPiece(row, col, type, isWhite));
            }
        }

        return differentBoards;
    }

    private ChessBoard createBoardWithPiece(int row, int col, ChessPiece.PieceType type, boolean isWhite) {
        var board = new ChessBoard();

        var teamColor = isWhite ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
        var piece = new ChessPiece(teamColor, type);

        var position = new ChessPosition(row, col);
        board.addPiece(position, piece);

        return board;
    }

}
