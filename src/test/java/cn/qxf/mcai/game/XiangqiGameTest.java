package cn.qxf.mcai.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XiangqiGameTest {
    @Test void inactiveGameStillHasSerializableInitialBoard() {
        XiangqiGame game = new XiangqiGame();
        assertFalse(game.isActive());
        assertEquals(90, game.saveBoard().length());
        assertFalse(game.saveBoard().contains("\0"));
    }

    @Test void startsWithReadableTenByNineBoard() {
        XiangqiGame game = new XiangqiGame();
        game.start();
        String board = game.boardText();
        assertTrue(board.contains("楚河"));
        assertTrue(board.contains("帥"));
        assertEquals(11, board.lines().count());
    }

    @Test void ownerCanMoveRedPawnForwardAndAiReplies() {
        XiangqiGame game = new XiangqiGame();
        game.start();
        XiangqiGame.Result result = game.play(0, 6, 0, 5, 7L);
        assertTrue(result.accepted());
        assertTrue(result.message().contains("龙龙从"));
    }

    @Test void rejectsBackwardPawnAndEmptySquare() {
        XiangqiGame game = new XiangqiGame();
        game.start();
        assertFalse(game.play(0, 6, 0, 7, 1L).accepted());
        assertFalse(game.play(4, 5, 4, 4, 1L).accepted());
    }

    @Test void stateCanRoundTrip() {
        XiangqiGame source = new XiangqiGame();
        source.start();
        source.play(0, 6, 0, 5, 3L);
        XiangqiGame restored = new XiangqiGame();
        restored.load(source.saveBoard(), source.isActive(), source.ownerWins(), source.longlongWins());
        assertEquals(source.boardText(), restored.boardText());
    }

    @Test void rejectsMoveThatExposesOwnerKingToRook() {
        XiangqiGame game = gameWithPieces(new Piece(4, 9, 'K'), new Piece(3, 0, 'k'),
            new Piece(4, 0, 'r'), new Piece(4, 8, 'R'));
        assertFalse(game.play(4, 8, 3, 8, 2L).accepted());
    }

    @Test void respectsHorseLegAndCannonScreenRules() {
        XiangqiGame horse = gameWithPieces(new Piece(4, 9, 'K'), new Piece(4, 0, 'k'),
            new Piece(4, 5, 'P'), new Piece(1, 9, 'N'), new Piece(1, 8, 'P'));
        assertFalse(horse.play(1, 9, 2, 7, 2L).accepted());

        XiangqiGame cannon = gameWithPieces(new Piece(4, 9, 'K'), new Piece(4, 0, 'k'),
            new Piece(4, 5, 'P'), new Piece(0, 9, 'C'), new Piece(0, 6, 'P'),
            new Piece(0, 3, 'p'), new Piece(0, 0, 'r'));
        assertFalse(cannon.play(0, 9, 0, 0, 2L).accepted());
    }

    private static XiangqiGame gameWithPieces(Piece... pieces) {
        char[] board = new char[90];
        java.util.Arrays.fill(board, '.');
        for (Piece piece : pieces) board[piece.y * 9 + piece.x] = piece.value;
        XiangqiGame game = new XiangqiGame();
        game.load(new String(board), true, 0, 0);
        return game;
    }

    private record Piece(int x, int y, char value) {}
}
