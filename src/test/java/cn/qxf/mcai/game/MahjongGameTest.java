package cn.qxf.mcai.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MahjongGameTest {
    @Test void dealUsesCompleteFourPlayerWall() {
        MahjongGame game = new MahjongGame();
        MahjongGame.Result result = game.start(1975L);
        assertTrue(result.accepted());
        assertTrue(game.isActive());
        assertEquals(14, game.ownerHand().length);
        assertEquals(83, game.wallRemaining());
    }

    @Test void ownerDiscardRunsAllAiTurnsAndDrawsAgain() {
        MahjongGame game = new MahjongGame();
        game.start(8L);
        MahjongGame.Result result = game.play(0, 9L);
        assertTrue(result.accepted());
        if (!result.gameOver()) {
            if (game.pendingTile() >= 0) {
                assertEquals(13, game.ownerHand().length);
                assertTrue(game.availableClaims() > 0);
                assertTrue(game.discards().length >= 2 && game.discards().length <= 4);
            } else {
                assertEquals(14, game.ownerHand().length);
                assertEquals(4, game.discards().length);
                assertEquals(79, game.wallRemaining());
            }
        }
    }

    @Test void recognizesStandardWinningHand() {
        int[] winning = {0, 0, 0, 1, 2, 3, 9, 10, 11, 18, 19, 20, 31, 31};
        assertTrue(MahjongGame.isWinning(winning));
        assertFalse(MahjongGame.isWinning(new int[]{0, 0, 1, 1, 2, 3, 9, 10, 11, 18, 19, 20, 31, 32}));
    }

    @Test void recognizesSevenPairsAndThirteenOrphans() {
        assertTrue(MahjongGame.isWinning(new int[]{0,0,8,8,9,9,17,17,18,18,26,26,31,31}));
        assertTrue(MahjongGame.isWinning(new int[]{0,0,8,9,17,18,26,27,28,29,30,31,32,33}));
    }

    @Test void exposesChiPengAndGangChecks() {
        assertTrue(MahjongGame.canChi(new int[]{0, 2, 7}, 1));
        assertFalse(MahjongGame.canChi(new int[]{0, 8}, 9));
        assertTrue(MahjongGame.canPeng(new int[]{31, 31, 4}, 31));
        assertTrue(MahjongGame.canGang(new int[]{5, 5, 5, 8}, 5));
    }

    @Test void pendingDiscardCanBeClaimedAsExposedKong() {
        MahjongGame game = new MahjongGame();
        game.load(String.join(";", "1", "0", "0", "5", "3", "4", "7", "0", "0", "0",
            "3,4,5,5,5,9,10,11,18,19,20,31,31", "", "", "", "0", "5"));
        MahjongGame.Result result = game.claim(MahjongGame.CLAIM_GANG, 6L);
        assertTrue(result.accepted());
        assertEquals(1, game.ownerMelds());
        assertEquals(11, game.ownerHand().length);
        assertEquals(-1, game.pendingTile());
    }

    @Test void pendingDiscardCanBePassed() {
        MahjongGame game = new MahjongGame();
        game.load(String.join(";", "1", "0", "0", "5", "3", "4", "1", "0", "0", "0",
            "3,4,6,7,8,9,10,11,18,19,20,31,31", "", "", "", "0", "5"));
        MahjongGame.Result result = game.pass(7L);
        assertTrue(result.accepted());
        assertEquals(-1, game.pendingTile());
        assertEquals(14, game.ownerHand().length);
    }

    @Test void rejectsInvalidSelection() {
        MahjongGame game = new MahjongGame();
        game.start(3L);
        assertFalse(game.play(-1, 4L).accepted());
        assertFalse(game.play(14, 4L).accepted());
    }

    @Test void stateCanRoundTrip() {
        MahjongGame source = new MahjongGame();
        source.start(11L);
        source.play(2, 12L);
        MahjongGame restored = new MahjongGame();
        restored.load(source.save());
        assertArrayEquals(source.ownerHand(), restored.ownerHand());
        assertArrayEquals(source.discards(), restored.discards());
        assertEquals(source.wallRemaining(), restored.wallRemaining());
        assertEquals(source.isActive(), restored.isActive());
    }
}
