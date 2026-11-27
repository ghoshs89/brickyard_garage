package com.brickyard.engine;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.MatchFinder;
import com.brickyard.engine.rules.Step;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;
import org.junit.jupiter.api.Test;

import static com.brickyard.engine.T.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchAndPowerTest {

    @Test
    void basePatternHasNoMatch() {
        Board b = new LB(base()).board();
        assertFalse(MatchFinder.hasAnyMatch(b));
    }

    @Test
    void invalidSwapBouncesWithoutSpendingAMove() {
        Board b = new LB(base()).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(0, 0), idx(0, 1)), true);
        assertTrue(r.bounced);
        assertFalse(r.moveSpent);
        assertEquals(20, b.movesLeft);
        assertEquals('B', base()[0].charAt(0));
        assertEquals(Kinds.BLUE_GEAR, b.color[idx(0, 0)]);
    }

    /** Row 3 becomes R R R R after swapping (4,2) up into (3,2). */
    static String[] fourInRow() {
        String[] L = base();
        L[3] = "RRYRGOBY";
        L = set(L, 4, 2, 'R');
        return L;
    }

    @Test
    void fourInARowMakesVerticalNitroAtSwipeDestination() {
        Board b = new LB(fourInRow()).goal("COLOR", "RED", 99).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(4, 2), idx(3, 2)), true);
        assertTrue(r.moveSpent);
        Step s = first(r, Step.Type.POWERUP_SPAWN);
        assertNotNull(s);
        assertEquals(Kinds.NITRO_V, s.kind);
        assertEquals(idx(3, 2), s.a);
        assertTrue(r.praise.contains(TurnResult.Praise.SETUP_CRAFTED));
        assertEquals(19, b.movesLeft);
    }

    @Test
    void fourInAColumnMakesHorizontalNitro() {
        String[] L = base();
        L = set(L, 1, 5, 'R');
        L = set(L, 2, 5, 'R');
        L = set(L, 4, 5, 'R');
        L = set(L, 3, 4, 'R');
        Board b = new LB(L).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(3, 4), idx(3, 5)), true);
        Step s = first(r, Step.Type.POWERUP_SPAWN);
        assertNotNull(s);
        assertEquals(Kinds.NITRO_H, s.kind);
        assertEquals(idx(3, 5), s.a);
    }

    @Test
    void squareMakesBuggy() {
        String[] L = base();
        L[2] = "BYRRBYGO";
        L[3] = "GORYROBY";
        Board b = new LB(L).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(3, 4), idx(3, 3)), true);
        Step s = first(r, Step.Type.POWERUP_SPAWN);
        assertNotNull(s);
        assertEquals(Kinds.BUGGY, s.kind);
        assertEquals(idx(3, 3), s.a);
    }

    @Test
    void lShapeMakesAirbagAtCorner() {
        String[] L = base();
        L[1] = "GOBRGOBY";
        L[2] = "BRROBYGO";
        L[3] = "GOBRGOBY";
        L[4] = "BYGRBYGO";
        Board b = new LB(L).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(1, 3), idx(2, 3)), true);
        Step s = first(r, Step.Type.POWERUP_SPAWN);
        assertNotNull(s, b.dump());
        assertEquals(Kinds.AIRBAG, s.kind);
        assertEquals(idx(2, 3), s.a);
    }

    @Test
    void fiveInALineMakesSprayer() {
        String[] L = base();
        L[5] = "RRBRROBY";
        L = set(L, 6, 2, 'R');
        Board b = new LB(L).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(6, 2), idx(5, 2)), true);
        Step s = first(r, Step.Type.POWERUP_SPAWN);
        assertNotNull(s);
        assertEquals(Kinds.SPRAYER, s.kind);
        assertEquals(idx(5, 2), s.a);
    }

    @Test
    void tappingNitroClearsItsRowAndSpendsAMove() {
        Board b = new LB(set(base(), 3, 3, 'h')).board();
        TurnResult r = TurnEngine.play(b, Action.tap(idx(3, 3)), true);
        Step s = first(r, Step.Type.POWERUP_FIRE);
        assertNotNull(s);
        assertEquals(8, s.cells.length);
        for (int c : s.cells) assertEquals(3, c / 8);
        assertEquals(19, b.movesLeft);
    }

    @Test
    void sprayerCannotBeTappedOnlySwapped() {
        Board b = new LB(set(base(), 0, 0, 's')).board();
        assertEquals(TurnResult.Status.INVALID, TurnEngine.play(b, Action.tap(0), true).status);
    }

    @Test
    void sprayerSwappedWithPieceClearsThatColour() {
        Board b = new LB(set(base(), 0, 0, 's')).goal("COLOR", "YELLOW", 99).board();
        int yellows = 0;
        for (int i = 0; i < 64; i++) if (b.color[i] == Kinds.YELLOW_PLUG) yellows++;
        TurnResult r = TurnEngine.play(b, Action.swap(idx(0, 0), idx(0, 1)), true);
        Step fire = first(r, Step.Type.POWERUP_FIRE);
        assertEquals(Kinds.SPRAYER, fire.kind);
        assertEquals(yellows + 1, fire.cells.length); // every yellow plus the sprayer's own cell
        assertTrue(b.collected[0] >= yellows);
    }

    @Test
    void nitroSwappedWithPieceFiresAtItsNewCell() {
        Board b = new LB(set(base(), 3, 3, 'h')).board();
        TurnResult r = TurnEngine.play(b, Action.swap(idx(3, 3), idx(4, 3)), true);
        Step fire = first(r, Step.Type.POWERUP_FIRE);
        for (int c : fire.cells) assertEquals(4, c / 8);
    }
}
