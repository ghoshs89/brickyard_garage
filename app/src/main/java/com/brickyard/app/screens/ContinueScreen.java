package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.GameView;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Level;

/** A saved board is waiting: carry on, or start the level fresh. */
public final class ContinueScreen extends Screen implements GameView.Overlay {
    private final Level L;
    private final Button cont, fresh, back;

    public ContinueScreen(final Game g, final Level L) {
        super(g);
        this.L = L;
        cont = new Button("Continue", Pal.GREEN_OK, new Runnable() { @Override public void run() { g.pop(); g.resumeSaved(); } });
        fresh = new Button("Start fresh", 0xFFFF8A1F, new Runnable() {
            @Override public void run() { g.profile.inProgress = null; g.save(); g.pop(); g.push(new BriefingScreen(g, L)); }
        });
        back = new Button("Back", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        buttons.add(cont);
        buttons.add(fresh);
        buttons.add(back);
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        cont.at(vw / 2 + 20, vh / 2 + 20, 300, 110);
        fresh.at(vw / 2 - 320, vh / 2 + 20, 300, 110);
        back.at(vw / 2 - 110, vh / 2 + 160, 220, 84);
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0x88000000));
        Ui.panel(c, vw / 2 - 420, vh / 2 - 220, vw / 2 + 420, vh / 2 + 270, Pal.PANEL);
        Ui.center(c, L.title, vw / 2, vh / 2 - 130, 48, Pal.INK);
        Ui.center(c, "Your board is saved right where you left it.", vw / 2, vh / 2 - 60, 30, 0xFF5A4A36);
        drawButtons(c);
    }
}
