package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.GameView;
import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Progress;
import com.brickyard.engine.rules.TurnResult;

/** End of a level. Wins show the coin maths; losses show how close he got, never "fail". */
public final class ResultScreen extends Screen implements GameView.Overlay {
    private final Level L;
    private final TurnResult r;
    private final double progress, prevBest;
    private final boolean won, together;
    private Progress.Reward reward;
    private int coins;
    private final Button primary, secondary;

    public ResultScreen(final Game g, final Level L, TurnResult r, double progress, double prevBest, final boolean together) {
        super(g);
        this.L = L;
        this.r = r;
        this.progress = progress;
        this.prevBest = prevBest;
        this.together = together;
        won = r.status == TurnResult.Status.WON;
        if (won) {
            if (L.kind == Level.Kind.STORY) {
                reward = g.progress.winStory(L.id, r.coinsAwarded);
                coins = reward.coins;
            } else if (L.isBlueprint()) {
                reward = g.progress.winBlueprint(L.id);
                coins = reward.coins;
            } else {
                coins = r.coinsAwarded;
                g.profile.coins += coins;
                g.profile.trackDayRound++;
                g.profile.trackDayBest = Math.max(g.profile.trackDayBest, g.profile.trackDayRound);
            }
            g.save();
            g.audio.play("win");
            if (reward != null && reward.star) g.audio.play("star");
        } else {
            g.audio.play("retry");
        }
        primary = new Button(won ? "Continue" : "Try again", won ? Pal.GREEN_OK : 0xFFFF8A1F, new Runnable() {
            @Override public void run() {
                if (won) {
                    g.home();
                    g.checkClosingTime();
                } else {
                    g.pop();
                    g.startLevel(L, together);
                }
            }
        });
        secondary = new Button(won ? "Play again" : "Back to garage", 0xFFBDB6A8, new Runnable() {
            @Override public void run() {
                if (won) { g.pop(); g.startLevel(L, together); }
                else g.home();
            }
        });
        buttons.add(primary);
        buttons.add(secondary);
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float cx = vw / 2;
        primary.at(cx + 20, vh - 190, 320, 110);
        secondary.at(cx - 340, vh - 190, 320, 110);
    }

    public boolean won() { return won; }
    public String levelId() { return L.id; }

    /** Test autopilot: press the main button. */
    public void autoContinue() {
        primary.action.run();
    }

    @Override
    public boolean onBack() {
        g.home();
        return true;
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0x99000000));
        float l = vw / 2 - 520, t = 50, rgt = vw / 2 + 520, b = vh - 60;
        Ui.panel(c, l, t, rgt, b, Pal.PANEL);
        CharacterArt.draw(c, won ? CharacterArt.JUNO : CharacterArt.BO, l + 20, t + 60, 300, won ? 3 : 0, time);
        float x = l + 330;
        if (won) {
            String title = L.isBlueprint() ? "Blueprint solved!" : L.kind == Level.Kind.TRACK_DAY ? "Round complete!" : "Level complete!";
            Ui.label(c, title, x, t + 90, 56, Pal.INK, Paint.Align.LEFT);
            float y = t + 170;
            if (reward != null && reward.star) {
                float pulse = 1 + 0.08f * (float) Math.sin(time * 5);
                Draw.star(c, x + 40, y - 14, 40 * pulse, Pal.GOLD, 4);
                Ui.label(c, "+1 star for repairs!", x + 100, y, 38, Pal.INK, Paint.Align.LEFT);
                y += 70;
            } else if (L.kind == Level.Kind.STORY) {
                Ui.label(c, "Replay: stars only come once, coins still count.", x, y, 28, 0xFF8A8072, Paint.Align.LEFT);
                y += 56;
            }
            // the coin maths, spelled out
            Draw.coin(c, x + 24, y - 12, 22);
            String eq;
            if (L.kind == Level.Kind.STORY || L.kind == Level.Kind.TRACK_DAY) {
                int base = L.baseCoins, left = r.movesLeftAtWin;
                eq = "Base " + base + " + 2 × " + left + " moves left = " + (base + 2 * left);
                if (reward != null && !reward.firstTime) eq += ", half for a replay = " + coins;
            } else eq = coins > 0 ? "Blueprint reward: " + coins + " coins" : "Already solved before: no new reward";
            Ui.label(c, eq, x + 60, y, 32, Pal.INK, Paint.Align.LEFT);
            y += 60;
            if (reward != null && reward.tool >= 0) {
                Ui.label(c, "New tool: " + Kinds.TOOL_NAMES[reward.tool] + " (costs no moves)", x, y, 30, Pal.TEAL, Paint.Align.LEFT);
                y += 52;
            }
            if (reward != null && reward.part != null) {
                Ui.label(c, "New brick part: " + g.content.garage.parts.get(reward.part).name + "!", x, y, 30, Pal.GOLD_DARK, Paint.Align.LEFT);
                y += 52;
            }
            for (TurnResult.Praise p : r.praise) {
                Ui.label(c, "• " + Story.praise(p, r.movesLeftAtWin), x, y, 30, 0xFF5A4A36, Paint.Align.LEFT);
                y += 46;
            }
            if (L.kind == Level.Kind.STORY && g.profile.stars > 0) {
                Ui.label(c, "You have " + g.profile.stars + " star" + (g.profile.stars == 1 ? "" : "s") + " to spend on repairs.", x, Math.max(y, b - 230), 28, Pal.TEAL, Paint.Align.LEFT);
            }
        } else {
            Ui.label(c, "Out of moves. So close!", x, t + 90, 50, Pal.INK, Paint.Align.LEFT);
            int pct = (int) Math.round(progress * 100), best = (int) Math.round(prevBest * 100);
            boolean closest = progress > prevBest + 1e-9 && prevBest > 0;
            String msg = closest ? "Goals " + pct + "% done: your closest yet!"
                    : prevBest > 0 ? "Goals " + pct + "% done. Your best so far: " + best + "%" : "Goals " + pct + "% done on your first try.";
            Ui.label(c, msg, x, t + 160, 34, closest ? Pal.GREEN_OK : Pal.INK, Paint.Align.LEFT);
            Ui.bar(c, x, t + 190, rgt - 50, t + 240, (float) progress, closest ? Pal.GREEN_OK : 0xFFFF8A1F, (float) Math.max(prevBest, progress));
            int attempts = g.progress.attempts(L.id);
            String tip = Story.TRY_AGAIN[attempts % Story.TRY_AGAIN.length];
            Ui.wrap(c, tip, x, t + 310, rgt - x - 50, 32, 0xFF5A4A36, Paint.Align.LEFT);
            if (g.progress.hintAvailable(L.id)) Ui.label(c, "The hint bulb is switched on for this board.", x, t + 420, 28, Pal.TEAL, Paint.Align.LEFT);
        }
        drawButtons(c);
    }
}
