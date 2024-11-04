package passoff.chess.game;

import chess.*;
import org.junit.jupiter.api.*;

import static passoff.chess.TestUtilities.loadBoard;

public class GameStatusTests {

    @Test
    @DisplayName("New Game sets up default values")
    public void newGame() {
        var game = new ChessGame();
        var expectedBoard = loadBoard("""
                |r|n|b|q|k|b|n|r|
                |p|p|p|p|p|p|p|p|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                |P|P|P|P|P|P|P|P|
                |R|N|B|Q|K|B|N|R|
                """);
        Assertions.assertEquals(expectedBoard, game.getBoard());
        Assertions.assertEquals(ChessGame.TeamColor.WHITE, game.getTeamTurn());
    }

    @Test
    @DisplayName("New Game No Statuses")
    public void noGameStatuses() {
        var game = new ChessGame();

        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.BLACK),
                "Black is not in check but isInCheck returned true");
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.WHITE),
                "White is not in check but isInCheck returned true");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is not in checkmate but isInCheckmate returned true");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK),
                "Black is not in stalemate but isInStalemate returned true");
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.WHITE),
                "White is not in stalemate but isInStalemate returned true");
    }


    @Test
    @DisplayName("White in Check")
    public void whiteCheck() {
        var game = new ChessGame();
        game.setBoard(loadBoard("""
                | | | | | | | |k|
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | | | | | | | | |
                | |K| | | |r| | |
                | | | | | | | | |
                | | | | | | | | |
                """));

        Assertions.assertTrue(game.isInCheck(ChessGame.TeamColor.WHITE),
                "White is in check but isInCheck returned false");
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.BLACK),
                "Black is not in check but isInCheck returned true");
    }


    @Test
    @DisplayName("Black in Check")
    public void blackCheck() {
        var game = new ChessGame();
        game.setBoard(loadBoard("""
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
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.WHITE),
                "White is not in check but isInCheck returned true");
    }


    @Test
    @DisplayName("White in Checkmate")
    public void whiteTeamCheckmate() {

        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is in checkmate but isInCheckmate returned false");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is not in checkmate but isInCheckmate returned true");
    }


    @Test
    @DisplayName("Black in Checkmate by Pawns")
    public void blackTeamPawnCheckmate() {
        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is in checkmate but isInCheckmate returned false");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");

    }

    @Test
    @DisplayName("Black can escape Check by capturing")
    public void escapeCheckByCapturingThreateningPiece() {

        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is not in checkmate but isInCheckmate returned true");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");
    }


    @Test
    @DisplayName("Black CANNOT escape Check by capturing")
    public void cannotEscapeCheckByCapturingThreateningPiece() {

        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is in checkmate but isInCheckmate returned false");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");
    }


    @Test
    @DisplayName("Checkmate, where blocking a threat reveals a new threat")
    public void checkmateWhereBlockingThreateningPieceOpensNewThreat() {

        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is in checkmate but isInCheckmate returned false");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");
    }


    @Test
    @DisplayName("Pinned King Causes Stalemate")
    public void stalemate() {
        var game = new ChessGame();
        game.setBoard(loadBoard("""
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

        Assertions.assertTrue(game.isInStalemate(ChessGame.TeamColor.WHITE),
                "White is in a stalemate but isInStalemate returned false");
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK),
                "Black is not in a stalemate but isInStalemate returned true");
    }
}
