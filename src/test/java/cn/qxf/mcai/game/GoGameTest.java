package cn.qxf.mcai.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoGameTest {
    @Test void startsAsEmptyThirteenByThirteenBoard() {
        GoGame game = new GoGame();
        game.start();
        assertTrue(game.isActive());
        assertEquals(169, game.board().length);
        assertEquals(169, game.saveBoard().length());
        for (byte point : game.board()) assertEquals(0, point);
    }

    @Test void ownerMoveAndLonglongReplyAreApplied() {
        GoGame game = new GoGame();
        game.start();
        GoGame.Result result = game.play(3, 3, 1975L);
        assertTrue(result.accepted());
        assertFalse(result.gameOver());
        assertEquals(1, result.board()[3 + 3 * GoGame.SIZE]);
        int white = 0;
        for (byte point : result.board()) if (point == 2) white++;
        assertEquals(1, white);
    }

    @Test void rejectsOccupiedAndOutOfRangePoints() {
        GoGame game = new GoGame();
        game.start();
        assertTrue(game.play(3, 3, 1L).accepted());
        assertFalse(game.play(3, 3, 2L).accepted());
        assertFalse(game.play(-1, 0, 3L).accepted());
        assertFalse(game.play(13, 12, 4L).accepted());
    }

    @Test void stateCanRoundTrip() {
        GoGame source = new GoGame();
        source.start();
        source.play(2, 2, 8L);
        GoGame restored = new GoGame();
        restored.load(source.saveBoard(), source.isActive(), source.ownerWins(), source.longlongWins(),
            source.ownerCaptures(), source.longlongCaptures(), source.consecutivePasses(), source.koForbidden());
        assertArrayEquals(source.board(), restored.board());
        assertEquals(source.isActive(), restored.isActive());
        assertEquals(source.koForbidden(), restored.koForbidden());
    }
}
