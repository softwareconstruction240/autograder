package passoff.chess;

import chess.ChessPosition;

import java.util.Collection;
import java.util.List;

public class ChessPositionTests extends EqualsTestingUtility<ChessPosition> {
    public ChessPositionTests() {
        super("ChessPosition", "positions");
    }

    @Override
    protected ChessPosition buildOriginal() {
        return new ChessPosition(3, 7);
    }

    @Override
    protected Collection<ChessPosition> buildAllDifferent() {
        return List.of(
                new ChessPosition(7, 3),
                new ChessPosition(6, 3),
                new ChessPosition(4, 3),
                new ChessPosition(3, 1),
                new ChessPosition(3, 2),
                new ChessPosition(3, 3)
        );
    }

}
