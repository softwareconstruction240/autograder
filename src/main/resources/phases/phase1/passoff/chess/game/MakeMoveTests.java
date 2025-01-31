package passoff.chess.game;

import chess.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import passoff.chess.TestUtilities;

public class MakeMoveTests {
    private static final String WRONG_BOARD = "Board not correct after move made";
    private ChessGame game;

    @BeforeEach
    public void setUp() {
        game = new ChessGame();
        game.setTeamTurn(ChessGame.TeamColor.WHITE);
        game.setBoard(TestUtilities.defaultBoard());
    }

    @Test
    @DisplayName("Make Valid King Move")
    public void makeValidKingMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | | | |
                |p| | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | | | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        var kingStartPosition = new ChessPosition(1, 2);
        var kingEndPosition = new ChessPosition(1, 1);
        game.makeMove(new ChessMove(kingStartPosition, kingEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | | | | | | | | |
                |p| | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Valid Queen Move")
    public void makeValidQueenMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | |q| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| |k| | | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        var queenStartPosition = new ChessPosition(6, 7);
        var queenEndPosition = new ChessPosition(1, 2);
        game.makeMove(new ChessMove(queenStartPosition, queenEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K|q|k| | | | | |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Valid Rook Move")
    public void makeValidRookMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | |R|
                | | | | | | | | |
                |K| | | | | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        var rookStartPosition = new ChessPosition(3, 8);
        var rookEndPosition = new ChessPosition(7, 8);
        game.makeMove(new ChessMove(rookStartPosition, rookEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | | | | | | |R|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Valid Knight Move")
    public void makeValidKnightMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | |n| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | |P|
                | | | | |K| | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        var knightStartPosition = new ChessPosition(6, 3);
        var knightEndPosition = new ChessPosition(4, 4);
        game.makeMove(new ChessMove(knightStartPosition, knightEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | |n| | | | |
                | | | | | | | | |
                | | | | | | | |P|
                | | | | |K| | | |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Valid Bishop Move")
    public void makeValidBishopMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | |k| | | |
                |p| | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | |B| |K| | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        var bishopStartPosition = new ChessPosition(1, 3);
        var bishopEndPosition = new ChessPosition(6, 8);
        game.makeMove(new ChessMove(bishopStartPosition, bishopEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | | | | |k| | | |
                |p| | | | | | | |
                | | | | | | | |B|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Valid Pawn Move")
    public void makeValidPawnMove() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                | |k| | | | | | |
                | |p| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | |P| |
                | | | | | | |K| |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        var pawnStartPosition = new ChessPosition(7, 2);
        var pawnEndPosition = new ChessPosition(6, 2);
        game.makeMove(new ChessMove(pawnStartPosition, pawnEndPosition, null));

        Assertions.assertEquals(TestUtilities.loadBoard("""
                | |k| | | | | | |
                | | | | | | | | |
                | |p| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | |P| |
                | | | | | | |K| |
                """), game.getBoard(), WRONG_BOARD);
    }

    @Test
    @DisplayName("Make Move Changes Team Turn")
    public void makeMoveChangesTurn() throws InvalidMoveException {
        String failureMessage = "Team color not changed after move made";

        game.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        Assertions.assertEquals(ChessGame.TeamColor.BLACK, game.getTeamTurn(), failureMessage);

        game.makeMove(new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null));
        Assertions.assertEquals(ChessGame.TeamColor.WHITE, game.getTeamTurn(), failureMessage);
    }

    @Test
    @DisplayName("Invalid Make Move Too Far")
    public void invalidMakeMoveTooFar() {
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(2, 1), new ChessPosition(5, 1), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Pawn Diagonal No Capture")
    public void invalidMakeMovePawnDiagonalNoCapture() {
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(2, 1), new ChessPosition(3, 2), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Out Of Turn")
    public void invalidMakeMoveOutOfTurn() {
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(7, 5), new ChessPosition(6, 5), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Through Piece")
    public void invalidMakeMoveThroughPiece() {
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(1, 1), new ChessPosition(4, 1), null)));
    }

    @Test
    @DisplayName("Invalid Make Move No Piece")
    public void invalidMakeMoveNoPiece() {
        //starting position does not have a piece
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(4, 4), new ChessPosition(4, 5), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Invalid Move")
    public void invalidMakeMoveInvalidMove() {
        //not a move the piece can ever take
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(8, 7), new ChessPosition(5, 5), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Take Own Piece")
    public void invalidMakeMoveTakeOwnPiece() {
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(1, 3), new ChessPosition(2, 4), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Captured Piece")
    public void invalidMakeMoveCapturedPiece() throws InvalidMoveException {
        game.setBoard(TestUtilities.loadBoard("""
                |r|n|b|q|k|b|n|r|
                |p|p|p|p| |p|p|p|
                | | | | | | | | |
                | | | | |p| | | |
                | | | | | | | | |
                | | | | | |N| | |
                |P|P|P|P|P|P|P|P|
                |R|N|B|Q|K|B| |R|
                """));

        game.makeMove(new ChessMove(new ChessPosition(3, 6), new ChessPosition(5, 5), null));
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(5, 5), new ChessPosition(4, 5), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Jump Enemy")
    public void invalidMakeMoveJumpEnemy() {
        game.setBoard(TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| |r| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """));
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(5, 1), new ChessPosition(5, 5), null)));
    }

    @Test
    @DisplayName("Invalid Make Move In Check")
    public void invalidMakeMoveInCheck() {
        game.setBoard(TestUtilities.loadBoard("""
                |r|n| |q|k|b| |r|
                |p| |p|p|p|p|p|p|
                |b|p| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |P| | |B| |n| | |
                |R|P|P| | |P|P|P|
                | |N|B|Q|K| |R| |
                """));
        //try to make an otherwise valid move that doesn't remove check
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(1, 7), new ChessPosition(1, 8), null)));
    }

    @Test
    @DisplayName("Invalid Make Move Double Move Moved Pawn")
    public void invalidMakeMoveDoubleMoveMovedPawn() {
        game.setBoard(TestUtilities.loadBoard("""
                |r|n|b|q|k|b|n|r|
                |p| |p|p|p|p|p|p|
                | | | | | | | | |
                | |p| | | | | | |
                | | | | | | | | |
                | | | | | | |P| |
                |P|P|P|P|P|P| |P|
                |R|N|B|Q|K|B|N|R|
                """));
        Assertions.assertThrows(InvalidMoveException.class,
                () -> game.makeMove(new ChessMove(new ChessPosition(3, 7), new ChessPosition(5, 7), null)));
    }


    @ParameterizedTest
    @EnumSource(value = ChessPiece.PieceType.class, names = {"QUEEN", "ROOK", "KNIGHT", "BISHOP"})
    @DisplayName("Pawn Promotion")
    public void promotionMoves(ChessPiece.PieceType promotionType) throws InvalidMoveException {
        String pieceAtStart = "After move, a piece is still present in the start position";
        String noPieceAtEnd = "After move, no piece found at the end position";
        String incorrectType = "Found piece at end position is not the correct piece type";
        String incorrectColor = "Found piece at end position is the wrong team color";

        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | | | |
                | | |P| | | | | |
                | | | | | | |k| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | |p| | | |
                | | | | | |Q| | |
                """));

        //White promotion
        ChessMove whitePromotion = new ChessMove(new ChessPosition(7, 3), new ChessPosition(8, 3), promotionType);
        game.makeMove(whitePromotion);

        Assertions.assertNull(game.getBoard().getPiece(whitePromotion.getStartPosition()), pieceAtStart);
        ChessPiece whiteEndPiece = game.getBoard().getPiece(whitePromotion.getEndPosition());
        Assertions.assertNotNull(whiteEndPiece, noPieceAtEnd);
        Assertions.assertEquals(promotionType, whiteEndPiece.getPieceType(), incorrectType);
        Assertions.assertEquals(ChessGame.TeamColor.WHITE, whiteEndPiece.getTeamColor(), incorrectColor);


        //Black take + promotion
        game.setTeamTurn(ChessGame.TeamColor.BLACK);
        ChessMove blackPromotion = new ChessMove(new ChessPosition(2, 5), new ChessPosition(1, 6), promotionType);
        game.makeMove(blackPromotion);

        Assertions.assertNull(game.getBoard().getPiece(blackPromotion.getStartPosition()), pieceAtStart);
        ChessPiece blackEndPiece = game.getBoard().getPiece(blackPromotion.getEndPosition());
        Assertions.assertNotNull(blackEndPiece, noPieceAtEnd);
        Assertions.assertEquals(promotionType, blackEndPiece.getPieceType(), incorrectType);
        Assertions.assertEquals(ChessGame.TeamColor.BLACK, blackEndPiece.getTeamColor(), incorrectColor);
    }
}
