package com.brickyard.app;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Screen;

/**
 * The single view. Lays out a virtual canvas 800 units tall (width follows the screen's shape,
 * at least 1280), so the same screens fit a Fire 7 (1024x600) and a Fire HD 10 (1920x1200).
 */
public final class GameView extends View {
    private final Game game;
    private long last = System.nanoTime();
    private float scale = 1, ox, oy, vw = 1280, vh = 800;
    private float clock;

    public GameView(Context ctx, Game game) {
        super(ctx);
        this.game = game;
        setFocusable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        float s = h / 800f;
        if (w / s < 1280) s = w / 1280f;
        scale = s;
        vw = w / s;
        vh = h / s;
        ox = 0;
        oy = 0;
        game.pieces.setTilePixels(0); // tile size is set by the board when it lays out
        game.resize(vw, vh, scale);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        clock += dt;
        Screen top = game.top();
        if (top != null && !game.talking()) top.update(dt);
        else if (top != null) top.update(0); // keep layouts valid while a conversation is open
        game.updateOverlay(dt);
        if (game.autopilot != null) game.autopilot.tick(dt);

        c.drawColor(Pal.WALL_DARK);
        c.save();
        c.translate(ox, oy);
        c.scale(scale, scale);
        if (top != null) {
            Screen under = game.below(top);
            if (under != null && top instanceof Overlay) under.draw(c);
            top.draw(c);
        }
        game.drawOverlay(c, clock);
        c.restore();
        if (top == null || top.animating() || game.talking()) postInvalidateOnAnimation();
    }

    /** Marker for screens drawn on top of the previous one (results, ledgers, pickers). */
    public interface Overlay {}

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = (e.getX() - ox) / scale, y = (e.getY() - oy) / scale;
        Screen top = game.top();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!game.overlayDown(x, y) && top != null) top.onDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!game.talking() && top != null) top.onMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (!game.talking() && top != null) top.onUp(x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (top != null) top.onUp(-9999, -9999);
                break;
            default:
        }
        postInvalidateOnAnimation();
        return true;
    }
}
