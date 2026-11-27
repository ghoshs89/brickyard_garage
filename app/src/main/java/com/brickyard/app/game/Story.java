package com.brickyard.app.game;

import com.brickyard.app.art.CharacterArt;

import java.util.ArrayList;
import java.util.List;

/** Short, skippable conversations. Process praise only; Bo is slow and his stories hold the answer. */
public final class Story {
    private Story() {}

    public static final class Line {
        public final int who, mood;
        public final String text;
        public Line(int who, int mood, String text) { this.who = who; this.mood = mood; this.text = text; }
    }

    static Line bo(int mood, String s) { return new Line(CharacterArt.BO, mood, s); }
    static Line juno(int mood, String s) { return new Line(CharacterArt.JUNO, mood, s); }
    static Line pup(int mood, String s) { return new Line(CharacterArt.SPROCKET, mood, s); }

    public static List<Line> intro(String player) {
        List<Line> l = new ArrayList<>();
        l.add(juno(2, "Grandpa Bo! The old garage! It's been shut for years!"));
        l.add(bo(0, "Ah, " + player + ". Good. I need a Chief Engineer with fast eyes. My eyes are... slow these days."));
        l.add(bo(1, "Under all this dust are three bays and three brick cars I never finished. The town Car Show is coming."));
        l.add(juno(0, "We match parts on the puzzle board to earn stars, and stars pay for repairs!"));
        l.add(pup(0, "Woof!"));
        l.add(bo(3, "That's Sprocket. If a part goes missing, check his bed first. Shall we start?"));
        return l;
    }

    public static List<Line> bayIntro(int bay, String text) {
        List<Line> l = new ArrayList<>();
        l.add(bo(1, text));
        if (bay == 2) l.add(juno(2, "A crane! And a whole rescue rig under that tarp!"));
        if (bay == 3) l.add(juno(2, "A wind tunnel? Grandpa, you built a WIND TUNNEL?"));
        return l;
    }

    public static List<Line> repair(String story) {
        List<Line> l = new ArrayList<>();
        // Story lines alternate speakers by sentence; Bo usually has the last word.
        l.add(bo(0, story));
        return l;
    }

    public static List<Line> bayDone(String outro, int bay) {
        List<Line> l = new ArrayList<>();
        l.add(juno(3, outro));
        if (bay < 3) l.add(bo(0, "One bay done. Slow and steady built every car I ever loved. Next bay is open!"));
        return l;
    }

    public static List<Line> finale(String player) {
        List<Line> l = new ArrayList<>();
        l.add(juno(2, "Grandpa, look! The whole town came to the Car Show!"));
        l.add(bo(3, "Three cars. Built by a fast thinker and a slow checker. Best team I ever had."));
        l.add(juno(0, player + ", you were Chief Engineer the whole way. Every repair, every gear, every puzzle."));
        l.add(pup(3, "Woof! (Sprocket drops a golden brick at your feet. He's been saving it.)"));
        l.add(bo(0, "The garage stays open. Track Day is unlocked: new boards every round, as many as you like."));
        return l;
    }

    public static Line closing() {
        return bo(0, "That's the shop closed for today. The cars are cooling down on the lifts. Great building today, see you tomorrow!");
    }

    public static String praise(com.brickyard.engine.rules.TurnResult.Praise p, int movesLeft) {
        switch (p) {
            case SETUP_CRAFTED: return "You built a power-up on purpose!";
            case COMBO_CHAIN: return "Power-up combo! Great plan!";
            case BIG_CLEAR: return "Cleared lots of obstacles in one move!";
            case SMART_CLAW: return "Smart Claw move: you made a Sprayer!";
            case MOVES_SAVED: return "Saved " + movesLeft + " moves!";
            default: return "";
        }
    }

    /** Loss lines: effort and data, never "you failed". */
    public static String[] TRY_AGAIN = {
            "Every try teaches the board something. Let's go again!",
            "Bo says: \"I needed six tries on this one in '72.\"",
            "You got closer! Look where the obstacles were left.",
            "Try a different first move this time and see what changes.",
    };
}
