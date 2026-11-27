package com.brickyard.app.screens;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.InputType;
import android.widget.EditText;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.profile.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Parent Settings behind a two-step gate: a multiplication question, then a 4-digit parent PIN
 * (set on the first visit). A child who knows the times tables still needs the PIN.
 */
public final class SettingsScreen extends Screen {
    private enum Stage { MATH, PIN_SET, PIN_CONFIRM, PIN, MENU }

    private Stage stage = Stage.MATH;
    private final int a, b;
    private String entry = "", firstPin = "", message = "";
    private final List<Button> keys = new ArrayList<>();
    private final List<Button> menu = new ArrayList<>();
    private final Button close;
    private Button sound, music, player, partner, closing, reopen, export, importB, debug, changePin;
    private static final String[] PARTNERS = {"Thamma", "Dadu", "Baba", "Ma", "Didun", "Dida", "Grandma", "Grandpa"};
    private static final String[] CLOSING = {"OFF", "MIN20", "MIN30", "LEVELS5"};
    private static final String[] CLOSING_NAMES = {"Off", "After 20 minutes", "After 30 minutes", "After 5 levels"};

    public SettingsScreen(final Game g) {
        super(g);
        Random r = new Random();
        a = 6 + r.nextInt(4);
        b = 12 + r.nextInt(8);
        for (int k = 0; k < 12; k++) {
            final String label = k < 9 ? String.valueOf(k + 1) : k == 9 ? "Clear" : k == 10 ? "0" : "OK";
            Button kb = new Button(label, k == 11 ? Pal.GREEN_OK : 0xFFE8C89A, new Runnable() { @Override public void run() { key(label); } });
            kb.textSize = k == 9 ? 26 : 38;
            keys.add(kb);
            buttons.add(kb);
        }
        close = new Button("Close", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        buttons.add(close);
        buildMenu();
    }

    private Button m(String label, Runnable r) {
        Button bt = new Button(label, 0xFFFFFFFF, r);
        bt.textSize = 26;
        bt.visible = false;
        menu.add(bt);
        buttons.add(bt);
        return bt;
    }

    private void buildMenu() {
        final Profile p = g.profile;
        sound = m("", new Runnable() { @Override public void run() { g.audio.soundOn = !g.audio.soundOn; g.save(); } });
        music = m("", new Runnable() { @Override public void run() { g.audio.setMusicOn(!g.audio.musicOn); g.save(); } });
        player = m("", new Runnable() { @Override public void run() { typeName(true); } });
        partner = m("", new Runnable() {
            @Override public void run() {
                int i = 0;
                for (int k = 0; k < PARTNERS.length; k++) if (PARTNERS[k].equals(p.partnerName)) i = k + 1;
                if (i >= PARTNERS.length) typeName(false);
                else { p.partnerName = PARTNERS[i]; g.save(); }
            }
        });
        closing = m("", new Runnable() {
            @Override public void run() {
                int i = 0;
                for (int k = 0; k < CLOSING.length; k++) if (CLOSING[k].equals(p.closingTime)) i = (k + 1) % CLOSING.length;
                p.closingTime = CLOSING[i];
                g.save();
            }
        });
        reopen = m("Reopen the shop today", new Runnable() { @Override public void run() { g.reopenShop(); message = "Shop reopened."; } });
        export = m("Export backup", new Runnable() {
            @Override public void run() { String path = g.repo.exportBackup(p); message = path == null ? "Export failed." : "Saved to " + path; }
        });
        importB = m("Import backup", new Runnable() { @Override public void run() { confirmImport(); } });
        debug = m("", new Runnable() { @Override public void run() { p.debugUnlock = !p.debugUnlock; g.save(); } });
        changePin = m("Change PIN", new Runnable() { @Override public void run() { stage = Stage.PIN_SET; entry = ""; message = ""; } });
    }

    private void typeName(final boolean isPlayer) {
        g.activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                final EditText et = new EditText(g.activity);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                et.setText(isPlayer ? g.profile.playerName : g.profile.partnerName);
                new AlertDialog.Builder(g.activity).setTitle(isPlayer ? "Player name" : "Together-mode partner")
                        .setView(et)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                String s = et.getText().toString().trim();
                                if (s.length() > 16) s = s.substring(0, 16);
                                if (s.isEmpty()) return;
                                if (isPlayer) g.profile.playerName = s; else g.profile.partnerName = s;
                                g.save();
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }
        });
    }

    private void confirmImport() {
        g.activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                new AlertDialog.Builder(g.activity).setTitle("Replace progress?")
                        .setMessage("This replaces the current progress with the backup file at:\n" + g.repo.backupPath())
                        .setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                Profile in = g.repo.importBackup();
                                if (in == null) { message = "No readable backup found."; return; }
                                in.pin = g.profile.pin;
                                g.profile = in;
                                g.progress = new com.brickyard.engine.profile.Progress(in, g.content.campaign, g.content.garage);
                                g.save();
                                message = "Backup restored.";
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
            }
        });
    }

    private void key(String k) {
        if (k.equals("Clear")) { entry = ""; return; }
        if (k.equals("OK")) { submit(); return; }
        if (entry.length() < 6) entry += k;
    }

    private void submit() {
        switch (stage) {
            case MATH:
                if (entry.equals(String.valueOf(a * b))) {
                    stage = g.profile.pin.isEmpty() ? Stage.PIN_SET : Stage.PIN;
                    message = "";
                } else message = "Not quite. Grown-ups only, please!";
                break;
            case PIN_SET:
                if (entry.length() != 4) { message = "Use exactly 4 digits."; break; }
                firstPin = entry;
                stage = Stage.PIN_CONFIRM;
                message = "";
                break;
            case PIN_CONFIRM:
                if (entry.equals(firstPin)) { g.profile.pin = entry; g.save(); stage = Stage.MENU; message = "PIN saved."; }
                else { stage = Stage.PIN_SET; message = "The PINs didn't match. Try again."; }
                break;
            case PIN:
                if (entry.equals(g.profile.pin)) { stage = Stage.MENU; message = ""; }
                else message = "Wrong PIN.";
                break;
            default:
        }
        entry = "";
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float kx = vw / 2 - 210, ky = 300;
        for (int k = 0; k < 12; k++) keys.get(k).at(kx + (k % 3) * 145, ky + (k / 3) * 110, 130, 96);
        close.at(24, 20, 180, 80);
        for (int k = 0; k < menu.size(); k++) menu.get(k).at(vw / 2 - 560 + (k % 2) * 570, 130 + (k / 2) * 118, 550, 100);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean inMenu = stage == Stage.MENU;
        for (Button k : keys) k.visible = !inMenu;
        for (Button k : menu) k.visible = inMenu;
        Profile p = g.profile;
        sound.label = "Sound effects: " + (g.audio.soundOn ? "On" : "Off");
        music.label = "Music: " + (g.audio.musicOn ? "On" : "Off");
        player.label = "Player name: " + p.playerName;
        partner.label = "Together partner: " + p.partnerName;
        int ci = 0;
        for (int k = 0; k < CLOSING.length; k++) if (CLOSING[k].equals(p.closingTime)) ci = k;
        closing.label = "Shop Closing Time: " + CLOSING_NAMES[ci];
        reopen.enabled = g.shopClosed();
        debug.label = "Unlock everything (testing): " + (p.debugUnlock ? "On" : "Off");
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, 0xFF3A4A5A, 0xFF22303C, 0);
        Ui.label(c, "Parent Settings", 230, 76, 46, 0xFFFFFFFF, Paint.Align.LEFT);
        if (stage != Stage.MENU) {
            String q = stage == Stage.MATH ? "What is " + a + " × " + b + "?" : stage == Stage.PIN_SET ? "Choose a 4-digit parent PIN" : stage == Stage.PIN_CONFIRM ? "Type the PIN again" : "Parent PIN";
            Ui.center(c, q, vw / 2, 170, 40, 0xFFFFFFFF);
            String shown = stage == Stage.MATH ? entry : entry.replaceAll(".", "● ");
            Draw.rrect(c, vw / 2 - 210, 200, vw / 2 + 210, 270, 16, 0xFFFFFFFF, 4);
            Ui.center(c, shown, vw / 2, 252, 40, Pal.INK);
        }
        if (!message.isEmpty()) Ui.center(c, message, vw / 2, vh - 30, 26, 0xFFFFE066);
        drawButtons(c);
    }
}
