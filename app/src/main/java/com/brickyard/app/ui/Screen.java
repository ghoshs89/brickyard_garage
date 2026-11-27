package com.brickyard.app.ui;

import android.graphics.Canvas;

import com.brickyard.app.game.Game;

import java.util.ArrayList;
import java.util.List;

/**
 * One screen of the game. Screens lay themselves out against the current virtual size
 * (vw x vh, height always 800 or more) so they fit 1024x600 and 1920x1200 alike.
 */
public abstract class Screen {
    protected final Game g;
    protected final List<Button> buttons = new ArrayList<>();
    protected float vw = 1280, vh = 800;
    protected float time;
    private Button down;

    protected Screen(Game g) {
        this.g = g;
    }

    /** Called when the screen becomes the top screen. */
    public void enter() {}

    /** Called whenever the virtual size changes, and before the first draw. */
    public void layout(float vw, float vh) {
        this.vw = vw;
        this.vh = vh;
    }

    public void update(float dt) {
        time += dt;
    }

    public abstract void draw(Canvas c);

    protected void drawButtons(Canvas c) {
        for (Button b : buttons) b.draw(c);
    }

    public boolean onDown(float x, float y) {
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button b = buttons.get(i);
            if (b.hit(x, y)) {
                down = b;
                b.pressed = true;
                return true;
            }
        }
        return false;
    }

    public void onMove(float x, float y) {
        if (down != null) down.pressed = down.hit(x, y);
    }

    public void onUp(float x, float y) {
        Button b = down;
        down = null;
        if (b == null) return;
        b.pressed = false;
        if (b.hit(x, y) && b.enabled && b.action != null) {
            g.audio.play("tap", 0.6f, 1f);
            b.action.run();
        }
    }

    /** Android back button. Return true if handled. */
    public boolean onBack() {
        return false;
    }

    /** Screens that keep the world moving (animations) return true so the view redraws every frame. */
    public boolean animating() {
        return true;
    }
}
