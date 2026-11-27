package com.brickyard.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Screen;

/** Single activity: full-screen landscape, one custom view, no network, no accounts. */
public final class MainActivity extends Activity {
    private Game game;
    private GameView view;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            game = new Game(this);
        } catch (Exception e) {
            TextView t = new TextView(this);
            t.setText("Brickyard Garage could not load its content: " + e.getMessage());
            setContentView(t);
            return;
        }
        // Test hook for scripted emulator runs (adb am start --ez autopilot true). A child cannot set intent extras.
        if (getIntent() != null && getIntent().getBooleanExtra("autopilot", false)) {
            game.autopilot = new com.brickyard.app.game.Autopilot(game, getIntent().getIntExtra("autopilotLevels", 999));
            game.speed = getIntent().getFloatExtra("autopilotSpeed", 4f);
        }
        view = new GameView(this, game);
        setContentView(view);
        immersive();
    }

    private void immersive() {
        if (view == null) return;
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) immersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (game != null) game.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null) game.onResume();
        if (view != null) view.postInvalidateOnAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (game != null) {
            game.save();
            game.audio.release();
        }
    }

    @Override
    public void onBackPressed() {
        if (game == null) { super.onBackPressed(); return; }
        if (game.talking()) return;
        Screen top = game.top();
        if (top != null && top.onBack()) return;
        if (top != null && game.below(top) != null) { game.pop(); return; }
        super.onBackPressed();
    }
}
