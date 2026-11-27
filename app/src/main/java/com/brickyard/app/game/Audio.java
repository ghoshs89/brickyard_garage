package com.brickyard.app.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

/** Sound effects through SoundPool, one looping music track through MediaPlayer. Separate toggles. */
public final class Audio {
    private final Context ctx;
    private final SoundPool pool;
    private final Map<String, Integer> ids = new HashMap<>();
    private MediaPlayer music;
    private String track;
    public boolean soundOn = true, musicOn = true;
    private boolean paused;

    private static final String[] SFX = {"snap_0", "snap_1", "snap_2", "snap_3", "snap_4", "swap", "bounce", "ratchet", "hiss",
            "whirr", "thump", "nitro", "spray", "mesh", "rev", "coin", "star", "win", "retry", "crate", "oil", "part", "tap", "unclamp", "jam", "horn"};

    public Audio(Context ctx) {
        this.ctx = ctx;
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build();
        for (String s : SFX) {
            AssetFileDescriptor fd = null;
            try {
                fd = ctx.getAssets().openFd("audio/" + s + ".wav");
                ids.put(s, pool.load(fd, 1));
            } catch (Exception ignored) {
                // a missing sound is never fatal
            } finally {
                if (fd != null) try { fd.close(); } catch (Exception ignored) {}
            }
        }
    }

    public void play(String name) {
        play(name, 1f, 1f);
    }

    public void play(String name, float volume, float rate) {
        if (!soundOn || paused) return;
        Integer id = ids.get(name);
        if (id != null) pool.play(id, volume, volume, 1, 0, rate);
    }

    /** "workshop", "focus" or "show". */
    public void music(String name) {
        if (name.equals(track) && music != null) {
            if (musicOn && !paused && !music.isPlaying()) music.start();
            return;
        }
        stopMusic();
        track = name;
        if (!musicOn || paused) return;
        try {
            AssetFileDescriptor fd = ctx.getAssets().openFd("audio/music_" + name + ".wav");
            music = new MediaPlayer();
            music.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            music.setLooping(true);
            music.setVolume(0.45f, 0.45f);
            music.prepare();
            music.start();
        } catch (Exception e) {
            music = null;
        }
    }

    public void setMusicOn(boolean on) {
        musicOn = on;
        if (!on) stopMusic();
        else if (track != null) { String t = track; track = null; music(t); }
    }

    private void stopMusic() {
        if (music != null) {
            try { music.stop(); } catch (Exception ignored) {}
            music.release();
            music = null;
        }
    }

    public void pause() {
        paused = true;
        if (music != null && music.isPlaying()) music.pause();
    }

    public void resume() {
        paused = false;
        if (track != null) { String t = track; if (music == null) { track = null; music(t); } else if (musicOn) music.start(); }
    }

    public void release() {
        stopMusic();
        pool.release();
    }
}
