package passoff.chess.game;

import chess.ChessGame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import passoff.chess.TestUtilities;

public class GameStatusTests {
    static final String INCORRECT_BLACK_CHECK = "Black is not in check but isInCheck returned true";
    static final String INCORRECT_WHITE_CHECK = "White is not in check but isInCheck returned true";
    static final String INCORRECT_BLACK_CHECKMATE = "Black is not in checkmate but isInCheckmate returned true";
    static final String INCORRECT_WHITE_CHECKMATE = "White is not in checkmate but isInCheckmate returned true";
    static final String INCORRECT_BLACK_STALEMATE = "Black is not in stalemate but isInStalemate returned true";
    static final String INCORRECT_WHITE_STALEMATE = "White is not in stalemate but isInStalemate returned true";
    static final String MISSING_BLACK_CHECK = "White is in check but isInCheck returned false";
    static final String MISSING_BLACK_CHECKMATE = "Black is in checkmate but isInCheckmate returned false";
    static final String MISSING_WHITE_CHECKMATE = "White is in checkmate but isInCheckmate returned false";
    static final String MISSING_WHITE_STALEMATE = "White is in stalemate but isInStalemate returned false";

    @Test
    @DisplayName("New Game Default Values")
    public void newGame() {
        var game = new ChessGame();
        var expectedBoard = TestUtilities.defaultBoard();
        Assertions.assertEquals(expectedBoard, game.getBoard(), "Incorrect starting board");
        Assertions.assertEquals(ChessGame.TeamColor.WHITE, game.getTeamTurn(), "Incorrect starting team turn");
    }

    @Test
    @DisplayName("Default Board No Statuses")
    public void noGameStatuses() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.defaultBoard());
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_CHECK);
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECK);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECKMATE);
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_STALEMATE);
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_STALEMATE);
    }


    @Test
    @DisplayName("White in Check")
    public void whiteCheck() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | | |r| | |
                | | | | | | | | |
                | | | | | | | | |
                """));

        Assertions.assertTrue(game.isInCheck(ChessGame.TeamColor.WHITE), MISSING_BLACK_CHECK);
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_CHECK);
    }


    @Test
    @DisplayName("Black in Check")
    public void blackCheck() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | |K| | | | |
                | | | | | | | | |
                | | | |k| | | | |
                | | | | | | | | |
                | | | | | | | | |
                |B| | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                """));

        Assertions.assertTrue(game.isInCheck(ChessGame.TeamColor.BLACK),
                "Black is in check but isInCheck returned false");
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECK);
    }


    @Test
    @DisplayName("White in Checkmate")
    public void whiteTeamCheckmate() {

        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | | | |
                | | |b|q| | | | |
                | | | | | | | | |
                | | | |p| | | |k|
                | | | | | |K| | |
                | | |r| | | | | |
                | | | | |n| | | |
                | | | | | | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.WHITE), MISSING_WHITE_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_CHECKMATE);
    }


    @Test
    @DisplayName("Black in Checkmate by Pawns")
    public void blackTeamPawnCheckmate() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | |k| | | | |
                | | | |P|P| | | |
                | |P| | |P|P| | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | |K| | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK), MISSING_BLACK_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECKMATE);

    }

    @Test
    @DisplayName("Black can escape Check by capturing")
    public void escapeCheckByCapturingThreateningPiece() {

        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | |r|k| |
                | | | | | |P| |p|
                | | | |N| | | | |
                | | | | |B| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |n| | | |
                |K| | | | | | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECKMATE);
    }


    @Test
    @DisplayName("Black CANNOT escape Check by capturing")
    public void cannotEscapeCheckByCapturingThreateningPiece() {

        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | |r|k| |
                | | | | | |P| |p|
                | | | |N| | | | |
                | | | | |B| | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |n| | | |
                |K| | | | | |R| |
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK), MISSING_BLACK_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECKMATE);
    }


    @Test
    @DisplayName("Checkmate, where blocking a threat reveals a new threat")
    public void checkmateWhereBlockingThreateningPieceOpensNewThreat() {

        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                | | | | | | |r|k|
                | | |R| | | | | |
                | | | | | | | | |
                | | | | |r| | | |
                | | | | | | | | |
                | | |B| | | | | |
                | | | | | | | | |
                |K| | | | | | |R|
                """));
        game.setTeamTurn(ChessGame.TeamColor.BLACK);

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK), MISSING_BLACK_CHECKMATE);
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_CHECKMATE);
    }


    @Test
    @DisplayName("Pinned King Causes Stalemate")
    public void stalemate() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                |k| | | | | | | |
                | | | | | | | |r|
                | | | | | | | | |
                | | | | |q| | | |
                | | | |n| | |K| |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |b| | | |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        Assertions.assertTrue(game.isInStalemate(ChessGame.TeamColor.WHITE), MISSING_WHITE_STALEMATE);
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_STALEMATE);
    }

    @Test
    @DisplayName("Stalemate Requires not in Check")
    public void checkmateNotStalemate() {
        var game = new ChessGame();
        game.setBoard(TestUtilities.loadBoard("""
                |k| | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | |P| | | |
                | | | | | | | |r|
                |K| | | | | |r| |
                """));
        game.setTeamTurn(ChessGame.TeamColor.WHITE);

        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.WHITE), INCORRECT_WHITE_STALEMATE);
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK), INCORRECT_BLACK_STALEMATE);
    }
}
