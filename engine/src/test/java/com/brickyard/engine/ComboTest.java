package com.brickyard.engine;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.Step;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.brickyard.engine.T.*;
import static org.junit.jupiter.api.Assertions.*;

/** All 10 power-up pairings. The swipe destination (3,4) is the detonation point. */
class ComboTest {

    private TurnResult combo(char a, char b) {
        String[] L = set(set(base(), 3, 3, a), 3, 4, b);
        Board bd = new LB(L).board();
        TurnResult r = TurnEngine.play(bd, Action.swap(idx(3, 3), idx(3, 4)), true);
        assertTrue(r.moveSpent);
        assertTrue(r.praise.contains(TurnResult.Praise.COMBO_CHAIN));
        return r;
    }

    private static int unique(int[] cells) {
        Set<Integer> s = new HashSet<>();
        for (int c : cells) s.add(c);
        return s.size();
    }

    @Test
    void nitroPlusNitroClearsRowAndColumn() {
        Step s = first(combo('h', 'v'), Step.Type.COMBO_DETONATE);
        assertEquals(15, unique(s.cells));
    }

    @Test
    void airbagPlusNitroClearsThreeRowsAndThreeColumns() {
        Step s = first(combo('a', 'h'), Step.Type.COMBO_DETONATE);
        assertEquals(24 + 24 - 9, unique(s.cells));
    }

    @Test
    void airbagPlusAirbagDoublesTheBlast() {
        Step s = first(combo('a', 'a'), Step.Type.COMBO_DETONATE);
        int expected = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) if (Math.abs(r - 3) + Math.abs(c - 4) <= 4) expected++;
        assertEquals(expected, unique(s.cells));
    }

    @Test
    void sprayerPlusSprayerHitsEveryCell() {
        Step s = first(combo('s', 's'), Step.Type.COMBO_DETONATE);
        assertEquals(64, unique(s.cells));
    }

    @Test
    void buggyPlusBuggyLaunchesThreeBuggies() {
        TurnResult r = combo('u', 'u');
        assertEquals(3, countSteps(r, Step.Type.BUGGY_FLIGHT));
    }

    @Test
    void buggyCarriesNitroToItsTarget() {
        TurnResult r = combo('u', 'v');
        Step flight = first(r, Step.Type.BUGGY_FLIGHT);
        assertEquals(Kinds.NITRO_V, flight.kind);
        assertTrue(flight.b >= 0);
        // The carried Nitro fires at the buggy's target cell.
        boolean firedAtTarget = false;
        for (Step s : r.steps) if (s.type == Step.Type.POWERUP_FIRE && s.a == flight.b && s.kind == Kinds.NITRO_V) firedAtTarget = true;
        assertTrue(firedAtTarget);
    }

    @Test
    void buggyCarriesAirbag() {
        TurnResult r = combo('a', 'u');
        Step flight = first(r, Step.Type.BUGGY_FLIGHT);
        assertEquals(Kinds.AIRBAG, flight.kind);
    }

    @Test
    void sprayerPlusPowerUpConvertsMostCommonColour() {
        // Base pattern minus the two combo cells: Blue 16, Orange 16, Yellow 15, Green 15 -> Blue (lowest index on a tie).
        TurnResult r = combo('s', 'u');
        Step s = first(r, Step.Type.COMBO_DETONATE);
        assertEquals(16, s.cells.length);
        assertTrue(countSteps(r, Step.Type.BUGGY_FLIGHT) >= 16);
    }

    @Test
    void sprayerPlusNitroAndAirbag() {
        assertEquals(16, first(combo('v', 's'), Step.Type.COMBO_DETONATE).cells.length);
        assertEquals(16, first(combo('s', 'a'), Step.Type.COMBO_DETONATE).cells.length);
    }
}
