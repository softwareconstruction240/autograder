package passoff.chess.game;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import chess.InvalidMoveException;
import passoff.chess.EqualsTestingUtility;
import passoff.chess.TestUtilities;

import java.util.ArrayList;
import java.util.Collection;

public class ChessGameTests extends EqualsTestingUtility<ChessGame> {
    public ChessGameTests() {
        super("ChessGame", "games");
    }

    @Override
    protected ChessGame buildOriginal() {
        return new ChessGame();
    }

    @Override
    protected Collection<ChessGame> buildAllDifferent() {
        Collection<ChessGame> differentGames = new ArrayList<>();

        try {
            // Different team turn
            ChessGame game1 = new ChessGame();
            game1.setTeamTurn(ChessGame.TeamColor.BLACK);
            differentGames.add(game1);

            // Move pawn
            ChessGame game2 = new ChessGame();
            game2.makeMove(new ChessMove(
                    new ChessPosition(2, 5),
                    new ChessPosition(4, 5),
                    null));
            differentGames.add(game2);

            // Move knight
            ChessGame game3 = new ChessGame();
            game3.makeMove(new ChessMove(
                    new ChessPosition(1, 7),
                    new ChessPosition(3, 6),
                    null));
            differentGames.add(game3);

            // Set board
            ChessGame game4 = new ChessGame();
            game4.setBoard(TestUtilities.loadBoard("""
                    | | | |R| | | | |
                    | | | | | | | | |
                    | | |p|n|p| | | |
                    |R| |n|k|r| | |R|
                    | | |p|q| | | | |
                    | | | | | |K| | |
                    | | | | |P| | | |
                    | | | |R| | | | |
                    """));
            differentGames.add(game4);

        } catch (InvalidMoveException e) {
            throw new RuntimeException("All moves in ChessGameTests are valid and should be allowed.", e);
        }

        return differentGames;
    }
}
