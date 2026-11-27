package com.brickyard.engine;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.Step;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.brickyard.engine.T.*;
import static org.junit.jupiter.api.Assertions.*;

class ObstacleTest {
    private static final Action FOUR = Action.swap(idx(4, 2), idx(3, 2)); // makes R R R R on row 3

    @Test
    void crateLosesOneLayerPerAdjacentMatch() {
        String[] L = set(MatchAndPowerTest.fourInRow(), 2, 1, '2');
        Board b = new LB(L).goal("CRATE", null, -1).board();
        TurnResult r = TurnEngine.play(b, FOUR, true);
        Step pop = first(r, Step.Type.MATCH_POP);
        assertEquals(Kinds.CRATE, pop.after.entity[idx(2, 1)]);
        assertEquals(1, pop.after.layers[idx(2, 1)]);
    }

    @Test
    void singleLayerCrateIsDestroyedAndCounted() {
        String[] L = set(MatchAndPowerTest.fourInRow(), 2, 1, '1');
        Board b = new LB(L).goal("CRATE", null, 1).board();
        TurnResult r = TurnEngine.play(b, FOUR, true);
        assertEquals(1, b.collected[0]);
        assertEquals(TurnResult.Status.WON, r.status);
    }

    @Test
    void clampedPieceMatchesInPlaceAndLosesItsClamp() {
        String[] ov = new String[8];
        for (int r = 0; r < 8; r++) ov[r] = "........";
        ov[3] = "1.......";
        Board b = new LB(MatchAndPowerTest.fourInRow()).overlay(ov).goal("CLAMP", null, -1).board();
        assertEquals(1, b.clamp[idx(3, 0)]);
        TurnResult r = TurnEngine.play(b, FOUR, true);
        Step pop = first(r, Step.Type.MATCH_POP);
        assertEquals(Kinds.PIECE, pop.after.entity[idx(3, 0)]);
        assertEquals(Kinds.RED_BRICK, pop.after.color[idx(3, 0)]);
        assertEquals(0, pop.after.clamp[idx(3, 0)]);
        assertEquals(TurnResult.Status.WON, r.status);
    }

    @Test
    void clampedPieceCannotBeSwapped() {
        String[] ov = new String[8];
        for (int r = 0; r < 8; r++) ov[r] = "........";
        ov[4] = "..1.....";
        Board b = new LB(MatchAndPowerTest.fourInRow()).overlay(ov).board();
        assertEquals(TurnResult.Status.INVALID, TurnEngine.play(b, FOUR, true).status);
    }

    @Test
    void oilSpreadsWhenATurnClearsNone() {
        Board b = new LB(set(MatchAndPowerTest.fourInRow(), 7, 7, '~')).goal("OIL", null, -1).board();
        TurnEngine.play(b, FOUR, true);
        assertEquals(2, b.count(Kinds.OIL));
    }

    @Test
    void oilNextToAMatchIsClearedAndDoesNotSpread() {
        Board b = new LB(set(MatchAndPowerTest.fourInRow(), 4, 0, '~')).goal("OIL", null, -1).board();
        TurnResult r = TurnEngine.play(b, FOUR, true);
        assertEquals(0, b.count(Kinds.OIL));
        assertEquals(TurnResult.Status.WON, r.status);
    }

    @Test
    void rustUnderAMatchedPieceIsPolishedAway() {
        String[] fl = new String[8];
        for (int r = 0; r < 8; r++) fl[r] = "........";
        fl[3] = "r.......";
        Board b = new LB(MatchAndPowerTest.fourInRow()).floor(fl).goal("RUST", null, -1).board();
        TurnResult r = TurnEngine.play(b, FOUR, true);
        assertEquals(Kinds.FLOOR_NONE, b.floor[idx(3, 0)]);
        assertEquals(TurnResult.Status.WON, r.status);
    }

    @Test
    void trackBaseplateSpreadsAcrossTheWholeMatch() {
        String[] fl = new String[8];
        for (int r = 0; r < 8; r++) fl[r] = "........";
        fl[3] = "t.......";
        Board b = new LB(MatchAndPowerTest.fourInRow()).floor(fl).goal("TRACK", null, -1).board();
        TurnEngine.play(b, FOUR, true);
        for (int c = 0; c < 4; c++) assertEquals(Kinds.FLOOR_TRACK, b.floor[idx(3, c)], "col " + c);
    }

    @Test
    void piecesFallStraightThroughHoles() {
        String[] L = set(MatchAndPowerTest.fourInRow(), 2, 1, '#');
        Board b = new LB(L).board();
        int above = b.uid[idx(1, 1)];
        TurnResult r = TurnEngine.play(b, FOUR, true);
        Step g = first(r, Step.Type.GRAVITY_DROP);
        assertEquals(above, g.after.uid[idx(3, 1)]);
    }

    @Test
    void piecesSlideDiagonallyAroundACrate() {
        String[] L = set(MatchAndPowerTest.fourInRow(), 2, 1, '3');
        Board b = new LB(L).board();
        TurnEngine.play(b, FOUR, true);
        for (int i = 0; i < 64; i++) assertNotEquals(Kinds.EMPTY, b.entity[i], "cell " + i + "\n" + b.dump());
        assertEquals(Kinds.CRATE, b.entity[idx(2, 1)]);
    }

    @Test
    void portalCarriesPiecesBetweenSections() {
        Map<String, Object> wall;
        List<Object> walls = new ArrayList<>();
        for (int c = 0; c < 3; c++) {
            wall = new LinkedHashMap<>();
            wall.put("r", 1L);
            wall.put("c", (long) c);
            wall.put("side", "below");
            walls.add(wall);
        }
        Map<String, Object> portal = new LinkedHashMap<>();
        portal.put("from", rc(1, 0));
        portal.put("to", rc(2, 2));
        List<Object> portals = new ArrayList<>();
        portals.add(portal);
        LB lb = new LB(new String[]{"BYG", "ROB", "YGO", "BRY", "GOR"}).put("walls", walls).put("portals", portals);
        Level lv = lb.level();
        assertEquals(lv.idx(1, 0), lv.feeder[lv.idx(2, 2)]);
        assertEquals(Level.SPAWN, lv.feeder[lv.idx(2, 0)]);
        Board b = Board.createUnchecked(lv, 1);
        b.tools[Kinds.TOOL_WRENCH] = 1;
        int portalPiece = b.uid[lv.idx(1, 0)];
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_WRENCH, lv.idx(4, 2), -1), true);
        assertFalse(r.moveSpent);
        assertEquals(portalPiece, first(r, Step.Type.GRAVITY_DROP).after.uid[lv.idx(2, 2)]);
        assertEquals(0, b.tools[Kinds.TOOL_WRENCH]);
    }

    @Test
    void partsDropOutOfExitsAndCount() {
        List<Object> exits = new ArrayList<>();
        exits.add(rc(7, 0));
        String[] L = set(base(), 6, 0, '*');
        Board b = new LB(L).put("exits", exits).put("partsTotal", 1L).goal("PART", null, 1).board();
        b.tools[Kinds.TOOL_WRENCH] = 1;
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_WRENCH, idx(7, 0), -1), true);
        assertNotNull(first(r, Step.Type.PART_COLLECT));
        assertEquals(1, b.collected[0]);
        assertEquals(TurnResult.Status.WON, r.status);
    }

    @Test
    void conveyorShiftsContentsOneStepAfterASpentTurn() {
        List<Object> loop = new ArrayList<>();
        loop.add(rc(0, 0));
        loop.add(rc(0, 1));
        loop.add(rc(0, 2));
        List<Object> conveyors = new ArrayList<>();
        conveyors.add(loop);
        Board b = new LB(set(base(), 7, 7, 'v')).put("conveyors", conveyors).board();
        int u0 = b.uid[0], u1 = b.uid[1], u2 = b.uid[2];
        TurnResult r = TurnEngine.play(b, Action.tap(idx(7, 7)), true);
        assertNotNull(first(r, Step.Type.CONVEYOR_SHIFT));
        assertEquals(u0, b.uid[1]);
        assertEquals(u1, b.uid[2]);
        assertEquals(u2, b.uid[0]);
    }

    @Test
    void toolsAreUnavailableOnBlueprintPuzzles() {
        Board b = new LB(base()).put("kind", "BLUEPRINT").put("exactMoves", 3L).board();
        b.tools[Kinds.TOOL_WRENCH] = 1;
        assertEquals(TurnResult.Status.INVALID, TurnEngine.play(b, Action.tool(Kinds.TOOL_WRENCH, 0, -1), true).status);
    }

    @Test
    void broomClearsARowForFree() {
        Board b = new LB(base()).board();
        b.tools[Kinds.TOOL_BROOM] = 1;
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_BROOM, idx(5, 3), -1), true);
        assertEquals(8, first(r, Step.Type.TOOL_USE).cells.length);
        assertEquals(20, b.movesLeft);
    }

    @Test
    void magneticClawSwapsWithoutNeedingAMatch() {
        Board b = new LB(base()).board();
        b.tools[Kinds.TOOL_CLAW] = 1;
        int u0 = b.uid[0], u1 = b.uid[1];
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_CLAW, 0, 1), true);
        assertNotEquals(TurnResult.Status.INVALID, r.status);
        Step use = first(r, Step.Type.TOOL_USE);
        assertEquals(u0, use.after.uid[1]);
        assertEquals(u1, use.after.uid[0]);
        assertEquals(20, b.movesLeft);
    }

    @Test
    void oilCannotSpreadThroughSteelBulkheadWalls() {
        List<Object> walls = new ArrayList<>();
        Map<String, Object> w1 = new LinkedHashMap<>();
        w1.put("r", 6L); w1.put("c", 7L); w1.put("side", "below");
        walls.add(w1);
        Map<String, Object> w2 = new LinkedHashMap<>();
        w2.put("r", 7L); w2.put("c", 6L); w2.put("side", "right");
        walls.add(w2);
        Board b = new LB(set(MatchAndPowerTest.fourInRow(), 7, 7, '~')).put("walls", walls).goal("OIL", null, -1).board();
        TurnEngine.play(b, FOUR, true);
        assertEquals(1, b.count(Kinds.OIL), "oil must stay boxed in by steel walls");
    }

    @Test
    void diagonalGravityDoesNotSlideThroughVerticalWall() {
        List<Object> walls = new ArrayList<>();
        Map<String, Object> w1 = new LinkedHashMap<>();
        w1.put("r", 1L); w1.put("c", 1L); w1.put("side", "right");
        walls.add(w1);
        Map<String, Object> w2 = new LinkedHashMap<>();
        w2.put("r", 2L); w2.put("c", 1L); w2.put("side", "right");
        walls.add(w2);
        Map<String, Object> w3 = new LinkedHashMap<>();
        w3.put("r", 1L); w3.put("c", 0L); w3.put("side", "right");
        walls.add(w3);
        Map<String, Object> w4 = new LinkedHashMap<>();
        w4.put("r", 2L); w4.put("c", 0L); w4.put("side", "right");
        walls.add(w4);
        String[] L = set(MatchAndPowerTest.fourInRow(), 2, 1, '3');
        Board b = new LB(L).put("walls", walls).board();
        TurnEngine.play(b, FOUR, true);
        assertEquals(Kinds.EMPTY, b.entity[idx(3, 1)], "sealed shaft under crate must not leak across vertical walls");
    }

    @Test
    void wrenchRejectsIndestructiblePartWithoutSpendingCharge() {
        Board b = new LB(set(base(), 2, 2, '*')).board();
        b.tools[Kinds.TOOL_WRENCH] = 1;
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_WRENCH, idx(2, 2), -1), true);
        assertEquals(TurnResult.Status.INVALID, r.status);
        assertEquals(1, b.tools[Kinds.TOOL_WRENCH]);
    }

    @Test
    void magneticClawCanTriggerPowerUpComboForFree() {
        String[] L = set(set(base(), 3, 3, 'h'), 3, 4, 'v');
        Board b = new LB(L).board();
        b.tools[Kinds.TOOL_CLAW] = 1;
        TurnResult r = TurnEngine.play(b, Action.tool(Kinds.TOOL_CLAW, idx(3, 3), idx(3, 4)), true);
        assertNotEquals(TurnResult.Status.INVALID, r.status);
        assertNotNull(first(r, Step.Type.COMBO_DETONATE));
        assertEquals(20, b.movesLeft);
        assertEquals(0, b.tools[Kinds.TOOL_CLAW]);
    }

    @Test
    void nitroCrossingTrackBaseplateSpreadsTrackAcrossRow() {
        String[] fl = new String[8];
        for (int r = 0; r < 8; r++) fl[r] = "........";
        fl[3] = ".......t";
        Board b = new LB(set(base(), 3, 0, 'h')).floor(fl).goal("TRACK", null, -1).board();
        TurnEngine.play(b, Action.tap(idx(3, 0)), true);
        for (int c = 0; c < 8; c++) assertEquals(Kinds.FLOOR_TRACK, b.floor[idx(3, c)], "col " + c);
    }

    @Test
    void blueprintHintFindsExactWinningMoveAndDetectsDeadEnd() {
        Board b = new LB(MatchAndPowerTest.fourInRow()).put("kind", "BLUEPRINT").put("moves", 1L).put("exactMoves", 1L)
                .goal("COLOR", "RED", 4).board();
        Action hint = com.brickyard.engine.rules.BlueprintHint.solveNext(b);
        assertNotNull(hint);
        TurnResult r = TurnEngine.play(b.copy(), hint, false);
        assertEquals(TurnResult.Status.WON, r.status);

        Board impossible = new LB(base()).put("kind", "BLUEPRINT").put("moves", 1L).put("exactMoves", 1L)
                .goal("COLOR", "RED", 99).board();
        assertNull(com.brickyard.engine.rules.BlueprintHint.solveNext(impossible));
    }
}
