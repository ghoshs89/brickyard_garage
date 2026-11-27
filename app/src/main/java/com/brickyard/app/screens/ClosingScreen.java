package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Path;

import com.brickyard.app.GameView;
import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;

/** Shop Closing Time: Bo hangs up the keys after the level or repair finishes, never mid-level. */
public final class ClosingScreen extends Screen implements GameView.Overlay {
    private final Button ok;

    public ClosingScreen(final Game g) {
        super(g);
        ok = new Button("Goodnight!", 0xFF2A9D8F, new Runnable() { @Override public void run() { g.pop(); } });
        buttons.add(ok);
        g.audio.music("focus");
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        ok.at(vw / 2 - 150, vh - 160, 300, 104);
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0xE01B1B2F));
        for (int k = 0; k < 30; k++) Draw.circle(c, (k * 137) % vw, (k * 71) % (vh / 2), 2, 0xFFFFFFFF, 0);
        CharacterArt.draw(c, CharacterArt.BO, vw / 2 - 460, vh / 2 - 260, 380, 0, time);
        // a hook with the shop keys, swinging gently
        float sw = (float) Math.sin(time * 1.5) * 8;
        c.save();
        c.rotate(sw, vw / 2 + 280, 120);
        c.drawLine(vw / 2 + 280, 120, vw / 2 + 280, 220, Draw.line(Pal.STEEL, 5));
        Draw.circle(c, vw / 2 + 280, 240, 22, Pal.GOLD, 4);
        Path key = new Path();
        key.addRect(vw / 2 + 274, 258, vw / 2 + 286, 330, Path.Direction.CW);
        Draw.path(c, key, Pal.GOLD, 4);
        Draw.rrect(c, vw / 2 + 286, 300, vw / 2 + 302, 310, 2, Pal.GOLD, 3);
        c.restore();
        Story.Line l = Story.closing();
        Ui.wrap(c, l.text, vw / 2 - 40, vh / 2 + 20, vw / 2 - 40, 34, 0xFFFFFFFF, android.graphics.Paint.Align.LEFT);
        drawButtons(c);
    }
}
