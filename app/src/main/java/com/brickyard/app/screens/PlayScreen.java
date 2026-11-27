package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.GoalSpec;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.Hint;
import com.brickyard.engine.rules.Moves;
import com.brickyard.engine.rules.Step;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The match-3 board: swipe or tap-tap to swap, tap a power-up to fire it. */
public final class PlayScreen extends Screen {
    private final Level L;
    private final Board board;
    private final boolean together;
    private int turnPlayer;            // Together mode: 0 = player, 1 = partner
    private final BoardRenderer br;

    // animation playback
    private Step.Snapshot shown;       // what is on screen when idle
    private final List<Step> queue = new ArrayList<>();
    private Step.Snapshot prev;
    private float stepT, stepDur;
    private int cascade;
    private TurnResult pending;

    // input
    private int downCell = -1, selected = -1;
    private float downX, downY;
    private boolean swiped;
    private int toolMode = -1, clawFirst = -1;
    private boolean suggestMode;
    private int suggestCell = -1;
    private float suggestT;
    private Action hint;
    private float hintT;

    // feedback
    private final List<float[]> popups = new ArrayList<>(); // {x, y, age}
    private final List<String> popupText = new ArrayList<>();
    private float bannerT = 3;

    private final Button back, hintBtn, suggestBtn;
    private final Button[] toolBtns = new Button[3];
    private final double bestBefore;

    public PlayScreen(Game g, Level L, Board board, boolean together, int turnPlayer) {
        super(g);
        this.L = L;
        this.board = board;
        this.together = together;
        this.turnPlayer = turnPlayer;
        this.br = new BoardRenderer(L);
        this.shown = new Step.Snapshot(board);
        Double b = g.profile.best.get(L.id);
        bestBefore = b == null ? 0 : b;
        back = new Button("", 0xFFBDB6A8, new Runnable() { @Override public void run() { leave(); } });
        back.icon = new Button.Icon() { @Override public void draw(Canvas c, float x, float y, float s) { arrowLeft(c, x, y, s); } };
        hintBtn = new Button("Hint", 0xFFFFE066, new Runnable() { @Override public void run() { showHint(); } });
        hintBtn.icon = new Button.Icon() { @Override public void draw(Canvas c, float x, float y, float s) { bulb(c, x, y, s); } };
        suggestBtn = new Button("Suggest", 0xFF9FD3E8, new Runnable() { @Override public void run() { suggestMode = !suggestMode; suggestCell = -1; } });
        buttons.add(back);
        buttons.add(hintBtn);
        buttons.add(suggestBtn);
        for (int k = 0; k < 3; k++) {
            final int tool = k;
            toolBtns[k] = new Button("", 0xFFE8C89A, new Runnable() { @Override public void run() { toggleTool(tool); } });
            toolBtns[k].icon = toolIcon(k);
            buttons.add(toolBtns[k]);
        }
    }

    @Override
    public void enter() {
        g.audio.music(L.isBlueprint() ? "focus" : "workshop");
        g.keepAwake(true);
        // Saved from the very start, so even a board with no moves yet reopens exactly as it was.
        if (pending == null && queue.isEmpty()) saveInProgress();
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float top = L.isBlueprint() ? 150 : 40;
        float avail = Math.min(vh - top - 40, vw - 560);
        float tile = (float) Math.floor(avail / Math.max(L.rows, L.cols));
        float bw = tile * L.cols, bh = tile * L.rows;
        float x0 = 60 + (vw - 560 - bw) / 2, y0 = top + (vh - top - 30 - bh) / 2;
        br.layout(Math.max(40, x0), y0, tile);
        g.pieces.setTilePixels(Math.round(tile * g.scale));
        float px = vw - 470;
        back.at(px + 330, 40, 110, 84);
        hintBtn.at(px, vh - 130, 200, 96);
        suggestBtn.at(px + 220, vh - 130, 210, 96);
        for (int k = 0; k < 3; k++) toolBtns[k].at(px + k * 145, vh - 250, 130, 100);
    }

    // =================================================================== update

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean canHint = !L.isBlueprint() || g.progress.attempts(L.id) >= 3;
        hintBtn.visible = g.progress.hintAvailable(L.id) && canHint && !busy();
        suggestBtn.visible = together && !busy();
        suggestBtn.label = suggestMode ? "Pointing..." : "Suggest";
        for (int k = 0; k < 3; k++) {
            toolBtns[k].visible = !L.isBlueprint() && board.tools[k] > 0;
            toolBtns[k].badge = String.valueOf(board.tools[k]);
            toolBtns[k].color = toolMode == k ? 0xFF7FDBFF : 0xFFE8C89A;
            toolBtns[k].enabled = !busy();
        }
        back.enabled = !busy();
        hintT += dt;
        suggestT += dt;
        bannerT += dt;
        for (int k = popups.size() - 1; k >= 0; k--) {
            popups.get(k)[2] += dt;
            if (popups.get(k)[2] > 1.8f) { popups.remove(k); popupText.remove(k); }
        }
        if (!queue.isEmpty()) {
            stepT += dt * g.speed;
            if (stepT >= stepDur) {
                Step s = queue.remove(0);
                prev = s.after;
                shown = s.after;
                if (!queue.isEmpty()) startStep();
                else finishTurn();
            }
        }
    }

    /** Test autopilot: plays the hint move whenever the board is idle. */
    public void autoStep() {
        if (busy() || g.talking()) return;
        Action a = Hint.best(board);
        if (a != null) play(a);
    }

    private boolean busy() {
        return !queue.isEmpty() || pending != null && pending.status != TurnResult.Status.PLAYING;
    }

    private void startStep() {
        Step s = queue.get(0);
        stepT = 0;
        switch (s.type) {
            case SWAP: stepDur = 0.16f; g.audio.play("swap"); break;
            case SWAP_BOUNCE: stepDur = 0.32f; g.audio.play("bounce"); break;
            case MATCH_POP: stepDur = 0.2f; g.audio.play("snap_" + Math.min(4, cascade), 0.9f, 1f); cascade++; break;
            case POWERUP_SPAWN: stepDur = 0.18f; g.audio.play("ratchet", 0.5f, 1.2f); break;
            case POWERUP_FIRE:
                stepDur = s.kind == Kinds.SPRAYER ? 0.45f : 0.28f;
                g.audio.play(s.kind == Kinds.AIRBAG ? "thump" : s.kind == Kinds.SPRAYER ? "spray" : s.kind == Kinds.BUGGY ? "whirr" : "nitro");
                break;
            case COMBO_DETONATE: stepDur = 0.5f; g.audio.play("thump"); break;
            case BUGGY_FLIGHT: stepDur = 0.38f; g.audio.play("whirr", 0.7f, 1.2f); break;
            case GRAVITY_DROP: stepDur = 0.24f; break;
            case CONVEYOR_SHIFT: stepDur = 0.32f; g.audio.play("mesh", 0.6f, 1f); break;
            case OIL_SPREAD: stepDur = 0.3f; g.audio.play("oil"); break;
            case PART_COLLECT: stepDur = 0.4f; g.audio.play("part"); break;
            case DEADLOCK_SHUFFLE: stepDur = 0.6f; g.audio.play("hiss"); popup("No moves left: free shuffle!", vw / 2 - 200, vh / 2); break;
            case VICTORY_LAP: stepDur = 0.7f; g.audio.play("win"); break;
            case TOOL_USE: stepDur = 0.28f; g.audio.play(s.kind == Kinds.TOOL_WRENCH ? "ratchet" : s.kind == Kinds.TOOL_CLAW ? "whirr" : "hiss"); break;
            default: stepDur = 0.2f;
        }
    }

    private void play(Action a) {
        if (busy()) return;
        hint = null;
        selected = -1;
        TurnResult r = TurnEngine.play(board, a, true);
        if (r.status == TurnResult.Status.INVALID) { g.audio.play("bounce"); return; }
        pending = r;
        cascade = 0;
        prev = shown;
        queue.clear();
        queue.addAll(r.steps);
        if (queue.isEmpty()) finishTurn();
        else startStep();
    }

    /** Called when every animation step of a turn has played. */
    private void finishTurn() {
        TurnResult r = pending;
        if (r == null) return;
        shown = new Step.Snapshot(board);
        for (TurnResult.Praise p : r.praise) popup(Story.praise(p, r.movesLeftAtWin), vw - 470, 520 - popups.size() * 50);
        if (r.status == TurnResult.Status.PLAYING) {
            pending = null;
            if (together && r.moveSpent) { turnPlayer = 1 - turnPlayer; bannerT = 0; suggestCell = -1; }
            saveInProgress();
        } else {
            g.profile.inProgress = null;
            double progress = board.progress();
            double prevBest = g.progress.recordAttempt(L.id, r.status == TurnResult.Status.WON ? 1.0 : progress);
            g.save();
            final ResultScreen rs = new ResultScreen(g, L, r, progress, prevBest, together);
            pending = null;
            g.keepAwake(false);
            g.replace(rs);
        }
    }

    private void saveInProgress() {
        Map<String, Object> ip = new LinkedHashMap<>();
        ip.put("level", L.id);
        ip.put("together", together);
        ip.put("turnPlayer", (long) turnPlayer);
        ip.put("board", board.toJson());
        g.profile.inProgress = ip;
        if (!L.isBlueprint()) System.arraycopy(board.tools, 0, g.profile.tools, 0, 3);
        g.save();
    }

    private void leave() {
        if (busy()) return;
        saveInProgress(); // the board waits for him on the hub's Continue button
        g.keepAwake(false);
        g.home();
    }

    @Override
    public boolean onBack() {
        leave();
        return true;
    }

    // =================================================================== input

    @Override
    public boolean onDown(float x, float y) {
        if (super.onDown(x, y)) return true;
        if (busy()) return true;
        int cell = br.cellAt(x, y);
        downCell = cell;
        downX = x;
        downY = y;
        swiped = false;
        return true;
    }

    @Override
    public void onMove(float x, float y) {
        super.onMove(x, y);
        if (downCell < 0 || swiped || toolMode >= 0 || suggestMode || busy()) return;
        float dx = x - downX, dy = y - downY;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < br.tile * 0.35f) return;
        int r = downCell / L.cols, c = downCell % L.cols;
        if (Math.abs(dx) > Math.abs(dy)) c += dx > 0 ? 1 : -1; else r += dy > 0 ? 1 : -1;
        swiped = true;
        if (r < 0 || c < 0 || r >= L.rows || c >= L.cols) return;
        int to = r * L.cols + c;
        if (!L.active[to]) return;
        play(Action.swap(downCell, to));
        downCell = -1;
    }

    @Override
    public void onUp(float x, float y) {
        super.onUp(x, y);
        int cell = downCell;
        downCell = -1;
        if (swiped || cell < 0 || busy()) return;
        if (suggestMode) {
            suggestCell = cell;
            suggestT = 0;
            suggestMode = false;
            g.audio.play("tap", 1f, 1.4f);
            return;
        }
        if (toolMode >= 0) { useTool(cell); return; }
        if (selected >= 0 && selected != cell && Moves.adjacent(board, selected, cell)) {
            play(Action.swap(selected, cell));
            return;
        }
        if (selected < 0 && Moves.isTappable(board, cell)) {
            play(Action.tap(cell));
            return;
        }
        selected = selected == cell ? -1 : cell;
        g.audio.play("tap", 0.5f, 1.2f);
    }

    private void toggleTool(int k) {
        if (busy()) return;
        toolMode = toolMode == k ? -1 : k;
        clawFirst = -1;
        selected = -1;
    }

    private void useTool(int cell) {
        if (toolMode == Kinds.TOOL_CLAW) {
            if (clawFirst < 0) { clawFirst = cell; return; }
            if (!Moves.adjacent(board, clawFirst, cell)) { clawFirst = cell; return; }
            int a = clawFirst;
            clawFirst = -1;
            int t = toolMode;
            toolMode = -1;
            play(Action.tool(t, a, cell));
        } else {
            int t = toolMode;
            toolMode = -1;
            play(Action.tool(t, cell, -1));
        }
    }

    private void showHint() {
        if (busy()) return;
        hint = Hint.best(board);
        hintT = 0;
        g.audio.play("star", 0.4f, 1.5f);
    }

    private void popup(String s, float x, float y) {
        popups.add(new float[]{x, y, 0});
        popupText.add(s);
    }

    // =================================================================== drawing

    @Override
    public void draw(Canvas c) {
        int bg = L.isBlueprint() ? 0xFF1F3B5C : L.bay == 2 ? 0xFF4F6B57 : L.bay == 3 ? 0xFF3E5566 : 0xFF7A4A38;
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, Pal.lighter(bg, 0.15f), bg, 0);
        if (L.isBlueprint()) { // blueprint grid paper
            for (float x = 0; x < vw; x += 40) c.drawLine(x, 0, x, vh, Draw.line(0x22FFFFFF, 1.5f));
            for (float y = 0; y < vh; y += 40) c.drawLine(0, y, vw, y, Draw.line(0x22FFFFFF, 1.5f));
        }
        Step cur = queue.isEmpty() ? null : queue.get(0);
        Step.Snapshot after = cur == null ? shown : cur.after;
        Step.Snapshot before = cur == null ? null : prev;
        float p = cur == null ? 1 : Math.min(1, stepT / stepDur);
        if (cur != null && cur.type == Step.Type.SWAP_BOUNCE) {
            // there and back again
            Step.Snapshot swapped = swappedCopy(prev, cur.a, cur.b);
            float pp = p < 0.5f ? p * 2 : (1 - p) * 2;
            br.drawBase(c, prev, time);
            br.drawEntities(c, g.pieces, prev, swapped, pp, Step.Type.SWAP, time, -1);
        } else {
            br.drawBase(c, before != null && p < 1 ? before : after, time);
            br.drawEntities(c, g.pieces, before, after, p, cur == null ? null : cur.type, time, selected);
        }
        if (cur != null) drawEffect(c, cur, p);
        if (L.isBlueprint()) drawRefillQueue(c);
        if (hint != null && hintT < 6) {
            br.ring(c, hint.a, 0xFFFFE066, time, false);
            if (hint.b >= 0) br.ring(c, hint.b, 0xFFFFE066, time, false);
        }
        if (suggestCell >= 0 && suggestT < 8) {
            br.ring(c, suggestCell, 0xFF7FDBFF, time, true);
            Ui.center(c, "What about here?", br.cx(suggestCell), br.cy(suggestCell) - br.tile * 0.7f, 26, 0xFFFFFFFF);
        }
        if (toolMode == Kinds.TOOL_CLAW && clawFirst >= 0) br.ring(c, clawFirst, 0xFF7FDBFF, time, false);
        drawSidePanel(c, after);
        drawButtons(c);
        for (int k = 0; k < popups.size(); k++) {
            float[] pu = popups.get(k);
            float a = pu[2];
            int alpha = (int) (255 * Math.max(0, Math.min(1, (1.8f - a) * 2)));
            float y = pu[1] - a * 40;
            float w = Ui.width(popupText.get(k), 30) + 40;
            Draw.rrect(c, pu[0], y - 40, pu[0] + w, y + 12, 24, Pal.alpha(Pal.GOLD, alpha), 0);
            Ui.label(c, popupText.get(k), pu[0] + 20, y - 4, 30, Pal.alpha(Pal.INK, alpha), Paint.Align.LEFT);
        }
        if (together) drawTurnBanner(c);
        if (toolMode >= 0) Ui.center(c, toolMode == Kinds.TOOL_CLAW ? "Claw: tap two neighbours to swap" : toolMode == Kinds.TOOL_WRENCH ? "Wrench: tap a tile to smash it" : "Broom: tap a row to sweep it", br.x0 + br.tile * L.cols / 2, vh - 10, 28, 0xFFFFFFFF);
    }

    private Step.Snapshot swappedCopy(Step.Snapshot s, int a, int b) {
        Board tmp = board.copy();
        System.arraycopy(s.entity, 0, tmp.entity, 0, s.entity.length);
        System.arraycopy(s.color, 0, tmp.color, 0, s.color.length);
        System.arraycopy(s.power, 0, tmp.power, 0, s.power.length);
        System.arraycopy(s.uid, 0, tmp.uid, 0, s.uid.length);
        System.arraycopy(s.clamp, 0, tmp.clamp, 0, s.clamp.length);
        System.arraycopy(s.layers, 0, tmp.layers, 0, s.layers.length);
        System.arraycopy(s.floor, 0, tmp.floor, 0, s.floor.length);
        int u = tmp.uid[a]; tmp.uid[a] = tmp.uid[b]; tmp.uid[b] = u;
        byte t = tmp.entity[a]; tmp.entity[a] = tmp.entity[b]; tmp.entity[b] = t;
        t = tmp.color[a]; tmp.color[a] = tmp.color[b]; tmp.color[b] = t;
        t = tmp.power[a]; tmp.power[a] = tmp.power[b]; tmp.power[b] = t;
        return new Step.Snapshot(tmp);
    }

    private void drawEffect(Canvas c, Step s, float p) {
        float e = BoardRenderer.ease(p);
        int alpha = (int) (220 * (1 - p));
        switch (s.type) {
            case POWERUP_FIRE:
                if (s.kind == Kinds.NITRO_H || s.kind == Kinds.NITRO_V) {
                    float y = br.cy(s.a), x = br.cx(s.a);
                    if (s.kind == Kinds.NITRO_H) c.drawRect(br.x0, y - br.tile * 0.3f, br.x0 + br.tile * L.cols, y + br.tile * 0.3f, Draw.fill(Pal.alpha(0xFF7FDBFF, alpha)));
                    else c.drawRect(x - br.tile * 0.3f, br.y0, x + br.tile * 0.3f, br.y0 + br.tile * L.rows, Draw.fill(Pal.alpha(0xFF7FDBFF, alpha)));
                } else if (s.kind == Kinds.AIRBAG) {
                    c.drawCircle(br.cx(s.a), br.cy(s.a), br.tile * (0.5f + 2.2f * e), Draw.line(Pal.alpha(Pal.GOLD, alpha), 14));
                } else if (s.kind == Kinds.SPRAYER) {
                    for (int cell : s.cells) c.drawLine(br.cx(s.a), br.cy(s.a), br.cx(s.a) + (br.cx(cell) - br.cx(s.a)) * e, br.cy(s.a) + (br.cy(cell) - br.cy(s.a)) * e, Draw.line(Pal.alpha(Pal.PIECE[cell % 6], alpha), 6));
                } else {
                    c.drawCircle(br.cx(s.a), br.cy(s.a), br.tile * (0.4f + 0.8f * e), Draw.line(Pal.alpha(0xFFFF5A36, alpha), 10));
                }
                break;
            case COMBO_DETONATE:
                for (int cell : s.cells) c.drawRect(br.cx(cell) - br.tile / 2, br.cy(cell) - br.tile / 2, br.cx(cell) + br.tile / 2, br.cy(cell) + br.tile / 2, Draw.fill(Pal.alpha(0xFFFFF4A8, (int) (180 * (1 - p)))));
                break;
            case BUGGY_FLIGHT:
                if (s.b >= 0) {
                    float x = br.cx(s.a) + (br.cx(s.b) - br.cx(s.a)) * e, y = br.cy(s.a) + (br.cy(s.b) - br.cy(s.a)) * e - (float) Math.sin(Math.PI * e) * br.tile * 1.4f;
                    RectF r = new RectF(x - br.tile * 0.45f, y - br.tile * 0.45f, x + br.tile * 0.45f, y + br.tile * 0.45f);
                    c.drawBitmap(g.pieces.power(Kinds.BUGGY), null, r, null);
                }
                break;
            case MATCH_POP:
                for (int cell : s.cells)
                    for (int k = 0; k < 3; k++) {
                        double a = k * 2.1 + cell;
                        Draw.stud(c, br.cx(cell) + (float) Math.cos(a) * br.tile * 0.6f * e, br.cy(cell) + (float) Math.sin(a) * br.tile * 0.6f * e - br.tile * 0.3f * e, br.tile * 0.07f * (1 - p) + 0.5f, Pal.PIECE[(cell + k) % 6]);
                    }
                break;
            case PART_COLLECT:
                for (int cell : s.cells) Draw.star(c, br.cx(cell), br.cy(cell) - br.tile * e, br.tile * 0.4f * (1 + e), Pal.alpha(Pal.GOLD, alpha + 30), 3);
                break;
            case VICTORY_LAP:
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6 + time;
                    Draw.star(c, br.x0 + br.tile * L.cols / 2 + (float) Math.cos(a) * 300 * e, br.y0 + br.tile * L.rows / 2 + (float) Math.sin(a) * 300 * e, 22, Pal.PIECE[k % 6], 3);
                }
                break;
            default:
        }
    }

    private void drawRefillQueue(Canvas c) {
        // The visible queue is what makes Blueprints a planning puzzle: you can see what will fall next.
        for (int col = 0; col < L.cols; col++) {
            String q = L.refills[col];
            int pos = board.refillPos[col];
            for (int k = 0; k < 3 && pos + k < q.length(); k++) {
                int color = Kinds.COLOR_LETTERS.indexOf(q.charAt(pos + k));
                if (color < 0) continue;
                float sz = br.tile * 0.36f;
                float x = br.x0 + col * br.tile + br.tile / 2 - sz / 2, y = br.y0 - 12 - (k + 1) * (sz + 4);
                c.drawBitmap(g.pieces.piece(color), null, new RectF(x, y, x + sz, y + sz), null);
            }
        }
    }

    private void drawSidePanel(Canvas c, Step.Snapshot s) {
        float px = vw - 480, pw = 460;
        Ui.panel(c, px, 26, px + pw, 470, Pal.PANEL);
        float ts = 36;
        while (Ui.width(L.title, ts) > 300 && ts > 20) ts -= 2;
        Ui.label(c, L.title, px + 24, 76, ts, Pal.INK, Paint.Align.LEFT);
        String sub = L.isBlueprint() ? "Blueprint: win in exactly " + L.exactMoves : L.kind == Level.Kind.TRACK_DAY ? "Track Day" : "Bay " + L.bay + (L.difficulty == Level.Difficulty.CHALLENGE ? "  •  Builder Challenge" : "");
        Ui.label(c, sub, px + 24, 112, 24, 0xFF8A6A40, Paint.Align.LEFT);
        // moves
        Draw.rrect(c, px + 24, 130, px + 180, 250, 20, s.movesLeft <= 3 ? 0xFFFFD6CF : 0xFFFFFFFF, 4);
        Ui.center(c, String.valueOf(s.movesLeft), px + 102, 214, 72, s.movesLeft <= 3 ? Pal.RED_BAD : Pal.INK);
        Ui.center(c, "moves", px + 102, 242, 22, 0xFF8A8072);
        // goals
        float gy = 140;
        for (int k = 0; k < L.goals.size(); k++) {
            GoalSpec goal = L.goals.get(k);
            float gx = px + 200 + (k % 2) * 125, yy = gy + (k / 2) * 110;
            Ui2.goalIcon(c, g.pieces, goal, gx, yy, 64);
            int rem = s.remaining[k];
            if (rem <= 0) Ui2.checkmark(c, gx + 72, yy + 18, 36);
            else Ui.label(c, String.valueOf(rem), gx + 70, yy + 48, 34, Pal.INK, Paint.Align.LEFT);
        }
        // closest-yet bar
        float prog = (float) progressOf(s);
        Ui.label(c, "Goal progress", px + 24, 300, 24, 0xFF8A8072, Paint.Align.LEFT);
        Ui.bar(c, px + 24, 314, px + pw - 24, 350, prog, Pal.GREEN_OK, (float) bestBefore);
        if (bestBefore > 0) Ui.label(c, "★ = your best so far", px + 24, 384, 22, Pal.GOLD_DARK, Paint.Align.LEFT);
        if (L.isBlueprint()) Ui.wrap(c, "The pieces above each column fall in next, in that order.", px + 24, 412, pw - 48, 22, 0xFF2E4F7A, Paint.Align.LEFT);
        else if (!L.tutorial.isEmpty() && g.progress.attempts(L.id) < 2) Ui.wrap(c, L.tutorial, px + 24, 412, pw - 48, 22, 0xFF5A4A36, Paint.Align.LEFT);
    }

    private double progressOf(Step.Snapshot s) {
        double done = 0, total = 0;
        for (int k = 0; k < s.remaining.length; k++) {
            int init = Math.max(1, board.initialRemaining[k]);
            total += init;
            done += Math.max(0, init - Math.min(s.remaining[k], init));
        }
        return total == 0 ? 1 : done / total;
    }

    private void drawTurnBanner(Canvas c) {
        String who = turnPlayer == 0 ? g.profile.playerName : g.profile.partnerName;
        int col = turnPlayer == 0 ? 0xFFFF8A1F : 0xFF2A9D8F;
        // coloured border around the board shows whose turn it is
        float l = br.x0 - 14, t = br.y0 - 14, r = br.x0 + br.tile * L.cols + 14, b = br.y0 + br.tile * L.rows + 14;
        c.drawRect(l, t, r, b, Draw.line(col, 10));
        // The banner sits in the right-hand column so it never covers the board or a refill queue.
        float slide = Math.min(1, bannerT * 3);
        String msg = "Your turn, " + who + "!";
        float px = vw - 480, w = 460;
        float by = 486 + (1 - slide) * 30;
        Draw.rrect(c, px, by, px + w, by + 60, 30, col, 4);
        Ui.center(c, msg, px + w / 2, by + 42, 34, 0xFFFFFFFF);
    }

    // =================================================================== little icons

    static void arrowLeft(Canvas c, float x, float y, float s) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(x + s * 0.65f, y + s * 0.15f);
        p.lineTo(x + s * 0.25f, y + s * 0.5f);
        p.lineTo(x + s * 0.65f, y + s * 0.85f);
        c.drawPath(p, Draw.line(Pal.INK, s * 0.14f));
    }

    static void bulb(Canvas c, float x, float y, float s) {
        Draw.circle(c, x + s / 2, y + s * 0.42f, s * 0.3f, 0xFFFFF4A8, 3);
        Draw.rrect(c, x + s * 0.36f, y + s * 0.7f, x + s * 0.64f, y + s * 0.88f, 3, Pal.STEEL, 3);
    }

    static Button.Icon toolIcon(final int tool) {
        return new Button.Icon() {
            @Override public void draw(Canvas c, float x, float y, float s) {
                if (tool == Kinds.TOOL_WRENCH) {
                    c.save();
                    c.rotate(-45, x + s / 2, y + s / 2);
                    Draw.rrect(c, x + s * 0.42f, y + s * 0.3f, x + s * 0.58f, y + s * 0.95f, 4, Pal.STEEL, 3);
                    Draw.circle(c, x + s / 2, y + s * 0.25f, s * 0.2f, Pal.STEEL, 3);
                    c.restore();
                } else if (tool == Kinds.TOOL_CLAW) {
                    Draw.rrect(c, x + s * 0.2f, y + s * 0.1f, x + s * 0.8f, y + s * 0.35f, 6, Pal.RED_BAD, 3);
                    c.drawArc(new RectF(x + s * 0.2f, y + s * 0.3f, x + s * 0.8f, y + s * 0.9f), 200, 140, false, Draw.line(Pal.STEEL_DARK, s * 0.1f));
                } else {
                    Draw.rrect(c, x + s * 0.45f, y + s * 0.05f, x + s * 0.55f, y + s * 0.6f, 3, Pal.WOOD, 3);
                    Draw.rrect(c, x + s * 0.2f, y + s * 0.6f, x + s * 0.8f, y + s * 0.9f, 6, 0xFFE0B13A, 3);
                }
            }
        };
    }
}
