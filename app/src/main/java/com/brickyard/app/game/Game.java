package com.brickyard.app.game;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.WindowManager;

import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.art.PieceArt;
import com.brickyard.app.screens.ClosingScreen;
import com.brickyard.app.screens.HubScreen;
import com.brickyard.app.screens.PlayScreen;
import com.brickyard.app.storage.SaveRepository;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Profile;
import com.brickyard.engine.profile.Progress;
import com.brickyard.engine.trackday.TrackDayGenerator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/** Holds content, the saved profile, audio, the screen stack and the dialogue overlay. */
public final class Game {
    public final Activity activity;
    public final ContentStore content;
    public final SaveRepository repo;
    public final Audio audio;
    public final PieceArt pieces = new PieceArt();
    public Profile profile;
    public Progress progress;

    private final List<Screen> stack = new ArrayList<>();
    public float vw = 1280, vh = 800, scale = 1;

    // dialogue overlay
    private List<Story.Line> lines;
    private int lineIdx;
    private Runnable afterLines;
    private final Button skip = new Button("Skip", 0xFFBDB6A8, null);
    private float lineTime;

    /** Animation speed multiplier (the test autopilot runs fast). */
    public float speed = 1;
    public Autopilot autopilot;

    // Shop Closing Time (persisted in Profile so app restarts cannot bypass daily limits)
    private long resumeAt = System.currentTimeMillis();

    private void syncSessionDay() {
        long d = today();
        if (profile.sessionDay != d) {
            profile.sessionDay = d;
            profile.sessionMillis = 0;
            profile.sessionLevels = 0;
        }
    }

    private void flushSessionTime() {
        syncSessionDay();
        long now = System.currentTimeMillis();
        if (now > resumeAt) profile.sessionMillis += Math.min(now - resumeAt, 3600_000L);
        resumeAt = now;
    }

    public Game(Activity activity) throws Exception {
        this.activity = activity;
        content = new ContentStore(activity.getAssets());
        repo = new SaveRepository(activity);
        audio = new Audio(activity);
        profile = repo.load();
        progress = new Progress(profile, content.campaign, content.garage);
        audio.soundOn = profile.sound;
        audio.musicOn = profile.music;
        push(new HubScreen(this));
        // Reopening the app drops him straight back onto the board he left.
        if (profile.inProgress != null && !shopClosed()) {
            try {
                resumeLevel(profile.inProgress);
            } catch (RuntimeException e) {
                profile.inProgress = null;
                save();
            }
        }
    }

    // ------------------------------------------------------------------ screens

    public Screen top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public Screen below(Screen s) {
        int i = stack.indexOf(s);
        return i > 0 ? stack.get(i - 1) : null;
    }

    public void push(Screen s) {
        stack.add(s);
        s.layout(vw, vh);
        s.enter();
    }

    public void pop() {
        if (stack.size() > 1) stack.remove(stack.size() - 1);
        Screen t = top();
        if (t != null) { t.layout(vw, vh); t.enter(); }
    }

    public void replace(Screen s) {
        if (!stack.isEmpty()) stack.remove(stack.size() - 1);
        push(s);
    }

    /** Back to the garage hub, clearing everything above it. */
    public void home() {
        while (stack.size() > 1) stack.remove(stack.size() - 1);
        Screen t = top();
        t.layout(vw, vh);
        t.enter();
    }

    public void resize(float vw, float vh, float scale) {
        this.vw = vw;
        this.vh = vh;
        this.scale = scale;
        for (Screen s : stack) s.layout(vw, vh);
    }

    public void save() {
        profile.sound = audio.soundOn;
        profile.music = audio.musicOn;
        repo.save(profile);
    }

    public void keepAwake(final boolean on) {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                if (on) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    // ------------------------------------------------------------------ levels

    public Level level(String id) {
        if (id.startsWith("trackday-")) return TrackDayGenerator.generate(Integer.parseInt(id.substring(9)));
        return content.levels.get(id);
    }

    public void startLevel(Level L, boolean together) {
        // Blueprints are fixed boards; story and Track Day boards vary per attempt so retries feel fresh.
        long seed = L.isBlueprint() ? L.seed : L.seed * 31 + progress.attempts(L.id) * 7919L + System.nanoTime() % 1000;
        Board b = Board.create(L, seed);
        if (!L.isBlueprint()) b.tools = profile.tools.clone();
        push(new PlayScreen(this, L, b, together, 0));
    }

    /** Hub "Continue": reopen the saved board exactly as it was. */
    public void resumeSaved() {
        if (profile.inProgress == null) return;
        try {
            resumeLevel(profile.inProgress);
        } catch (RuntimeException e) {
            profile.inProgress = null;
            save();
        }
    }

    @SuppressWarnings("unchecked")
    private void resumeLevel(Map<String, Object> ip) {
        Level L = level((String) ip.get("level"));
        if (L == null) throw new IllegalStateException("level gone");
        Board b = Board.fromJson(L, (Map<String, Object>) ip.get("board"));
        boolean together = Boolean.TRUE.equals(ip.get("together"));
        int turn = ip.get("turnPlayer") instanceof Number ? ((Number) ip.get("turnPlayer")).intValue() : 0;
        push(new PlayScreen(this, L, b, together, turn));
    }

    // ------------------------------------------------------------------ Shop Closing Time

    public static long today() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }

    public boolean shopClosed() {
        return profile.closedUntilDay >= 0 && today() <= profile.closedUntilDay;
    }

    /** Called after a level (countLevel=true) or a repair (countLevel=false): never cuts off mid-level. */
    public boolean checkClosingTime() {
        return checkClosingTime(true);
    }

    public boolean checkClosingTime(boolean countLevel) {
        flushSessionTime();
        if (countLevel) profile.sessionLevels++;
        String mode = profile.closingTime;
        long mins = profile.sessionMillis / 60000L;
        boolean close = (mode.equals("MIN20") && mins >= 20) || (mode.equals("MIN30") && mins >= 30)
                || (mode.equals("LEVELS5") && profile.sessionLevels >= 5);
        if (close) {
            profile.closedUntilDay = today();
            save();
            home();
            push(new ClosingScreen(this));
        } else {
            save();
        }
        return close;
    }

    public void reopenShop() {
        profile.closedUntilDay = -1;
        profile.sessionDay = today();
        profile.sessionMillis = 0;
        profile.sessionLevels = 0;
        resumeAt = System.currentTimeMillis();
        save();
    }

    // ------------------------------------------------------------------ dialogue overlay

    public void say(List<Story.Line> l, Runnable then) {
        if (l == null || l.isEmpty()) { if (then != null) then.run(); return; }
        lines = l;
        lineIdx = 0;
        lineTime = 0;
        afterLines = then;
    }

    public boolean talking() {
        return lines != null;
    }

    public void updateOverlay(float dt) {
        lineTime += dt;
    }

    private void endLines() {
        lines = null;
        Runnable r = afterLines;
        afterLines = null;
        if (r != null) r.run();
    }

    /** Autopilot: finish the current conversation immediately. */
    public void skipLines() {
        if (lines != null) endLines();
    }

    public boolean overlayDown(float x, float y) {
        if (lines == null) return false;
        if (skip.hit(x, y)) { endLines(); return true; }
        if (lineTime < 0.25f) return true;
        lineIdx++;
        lineTime = 0;
        if (lineIdx >= lines.size()) endLines();
        return true;
    }

    public void drawOverlay(Canvas c, float t) {
        if (lines == null) return;
        Story.Line line = lines.get(Math.min(lineIdx, lines.size() - 1));
        c.drawRect(0, 0, vw, vh, Draw.fill(0x66000000));
        float boxL = 60, boxR = vw - 60, boxT = vh - 300, boxB = vh - 40;
        CharacterArt.draw(c, line.who, boxL + 10, boxT - 250, 300, line.mood, t);
        Ui.panel(c, new RectF(boxL + 260, boxT, boxR, boxB), Pal.PANEL);
        Ui.label(c, CharacterArt.NAMES[line.who], boxL + 300, boxT + 56, 38, line.who == 0 ? 0xFF2E4F7A : line.who == 1 ? Pal.TEAL : Pal.RED_BAD, android.graphics.Paint.Align.LEFT);
        // type-on effect, fast
        int chars = Math.min(line.text.length(), (int) (lineTime * 90));
        Ui.wrap(c, line.text.substring(0, chars), boxL + 300, boxT + 110, boxR - boxL - 340, 34, Pal.INK, android.graphics.Paint.Align.LEFT);
        if (chars >= line.text.length() && ((int) (t * 2)) % 2 == 0) Ui.label(c, "tap ▶", boxR - 30, boxB - 24, 26, 0xFF8A8072, android.graphics.Paint.Align.RIGHT);
        skip.at(vw - 200, 30, 160, 76);
        skip.draw(c);
    }

    public void onPause() {
        flushSessionTime();
        save();
        audio.pause();
    }

    public void onResume() {
        syncSessionDay();
        resumeAt = System.currentTimeMillis();
        audio.resume();
    }
}
