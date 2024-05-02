package passoff.chess.game;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FullGameTest {
    @Test
    @DisplayName("Full Game Checkmate")
    public void scholarsMate() throws InvalidMoveException {
        var game = new ChessGame();
        game.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));
        /*
        |r|n|b|q|k|b|n|r|
		|p|p|p|p|p|p|p|p|
		| | | | | | | | |
		| | | | | | | | |
		| | | | |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B|Q|K|B|N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(7, 5), new ChessPosition(5, 5), null));
        /*
        |r|n|b|q|k|b|n|r|
		|p|p|p|p| |p|p|p|
		| | | | | | | | |
		| | | | |p| | | |
		| | | | |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B|Q|K|B|N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(1, 6), new ChessPosition(4, 3), null));
        /*
        |r|n|b|q|k|b|n|r|
		|p|p|p|p| |p|p|p|
		| | | | | | | | |
		| | | | |p| | | |
		| | |B| |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B|Q|K| |N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(8, 7), new ChessPosition(6, 6), null));
        /*
        |r|n|b|q|k|b| |r|
		|p|p|p|p| |p|p|p|
		| | | | | |n| | |
		| | | | |p| | | |
		| | |B| |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B|Q|K| |N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(1, 4), new ChessPosition(5, 8), null));
        /*
        |r|n|b|q|k|b| |r|
		|p|p|p|p| |p|p|p|
		| | | | | |n| | |
		| | | | |p| | |Q|
		| | |B| |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B| |K| |N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(8, 2), new ChessPosition(6, 3), null));
        /*
        |r| |b|q|k|b| |r|
		|p|p|p|p| |p|p|p|
		| | |n| | |n| | |
		| | | | |p| | |Q|
		| | |B| |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B| |K| |N|R|
         */
        game.makeMove(new ChessMove(new ChessPosition(5, 8), new ChessPosition(7, 6), null));
        /*
        |r| |b|q|k|b| |r|
		|p|p|p|p| |Q|p|p|
		| | |n| | |n| | |
		| | | | |p| | | |
		| | |B| |P| | | |
		| | | | | | | | |
		|P|P|P|P| |P|P|P|
		|R|N|B| |K| |N|R|
         */
        Assertions.assertTrue(game.isInCheck(ChessGame.TeamColor.BLACK),
                "Black is in check but isInCheck returned false");
        Assertions.assertFalse(game.isInCheck(ChessGame.TeamColor.WHITE),
                "White is not in check but isInCheck returned true");
        Assertions.assertTrue(game.isInCheckmate(ChessGame.TeamColor.BLACK),
                "Black is in checkmate but isInCheckmate returned false");
        Assertions.assertFalse(game.isInCheckmate(ChessGame.TeamColor.WHITE),
                "White is not in checkmate but isInCheckmate returned true");
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.BLACK),
                "Black is not in stalemate but isInStalemate returned true");
        Assertions.assertFalse(game.isInStalemate(ChessGame.TeamColor.WHITE),
                "White is not in stalemate but isInStalemate returned true");
    }
}
