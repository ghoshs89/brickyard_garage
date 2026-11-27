package com.brickyard.engine.rules;

import java.util.ArrayList;
import java.util.List;

public final class TurnResult {
    public enum Status { INVALID, PLAYING, WON, LOST }

    /** Process-praise tags: they celebrate how he played, never what he "is". */
    public enum Praise { SETUP_CRAFTED, COMBO_CHAIN, BIG_CLEAR, SMART_CLAW, MOVES_SAVED, TEAM_COMBO }

    public Status status = Status.PLAYING;
    public boolean moveSpent;
    public boolean bounced;
    public final List<Step> steps = new ArrayList<>();
    public final List<Praise> praise = new ArrayList<>();
    public int obstaclesCleared;
    public int coinsAwarded;      // on WON: baseCoins + 2 * movesLeft
    public int movesLeftAtWin;
    public boolean shuffled;
}
