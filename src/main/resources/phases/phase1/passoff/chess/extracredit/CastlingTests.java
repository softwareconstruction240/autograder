package passoff.chess.extracredit;

import chess.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

/**
 * Tests if the ChessGame implementation can handle Castling moves
 * Castling is a situational move the king can make as it's first move. If one of the rooks has not yet moved
 * and there are no pieces between the rook and the king, and the path is "safe", the king can castle. Castling is
 * performed by moving the king 2 spaces towards the qualifying rook, and the rook "jumping" the king to sit next
 * to the king on the opposite side it was previously. A path is considered "safe" if 1: the king is not in check
 * and 2: neither the space the king moves past nor the space the king ends up at can be reached by an opponents piece.
 */
public class CastlingTests {
    private static final String INVALID_CASTLE_PRESENT = "ChessGame validMoves contained an invalid castling move";
    private static final String VALID_CASTLE_MISSING = "ChessGame validMoves did not contain valid castle move";
    private static final String INCORRECT_BOARD = "Wrong board after castle move made";

    private static final ChessPosition WHITE_KING_POSITION = new ChessPosition(1, 5);
    private static final ChessMove WHITE_QUEENSIDE_CASTLE = new ChessMove(WHITE_KING_POSITION, new ChessPosition(1, 3), null);
    private static final ChessMove WHITE_KINGSIDE_CASTLE = new ChessMove(WHITE_KING_POSITION, new ChessPosition(1, 7), null);

    private static final ChessPosition BLACK_KING_POSITION = new ChessPosition(8, 5);
    private static final ChessMove BLACK_QUEENSIDE_CASTLE = new ChessMove(BLACK_KING_POSITION, new ChessPosition(8, 3), null);
    private static final ChessMove BLACK_KINGSIDE_CASTLE = new ChessMove(BLACK_KING_POSITION, new ChessPosition(8, 7), null);


    @Test
    @Order(0)
    @DisplayName("White Team Can Castle")
    public void castleWhite() {
        ChessGame game1 = createNewGameWithBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
                """, ChessGame.TeamColor.WHITE);

        //check that with nothing in way, king can castle
        assertWhiteCanCastle(game1, true, true);

        //queen side castle works correctly
        makeMoveAndAssertBoard(game1, WHITE_QUEENSIDE_CASTLE, """
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | |K|R| | | |R|
                """);

        //reset board
        ChessGame game2 = createNewGameWithBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
                """, ChessGame.TeamColor.WHITE);

        //king side castle works correctly
        makeMoveAndAssertBoard(game2, WHITE_KINGSIDE_CASTLE, """
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | | |R|K| |
                """);
    }

    @Test
    @Order(0)
    @DisplayName("Black Team Can Castle")
    public void castleBlack() {
        ChessGame game1 = createNewGameWithBoard("""
                |r| | | |k| | |r|
                | |p| | | | | |q|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                |R| | | | | | | |
                """, ChessGame.TeamColor.BLACK);

        //check that with nothing in way, king can castle
        assertBlackCanCastle(game1, true, true);

        //queen side castle works correctly
        makeMoveAndAssertBoard(game1, BLACK_QUEENSIDE_CASTLE, """
                | | |k|r| | | |r|
                | |p| | | | | |q|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                |R| | | | | | | |
                """);


        //reset board
        ChessGame game2 = createNewGameWithBoard("""
                |r| | | |k| | |r|
                | |p| | | | | |q|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                |R| | | | | | | |
                """, ChessGame.TeamColor.BLACK);

        //king side castle works correctly
        makeMoveAndAssertBoard(game2, BLACK_KINGSIDE_CASTLE, """
                |r| | | | |r|k| |
                | |p| | | | | |q|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |K| | | |
                |R| | | | | | | |
                """);
    }

    @Test
    @Order(1)
    @DisplayName("Cannot Castle After Moving")
    public void noCastleAfterMove() throws InvalidMoveException {
        ChessGame game = createNewGameWithBoard("""
                | | |k| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
                """, ChessGame.TeamColor.WHITE);

        //move left rook
        game.makeMove(new ChessMove(new ChessPosition(1, 1), new ChessPosition(1, 4), null));
        game.makeMove(new ChessMove(new ChessPosition(8, 3), new ChessPosition(8, 2), null));

        //move rook back to starting spot
        game.makeMove(new ChessMove(new ChessPosition(1, 4), new ChessPosition(1, 1), null));
        /*
                | |k| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
         */


        //make sure king can't castle towards moved rook, but still can to unmoved rook
        assertWhiteCanCastle(game, false, true);

        //move king
        game.makeMove(new ChessMove(new ChessPosition(8, 2), new ChessPosition(8, 3), null));
        game.makeMove(new ChessMove(WHITE_KING_POSITION, new ChessPosition(1, 6), null));
        /*
                | | |k| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | | |K| |R|
         */

        //move king back to starting position
        game.makeMove(new ChessMove(new ChessPosition(8, 3), new ChessPosition(8, 4), null));
        game.makeMove(new ChessMove(new ChessPosition(1, 6), WHITE_KING_POSITION, null));
        /*
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
         */

        //make sure king can't castle anymore
        assertWhiteCanCastle(game, false, false);
    }

    @Test
    @Order(2)
    @DisplayName("Cannot Castle Through Pieces")
    public void noCastleThroughPieces() {
        ChessGame game = createNewGameWithBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| |B| |K| |Q|R|
                """, ChessGame.TeamColor.WHITE);

        //make sure king cannot castle
        assertWhiteCanCastle(game, false, false);
    }

    @Test
    @Order(3)
    @DisplayName("Cannot Castle From Check")
    public void noCastleFromCheck() {
        ChessGame game = createNewGameWithBoard("""
                |r| | | |k| | |r|
                | | | | | | |N| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                """, ChessGame.TeamColor.BLACK);

        //make sure king cannot castle on either side
        assertBlackCanCastle(game, false, false);
    }

    @Test
    @Order(3)
    @DisplayName("Cannot Castle Through Check")
    public void noCastleThroughCheck() {
        ChessGame game = createNewGameWithBoard("""
                |r| | | |k| | |r|
                | | | | | | | | |
                | |B| | | |R| | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                """, ChessGame.TeamColor.BLACK);

        //make sure king cannot castle on either side
        assertBlackCanCastle(game, false, false);
    }

    @Test
    @Order(3)
    @DisplayName("Cannot Castle Into Check")
    public void noCastleIntoCheck() {
        ChessGame game1 = createNewGameWithBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | |r| | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
                """, ChessGame.TeamColor.WHITE);

        // King cannot castle into check
        assertWhiteCanCastle(game1, false, true);


        // Try again in the other direction
        ChessGame game2 = createNewGameWithBoard("""
                | | | | |k| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | |r| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |R| | | |K| | |R|
                """, ChessGame.TeamColor.WHITE);

        // King cannot castle into check
        assertWhiteCanCastle(game2, true, false);
    }


    private ChessGame createNewGameWithBoard(String boardText, ChessGame.TeamColor teamTurn) {
        ChessBoard board = TestUtilities.loadBoard(boardText);
        ChessGame game = new ChessGame();
        game.setBoard(board);
        game.setTeamTurn(teamTurn);
        return game;
    }


    /**
     * Asserts that WHITE can or cannot make the queenside & kingside castle moves.
     * <br>
     * The parameter order aligns with the board when drawn from WHITE's perspective:
     * <ul>
     *     <li><b>Queenside</b> is to the <i>left</i></li>
     *     <li><b>Kingside</b> is to the <i>right</i></li>
     * </ul>
     */
    private void assertWhiteCanCastle(ChessGame game, boolean allowQueensideCastle, boolean allowKingsideCastle) {
        assertCanCastle(game, allowQueensideCastle, allowKingsideCastle, WHITE_KING_POSITION, WHITE_QUEENSIDE_CASTLE, WHITE_KINGSIDE_CASTLE);
    }
    /**
     * Asserts that BLACK can or cannot make the queenside & kingside castle moves.
     * <br>
     * The parameter order aligns with the board when drawn from WHITE's perspective:
     * <ul>
     *     <li><b>Kingside</b> is to the <i>left</i></li>
     *     <li><b>Queenside</b> is to the <i>right</i></li>
     * </ul>
     */
    private void assertBlackCanCastle(ChessGame game, boolean allowKingsideCastle, boolean allowQueensideCastle) {
        assertCanCastle(game, allowQueensideCastle, allowKingsideCastle, BLACK_KING_POSITION, BLACK_QUEENSIDE_CASTLE, BLACK_KINGSIDE_CASTLE);
    }
    private void assertCanCastle(ChessGame game, boolean allowQueensideCastle, boolean allowKingsideCastle,
                                 ChessPosition kingPosition, ChessMove queensideCastleMove, ChessMove kingsideCastleMove) {
        Assertions.assertEquals(allowQueensideCastle,
                game.validMoves(kingPosition).contains(queensideCastleMove),
                allowQueensideCastle ? VALID_CASTLE_MISSING : INVALID_CASTLE_PRESENT);
        Assertions.assertEquals(allowKingsideCastle,
                game.validMoves(kingPosition).contains(kingsideCastleMove),
                allowKingsideCastle ? VALID_CASTLE_MISSING : INVALID_CASTLE_PRESENT);
    }

    private void makeMoveAndAssertBoard(ChessGame game, ChessMove move, String boardText) {
        Assertions.assertDoesNotThrow(() -> game.makeMove(move));
        Assertions.assertEquals(TestUtilities.loadBoard(boardText), game.getBoard(), INCORRECT_BOARD);
    }

}
