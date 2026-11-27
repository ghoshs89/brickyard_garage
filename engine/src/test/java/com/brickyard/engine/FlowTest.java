package com.brickyard.engine;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.MatchFinder;
import com.brickyard.engine.rules.Moves;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;
import com.brickyard.engine.util.Json;
import com.brickyard.engine.util.Rng;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.brickyard.engine.T.*;
import static org.junit.jupiter.api.Assertions.*;

class FlowTest {

    @Test
    void winningOnTheFinalMoveIsAWinNotALoss() {
        Board b = new LB(MatchAndPowerTest.fourInRow()).put("moves", 1L).goal("COLOR", "RED", 4).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(4, 2), idx(3, 2)), true);
        assertEquals(0, b.movesLeft);
        assertEquals(TurnResult.Status.WON, r.status);
        assertEquals(b.level.baseCoins, r.coinsAwarded);
    }

    @Test
    void runningOutOfMovesLoses() {
        Board b = new LB(MatchAndPowerTest.fourInRow()).put("moves", 1L).goal("COLOR", "RED", 50).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(4, 2), idx(3, 2)), true);
        assertEquals(TurnResult.Status.LOST, r.status);
        assertEquals(TurnResult.Status.INVALID, TurnEngine.play(b, Action.tap(0), true).status);
    }

    @Test
    void victoryLapAwardsTwoCoinsPerMoveLeft() {
        Board b = new LB(MatchAndPowerTest.fourInRow()).put("moves", 7L).goal("COLOR", "RED", 4).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(4, 2), idx(3, 2)), true);
        assertEquals(TurnResult.Status.WON, r.status);
        assertEquals(b.level.baseCoins + 12, r.coinsAwarded);
        assertTrue(r.praise.contains(TurnResult.Praise.MOVES_SAVED));
    }

    static Level smallLevel(int colors) {
        return new LB(new String[]{".....", ".....", ".....", ".....", "....."})
                .put("id", "fuzz").put("moves", 400L).put("colors", (long) colors)
                .goal("COLOR", "RED", 100000).level();
    }

    /** Play random legal moves; after every turn the board must be settled, match-free and playable. */
    private int fuzz(Level lv, long seed, int turns) {
        Board b = Board.create(lv, seed);
        // A level with refills uses them; strip them so play is random.
        Rng pick = new Rng(seed ^ 99);
        int shuffles = 0;
        for (int t = 0; t < turns; t++) {
            List<Action> legal = Moves.legal(b);
            assertFalse(legal.isEmpty(), "no legal move on a PLAYING board\n" + b.dump());
            TurnResult r = TurnEngine.play(b, legal.get(pick.nextInt(legal.size())), false);
            assertNotEquals(TurnResult.Status.INVALID, r.status);
            if (r.shuffled) shuffles++;
            if (r.status != TurnResult.Status.PLAYING) break;
            assertFalse(MatchFinder.hasAnyMatch(b), "unsettled match after turn\n" + b.dump());
            for (int i = 0; i < b.n; i++) {
                assertNotEquals(Kinds.EMPTY, b.entity[i], "hole left in the board\n" + b.dump());
                if (b.entity[i] == Kinds.PIECE) assertTrue(b.color[i] >= 0 || b.power[i] != Kinds.NONE);
            }
        }
        return shuffles;
    }

    @Test
    void randomPlayKeepsTheBoardValidAndDeadlocksReshuffle() {
        Level lv = new LB(new String[]{".....", ".....", ".....", ".....", "....."}).put("id", "fuzz")
                .put("moves", 400L).put("colors", 6L).goal("COLOR", "RED", 100000).level();
        lv.refills = null; // random refills
        int shuffles = 0;
        for (long seed = 1; seed <= 12; seed++) shuffles += fuzz(lv, seed, 250);
        assertTrue(shuffles > 0, "expected at least one dead-board reshuffle on a cramped 6-colour board");
    }

    @Test
    void sameSeedAndMovesGiveTheSameGame() {
        Level lv = smallLevel(5);
        lv.refills = null;
        long h1 = playout(lv, 42, 60), h2 = playout(lv, 42, 60);
        assertEquals(h1, h2);
        assertNotEquals(h1, playout(lv, 43, 60));
    }

    private long playout(Level lv, long seed, int turns) {
        Board b = Board.create(lv, seed);
        Rng pick = new Rng(5);
        for (int t = 0; t < turns; t++) {
            List<Action> legal = Moves.legal(b);
            TurnResult r = TurnEngine.play(b, legal.get(pick.nextInt(legal.size())), false);
            if (r.status != TurnResult.Status.PLAYING) break;
        }
        return b.stateHash();
    }

    @Test
    void saveAndRestoreMidGameContinuesIdentically() {
        Level lv = smallLevel(5);
        lv.refills = null;
        Board a = Board.create(lv, 9);
        Rng pick = new Rng(3);
        for (int t = 0; t < 20; t++) {
            List<Action> legal = Moves.legal(a);
            TurnEngine.play(a, legal.get(pick.nextInt(legal.size())), false);
        }
        String saved = Json.write(a.toJson());
        Board b = Board.fromJson(lv, Json.parseObject(saved));
        assertEquals(a.stateHash(), b.stateHash());
        Rng p1 = new Rng(11), p2 = new Rng(11);
        for (int t = 0; t < 30; t++) {
            List<Action> l1 = Moves.legal(a), l2 = Moves.legal(b);
            assertEquals(l1.size(), l2.size());
            TurnEngine.play(a, l1.get(p1.nextInt(l1.size())), false);
            TurnEngine.play(b, l2.get(p2.nextInt(l2.size())), false);
        }
        assertEquals(a.stateHash(), b.stateHash());
        assertEquals(a.movesLeft, b.movesLeft);
    }

    @Test
    void recordingDoesNotChangeTheOutcome() {
        Level lv = smallLevel(5);
        lv.refills = null;
        Board a = Board.create(lv, 77), b = Board.create(lv, 77);
        Rng p1 = new Rng(1), p2 = new Rng(1);
        for (int t = 0; t < 40; t++) {
            List<Action> l1 = Moves.legal(a), l2 = Moves.legal(b);
            TurnResult r1 = TurnEngine.play(a, l1.get(p1.nextInt(l1.size())), false);
            TurnEngine.play(b, l2.get(p2.nextInt(l2.size())), true);
            if (r1.status != TurnResult.Status.PLAYING) break;
        }
        assertEquals(a.stateHash(), b.stateHash());
    }

    @Test
    void startingBoardHasNoMatchAndALegalMove() {
        Level lv = smallLevel(4);
        lv.refills = null;
        for (long s = 0; s < 50; s++) {
            Board b = Board.create(lv, s);
            assertFalse(MatchFinder.hasAnyMatch(b));
            assertTrue(Moves.hasLegalMove(b));
        }
    }
}
