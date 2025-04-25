package passoff.chess;

import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Collection;
import java.util.List;


public class ChessMoveTests extends EqualsTestingUtility<ChessMove> {
    public ChessMoveTests() {
        super("ChessMove", "moves");
    }

    @Override
    protected ChessMove buildOriginal() {
        return new ChessMove(new ChessPosition(2, 6), new ChessPosition(1, 5), null);
    }

    @Override
    protected Collection<ChessMove> buildAllDifferent() {
        return List.of(
                new ChessMove(new ChessPosition(1, 5), new ChessPosition(2, 6), null),
                new ChessMove(new ChessPosition(2, 4), new ChessPosition(1, 5), null),
                new ChessMove(new ChessPosition(2, 6), new ChessPosition(5, 3), null),
                new ChessMove(new ChessPosition(2, 6), new ChessPosition(1, 5), ChessPiece.PieceType.QUEEN),
                new ChessMove(new ChessPosition(2, 6), new ChessPosition(1, 5), ChessPiece.PieceType.ROOK),
                new ChessMove(new ChessPosition(2, 6), new ChessPosition(1, 5), ChessPiece.PieceType.BISHOP),
                new ChessMove(new ChessPosition(2, 6), new ChessPosition(1, 5), ChessPiece.PieceType.KNIGHT)
        );
    }

}
