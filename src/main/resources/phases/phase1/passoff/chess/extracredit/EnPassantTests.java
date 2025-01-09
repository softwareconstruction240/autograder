package passoff.chess.extracredit;

import chess.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

/**
 * Tests if the ChessGame implementation can handle En Passant moves
 * En Passant is a situational move in chess taken directly after your opponent has double moved a pawn
 * If their pawn moves next to one of your pawns, so it passes where your pawn could have captured it, you
 * may capture their pawn with your pawn as if they had only moved a single space. You may only take this move
 * if you do so the turn directly following the pawns double move. This is as if you had caught their
 * pawn "in passing", or translated to French: "En Passant".
 */
public class EnPassantTests {

    @Test
    @DisplayName("White En Passant Right")
    public void enPassantWhiteRight() throws InvalidMoveException {
        ChessBoard board = TestUtilities.loadBoard("""
                | | | | | | | | |
                | | |p| | | | | |
                | | | | | | | | |
                | |P| | | | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """);
        ChessMove setupMove = new ChessMove(new ChessPosition(7, 3), new ChessPosition(5, 3), null);
        /*
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |P|p| | | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
         */

        ChessMove enPassantMove = new ChessMove(new ChessPosition(5, 2), new ChessPosition(6, 3), null);
        ChessBoard endBoard = TestUtilities.loadBoard("""
                | | | | | | | | |
                | | | | | | | | |
                | | |P| | | | | |
                | | | | | | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """);

        assertValidEnPassant(board, ChessGame.TeamColor.BLACK, setupMove, enPassantMove, endBoard);
    }


    @Test
    @DisplayName("White En Passant Left")
    public void enPassantWhiteLeft() throws InvalidMoveException {
        ChessBoard board = TestUtilities.loadBoard("""
                | | | | | | | | |
                | | |p| | | | | |
                | | | | | | | | |
                | | | |P| | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """);

        ChessMove setupMove = new ChessMove(new ChessPosition(7, 3), new ChessPosition(5, 3), null);
        /*
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | |p|P| | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
         */
        ChessMove enPassantMove = new ChessMove(new ChessPosition(5, 4), new ChessPosition(6, 3), null);
        ChessBoard endBoard = TestUtilities.loadBoard("""
                | | | | | | | | |
                | | | | | | | | |
                | | |P| | | | | |
                | | | | | | | | |
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                """);

        assertValidEnPassant(board, ChessGame.TeamColor.BLACK, setupMove, enPassantMove, endBoard);
    }


    @Test
    @DisplayName("Black En Passant Right")
    public void enPassantBlackRight() throws InvalidMoveException {
        ChessBoard board = TestUtilities.loadBoard("""
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | |p| | |
                | | | | | | | | |
                | | | | | | |P| |
                | | | | | | | | |
                """);
        ChessMove setupMove = new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null);
        /*
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | |p|P| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
         */
        ChessMove enPassantMove = new ChessMove(new ChessPosition(4, 6), new ChessPosition(3, 7), null);
        ChessBoard endBoard = TestUtilities.loadBoard("""
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | | | | |
                | | | | | | |p| |
                | | | | | | | | |
                | | | | | | | | |
                """);

        assertValidEnPassant(board, ChessGame.TeamColor.WHITE, setupMove, enPassantMove, endBoard);
    }


    @Test
    @DisplayName("Black En Passant Left")
    public void enPassantBlackLeft() throws InvalidMoveException {
        ChessBoard board = TestUtilities.loadBoard("""
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | | | |p|
                | | | | | | | | |
                | | | | | | |P| |
                | | | | | | | | |
                """);
        ChessMove setupMove = new ChessMove(new ChessPosition(2, 7), new ChessPosition(4, 7), null);
        /*
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | | |P|p|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
         */
        ChessMove enPassantMove = new ChessMove(new ChessPosition(4, 8), new ChessPosition(3, 7), null);
        ChessBoard endBoard = TestUtilities.loadBoard("""
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |K| | | | | | | |
                | | | | | | | | |
                | | | | | | |p| |
                | | | | | | | | |
                | | | | | | | | |
                """);
        assertValidEnPassant(board, ChessGame.TeamColor.WHITE, setupMove, enPassantMove, endBoard);
    }


    @Test
    @DisplayName("Can Only En Passant on Next Turn")
    public void missedEnPassant() throws InvalidMoveException {
        ChessBoard board = TestUtilities.loadBoard("""
                | | | | |k| | | |
                | | |p| | | | | |
                | | | | | | | |P|
                | |P| | | | | | |
                | | | | | | | | |
                | | | | | | | |p|
                | | | | | | | | |
                | | | |K| | | | |
                """);
        ChessGame game = new ChessGame();
        game.setBoard(board);
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        //move black piece 2 spaces
        game.makeMove(new ChessMove(new ChessPosition(7, 3), new ChessPosition(5, 3), null));
        /*
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | |P|
                | |P|p| | | | | |
                | | | | | | | | |
                | | | | | | | |p|
                | | | | | | | | |
                | | | |K| | | | |
         */

        //filler moves
        game.makeMove(new ChessMove(new ChessPosition(6, 8), new ChessPosition(7, 8), null));
        game.makeMove(new ChessMove(new ChessPosition(3, 8), new ChessPosition(2, 8), null));
        /*
                | | | | |k| | | |
                | | | | | | | |P|
                | | | | | | | | |
                | |P|p| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | |p|
                | | | |K| | | | |
         */

        //make sure pawn cannot do En Passant move
        ChessPosition enPassantPosition = new ChessPosition(5, 2);
        ChessMove enPassantMove = new ChessMove(enPassantPosition, new ChessPosition(6, 3), null);
        Assertions.assertFalse(game.validMoves(enPassantPosition).contains(enPassantMove),
                "ChessGame validMoves contained a En Passant move after the move became invalid");
    }

    private void assertValidEnPassant(ChessBoard board, ChessGame.TeamColor turn, ChessMove setupMove,
                                      ChessMove enPassantMove, ChessBoard endBoard) throws InvalidMoveException {
        ChessGame game = new ChessGame();
        game.setBoard(board);
        game.setTeamTurn(turn);

        //setup prior move for en passant
        game.makeMove(setupMove);

        //make sure pawn has En Passant move
        Assertions.assertTrue(game.validMoves(enPassantMove.getStartPosition()).contains(enPassantMove),
                "ChessGame validMoves did not contain a valid En Passant move");

        //en passant move works correctly
        Assertions.assertDoesNotThrow(() -> game.makeMove(enPassantMove));
        Assertions.assertEquals(endBoard, game.getBoard(), "Incorrect Board after En Passant Move");
    }

}
